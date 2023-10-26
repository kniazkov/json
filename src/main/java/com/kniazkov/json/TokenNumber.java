package com.kniazkov.json;

/**
 * Token representing a number.
 */
class TokenNumber extends TokenLiteral {
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

    @Override
    JsonElement toElement(JsonContainer parent) {
        return new JsonNumber(parent, value);
    }
}
