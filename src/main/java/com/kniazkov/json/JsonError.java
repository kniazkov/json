/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * An error occurred while parsing a JSON document.
 */
public abstract class JsonError {
    /**
     * Specifies the location in the document where the error was found.
     */
    private final JsonLocation loc;

    /**
     * Constructor.
     * @param loc Position in the document where the error was found
     */
    JsonError(JsonLocation loc) {
        this.loc = loc;
    }

    /**
     * Returns the location in the document where the error was found.
     * @return Location
     */
    public JsonLocation getLocation() {
        return loc;
    }

    /**
     * Returns the text description of the error (in English).
     * @return Text description of the error
     */
    public String getMessage() {
        return getLocalizedMessage("en");
    }

    /**
     * Returns the localized text description of the error.
     * @param lang Language of message ('en', 'ru'...)
     * @return Text description of the error
     */
    public abstract String getLocalizedMessage(String lang);

    @Override
    public String toString() {
        return String.format("%d.%d, %s", loc.row, loc.column, getMessage());
    }

    /**
     * Error 'A number after the minus sign is expected'.
     */
    public final static class ExpectedNumberAfterMinus extends IncorrectNumberFormat {
        /**
         * Constructor.
         * @param loc Position in the document where the error was found
         */
        ExpectedNumberAfterMinus(JsonLocation loc) {
            super(loc);
        }

        @Override
        public String getLocalizedMessage(String lang) {
            if ("ru".equals(lang)) {
                return "После знака минуса должно быть число";
            }
            return "A number after the minus sign is expected";
        }
    }

    /**
     * Error 'A number after the plus sign is expected'.
     */
    public final static class ExpectedNumberAfterPlus extends IncorrectNumberFormat {
        /**
         * Constructor.
         * @param loc Position in the document where the error was found
         */
        ExpectedNumberAfterPlus(JsonLocation loc) {
            super(loc);
        }

        @Override
        public String getLocalizedMessage(String lang) {
            if ("ru".equals(lang)) {
                return "После знака плюса должно быть число";
            }
            return "A number after the plus sign is expected";
        }
    }

    /**
     * Error 'A number after the decimal point is expected'.
     */
    public final static class ExpectedNumberAfterPoint extends IncorrectNumberFormat {
        /**
         * Constructor.
         * @param loc Position in the document where the error was found
         */
        ExpectedNumberAfterPoint(JsonLocation loc) {
            super(loc);
        }

        @Override
        public String getLocalizedMessage(String lang) {
            if ("ru".equals(lang)) {
                return "После десятичной точки должно быть число";
            }
            return "A number after the decimal point is expected";
        }
    }

    /**
     * Error 'Incorrect exponent notation'.
     */
    public final static class IncorrectExponentNotation extends IncorrectNumberFormat {
        /**
         * Constructor.
         * @param loc Position in the document where the error was found
         */
        IncorrectExponentNotation(JsonLocation loc) {
            super(loc);
        }

        @Override
        public String getLocalizedMessage(String lang) {
            if ("ru".equals(lang)) {
                return "Неправильный формат экспоненты числа";
            }
            return "Incorrect exponent notation";
        }
    }

    /**
     * Error 'Hexadecimal digit after 'x' is expected'.
     */
    public final static class ExpectedHexDigit extends IncorrectNumberFormat {
        /**
         * Constructor.
         * @param loc Position in the document where the error was found
         */
        ExpectedHexDigit(JsonLocation loc) {
            super(loc);
        }

        @Override
        public String getLocalizedMessage(String lang) {
            if ("ru".equals(lang)) {
                return "После 'x' должна быть шестнадцатеричная цифра";
            }
            return "Hexadecimal digit after 'x' is expected";
        }
    }

    /**
     * Error 'Incorrect number format' (common error related to numbers parsing).
     */
    public static class IncorrectNumberFormat extends JsonError {
        /**
         * Constructor.
         * @param loc Position in the document where the error was found
         */
        IncorrectNumberFormat(JsonLocation loc) {
            super(loc);
        }

        @Override
        public String getLocalizedMessage(String lang) {
            if ("ru".equals(lang)) {
                return "Неправильный формат числа";
            }
            return "Incorrect number format";
        }
    }

    /**
     * Error 'Invalid character'.
     */
    public final static class InvalidCharacter extends JsonError {
        /**
         * The symbol.
         */
        private final char ch;
        /**
         * Constructor.
         * @param ch The symbol
         * @param loc Position in the document where the error was found
         */
        InvalidCharacter(JsonLocation loc, char ch) {
            super(loc);
            this.ch = ch;
        }

