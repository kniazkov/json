package com.kniazkov.json;

/**
 * JSON parser.
 */
public final class JsonParser {
    /**
     * Parses a string containing JSON document into a JSON element.
     * @param source String containing JSON document
     * @return JSON element
     * @throws JsonException If parsing fails
     */
    public static JsonElement parseString(String source) throws JsonException {
        return parseString(source, JsonParsingMode.ENHANCED);
    }

    /**
     * Parses a string containing JSON document into a JSON element.
     * @param source String containing JSON document
     * @param mode Parsing mode
     * @return JSON element
     * @throws JsonException If parsing fails
     */
    public static JsonElement parseString(String source, JsonParsingMode mode) throws JsonException {
        JsonParser parser = new JsonParser(new Source(source), mode);
        return parser.parse(null);
    }

    /**
     * Lexer that splits JSON documents into separate tokens.
     */
    private final Lexer lexer;

    /**
     * Parsing mode.
     */
    private final JsonParsingMode mode;

    /**
     * Constructor.
     * @param src Object containing JSON document for parsing
     */
    private JsonParser(Source src, JsonParsingMode mode) {
        this.lexer = new Lexer(src);
        this.mode = mode;
    }

    /**
     * Parses the next JSON element from the current position.
     * @param parent The container that will contain the parsed element
     * @return JSON element
     * @throws JsonException If parsing fails
     */
    private JsonElement parse(JsonContainer parent) throws JsonException {
        Token token = lexer.getToken(mode);
        if (token instanceof TokenLiteral) {
            return ((TokenLiteral) token).toElement(parent);
        }
        if (token instanceof TokenOpeningSquareBracket) {
            return parseArray(parent);
        }
        return null;
    }

    /**
     * Parses a sequence of tokens as a JSON array.
     * @param parent The container that will contain the parsed array
     * @return JSON array
     * @throws JsonException If parsing fails
     */
    private JsonArray parseArray(JsonContainer parent) throws JsonException {
        JsonArray array = new JsonArray(parent);
        Token token = lexer.getToken(mode);
        while (token != null) {
            if (token instanceof TokenClosingSquareBracket) {
                return array;
            }
            token = lexer.getToken(mode);
        }
        return array;
    }
}
