/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.lang.reflect.Field;
import java.util.List;

/**
 * JSON parser.
 */
public final class JsonParser {
    /**
     * Lexer that splits JSON documents into separate tokens.
     */
    private final Lexer lexer;

    /** Input shared with the lexer, used to validate or discard the document tail. */
    private final Source source;

    /** Closing token retained for an enclosing container during recovery. */
    private Token pendingToken;

    /**
     * Parsing mode.
     */
    private final JsonParsingMode mode;

    /**
     * List of errors that occurred during parsing in extended mode
     * and were intentionally ignored.
     */
    private final List<JsonError> warnings;

    /**
     * Parses a string containing JSON document into a JSON element
     *  in {@link JsonParsingMode#EXTENDED} mode.
     * @param source String containing JSON document
     * @return JSON element
     * @throws JsonException If parsing fails
     */
    public static JsonElement parseString(String source) throws JsonException {
        return parseString(source, JsonParsingMode.EXTENDED);
    }

    /**
     * Parses a string containing JSON document into a JSON element.
     *  The parsing mode is specified.
     * @param source String containing JSON document
     * @param mode Parsing mode
     * @return JSON element
     * @throws JsonException If parsing fails
     */
    public static JsonElement parseString(String source, JsonParsingMode mode) throws JsonException {
        JsonParser parser = new JsonParser(new Source(source), mode, null);
        return parser.parseRoot();
    }

    /**
     * Parses a string containing JSON document into a JSON element.
     *  in {@link JsonParsingMode#EXTENDED} mode. Errors that occur during this process, but are intentionally
     *  ignored by the parser, are saved to a list. Root padding is ignored silently;
     *  an arbitrary suffix after the root produces one {@link JsonError.TrailingContent}
     *  warning at its first non-whitespace character. Existing list entries are retained.
     * @param source String containing JSON document
     * @param warnings List where to save errors that occurred during parsing in extended mode
     *  and were intentionally ignored.
     * @return JSON element
     * @throws JsonException If parsing failed, that is, an error occurred that could not be ignored
     */
    public static JsonElement parseString(String source, List<JsonError> warnings) throws JsonException {
        JsonParser parser = new JsonParser(new Source(source), JsonParsingMode.EXTENDED, warnings);
        return parser.parseRoot();
    }

    /** Parses an already opened source without closing it. */
    static JsonElement parseSource(Source source, JsonParsingMode mode) throws JsonException {
        return new JsonParser(source, mode, null).parseRoot();
    }

    /**
     * Parses a Java object and tries to represent it as a JSON element.
     * @param obj Java object
     * @return JSON element
     */
    public static JsonElement parseJavaObject(Object obj) {
        return parseJavaObject(obj, null);
    }

    /**
     * Constructor.
     * @param src Object containing JSON document for parsing
     */
    private JsonParser(Source src, JsonParsingMode mode, List<JsonError> warnings) {
        this.source = src;
        this.lexer = new Lexer(src, mode);
        this.mode = mode;
        this.warnings = warnings;
    }

    /**
     * Parses the root element from the first position.
     * @return JSON element
     * @throws JsonException If parsing fails
     */
    private JsonElement parseRoot() throws JsonException {
        lexer.skipPadding(true);
        Token first = lexer.token();
        if (first instanceof TokenEnd) {
            throw new JsonException(new JsonError.ExpectedRoot(first.location()));
        }
        JsonElement root = parseJsonElement(null, first);
        lexer.skipPadding(true);
        if (source.charValue() != -1) {
            JsonError error = new JsonError.TrailingContent(source.location());
            if (mode != JsonParsingMode.EXTENDED) {
                throw new JsonException(error);
            }
            if (warnings != null) {
                warnings.add(error);
            }
            // The suffix need not be valid JSON. Do not tokenize it or retain it in memory.
            while (source.nextChar() != -1) {
                // Still propagate input/decoding failures while discarding the suffix.
            }
        }
        return root;
    }

