package com.kniazkov.json;

/**
 * JSON parser.
 */
public final class JsonParser {
    /**
     * Parses a string containing JSON document into a JSON element.
     * @param source String containing JSON document
     * @return JSON element
     */
    public static JsonElement parseString(String source) {
        JsonParser parser = new JsonParser(new Source(source));
        return parser.parse();
    }

    /**
     * Object containing JSON document for parsing.
     */
    private final Source src;

    /**
     * Constructor.
     * @param src Object containing JSON document for parsing
     */
    private JsonParser(Source src) {
        this.src = src;
    }

    /**
     * Parses the next JSON element from the current position.
     * @return JSON element
     */
    private JsonElement parse() {
        return null;
    }
}
