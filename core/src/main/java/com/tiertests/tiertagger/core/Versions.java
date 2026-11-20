package com.tiertests.tiertagger.core;

/** Compares dotted version numbers such as 1.3.2 and 2.0.0. Trailing text after a '+' or '-' is ignored. */
public final class Versions {
    private Versions() {
    }

    public static int compare(String a, String b) {
        int[] left = parts(a);
        int[] right = parts(b);
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int x = i < left.length ? left[i] : 0;
            int y = i < right.length ? right[i] : 0;
            if (x != y) return x < y ? -1 : 1;
        }
        return 0;
    }

    private static int[] parts(String version) {
        if (version == null) return new int[0];
        String core = version.trim().split("[+\\-\\s]", 2)[0];
        String[] pieces = core.split("\\.");
        int[] numbers = new int[pieces.length];
        for (int i = 0; i < pieces.length; i++) {
            String digits = pieces[i].replaceAll("\\D.*$", "");
            numbers[i] = digits.isEmpty() ? 0 : Integer.parseInt(digits);
        }
        return numbers;
    }
}
