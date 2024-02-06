/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * A token representing an opening curly bracket.
 */
final class TokenOpeningCurlyBracket extends Token {
    /**
     * Constructor.
     * @param loc Location of the first character of the token
     */
    TokenOpeningCurlyBracket(JsonLocation loc) {
        super(loc);
    }

    @Override
    public String toString() {
        return "{";
    }
}
