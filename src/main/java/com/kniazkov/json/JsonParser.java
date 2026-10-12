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
        if (first == null) {
            throw new JsonException(new JsonError.ExpectedRoot(source.location()));
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

    /**
     * Parses a sequence of tokens as a JSON array.
     * @param parent The container that will contain the parsed array
     * @return JSON array
     * @throws JsonException If parsing fails
     */
    private JsonArray parseJsonArray(JsonContainer parent) throws JsonException {
        final JsonArray array = new JsonArray(parent);
        Token token = lexer.token();
        if (token instanceof TokenClosingSquareBracket) {
            // empty
            return array;
        }
        while (true) {
            JsonElement child = parseJsonElement(array, token);
            assert child != null;
            array.addChild(child);
            token = lexer.token();
            if (token instanceof TokenClosingSquareBracket) {
                return array;
            }
            if (token instanceof TokenComma) {
                token = lexer.token();
                if (token instanceof TokenClosingSquareBracket) {
                    // extra comma
                    if (mode == JsonParsingMode.STRICT) {
                        throw new JsonException(
                                new JsonError.ExpectedElementAfterComma(token.location())
                        );
                    }
                    return array;
                }
            }
            else {
                JsonError error = new JsonError.MissingComma(token.location());
                if (mode != JsonParsingMode.EXTENDED) {
                    throw new JsonException(error);
                }
                if (warnings != null) {
                    warnings.add(error);
                }
            }
        }
    }

    /**
     * Parses a sequence of tokens as a JSON object.
     * @param parent The container that will contain the parsed object
     * @return JSON object
     * @throws JsonException If parsing fails
     */
    private JsonObject parseJsonObject(JsonContainer parent) throws JsonException {
        final JsonObject obj = new JsonObject(parent);
        Token token = lexer.token();
        boolean expectedElement = false;
        do {
            if (token instanceof TokenClosingCurlyBracket) {
                if (expectedElement && mode == JsonParsingMode.STRICT) {
                    throw new JsonException(
                            new JsonError.ExpectedPairAfterComma(token.location())
                    );
                }
                return obj;
            }
            String key = null;
            if (token instanceof TokenString) {
                key = ((TokenString)token).value();
            }
            else if (token instanceof TokenIdentifier && mode != JsonParsingMode.STRICT) {
                key = ((TokenIdentifier)token).identifier();
            }
            if (key == null) {
                throw new JsonException(new JsonError.ExpectedKey(token.location()));
            }

            token = lexer.token();
            if (!(token instanceof TokenColon)) {
                throw new JsonException(new JsonError.ExpectedSeparator(token.location()));
            }

            token = lexer.token();
            JsonElement value = parseJsonElement(obj, token);
            if (value != null) {
                obj.addChild(key, value);
                expectedElement = false;
                token = lexer.token();
                if (token instanceof TokenComma) {
                    expectedElement = true;
                    token = lexer.token();
                }
            } else {
                throw new JsonException(new JsonError.ExpectedElementAfterSeparator(token.location()));
            }
        } while(expectedElement);
        return obj;
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