    /**
     * Parses the next JSON element from the current position.
     * @param parent The container that will contain the parsed element
     * @param firstToken The first token in the token sequence
     * @return JSON element
     * @throws JsonException If parsing fails
     */
    private JsonElement parseJsonElement(JsonContainer parent, Token firstToken) throws JsonException {
        if (firstToken instanceof TokenEnd) {
            throw new JsonException(new JsonError.UnexpectedEnd(firstToken.location()));
        }
        if (firstToken instanceof TokenLiteral) {
            return ((TokenLiteral) firstToken).toElement(parent);
        }
        if (firstToken instanceof TokenOpeningSquareBracket) {
            return parseJsonArray(parent);
        }
        if (firstToken instanceof TokenOpeningCurlyBracket) {
            return parseJsonObject(parent);
        }
        throw new JsonException(new JsonError.UnexpectedToken(firstToken.location(), firstToken));
    }

    /** Returns a token, including a closing bracket left for an enclosing container. */
    private Token nextToken() throws JsonException {
        if (pendingToken != null) {
            Token token = pendingToken;
            pendingToken = null;
            return token;
        }
        return lexer.token();
    }

    /** Reports a recoverable syntax error, or rejects it in the strict modes. */
    private void recover(JsonError error) throws JsonException {
        if (mode != JsonParsingMode.EXTENDED) {
            throw new JsonException(error);
        }
        if (warnings != null) {
            warnings.add(error);
        }
    }

    /** Whether a closing token belongs to this container. */
    private static boolean matches(JsonContainer container, Token token) {
        return container instanceof JsonArray && token instanceof TokenClosingSquareBracket
                || container instanceof JsonObject && token instanceof TokenClosingCurlyBracket;
    }

    /**
     * Called only at positions where a container may end. At EOF only the root may be
     * completed. A foreign bracket may close an ancestor, but is never swallowed here.
     */
    private boolean closeContainer(JsonContainer container, Token token) throws JsonException {
        if (matches(container, token)) {
            return true;
        }
        char bracket = container instanceof JsonArray ? ']' : '}';
        if (token instanceof TokenEnd) {
            if (container.parent() != null) {
                throw new JsonException(new JsonError.UnexpectedEnd(token.location()));
            }
            recover(new JsonError.MissingClosingBracket(token.location(), bracket));
            return true;
        }
        if (token instanceof TokenClosingSquareBracket || token instanceof TokenClosingCurlyBracket) {
            for (JsonContainer ancestor = container.parent(); ancestor != null; ancestor = ancestor.parent()) {
                if (matches(ancestor, token)) {
                    recover(new JsonError.MissingClosingBracket(token.location(), bracket));
                    pendingToken = token;
                    return true;
                }
            }
            throw new JsonException(new JsonError.UnexpectedToken(token.location(), token));
        }
        return false;
    }

    /** Whether a token can start an array value, without guessing a malformed token's meaning. */
    private static boolean startsValue(Token token) {
        return token instanceof TokenLiteral || token instanceof TokenOpeningSquareBracket
                || token instanceof TokenOpeningCurlyBracket;
    }

    /** Parses array elements and separators without manufacturing values for empty slots. */
    private JsonArray parseJsonArray(JsonContainer parent) throws JsonException {
        JsonArray array = new JsonArray(parent);
        boolean expectValue = true;
        boolean afterComma = false;
        Token token = nextToken();
        while (true) {
            if (matches(array, token) && afterComma && mode == JsonParsingMode.STRICT) {
                throw new JsonException(new JsonError.ExpectedElementAfterComma(token.location()));
            }
            if (closeContainer(array, token)) {
                return array;
            }
            if (token instanceof TokenComma) {
                if (expectValue) {
                    recover(new JsonError.MissingArrayElement(token.location()));
                }
                expectValue = true;
                afterComma = true;
                token = nextToken();
                continue;
            }
            if (!expectValue && startsValue(token)) {
                recover(new JsonError.MissingComma(token.location()));
            }
            array.addChild(parseJsonElement(array, token));
            expectValue = false;
            afterComma = false;
            token = nextToken();
        }
    }

