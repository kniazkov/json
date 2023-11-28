package com.kniazkov.json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Some auxiliary procedures.
 */
final class Utils {
    /**
     * Converts a number to a string.
     * @param value A number
     * @return A string representing number
     */
    static String doubleToString(double value) {
        long intVal = (long)value;
        if (value == intVal) {
            return String.valueOf(intVal);
        }
        return String.valueOf(value);
    }

    /**
     * Escapes entities that are not allowed in a string into special sequences.
     * @param value A string
     * @return String with escaped entities
     */
    static String escapeEntities(String value) {
        int length = value.length();
        if (length > 0) {
            final StringBuilder result = new StringBuilder();
            for (int i = 0; i < length; i++) {
                char ch = value.charAt(i);
                switch (ch) {
                    case '"':
                        result.append("\\\"");
                        break;
                    case '\\':
                        result.append("\\\\");
                        break;
                    case '\b':
                        result.append("\\b");
                        break;
                    case '\f':
                        result.append("\\f");
                        break;
                    case '\n':
                        result.append("\\n");
                        break;
                    case '\r':
                        result.append("\\r");
                        break;
                    case '\t':
                        result.append("\\t");
                        break;
                    default:
                        if (ch < 0x20) {
                            result.append("\\u").append(String.format("%04x", (int)ch));
                        } else {
                            result.append(ch);
                        }
                }
            }
            return result.toString();
        }
        return "";
    }

    /**
     * Adds a repeating string to a StringBuilder.
     * @param builder Builder
     * @param string String
     * @param count Number of repetitions
     */
    static void addRepeatingString(StringBuilder builder, String string, int count) {
        for (int i = 0; i < count; i++) {
            builder.append(string);
        }
    }

    /**
     * Reads a file into a string. Does not throw exceptions.
     * @param file The file
     * @return The file contents or {@code null} if the file cannot be read for some reason
     */
    static String readFileToString(File file) {
        if (file.exists()) {
            try {
                byte[] data = Files.readAllBytes(file.toPath());
                return new String(data);
            } catch (IOException ignored) {
            }
        }
        return null;
    }
}
