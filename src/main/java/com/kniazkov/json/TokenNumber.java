package com.kniazkov.json;

/**
 * Token representing a number
 */
class TokenNumber extends Token {
    /**
     * Value of the number.
     */
    private final double value;

    /**
     * Constructor.
     * @param value Value of the number
     */
    TokenNumber(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Utils.doubleToString(value);
    }
}
