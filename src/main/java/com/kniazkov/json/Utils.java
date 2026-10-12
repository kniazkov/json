/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Some auxiliary procedures.
 */
final class Utils {
    /** Whether an instance field participates in automatic serialization. */
    static boolean isReadableField(Field field) {
        int modifiers = field.getModifiers();
        return !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers) && !field.isSynthetic();
    }

    /** Final instance fields may be read, but must retain their constructor-provided values. */
    static boolean isWritableField(Field field) {
        return isReadableField(field) && !Modifier.isFinal(field.getModifiers());
    }

    /**
     * Resolves Java-name hiding before filtering, then maps readable fields to JSON names.
     * Both directions validate the same schema, including final fields.
     * @throws IllegalArgumentException if distinct visible fields have the same JSON name
     */
    static Map<String, Field> mappedFields(Class<?> type) {
        Map<String, Field> visible = new LinkedHashMap<>();
        collectFields(type, visible);
        Map<String, Field> result = new LinkedHashMap<>();
        for (Field field : visible.values()) {
            if (!isReadableField(field)) {
                continue;
            }
            JsonProperty annotation = field.getAnnotation(JsonProperty.class);
            String name = annotation == null || annotation.name().isEmpty() ? field.getName() : annotation.name();
            Field previous = result.putIfAbsent(name, field);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate JSON property '" + name + "': "
                        + previous.getDeclaringClass().getName() + "." + previous.getName()
                        + " and " + field.getDeclaringClass().getName() + "." + field.getName());
            }
        }
        return result;
    }

    private static void collectFields(Class<?> type, Map<String, Field> fields) {
        if (type == null || type == Object.class) {
            return;
        }
        collectFields(type.getSuperclass(), fields);
        for (Field field : type.getDeclaredFields()) {
            fields.put(field.getName(), field);
        }
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
                        if (ch < 0x20 || ch == 0x2028 || ch == 0x2029) {
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

}
