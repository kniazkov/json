/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.math.BigDecimal;
import java.math.BigInteger;

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
     * @return A token, including TokenEnd at EOF; never null.
     * @throws JsonException If parsing fails
     */
    public Token token() throws JsonException {
        skipPadding(false);
        int ch = source.charValue();

        if (ch == -1) {
            return new TokenEnd(source.location());
        }

        JsonLocation loc = source.location();

        if (isLetter(ch)) {
            StringBuilder builder = new StringBuilder();
            do {
                builder.append((char) ch);
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

        if (ch == '-' || ch == '+' || ch == '.' || isDigit(ch)) {
            return parseNumber(loc);
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

        throw new JsonException(new JsonError.InvalidCharacter(loc, (char) ch));
    }

    /** Skips mode-specific padding, with broader EXTENDED whitespace at root boundaries. */
    void skipPadding(boolean rootBoundary) throws JsonException {
        while (true) {
            int ch = source.charValue();
            while (isWhiteSpace(ch)
                    || mode == JsonParsingMode.JSON5 && isJson5Space(ch)
                    || rootBoundary && mode == JsonParsingMode.EXTENDED && isExtendedSpace(ch)) {
                ch = source.nextChar();
            }
            if (mode != JsonParsingMode.JSON5 || ch != '/') {
                return;
            }
            JsonLocation start = source.location();
            ch = source.nextChar();
            if (ch == '/') {
                do {
                    ch = source.nextChar();
                } while (ch != -1 && ch != '\r' && ch != '\n' && ch != 0x2028 && ch != 0x2029);
            } else if (ch == '*') {
                int previous = -1;
                ch = source.nextChar();
                while (previous != '*' || ch != '/') {
                    if (ch == -1) {
                        throw new JsonException(new JsonError.UnclosedComment(start));
                    }
                    previous = ch;
                    ch = source.nextChar();
                }
                source.nextChar();
            } else {
                throw new JsonException(new JsonError.InvalidCharacter(start, '/'));
            }
        }
    }

    /** JSON5 whitespace and line terminators; NEL and Java control separators are excluded. */
    private static boolean isJson5Space(int ch) {
        return ch == 0x0b || ch == 0x0c || ch == 0xfeff || ch == 0x2028 || ch == 0x2029
                || Character.getType(ch) == Character.SPACE_SEPARATOR;
    }

    /** Unicode spaces and Java whitespace, plus NEL and the BOM used as space by JSON5. */
    private static boolean isExtendedSpace(int ch) {
        return Character.isWhitespace(ch) || Character.isSpaceChar(ch) || ch == 0x85 || ch == 0xfeff;
    }

    /**
     * Checks if the character is a space.
     * @param ch Character
     * @return Checking result
     */
    private static boolean isWhiteSpace(int ch) {
        return ch == ' ' || ch == '\r' || ch == '\n' || ch == '\t';
    }

    /**
     * Checks if the character is a letter.
     * @param ch Character
     * @return Checking result
     */
    private static boolean isLetter(int ch) {
        return ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z' || ch == '_';
    }

    /**
     * Checks if the character is a digit.
     * @param ch Character
     * @return Checking result
     */
    private static boolean isDigit(int ch) {
        return ch >= '0' && ch <= '9';
    }

    /**
     * Checks if the character is a hexadecimal digit.
     * @param ch Character
     * @return Checking result
     */
    private static boolean isHexDigit(int ch) {
        return isDigit(ch) || ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F' ;
    }

    /** Parses a numeric lexeme without intermediate floating-point conversion. */
    private TokenNumber parseNumber(JsonLocation loc) throws JsonException {
        StringBuilder text = new StringBuilder();
        int ch = source.charValue();
        boolean negative = ch == '-';
        if (ch == '-' || ch == '+') {
            if (ch == '+' && mode == JsonParsingMode.STRICT) {
                throw new JsonException(new JsonError.InvalidCharacter(loc, (char) ch));
            }
            text.append((char) ch);
            ch = source.nextChar();
            if (!isDigit(ch) && ch != '.') {
                throw new JsonException(negative
                        ? new JsonError.ExpectedNumberAfterMinus(source.location())
                        : new JsonError.ExpectedNumberAfterPlus(source.location()));
            }
        }
        boolean integerDigits = false;
        if (ch == '0') {
            integerDigits = true;
            text.append((char) ch);
            ch = source.nextChar();
            if (ch == 'x' || ch == 'X') {
                if (mode == JsonParsingMode.STRICT) {
                    throw new JsonException(new JsonError.InvalidCharacter(loc, (char) ch));
                }
                StringBuilder hex = new StringBuilder();
                ch = source.nextChar();
                if (!isHexDigit(ch)) {
                    throw new JsonException(new JsonError.ExpectedHexDigit(source.location()));
                }
                while (isHexDigit(ch)) {
                    hex.append((char) ch);
                    ch = source.nextChar();
                }
                BigInteger value = new BigInteger(hex.toString(), 16);
                return new TokenNumber(loc, JsonNumber.of(negative ? value.negate() : value));
            }
            if (isDigit(ch) && mode != JsonParsingMode.EXTENDED) {
                throw new JsonException(new JsonError.IncorrectNumberFormat(loc));
            }
        }
        while (isDigit(ch)) {
            integerDigits = true;
            text.append((char) ch);
            ch = source.nextChar();
        }
        boolean real = false;
        if (ch == '.') {
            JsonLocation point = source.location();
            if (!integerDigits && mode == JsonParsingMode.STRICT) {
                throw new JsonException(new JsonError.InvalidCharacter(loc, (char) ch));
            }
            real = true;
            text.append((char) ch);
            ch = source.nextChar();
            if (!isDigit(ch)) {
                if (!integerDigits) {
                    throw new JsonException(new JsonError.IncorrectNumberFormat(loc));
                }
                if (mode == JsonParsingMode.STRICT) {
                    throw new JsonException(new JsonError.ExpectedNumberAfterPoint(point));
                }
            }
            while (isDigit(ch)) {
                text.append((char) ch);
                ch = source.nextChar();
            }
        }
        if (ch == 'e' || ch == 'E') {
            real = true;
            JsonLocation exponent = source.location();
            text.append((char) ch);
            ch = source.nextChar();
            if (ch == '+' || ch == '-') {
                text.append((char) ch);
                ch = source.nextChar();
            }
            if (!isDigit(ch)) {
                throw new JsonException(new JsonError.IncorrectExponentNotation(exponent));
            }
            while (isDigit(ch)) {
                text.append((char) ch);
                ch = source.nextChar();
            }
        }
        try {
            JsonNumber value = real ? JsonNumber.of(new BigDecimal(text.toString()))
                    : JsonNumber.of(new BigInteger(text.toString()));
            return new TokenNumber(loc, value);
        } catch (NumberFormatException exception) {
            // BigDecimal's exponent/scale range is finite; never wrap it silently.
            throw new JsonException(new JsonError.IncorrectNumberFormat(loc));
        }
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
        int ch = source.nextChar();
        while(ch != quote) {
            if (ch == '\\') {
                ch = source.nextChar();
                switch (ch) {
                    case '"':
                    case '\'':
                    case '\\':
                    case '/':
                        value.append((char) ch);
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
                        int hex0 = source.nextChar();
                        int hex1 = source.nextChar();
                        int hex2 = source.nextChar();
                        int hex3 = source.nextChar();
                        if (!isHexDigit(hex0) || !isHexDigit(hex1) || !isHexDigit(hex2) || !isHexDigit(hex3)) {
                            String errorSequence = "u" + (hex0 > ' ' ? (char) hex0 : '?') + (hex1 > ' ' ? (char) hex1 : '?')
                                    + (hex2 > ' ' ? (char) hex2 : '?') + (hex3 > ' ' ? (char) hex3 : '?');
                            throw new JsonException(new JsonError.IncorrectStringSequence(
                                    source.location(),
                                    errorSequence
                            ));
                        }
                        value.append((char) (hexToDecimal(hex0) * 0x1000 + hexToDecimal(hex1) * 0x100
                                + hexToDecimal(hex2) * 0x10 + hexToDecimal(hex3)));
                        break;
                    case ' ':
                        JsonLocation start = source.location();
                        if (mode == JsonParsingMode.STRICT) {
                            throw new JsonException(new JsonError.IncorrectStringSequence(start, "?"));
                        }
                        while (ch == ' ' || ch == '\r' || ch == '\t') {
                            ch = source.nextChar();
                        }
                        if (ch != '\n') {
                            throw new JsonException(new JsonError.IncorrectStringSequence(start, "?"));
                        }
                        break;
                    case '\n':
                        if (mode == JsonParsingMode.STRICT) {
                            throw new JsonException(
                                    new JsonError.IncorrectStringSequence(source.location(), "?")
                            );
                        }
                        break;
                    default:
                        throw new JsonException(new JsonError.IncorrectStringSequence(
                                source.location(),
                                ch > ' ' ? "" + (char) ch : "?"
                        ));
                }
            } else if (ch == -1) {
                throw new JsonException(new JsonError.UnclosedString(loc));
            } else if (ch == 0) {
                throw new JsonException(new JsonError.InvalidCharacter(source.location(), (char) ch));
            } else {
                value.append((char) ch);
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
    private static int hexToDecimal(int ch) {
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
