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
    public final static class ExpectedNumberAfterMinus extends JsonError {
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
    public final static class ExpectedNumberAfterPlus extends JsonError {
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
}
