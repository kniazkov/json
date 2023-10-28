package com.kniazkov.json;

/**
 * A token representing a comma.
 */
final class TokenComma extends Token {
    /**
     * Constructor.
     * @param loc Location of the first character of the token
     */
    TokenComma(JsonLocation loc) {
        super(loc);
    }

    @Override
    public String toString() {
        return ",";
    }
}
