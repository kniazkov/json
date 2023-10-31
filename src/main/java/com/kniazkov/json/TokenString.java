package com.kniazkov.json;

/**
 * Token representing a string.
 */
final class TokenString extends TokenLiteral {
    /**
     * Value of the string.
     */
    private final String value;

    /**
     * Constructor.
     * @param loc Location of the first character of the token
     * @param value Value of the string
     */
    TokenString(JsonLocation loc, String value) {
        super(loc);
        this.value = value;
    }

    @Override
    public String toString() {
        return "\"" + Utils.escapeEntities(value) + '"';
    }

    @Override
    JsonElement toElement(JsonContainer parent) {
        return new JsonString(parent, value);
    }
}
