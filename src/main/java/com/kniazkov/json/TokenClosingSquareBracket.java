package com.kniazkov.json;

/**
 * A token representing a closing square bracket.
 */
class TokenClosingSquareBracket extends Token {
    /**
     * Constructor.
     * @param loc Location of the first character of the token
     */
    TokenClosingSquareBracket(JsonLocation loc) {
        super(loc);
    }

    @Override
    public String toString() {
        return "]";
    }
}