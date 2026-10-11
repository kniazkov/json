/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
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
        this(error, null);
    }

    /**
     * Creates a parsing or input exception retaining the original cause.
     * @param error Error description and location
     * @param cause Original cause, or null
     */
    public JsonException(JsonError error, Throwable cause) {
        super(cause);
        this.error = error;
    }

    /**
     * Returns a description of the error that occurred while parsing a JSON document.
     * @return Error description
     */
    public JsonError error() {
        return error;
    }

    @Override
    public String getMessage() {
        JsonLocation loc = error.location();
        return loc + ", " + error.message();
    }

    @Override
    public String getLocalizedMessage() {
        JsonLocation loc = error.location();
        String lang = System.getProperty("user.language");
        return loc + ", " + error.localizedMessage(lang);
    }
}
