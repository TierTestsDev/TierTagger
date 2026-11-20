package com.tiertests.tiertagger.core.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

/**
 * Small blocking HTTP client shared by every request the mod makes.
 *
 * Connections are left to the JDK keep-alive pool (never disconnected), responses are requested gzipped,
 * and each host is throttled with a token bucket so a crowded server never trips an API's rate limit.
 * A 429 pauses all requests to that host for the time the server asks for.
 */
public final class Http {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final int MAX_BODY_BYTES = 4 * 1024 * 1024;

    private final String userAgent;
    private final ConcurrentHashMap<String, HostLimiter> limiters = new ConcurrentHashMap<String, HostLimiter>();

    public Http(String userAgent) {
        this.userAgent = userAgent;
    }

    /** Sets the request budget for a host: a burst of {@code burst} requests, refilled at {@code perSecond}. */
    public void limit(String host, int burst, double perSecond) {
        limiters.put(host.toLowerCase(Locale.ROOT), new HostLimiter(burst, perSecond));
    }

    public Response get(String url) throws IOException {
        URL target = new URL(url);
        HostLimiter limiter = limiter(target.getHost());
        limiter.acquire();

        HttpURLConnection connection = (HttpURLConnection) target.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Accept-Encoding", "gzip");

        int code = connection.getResponseCode();
        if (code == 429) {
            limiter.pause(retryAfterMillis(connection.getHeaderField("Retry-After")));
        }
        InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String body = read(stream, connection.getContentEncoding());
        if (code == 429) throw new RateLimitedException(target.getHost());
        return new Response(code, body);
    }

    private HostLimiter limiter(String host) {
        String key = host.toLowerCase(Locale.ROOT);
        HostLimiter limiter = limiters.get(key);
        if (limiter == null) {
            limiter = new HostLimiter(10, 5);
            HostLimiter existing = limiters.putIfAbsent(key, limiter);
            if (existing != null) limiter = existing;
        }
        return limiter;
    }

    private static long retryAfterMillis(String header) {
        if (header != null) {
            try {
                return Math.max(1, Math.min(300, Long.parseLong(header.trim()))) * 1000L;
            } catch (NumberFormatException ignored) {
            }
        }
        return 30_000L;
    }

    private static String read(InputStream stream, String encoding) throws IOException {
        if (stream == null) return "";
        InputStream in = "gzip".equalsIgnoreCase(encoding) ? new GZIPInputStream(stream) : stream;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                if (out.size() > MAX_BODY_BYTES) throw new IOException("Response too large");
            }
            return new String(out.toByteArray(), UTF_8);
        } finally {
            in.close();
        }
    }

    public static final class Response {
        private final int code;
        private final String body;

        Response(int code, String body) {
            this.code = code;
            this.body = body;
        }

        public int code() {
            return code;
        }

        public String body() {
            return body;
        }

        public boolean ok() {
            return code >= 200 && code < 300;
        }

        public boolean notFound() {
            return code == 404 || code == 422;
        }
    }

    public static final class RateLimitedException extends IOException {
        RateLimitedException(String host) {
            super("Rate limited by " + host);
        }
    }

    private static final class HostLimiter {
        private final double capacity;
        private final double refillPerMilli;
        private double tokens;
        private long lastRefill = System.currentTimeMillis();
        private long pausedUntil;

        HostLimiter(int burst, double perSecond) {
            this.capacity = burst;
            this.refillPerMilli = perSecond / 1000.0;
            this.tokens = burst;
        }

        void acquire() throws IOException {
            long wait;
            synchronized (this) {
                long now = System.currentTimeMillis();
                tokens = Math.min(capacity, tokens + (now - lastRefill) * refillPerMilli);
                lastRefill = now;
                tokens -= 1;
                wait = Math.max(pausedUntil - now, tokens >= 0 ? 0 : (long) Math.ceil(-tokens / refillPerMilli));
            }
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for rate limit");
                }
            }
        }

        synchronized void pause(long millis) {
            pausedUntil = Math.max(pausedUntil, System.currentTimeMillis() + millis);
        }
    }
}
