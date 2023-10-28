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
        return parser.parseRoot();
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
     * Parses the root element from the first position.
     * @return JSON element
     * @throws JsonException If parsing fails
     */
    private JsonElement parseRoot() throws JsonException {
        return parseElement(null, lexer.getToken(mode));
    }

    /**
     * Parses the next JSON element from the current position.
     * @param parent The container that will contain the parsed element
     * @param firstToken The first token in the token sequence
     * @return JSON element
     * @throws JsonException If parsing fails
     */
    private JsonElement parseElement(JsonContainer parent, Token firstToken) throws JsonException {
        if (firstToken instanceof TokenLiteral) {
            return ((TokenLiteral) firstToken).toElement(parent);
        }
        if (firstToken instanceof TokenOpeningSquareBracket) {
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
        final JsonArray array = new JsonArray(parent);
        Token token = lexer.getToken(mode);
        boolean expectedElement = false;
        do {
            if (token instanceof TokenClosingSquareBracket) {
                if (expectedElement && mode == JsonParsingMode.STRICT) {
                    throw new JsonException(
                            new JsonError.ExpectedElementAfterComma(token.getLocation())
                    );
                }
                return array;
            }

            JsonElement child = parseElement(array, token);
            if (child != null) {
                array.addChild(child);
                expectedElement = false;
                token = lexer.getToken(mode);
                if (token instanceof TokenComma) {
                    expectedElement = true;
                    token = lexer.getToken(mode);
                }
            }
        } while(expectedElement);
        return array;
    }
}
