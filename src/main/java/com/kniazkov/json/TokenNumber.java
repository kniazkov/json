/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * Token representing a number.
 */
final class TokenNumber extends TokenLiteral {
    /**
     * Value of the number.
     */
    private final JsonNumber value;

    /**
     * Constructor.
     * @param loc Location of the first character of the token
     * @param value Value of the number
     */
    TokenNumber(JsonLocation loc, JsonNumber value) {
        super(loc);
        this.value = value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    JsonElement toElement(JsonContainer parent) {
        return value.clone(parent);
    }
}
