package com.kniazkov.json;

/**
 * An exception that occurred while parsing a JSON document.
 */
public final class JsonException extends Exception {
    /**
     * The error occurred while parsing a JSON document.
     */
    private final JsonError error;

    /**
     * Constructor.
     * @param error The error occurred while parsing a JSON document
     */
    public JsonException(JsonError error) {
        this.error = error;
    }

    /**
     * Returns a description of the error that occurred while parsing a JSON document.
     * @return Error description
     */
    public JsonError getError() {
        return error;
    }

    @Override
    public String getMessage() {
        JsonLocation loc = error.getLocation();
        return String.format("%d.%d: %s", loc.row, loc.column, error.getMessage());
    }

    @Override
    public String getLocalizedMessage() {
        JsonLocation loc = error.getLocation();
        String lang = System.getProperty("user.language");
        return String.format("%d.%d: %s", loc.row, loc.column, error.getLocalizedMessage(lang));
    }
}
