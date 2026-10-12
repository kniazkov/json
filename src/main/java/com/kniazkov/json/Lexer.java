/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/** Splits JSON documents into separate tokens. */
final class Lexer {
    /** Object containing JSON document for parsing. */
    private final Source source;

    /** Parsing mode. */
    private final JsonParsingMode mode;

    private final List<JsonError> warnings;

    /**
     * Constructor.
     *
     * @param source Object containing JSON document for parsing
     */
    Lexer(Source source, JsonParsingMode mode) {
        this(source, mode, null);
    }

    Lexer(Source source, JsonParsingMode mode, List<JsonError> warnings) {
        this.source = source;
        this.mode = mode;
        this.warnings = warnings;
    }

    /**
     * Returns the next token from a sequence.
     *
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

        if (isIdentifierStart(ch) || ch == '\\' || Character.isHighSurrogate((char) ch)) {
            return parseIdentifier(loc);
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
                    || mode != JsonParsingMode.STRICT && isJson5Space(ch)
                    || rootBoundary && mode == JsonParsingMode.EXTENDED && isExtendedSpace(ch)) {
                ch = source.nextChar();
            }
            if (mode == JsonParsingMode.STRICT
                    || (rootBoundary && mode == JsonParsingMode.EXTENDED)
                    || ch != '/') {
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
        return ch == 0x0b
                || ch == 0x0c
                || ch == 0xfeff
                || ch == 0x2028
                || ch == 0x2029
                || Character.getType(ch) == Character.SPACE_SEPARATOR;
    }

    /** Unicode spaces and Java whitespace, plus NEL and the BOM used as space by JSON5. */
    private static boolean isExtendedSpace(int ch) {
        return Character.isWhitespace(ch)
                || Character.isSpaceChar(ch)
                || ch == 0x85
                || ch == 0xfeff;
    }

    /**
     * Checks if the character is a space.
     *
     * @param ch Character
     * @return Checking result
     */
    private static boolean isWhiteSpace(int ch) {
        return ch == ' ' || ch == '\r' || ch == '\n' || ch == '\t';
    }

    /**
     * Checks if the character is a digit.
     *
     * @param ch Character
     * @return Checking result
     */
    private static boolean isDigit(int ch) {
        return ch >= '0' && ch <= '9';
    }

    /**
     * Checks if the character is a hexadecimal digit.
     *
     * @param ch Character
     * @return Checking result
     */
    private static boolean isHexDigit(int ch) {
        return isDigit(ch) || ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F';
    }

    /** ECMAScript IdentifierStart categories, plus $ and _. */
    private static boolean isIdentifierStart(int ch) {
        int type = Character.getType(ch);
        return ch == '$'
                || ch == '_'
                || type == Character.UPPERCASE_LETTER
                || type == Character.LOWERCASE_LETTER
                || type == Character.TITLECASE_LETTER
                || type == Character.MODIFIER_LETTER
                || type == Character.OTHER_LETTER
                || type == Character.LETTER_NUMBER;
    }

    private static boolean isIdentifierPart(int ch) {
        int type = Character.getType(ch);
        return isIdentifierStart(ch)
                || type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.DECIMAL_DIGIT_NUMBER
                || type == Character.CONNECTOR_PUNCTUATION
                || ch == 0x200c
                || ch == 0x200d;
    }

