/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

/** Field-level policy for null or invalid JSON data. */
public enum JsonMappingAction {
    /** Only for onNull: assign null to references, reject null for primitives. */
    DEFAULT,
    /** Reject the value with JsonDataException. */
    ERROR,
    /** Leave the entire field at its constructor-provided value. */
    KEEP_DEFAULT,
    /** Assign null; invalid configuration for primitive fields. */
    SET_NULL
}
