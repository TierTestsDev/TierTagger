package com.tiertests.tiertagger.core.text;

import com.tiertests.tiertagger.core.model.Colors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Platform-neutral styled text. Each platform turns it into its own chat component;
 * {@link #toLegacyString()} covers anything that only understands section-sign color codes.
 */
public final class Text {
    public static final Text EMPTY = new Text(Collections.<Span>emptyList());

    private final List<Span> spans;

    private Text(List<Span> spans) {
        this.spans = spans;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Span> spans() {
        return spans;
    }

    public boolean isEmpty() {
        return spans.isEmpty();
    }

    public String plain() {
        StringBuilder out = new StringBuilder();
        for (Span span : spans) out.append(span.text);
        return out.toString();
    }

    public String toLegacyString() {
        StringBuilder out = new StringBuilder();
        for (Span span : spans) {
            out.append('§').append(span.color >= 0 ? Colors.nearestLegacyCode(span.color) : 'f');
            out.append(span.text);
        }
        if (!spans.isEmpty()) out.append("§r");
        return out.toString();
    }

    /** A run of text in one color. Icon spans hold characters from the mod's icon font. */
    public static final class Span {
        private final String text;
        private final int color;
        private final boolean icon;

        Span(String text, int color, boolean icon) {
            this.text = text;
            this.color = color;
            this.icon = icon;
        }

        public String text() {
            return text;
        }

        /** RGB color, or -1 to inherit. */
        public int color() {
            return color;
        }

        public boolean icon() {
            return icon;
        }
    }

    public static final class Builder {
        private final List<Span> spans = new ArrayList<Span>();

        public Builder append(String text, int color) {
            if (text != null && !text.isEmpty()) spans.add(new Span(text, color, false));
            return this;
        }

        public Builder icon(char glyph) {
            spans.add(new Span(String.valueOf(glyph), -1, true));
            return this;
        }

        public Builder append(Text text) {
            if (text != null) spans.addAll(text.spans);
            return this;
        }

        public boolean isEmpty() {
            return spans.isEmpty();
        }

        public Text build() {
            return spans.isEmpty() ? EMPTY : new Text(Collections.unmodifiableList(new ArrayList<Span>(spans)));
        }
    }
}
