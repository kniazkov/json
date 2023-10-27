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
        return String.format("%d.%d: %s", loc.row, loc.column, getMessage());
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
}
