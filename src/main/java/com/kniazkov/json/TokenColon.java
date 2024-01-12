/*
 * Copyright (c) 2023 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * A token representing a colon.
 */
final class TokenColon extends Token {
    /**
     * Constructor.
     * @param loc Location of the first character of the token
     */
    TokenColon(JsonLocation loc) {
        super(loc);
    }

    @Override
    public String toString() {
        return ":";
    }
}
