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
     * Extended syntax is allowed (<a href="http://json5.org">...</a>).
     * JSON5 is an extension to the base format that aims to be easier to write
     * and maintain files by hand.
     */
    JSON5,

    /**
     * JSON5 syntax is allowed. Besides, if a JSON document contains some errors,
     * the parser will still try to parse such a document.
     */
    ENHANCED
}
