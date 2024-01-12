/*
 * Copyright (c) 2023 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * Token representing a boolean value.
 */
final class TokenBoolean extends TokenLiteral {
    /**
     * Boolean value.
     */
    private final boolean value;

    /**
     * Constructor.
     * @param loc Location of the first character of the token
     * @param value Boolean value
     */
    TokenBoolean(JsonLocation loc, boolean value) {
        super(loc);
        this.value = value;
    }

    @Override
    public String toString() {
        return value ? "true" : "false";
    }

    @Override
    JsonElement toElement(JsonContainer parent) {
        return new JsonBoolean(parent, value);
    }
}
