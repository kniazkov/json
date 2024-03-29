/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * JSON element representing a string.
 */
public final class JsonString extends JsonElement {
    /**
     * Value of the string.
     */
    private final String value;

    /**
     * Constructor.
     * @param value Value of the string
     */
    public JsonString(String value) {
        this(null, value);
    }

    /**
     * Constructor (for internal use).
     * @param parent The parent of this element
     * @param value Value of the string
     */
    JsonString(JsonContainer parent, String value) {
        super(parent);
        this.value = value;
    }

    @Override
    JsonElement clone(JsonContainer anotherParent) {
        return new JsonString(anotherParent, value);
    }

    @Override
    public String toString() {
        return "\"" + Utils.escapeEntities(value) + '"';
    }

    @Override
    public Object toJavaObject() {
        return value;
    }

    @Override
    public <T> T toJavaObject(Class<T> type) {
        if (type == java.lang.String.class) {
            return (T)value;
        }
        return null;
    }

    @Override
    public boolean isString() {
        return true;
    }

    @Override
    public String getStringValue() {
        return value;
    }
}
