package com.meridianenergy.etrm.web;

import java.util.List;
import java.util.Map;

public final class JsonUtil {

    private JsonUtil() {
    }

    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder output = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '"' || character == '\n'
                    || character == '\r') {
                output.append('\\');
            }
            output.append(character);
        }
        return output.append('"').toString();
    }

    public static String value(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return quote(String.valueOf(value));
    }

    public static String object(Map<String, ?> values) {
        StringBuilder output = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (!first) {
                output.append(',');
            }
            first = false;
            output.append(quote(entry.getKey()));
            output.append(':');
            output.append(value(entry.getValue()));
        }
        return output.append('}').toString();
    }

    public static String array(List<String> values) {
        StringBuilder output = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                output.append(',');
            }
            output.append(values.get(index));
        }
        return output.append(']').toString();
    }
}
