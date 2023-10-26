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
     * Lexer that splits JSON documents into separate tokens.
     */
    private final Lexer lexer;

    /**
     * Constructor.
     * @param src Object containing JSON document for parsing
     */
    private JsonParser(Source src) {
        this.lexer = new Lexer(src);
    }

    /**
     * Parses the next JSON element from the current position.
     * @return JSON element
     */
    private JsonElement parse() {
        return null;
    }
}