    /** Parses complete key:value pairs; missing keys, colons and values are never invented. */
    private JsonObject parseJsonObject(JsonContainer parent) throws JsonException {
        JsonObject obj = new JsonObject(parent);
        boolean expectPair = true;
        boolean afterComma = false;
        Token token = nextToken();
        while (true) {
            if (matches(obj, token) && afterComma && mode == JsonParsingMode.STRICT) {
                throw new JsonException(new JsonError.ExpectedPairAfterComma(token.location()));
            }
            if (closeContainer(obj, token)) {
                return obj;
            }
            if (!expectPair && token instanceof TokenComma) {
                expectPair = true;
                afterComma = true;
                token = nextToken();
                continue;
            }
            String key;
            if (token instanceof TokenString string) {
                key = string.value();
            } else if (token instanceof TokenIdentifier identifier && mode != JsonParsingMode.STRICT) {
                key = identifier.identifier();
            } else {
                throw new JsonException(new JsonError.ExpectedKey(token.location()));
            }
            if (!expectPair) {
                recover(new JsonError.MissingComma(token.location()));
            }
            token = nextToken();
            if (!(token instanceof TokenColon)) {
                throw new JsonException(new JsonError.ExpectedSeparator(
                        token.location()));
            }
            token = nextToken();
            if (token instanceof TokenEnd) {
                throw new JsonException(new JsonError.ExpectedElementAfterSeparator(token.location()));
            }
            obj.addChild(key, parseJsonElement(obj, token));
            expectPair = false;
            afterComma = false;
            token = nextToken();
        }
    }

    /**
     * Parses a Java object and tries to represent it as a JSON element (internal method).
     * @param obj Java object
     * @param parent The container that will contain the parsed object
     * @return JSON element
     */
    private static JsonElement parseJavaObject(Object obj, JsonContainer parent) {
        if (obj != null) {
            Class<?> cls = obj.getClass();
            if (obj instanceof Number number) {
                return JsonNumber.of(parent, number);
            }
            if (cls == Boolean.class) {
                return new JsonBoolean(parent, (Boolean) obj);
            }
            if (cls == String.class) {
                return new JsonString(parent, (String) obj);
            }
            if (obj instanceof List) {
                JsonArray result = new JsonArray(parent);
                for (Object item : (List<?>) obj) {
                    result.addChild(parseJavaObject(item, result));
                }
                return result;
            }
            if (!cls.isInterface() && !cls.isPrimitive()) {
                JsonObject result = new JsonObject(parent);
                Field[] fields = cls.getDeclaredFields();
                for (Field field : fields) {
                    String propertyName = field.getName();
                    if (field.isAnnotationPresent(JsonProperty.class)) {
                        JsonProperty annotation = field.getAnnotation(JsonProperty.class);
                        propertyName = annotation.name();
                    }
                    Class<?> fieldType = field.getType();
                    try {
                        field.setAccessible(true);
                        if (fieldType == byte.class) {
                            result.addNumber(propertyName, field.getByte(obj));
                        } else if (fieldType == short.class) {
                            result.addNumber(propertyName, field.getShort(obj));
                        } else if (fieldType == int.class) {
                            result.addNumber(propertyName, field.getInt(obj));
                        } else if (fieldType == long.class) {
                            result.addNumber(propertyName, field.getLong(obj));
                        } else if (fieldType == float.class) {
                            result.addNumber(propertyName, field.getFloat(obj));
                        } else if (fieldType == double.class) {
                            result.addNumber(propertyName, field.getDouble(obj));
                        } else if (fieldType == boolean.class) {
                            result.addBoolean(propertyName, field.getBoolean(obj));
                        } else {
                            result.addChild(propertyName, parseJavaObject(field.get(obj), result));
                        }
                    } catch (IllegalAccessException ignored) {
                        return result;
                    }
                }
                return result;
            }
        }
        if (parent == null) {
            return JsonNull.INSTANCE;
        }
        return new JsonNull(parent);
    }
}
