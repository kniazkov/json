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
            if (number == null) {
                number = new NumberParsing(loc, false);
            }
            ch = source.nextChar();
            if (ch == 'x') {
                if (mode == JsonParsingMode.STRICT) {
                    throw new JsonException(new JsonError.InvalidCharacter(loc, 'x'));
                }
                ch = source.nextChar();
                if (isHexDigit(ch)) {
                    return parseHexNumber(loc, ch, number.negative);
                }
                throw new JsonException(new JsonError.ExpectedHexDigit(source.getLocation()));
            } else if (isDigit(ch)) {
                return parseNumber(number, ch);
            } else if (ch == '.') {
                return parseRealNumber(number);
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

        if (ch == '.') {
            if (mode == JsonParsingMode.STRICT) {
                throw new JsonException(new JsonError.InvalidCharacter(loc, '.'));
            }
            if (number == null) {
                number = new NumberParsing(loc, false);
            }
            return parseRealNumber(number);
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
        final JsonLocation start;

        /**
         * Flag that determines that the number is negative.
         */
        final boolean negative;

        /**
         * Integer part of the number.
         */
        long intPart;

        /**
         * Flag that indicates that the number has an integer part.
         */
        boolean hasIntPart;

        /**
         * Fractional part of the number.
         */
        long fractPart;

        /**
         * Divisor by which the fractional part is to be divided.
         */
        long divisor;

        /**
         * Decimal point location.
         */
        JsonLocation point;

        /**
         * Exponent.
         */
        int exponent;

        /**
         * Constructor.
         * @param start Location in the JSON document
         * @param negative Flag that determines that the number is negative
         */
        NumberParsing(JsonLocation start, boolean negative) {
            this.start = start;
            this.negative = negative;
            this.intPart = 0;
            this.hasIntPart = false;
            this.fractPart = 0;
            this.divisor = 1;
            this.point = null;
            this.exponent = 0;
        }

        /**
         * Generates a token from the collected data.
         * @return Token representing a number
         * @throws JsonException If parsing fails
         */
        TokenNumber createToken() throws JsonException {
            if (divisor == 1) {
                if (!hasIntPart) {
                    throw new JsonException(new JsonError.IncorrectNumberFormat(start));
                }
                if (point != null && mode == JsonParsingMode.STRICT) {
                    throw new JsonException(new JsonError.ExpectedNumberAfterPoint(point));
                }
            }
            final double sign = negative ? -1 : 1;
            double value = sign * (intPart + (double)fractPart / (double)divisor);
            if (exponent != 0) {
                value = value * Math.pow(10, exponent);
            }
            return new TokenNumber(start, value);
        }
    }

    /**
     * Parses the character sequence as a number.
     * @param data Data needed for parsing numbers
     * @param firstDigit First digit
     * @return A token representing a number
     * @throws JsonException If parsing fails
     */
    private TokenNumber parseNumber(NumberParsing data, char firstDigit) throws JsonException {
        data.hasIntPart = true;
        char ch = firstDigit;
        do {
            data.intPart = data.intPart * 10 + (ch - '0');
            ch = source.nextChar();
        } while(isDigit(ch));
        if (ch == '.') {
            return parseRealNumber(data);
        }
        if (ch == 'e' || ch == 'E') {
            return parseExponent(data);
        }
        return data.createToken();
    }

    /**
     * Parses the character sequence as a real number.
     * @param data Data needed for parsing numbers
     * @return A token representing a number
     * @throws JsonException If parsing fails
     */
    private TokenNumber parseRealNumber(NumberParsing data) throws JsonException {
        char ch = source.getChar();
        assert ch == '.';
        data.point = source.getLocation();
        ch = source.nextChar();
        while (isDigit(ch)) {
            data.fractPart = data.fractPart * 10 + (ch - '0');
            data.divisor = data.divisor * 10;
            ch = source.nextChar();
        }
        if (ch == 'e' || ch == 'E') {
            return parseExponent(data);
        }
        return data.createToken();
    }

    /**
     * Parses the character sequence as an exponent (the part of number after 'e').
     * @param data Data needed for parsing numbers
     * @return A token representing a number
     * @throws JsonException If parsing fails
     */
    private TokenNumber parseExponent(NumberParsing data) throws JsonException {
        char ch = source.getChar();
        assert ch == 'e' || ch == 'E';
        JsonLocation loc = source.getLocation();
        ch = source.nextChar();
        int sign = 1;
        if (ch == '+') {
            ch = source.nextChar();
        } else if (ch == '-') {
            sign = -1;
            ch = source.nextChar();
        }
        if (!isDigit(ch)) {
            throw new JsonException(new JsonError.IncorrectExponentNotation(loc));
        }
        int value = 0;
        while (isDigit(ch)) {
            value = value * 10 + (ch - '0');
            ch = source.nextChar();
        }
        data.exponent = value * sign;
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
