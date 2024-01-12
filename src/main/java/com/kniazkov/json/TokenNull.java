/*
 * Copyright (c) 2023 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * Token representing a {@code null} literal.
 */
final class TokenNull extends TokenLiteral {
    /**
     * Constructor.
     * @param loc Location of the first character of the token
     */
    TokenNull(JsonLocation loc) {
        super(loc);
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    JsonElement toElement(JsonContainer parent) {
        return new JsonNull(parent);
    }
}