    private Token parseIdentifier(JsonLocation loc) throws JsonException {
        StringBuilder name = new StringBuilder();
        boolean first = true;
        boolean escaped = false;
        int ch = source.charValue();
        while (ch != -1) {
            JsonLocation position = source.location();
            int point = ch;
            if (ch == '\\') {
                escaped = true;
                if (source.nextChar() != 'u') {
                    throw new JsonException(new JsonError.InvalidCharacter(position, '\\'));
                }
                point = readHexEscape(4, position);
                if (!(first ? isIdentifierStart(point) : isIdentifierPart(point))) {
                    throw new JsonException(new JsonError.InvalidCharacter(position, (char) point));
                }
            } else if (Character.isHighSurrogate((char) ch)) {
                int low = source.nextChar();
                if (low == -1 || !Character.isLowSurrogate((char) low)) {
                    throw new JsonException(new JsonError.InvalidCharacter(position, (char) ch));
                }
                point = Character.toCodePoint((char) ch, (char) low);
                if (!(first ? isIdentifierStart(point) : isIdentifierPart(point))) {
                    throw new JsonException(new JsonError.InvalidCharacter(position, (char) ch));
                }
            } else if (!(first ? isIdentifierStart(point) : isIdentifierPart(point))) {
                break;
            }
            name.appendCodePoint(point);
            first = false;
            ch = source.nextChar();
        }
        String identifier = name.toString();
        // Escaped spellings are IdentifierNames for keys, never literal values.
        if (!escaped) {
            if ("null".equals(identifier)) {
                return new TokenNull(loc);
            }
            if ("true".equals(identifier)) {
                return new TokenBoolean(loc, true);
            }
            if ("false".equals(identifier)) {
                return new TokenBoolean(loc, false);
            }
            if (mode != JsonParsingMode.STRICT) {
                if ("NaN".equals(identifier)) {
                    return new TokenNumber(loc, new JsonNaN(), identifier);
                }
                if ("Infinity".equals(identifier)) {
                    return new TokenNumber(loc, new JsonInfinity(false), identifier);
                }
            }
        }
        return new TokenIdentifier(loc, identifier);
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
            if (mode != JsonParsingMode.STRICT && (ch == 'N' || ch == 'I')) {
                Token special = parseIdentifier(source.location());
                if (special instanceof TokenNumber number) {
                    JsonNumber value = (JsonNumber) number.toElement(null);
                    return new TokenNumber(
                            loc, value.isNaN() ? new JsonNaN() : new JsonInfinity(negative));
                }
                throw new JsonException(new JsonError.IncorrectNumberFormat(loc));
            }
            if (!isDigit(ch) && ch != '.') {
                throw new JsonException(
                        negative
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
            JsonNumber value =
                    real
                            ? JsonNumber.of(new BigDecimal(text.toString()))
                            : JsonNumber.of(new BigInteger(text.toString()));
            return new TokenNumber(loc, value);
        } catch (NumberFormatException exception) {
            // BigDecimal's exponent/scale range is finite; never wrap it silently.
            throw new JsonException(new JsonError.IncorrectNumberFormat(loc));
        }
    }

    /**
     * Parses the character sequence as a string.
     *
     * @param loc Location of the first character of the token
     * @param quote The quote character that opened the string
     * @return A token representing a string
     * @throws JsonException If parsing fails
     */
    private TokenString parseString(JsonLocation loc, char quote) throws JsonException {
        StringBuilder value = new StringBuilder();
        int ch = source.nextChar();
        while (ch != quote) {
            JsonLocation position = source.location();
            if (ch == -1) {
                throw new JsonException(new JsonError.UnclosedString(loc));
            }
            if (ch == '\\') {
                ch = source.nextChar();
                position = source.location();
                switch (ch) {
                    case '"':
                    case '\\':
                    case '/':
                        value.append((char) ch);
                        break;
                    case '\'':
                        requireExtendedEscape(position, ch);
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
                    case 'v':
                        requireExtendedEscape(position, ch);
                        value.append((char) 0x0b);
                        break;
                    case '0':
                        requireExtendedEscape(position, ch);
                        ch = source.nextChar();
                        if (isDigit(ch)) {
                            throw new JsonException(
                                    new JsonError.IncorrectStringSequence(position, "0"));
                        }
                        value.append((char) 0);
                        continue;
                    case 'u':
                        value.append((char) readHexEscape(4, position));
                        break;
                    case 'x':
                        requireExtendedEscape(position, ch);
                        value.append((char) readHexEscape(2, position));
                        break;
                    case '\r':
                    case '\n':
                    case 0x2028:
                    case 0x2029:
                        requireExtendedEscape(position, ch);
                        int lineBreak = ch;
                        ch = source.nextChar();
                        if (lineBreak == '\r' && ch == '\n') {
                            ch = source.nextChar();
                        }
                        continue;
                    case ' ':
                        requireExtendedEscape(position, ch);
                        // Retain the historical EXTENDED continuation with intervening spaces.
                        if (mode == JsonParsingMode.EXTENDED) {
                            StringBuilder spaces = new StringBuilder();
                            while (ch == ' ' || ch == '\r' || ch == '\t') {
                                spaces.append((char) ch);
                                ch = source.nextChar();
                            }
                            if (ch != '\n') {
                                value.append(spaces);
                                continue;
                            }
                        } else {
                            value.append(' ');
                        }
                        break;
                    default:
                        requireExtendedEscape(position, ch);
                        if (ch == -1 || isDigit(ch)) {
                            throw new JsonException(
                                    new JsonError.IncorrectStringSequence(position, "?"));
                        }
                        value.append((char) ch);
                }
            } else {
                if (mode == JsonParsingMode.STRICT && ch < 0x20
                        || mode == JsonParsingMode.JSON5 && (ch == '\r' || ch == '\n')
                        || mode == JsonParsingMode.EXTENDED && ch == 0) {
                    throw new JsonException(new JsonError.InvalidCharacter(position, (char) ch));
                }
                if (mode != JsonParsingMode.STRICT
                        && (ch == 0x2028 || ch == 0x2029)
                        && warnings != null) {
                    warnings.add(new JsonError.UnescapedLineSeparator(position));
                }
                value.append((char) ch);
            }
            ch = source.nextChar();
        }
        source.nextChar();
        return new TokenString(loc, value.toString());
    }

    /** Rejects JSON5-only escapes in STRICT. */
    private void requireExtendedEscape(JsonLocation loc, int ch) throws JsonException {
        if (mode == JsonParsingMode.STRICT) {
            throw new JsonException(
                    new JsonError.IncorrectStringSequence(loc, ch > ' ' ? "" + (char) ch : "?"));
        }
    }

    /**
     * Reads exactly the requested hex digits after the current u/x, leaving the last digit current.
     */
    private int readHexEscape(int digits, JsonLocation loc) throws JsonException {
        int value = 0;
        StringBuilder sequence = new StringBuilder().append((char) source.charValue());
        for (int i = 0; i < digits; i++) {
            int ch = source.nextChar();
            sequence.append(ch > ' ' ? (char) ch : '?');
            if (!isHexDigit(ch)) {
                throw new JsonException(
                        new JsonError.IncorrectStringSequence(loc, sequence.toString()));
            }
            value = value * 16 + hexToDecimal(ch);
        }
        return value;
    }

    /**
     * Converts hexadecimal number to decimal.
     *
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
