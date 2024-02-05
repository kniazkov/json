/*
 * Copyright (c) 2023 Ivan Kniazkov
 */
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
     * Parsing mode.
     */
    private final JsonParsingMode mode;

    /**
     * Constructor.
     * @param source Object containing JSON document for parsing
     */
    public Lexer(Source source, JsonParsingMode mode) {
        this.source = source;
        this.mode = mode;
    }

    /**
     * Returns the next token from a sequence.
     * @return A token.
     * @throws JsonException If parsing fails
     */
    public Token getToken() throws JsonException {
        char ch = source.getChar();

        while(isWhiteSpace(ch)) {
            ch = source.nextChar();
        }

        if (ch == 0) {
            return null;
        }

        JsonLocation loc = source.getLocation();

        if (isLetter(ch)) {
            StringBuilder builder = new StringBuilder();
            do {
                builder.append(ch);
                ch = source.nextChar();
            } while(isLetter(ch) || isDigit(ch));
            String identifier = builder.toString();
            if ("null".equals(identifier)) {
                return new TokenNull(loc);
            }
            if ("true".equals(identifier)) {
                return new TokenBoolean(loc, true);
            }
            if ("false".equals(identifier)) {
                return new TokenBoolean(loc, false);
            }
            return new TokenIdentifier(loc, identifier);
        }

        NumberParsing number = null;

        if (ch == '-') {
            ch = source.nextChar();
            number = new NumberParsing(loc, true);
        }
        else if (ch == '+') {
            if (mode == JsonParsingMode.STRICT) {
                throw new JsonException(new JsonError.InvalidCharacter(loc, '+'));
            }
            ch = source.nextChar();
            number = new NumberParsing(loc, false);
        }

        if (ch == '0') {
            ch = source.nextChar();
            if (ch == 'x') {
                if (mode == JsonParsingMode.STRICT) {
                    throw new JsonException(new JsonError.InvalidCharacter(loc, 'x'));
                }
                ch = source.nextChar();
                if (isHexDigit(ch)) {
                    return parseHexNumber(loc, ch, number != null && number.negative);
                }
                throw new JsonException(new JsonError.ExpectedHexDigit(source.getLocation()));
            } else if (isDigit(ch)) {
                if (number == null) {
                    number = new NumberParsing(loc, false);
                }
                return parseNumber(number, ch);
            } else {
                return new TokenNumber(loc, 0);
            }
        }

        if (isDigit(ch)) {
            if (number == null) {
                number = new NumberParsing(loc, false);
            }
           return parseNumber(number, ch);
        }

        if (number != null) {
            if (number.negative) {
                throw new JsonException(new JsonError.ExpectedNumberAfterMinus(source.getLocation()));
            } else {
                throw new JsonException(new JsonError.ExpectedNumberAfterPlus(source.getLocation()));
            }
        }

        if (ch == '"') {
            return parseString(loc, '"');
        }

        if (ch == '\'') {
            if (mode == JsonParsingMode.STRICT) {
                throw new JsonException(new JsonError.InvalidCharacter(loc, '\''));
            }
            return parseString(loc, '\'');
        }

        if (ch == '[') {
            source.nextChar();
            return new TokenOpeningSquareBracket(loc);
        }

        if (ch == ']') {
            source.nextChar();
            return new TokenClosingSquareBracket(loc);
        }

        if (ch == '{') {
            source.nextChar();
            return new TokenOpeningCurlyBracket(loc);
        }

        if (ch == '}') {
            source.nextChar();
            return new TokenClosingCurlyBracket(loc);
        }

        if (ch == ',') {
            source.nextChar();
            return new TokenComma(loc);
        }

        if (ch == ':') {
            source.nextChar();
            return new TokenColon(loc);
        }

        throw new JsonException(new JsonError.InvalidCharacter(loc, ch));
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
     * Checks if the character is a letter.
     * @param ch Character
     * @return Checking result
     */
    private static boolean isLetter(char ch) {
        return ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z' || ch == '_';
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
     * Checks if the character is a hexadecimal digit.
     * @param ch Character
     * @return Checking result
     */
    private static boolean isHexDigit(char ch) {
        return isDigit(ch) || ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F' ;
    }

    /**
     * Data needed for parsing numbers.
     */
    private class NumberParsing {
        /**
         * Location in the JSON document.
         */
        final JsonLocation loc;

        /**
         * Flag that determines that the number is negative.
         */
        final boolean negative;

        /**
         * Integer part of the number.
         */
        long intPart;

        /**
         * Fractional part of the number.
         */
        long fractPart;

        /**
         * Divisor by which the fractional part is to be divided.
         */
        long divisor;

        /**
         * Constructor.
         * @param loc Location in the JSON document
         * @param negative Flag that determines that the number is negative
         */
        NumberParsing(JsonLocation loc, boolean negative) {
            this.loc = loc;
            this.negative = negative;
            this.intPart = 0;
            this.fractPart = 0;
            this.divisor = 1;
        }

        /**
         * Generates a token from the collected data.
         * @return Token representing a number
         */
        TokenNumber createToken() {
            final double sign = negative ? -1 : 1;
            return new TokenNumber(loc, sign * (intPart + (double)fractPart / (double)divisor));
        }
    }

    /**
     * Parses the character sequence as a number.
     * @param data Data needed for parsing numbers
     * @param firstDigit First digit
     * @return A token representing a number
     */
    private TokenNumber parseNumber(NumberParsing data, char firstDigit) {
        char ch = firstDigit;
        do {
            data.intPart = data.intPart * 10 + (ch - '0');
            ch = source.nextChar();
        } while(isDigit(ch));
        if (ch == '.') {
            ch = source.nextChar();
            while (isDigit(ch)) {
                data.fractPart = data.fractPart * 10 + (ch - '0');
                data.divisor = data.divisor * 10;
                ch = source.nextChar();
            }
        }
        return data.createToken();
    }

    /**
     * Parses the character sequence as a hexadecimal number.
     * @param loc Location of the first character of the token
     * @param firstDigit First digit
     * @param negative Is the number negative
     * @return A token representing a number
     */
    private TokenNumber parseHexNumber(JsonLocation loc, char firstDigit, boolean negative) {
        final long sign = negative ? -1 : 1;
        char ch = firstDigit;
        long value = 0;
        do {
            value = value * 16 + hexToDecimal(ch);
            ch = source.nextChar();
        } while(isHexDigit(ch));
        return new TokenNumber(loc, sign * value);
    }

    /**
     * Parses the character sequence as a string.
     * @param loc Location of the first character of the token
     * @param quote The quote character that opened the string
     * @return A token representing a string
     * @throws JsonException If parsing fails
     */
    private TokenString parseString(JsonLocation loc, char quote) throws JsonException {
        StringBuilder value = new StringBuilder();
        char ch = source.nextChar();
        while(ch != quote) {
            if (ch == '\\') {
                ch = source.nextChar();
                switch (ch) {
                    case '"':
                    case '\'':
                    case '\\':
                    case '/':
                        value.append(ch);
                        break;
                    case 'b':
                        value.append('\b');
                        break;
                    case 'f':
                        value.append('\f');
                        break;
                    case 'n':
                        value.append('\n');
                        break;
                    case 'r':
                        value.append('\r');
                        break;
                    case 't':
                        value.append('\t');
                        break;
                    case 'u':
                        char hex0 = source.nextChar();
                        char hex1 = source.nextChar();
                        char hex2 = source.nextChar();
                        char hex3 = source.nextChar();
                        if (!isHexDigit(hex0) || !isHexDigit(hex1) || !isHexDigit(hex2) || !isHexDigit(hex3)) {
                            String errorSequence = "u" + (hex0 > ' ' ? hex0 : '?') + (hex1 > ' ' ? hex1 : '?')
                                    + (hex2 > ' ' ? hex2 : '?') + (hex3 > ' ' ? hex3 : '?');
                            throw new JsonException(new JsonError.IncorrectStringSequence(
                                    source.getLocation(),
                                    errorSequence
                            ));
                        }
                        value.append((char) (hexToDecimal(hex0) * 0x1000 + hexToDecimal(hex1) * 0x100
                                + hexToDecimal(hex2) * 0x10 + hexToDecimal(hex3)));
                        break;
                    default:
                        throw new JsonException(new JsonError.IncorrectStringSequence(
                                source.getLocation(),
                                ch > ' ' ? "" + ch : "?"
                        ));
                }
            } else if (ch == 0) {
                throw new JsonException(new JsonError.UnclosedString(loc));
            } else {
                value.append(ch);
            }
            ch = source.nextChar();
        }
        source.nextChar();
        return new TokenString(loc, value.toString());
    }

    /**
     * Converts hexadecimal number to decimal
     * @param ch Hexadecimal number
     * @return Decimal number
     */
    private static int hexToDecimal(char ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }
        if (ch >= 'a' && ch <= 'f') {
            return ch - 'a' + 10;
        }
        if (ch >= 'A' && ch <= 'F') {
            return ch - 'A' + 10;
        }
        throw new IllegalArgumentException();
    }
}
