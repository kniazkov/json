/*
 * Copyright (c) 2023 Ivan Kniazkov
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

    /**
     * Parsing mode.
     */
    private final JsonParsingMode mode;

    /**
     * Parses a string containing JSON document into a JSON element.
     * @param source String containing JSON document
     * @return JSON element
     * @throws JsonException If parsing fails
     */
    public static JsonElement parseString(String source) throws JsonException {
        return parseString(source, JsonParsingMode.EXTENDED);
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
    private JsonParser(Source src, JsonParsingMode mode) {
        this.lexer = new Lexer(src, mode);
        this.mode = mode;
    }

    /**
     * Parses the root element from the first position.
     * @return JSON element
     * @throws JsonException If parsing fails
     */
    private JsonElement parseRoot() throws JsonException {
        return parseJsonElement(null, lexer.getToken());
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
        return null;
    }

    /**
     * Parses a sequence of tokens as a JSON array.
     * @param parent The container that will contain the parsed array
     * @return JSON array
     * @throws JsonException If parsing fails
     */
    private JsonArray parseJsonArray(JsonContainer parent) throws JsonException {
        final JsonArray array = new JsonArray(parent);
        Token token = lexer.getToken();
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

            JsonElement child = parseJsonElement(array, token);
            if (child != null) {
                array.addChild(child);
                expectedElement = false;
                token = lexer.getToken();
                if (token instanceof TokenComma) {
                    expectedElement = true;
                    token = lexer.getToken();
                }
            }
        } while(expectedElement);
        return array;
    }

    /**
     * Parses a sequence of tokens as a JSON object.
     * @param parent The container that will contain the parsed object
     * @return JSON object
     * @throws JsonException If parsing fails
     */
    private JsonObject parseJsonObject(JsonContainer parent) throws JsonException {
        final JsonObject obj = new JsonObject(parent);
        Token token = lexer.getToken();
        boolean expectedElement = false;
        do {
            if (token instanceof TokenClosingCurlyBracket) {
                if (expectedElement && mode == JsonParsingMode.STRICT) {
                    throw new JsonException(
                            new JsonError.ExpectedPairAfterComma(token.getLocation())
                    );
                }
                return obj;
            }
            String key = null;
            if (token instanceof TokenString) {
                key = ((TokenString)token).getValue();
            }
            else if (token instanceof TokenIdentifier && mode != JsonParsingMode.STRICT) {
                key = ((TokenIdentifier)token).getIdentifier();
            }
            if (key == null) {
                throw new JsonException(new JsonError.ExpectedKey(token.getLocation()));
            }

            token = lexer.getToken();
            if (!(token instanceof TokenColon)) {
                throw new JsonException(new JsonError.ExpectedSeparator(token.getLocation()));
            }

            token = lexer.getToken();
            JsonElement value = parseJsonElement(obj, token);
            if (value != null) {
                obj.addChild(key, value);
                expectedElement = false;
                token = lexer.getToken();
                if (token instanceof TokenComma) {
                    expectedElement = true;
                    token = lexer.getToken();
                }
            } else {
                throw new JsonException(new JsonError.ExpectedElementAfterSeparator(token.getLocation()));
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
            if (cls == Byte.class) {
                return new JsonNumber(parent, (Byte) obj);
            }
            if (cls == Short.class) {
                return new JsonNumber(parent, (Short) obj);
            }
            if (cls == Integer.class) {
                return new JsonNumber(parent, (Integer) obj);
            }
            if (cls == Long.class) {
                return new JsonNumber(parent, (Long) obj);
            }
            if (cls == Float.class) {
                return new JsonNumber(parent, (Float) obj);
            }
            if (cls == Double.class) {
                return new JsonNumber(parent, (Double) obj);
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
                    String fieldName = field.getName();
                    Class<?> fieldType = field.getType();
                    try {
                        field.setAccessible(true);
                        if (fieldType == byte.class) {
                            result.addNumber(fieldName, field.getByte(obj));
                        } else if (fieldType == short.class) {
                            result.addNumber(fieldName, field.getShort(obj));
                        } else if (fieldType == int.class) {
                            result.addNumber(fieldName, field.getInt(obj));
                        } else if (fieldType == long.class) {
                            result.addNumber(fieldName, field.getLong(obj));
                        } else if (fieldType == float.class) {
                            result.addNumber(fieldName, field.getFloat(obj));
                        } else if (fieldType == double.class) {
                            result.addNumber(fieldName, field.getDouble(obj));
                        } else if (fieldType == boolean.class) {
                            result.addBoolean(fieldName, field.getBoolean(obj));
                        } else {
                            result.addChild(fieldName, parseJavaObject(field.get(obj), result));
                        }
                    } catch (IllegalAccessException ignored) {
                        return result;
                    }
                }
                return result;
            }
        }
        if (parent == null) {
            return JsonNull.instance;
        }
        return new JsonNull(parent);
    }
}
