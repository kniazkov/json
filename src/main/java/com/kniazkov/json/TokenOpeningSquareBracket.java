/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * A token representing an opening square bracket.
 */
final class TokenOpeningSquareBracket extends Token {
    /**
     * Constructor.
     * @param loc Location of the first character of the token
     */
    TokenOpeningSquareBracket(JsonLocation loc) {
        super(loc);
    }

    @Override
    public String toString() {
        return "[";
    }
}
