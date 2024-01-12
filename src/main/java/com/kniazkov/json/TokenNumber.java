/*
 * Copyright (c) 2023 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * Token representing a number.
 */
final class TokenNumber extends TokenLiteral {
    /**
     * Value of the number.
     */
    private final double value;

    /**
     * Constructor.
     * @param loc Location of the first character of the token
     * @param value Value of the number
     */
    TokenNumber(JsonLocation loc, double value) {
        super(loc);
        this.value = value;
    }

    @Override
    public String toString() {
        return Utils.doubleToString(value);
    }

    @Override
    JsonElement toElement(JsonContainer parent) {
        return new JsonNumber(parent, value);
    }
}
