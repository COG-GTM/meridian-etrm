package com.meridianenergy.etrm.util;

public final class StringUtils {

    private StringUtils() {
    }

    public static String padRight(String value, int length) {
        StringBuilder builder = new StringBuilder(value == null ? "" : value);
        while (builder.length() < length) {
            builder.append(' ');
        }
        return builder.length() > length
                ? builder.substring(0, length) : builder.toString();
    }

    public static String padLeft(String value, int length) {
        String text = value == null ? "" : value;
        StringBuilder builder = new StringBuilder();
        while (builder.length() + text.length() < length) {
            builder.append(' ');
        }
        builder.append(text);
        return builder.length() > length
                ? builder.substring(builder.length() - length) : builder.toString();
    }
}
