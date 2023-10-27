package com.kniazkov.json;

/**
 * Splits JSON documents into separate tokens.
 */
final class Lexer {
    /**
     * Object containing JSON document for parsing.
     */
    private final Source source;

    /**
     * Constructor.
     * @param source Object containing JSON document for parsing
     */
    public Lexer(Source source) {
        this.source = source;
    }

    /**
     * Returns the next token from a sequence.
     * @return A token.
     */
    public Token getToken() throws JsonException {
        char ch = source.getChar();

        while(isWhiteSpace(ch)) {
            ch = source.nextChar();
        }

        JsonLocation loc = source.getLocation();

        if (isDigit(ch)) {
            return parseNumber(loc, ch, false);
        }

        if (ch == '-') {
            ch = source.nextChar();
            if (isDigit(ch)) {
                return parseNumber(loc, ch, true);
            }
            throw new JsonException(new JsonError.ExpectedNumberAfterMinus(source.getLocation()));
        }

        return null;
    }

    /**
     * Checks if the character is a space.
     * @param ch Character
     * @return Checking result
     */
    private static boolean isWhiteSpace(char ch) {
        return ch == ' ' || ch == '\r' || ch == '\n' || ch == '\t';
    }

    /**
     * Checks if the character is a digit.
     * @param ch Character
     * @return Checking result
     */
    private static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    /**
     * Parses the character sequence as a number.
     * @param loc Location of the first character of the token
     * @param firstDigit First digit
     * @param negative Is the number negative
     * @return A token representing a number
     */
    private TokenNumber parseNumber(JsonLocation loc, char firstDigit, boolean negative) {
        char ch = firstDigit;
        long intPart = 0;
        do {
            intPart = intPart * 10 + (ch - '0');
            ch = source.nextChar();
        } while(isDigit(ch));
        if (negative) {
            intPart = -intPart;
        }
        return new TokenNumber(loc, intPart);
    }
}