        @Override
        public String getLocalizedMessage(String lang) {
            if ("ru".equals(lang)) {
                return "Недопустимый символ: '" + ch + '\'';
            }
            return "Invalid character: '" + ch + '\'';
        }
    }

    /**
     * Error 'Some element after the comma is expected'.
     */
    public final static class ExpectedElementAfterComma extends JsonError {
        /**
         * Constructor.
         * @param loc Position in the document where the error was found
         */
        ExpectedElementAfterComma(JsonLocation loc) {
            super(loc);
        }

        @Override
        public String getLocalizedMessage(String lang) {
            if ("ru".equals(lang)) {
                return "После запятой должен быть элемент";
            }
            return "An element after the comma is expected";
        }
    }

    /**
     * Error 'Unclosed string'.
     */
    public final static class UnclosedString extends JsonError {
        /**
         * Constructor.
         * @param loc Position in the document where the error was found
         */
        UnclosedString(JsonLocation loc) {
            super(loc);
        }

        @Override
        public String getLocalizedMessage(String lang) {
            if ("ru".equals(lang)) {
                return "Пропущена закрывающая кавычка";
            }
            return "Unclosed string";
        }
    }

    /**
     * Error 'Incorrect string sequence'.
     */
    public final static class IncorrectStringSequence extends JsonError {
        /**
         * The sequence.
         */
        private final String sequence;
        /**
         * Constructor.
         * @param sequence The sequence.
         * @param loc Position in the document where the error was found
         */
        IncorrectStringSequence(JsonLocation loc, String sequence) {
            super(loc);
            this.sequence = sequence;
        }

        @Override
        public String getLocalizedMessage(String lang) {
            if ("ru".equals(lang)) {
                return "Неправильная строковая последовательность: '\\" + sequence + '\'';
            }
            return "Incorrect string sequence: '\\" + sequence + '\'';
        }
    }

    /**
     * Error 'A key:value pair after the comma is expected'.
     */
    public final static class ExpectedPairAfterComma extends JsonError {
        /**
         * Constructor.
         * @param loc Position in the document where the error was found
         */
        ExpectedPairAfterComma(JsonLocation loc) {
            super(loc);
        }

        @Override
        public String getLocalizedMessage(String lang) {
            if ("ru".equals(lang)) {
                return "После запятой должна быть пара ключ:значение";
            }
            return "A key:value pair after the comma is expected";
        }
    }

    /**
     * Error 'A key (string) is expected'.
     */
    public final static class ExpectedKey extends JsonError {
        /**
         * Constructor.
         * @param loc Position in the document where the error was found
         */
        ExpectedKey(JsonLocation loc) {
            super(loc);
        }

        @Override
        public String getLocalizedMessage(String lang) {
            if ("ru".equals(lang)) {
                return "Ожижается ключ (строка)";
            }
            return "A key (string) is expected";
        }
    }

    /**
     * Error 'A colon as a separator after the key is expected'.
     */
    public final static class ExpectedSeparator extends JsonError {
        /**
         * Constructor.
         * @param loc Position in the document where the error was found
         */
        ExpectedSeparator(JsonLocation loc) {
            super(loc);
        }

        @Override
        public String getLocalizedMessage(String lang) {
            if ("ru".equals(lang)) {
                return "После ключа должно быть двоеточие в качестве разделителя";
            }
            return "A colon as a separator after the key is expected";
        }
    }

    /**
     * Error 'Some element after the separator is expected'.
     */
    public final static class ExpectedElementAfterSeparator extends JsonError {
        /**
         * Constructor.
         * @param loc Position in the document where the error was found
         */
        ExpectedElementAfterSeparator(JsonLocation loc) {
            super(loc);
        }

        @Override
        public String getLocalizedMessage(String lang) {
            if ("ru".equals(lang)) {
                return "После разделителя должен быть элемент";
            }
            return "An element after the separator is expected";
        }
    }

    /**
     * Error 'Missing a comma'.
     */
    public final static class MissingComma extends JsonError {
        /**
         * Constructor.
         * @param loc Position in the document where the error was found
         */
        MissingComma(JsonLocation loc) {
            super(loc);
        }

        @Override
        public String getLocalizedMessage(String lang) {
            if ("ru".equals(lang)) {
                return "Пропущена запятая";
            }
            return "Missing a comma";
        }
    }
}
