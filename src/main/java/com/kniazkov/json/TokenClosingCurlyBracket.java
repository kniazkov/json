/*
 * Copyright (c) 2023 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * A token representing a closing curly bracket.
 */
final class TokenClosingCurlyBracket extends Token {
    /**
     * Constructor.
     * @param loc Location of the first character of the token
     */
    TokenClosingCurlyBracket(JsonLocation loc) {
        super(loc);
    }

    @Override
    public String toString() {
        return "}";
    }
}
