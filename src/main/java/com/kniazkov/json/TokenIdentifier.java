/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * Token representing an identifier.
 */
final class TokenIdentifier extends Token {
    /**
     * Identifier.
     */
    private final String identifier;

    /**
     * Constructor.
     * @param loc Location of the first character of the token
     * @param identifier Identifier
     */
    TokenIdentifier(JsonLocation loc, String identifier) {
        super(loc);
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        return null;
    }

    /**
     * Returns identifier.
     * @return An identifier
     */
    String getIdentifier() {
        return identifier;
    }
}
