/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * A JSON value has the wrong type or cannot be represented in the requested format.
 * Unlike JsonException (document syntax errors), this unchecked exception reports
 * errors when accessing a JSON tree or mapping it to Java values.
 */
public final class JsonDataException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a data-format exception.
     * @param message Description of the expected format and actual data type
     */
    public JsonDataException(String message) {
        super(message);
    }
    /**
     * Creates a mapping error retaining its underlying cause.
     * @param message Description including the data path
     * @param cause Underlying conversion or reflection failure
     */
    public JsonDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
