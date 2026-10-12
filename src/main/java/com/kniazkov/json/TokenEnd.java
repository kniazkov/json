/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

/** End of input, located immediately after the last consumed code unit. */
final class TokenEnd extends Token {
    /** Creates an EOF token with an immutable location snapshot. */
    TokenEnd(JsonLocation loc) {
        super(loc);
    }

    @Override
    public String toString() {
        return "<EOF>";
    }
}
