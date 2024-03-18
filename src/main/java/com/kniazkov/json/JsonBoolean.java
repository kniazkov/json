/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * JSON element representing a boolean value.
 */
public final class JsonBoolean extends JsonElement {
    /**
     * The instance of {@code true} literal.
     */
    public static final JsonBoolean TRUE = new JsonBoolean(null, true);

    /**
     * The instance of {@code false} literal.
     */
    public static final JsonBoolean FALSE = new JsonBoolean(null, false);

    /**
     * Boolean value.
     */
    private final boolean value;

    /**
     * Returns an instance of Boolean element.
     * @param value Boolean value
     * @return An instance
     */
    public static JsonBoolean getInstance(boolean value) {
        return value ? TRUE : FALSE;
    }

    /**
     * Constructor (for internal use).
     * @param parent The parent of this element
     * @param value Boolean value
     */
    JsonBoolean(JsonContainer parent, boolean value) {
        super(parent);
        this.value = value;
    }

    @Override
    JsonElement clone(JsonContainer anotherParent) {
        return new JsonBoolean(anotherParent, value);
    }

    @Override
    public String toString() {
        return value ? "true" : "false";
    }

    @Override
    public Object toJavaObject() {
        return value;
    }

    @Override
    public <T> T toJavaObject(Class<T> type) {
        if (type == java.lang.Boolean.class) {
            Boolean obj = value;
            return (T)obj;
        }
        return null;
    }

    @Override
    public boolean isBoolean() {
        return true;
    }

    @Override
    public boolean getBooleanValue() {
        return value;
    }

    @Override
    public String getStringValue() {
        return value ? "true" : "false";
    }
}
