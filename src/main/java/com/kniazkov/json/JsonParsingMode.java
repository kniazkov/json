/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * Defines parsing modes for JSON document parsing.
 */
public enum JsonParsingMode {
    /**
     * Only the syntax defined by the ECMA-404 standard ("The JSON Data Interchange Standard")
     * is allowed. Parsing syntax constructs not defined by this standard will raise an exception.
     */
    STRICT,

    /**
     * JSON5 syntax (<a href="http://json5.org">...</a>) is allowed.
     * This is an extension to the base format that aims to be easier to write
     * and maintain files by hand.
     */
    JSON5,

    /**
     * Extended mode. JSON5 syntax is allowed. Besides, if a JSON document contains some errors,
     * the parser will still try to parse such a document.
     */
    EXTENDED
}
