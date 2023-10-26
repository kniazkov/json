package com.kniazkov.json;

/**
 * Minimum indivisible entity of JSON document.
 */
abstract class Token {
    /**
     * Represents the token as a string.
     * @return String representation of the token
     */
    @Override
    public abstract String toString();
}
