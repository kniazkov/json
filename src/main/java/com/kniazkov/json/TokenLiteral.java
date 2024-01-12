/*
 * Copyright (c) 2023 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * A token representing a literal, that is, some value.
 * A JSON element can be generated directly from such a token.
 */
abstract class TokenLiteral extends Token {
    /**
     * Constructor.
     * @param loc Location of the first character of the token
     */
    TokenLiteral(JsonLocation loc) {
        super(loc);
    }

    /**
     * Generates a JSON element from this literal.
     * @param parent The container that will contain the generated element
     * @return JSON element
     */
    abstract JsonElement toElement(JsonContainer parent);
}
