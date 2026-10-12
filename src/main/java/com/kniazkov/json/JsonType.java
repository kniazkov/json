/* Copyright (c) 2026 Ivan Kniazkov */
package com.kniazkov.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Captures a generic conversion target, for example {@code new JsonType<Map<String, Integer>>()
 * {}}. Extend this class directly with a concrete type argument; unresolved variables are not
 * substituted.
 *
 * @param <T> Target Java type
 */
public abstract class JsonType<T> {
    private final Type type;

    /**
     * Captures the direct subclass's generic argument.
     *
     * @throws IllegalArgumentException If the subclass is raw or does not directly extend JsonType
     */
    protected JsonType() {
        Type parent = getClass().getGenericSuperclass();
        if (!(parent instanceof ParameterizedType parameterized)
                || parameterized.getRawType() != JsonType.class) {
            throw new IllegalArgumentException(
                    "Extend JsonType directly with a concrete type argument");
        }
        type = parameterized.getActualTypeArguments()[0];
    }

    /**
     * Returns the captured target type.
     *
     * @return Captured Java reflection type
     */
    public final Type type() {
        return type;
    }
}
