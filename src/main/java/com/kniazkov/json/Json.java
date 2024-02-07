/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.io.File;

/**
 * Class that combines all methods for parsing and serialising JSON documents.
 */
public class Json {
    /**
     * Parses a JSON document, represented as a string, into a tree.
     * @param source JSON document
     * @return JSON element, root of the tree
     * @throws JsonException If parsing fails
     */
    public static JsonElement parse(String source) throws JsonException {
        return JsonParser.parseString(source);
    }

    /**
     * Parses a JSON document from a file into a tree.
     * @param file File containing JSON document
     * @return JSON element, root of the tree, or {@code null} if the file cannot be read for some reason
     * @throws JsonException If parsing fails
     */
    public static JsonElement parse(File file) throws JsonException {
        final String source = Utils.readFileToString(file);
        if (source == null) {
            return null;
        }
        return parse(source);
    }

    /**
     * Parses a JSON document, represented as a string, into a tree.
     * @param source JSON document
     * @param mode Parsing mode defining the allowed JSON syntax
     * @return JSON element, root of the tree
     * @throws JsonException If parsing fails
     */
    public static JsonElement parse(String source, JsonParsingMode mode) throws JsonException {
        return JsonParser.parseString(source, mode);
    }

    /**
     * Parses a JSON document from a file into a tree.
     * @param file File containing JSON document
     * @param mode Parsing mode defining the allowed JSON syntax
     * @return JSON element, root of the tree, or {@code null} if the file cannot be read for some reason
     * @throws JsonException If parsing fails
     */
    public static JsonElement parse(File file, JsonParsingMode mode) throws JsonException {
        final String source = Utils.readFileToString(file);
        if (source == null) {
            return null;
        }
        return parse(source, mode);
    }

    /**
     * Parses a JSON document, represented as a string, into Java object of specified type.
     * @param source JSON document
     * @param type The type descriptor
     * @return Object containing parsed data or {@code null} if document can't be represented
     *   as an object of specified type
     * @param <T> The type
     * @throws JsonException If parsing fails
     */
    public static <T> T parse(String source, Class<T> type) throws JsonException {
        return parse(source).toJavaObject(type);
    }

    /**
     * Parses a JSON document from a file into Java object of specified type.
     * @param file File containing JSON document
     * @param type The type descriptor
     * @return Object containing parsed data or {@code null} the file cannot be read for some reason
     *   or document can't be represented as an object of specified type
     * @param <T> The type
     * @throws JsonException If parsing fails
     */
    public static <T> T parse(File file, Class<T> type) throws JsonException {
        final String source = Utils.readFileToString(file);
        if (source == null) {
            return null;
        }
        return parse(source).toJavaObject(type);
    }

    /**
     * Parses a JSON document, represented as a string, into Java object of specified type.
     * @param source JSON document
     * @param type The type descriptor
     * @param mode Parsing mode defining the allowed JSON syntax
     * @return Object containing parsed data or {@code null} if document can't be represented
     *   as an object of specified type
     * @param <T> The type
     * @throws JsonException If parsing fails
     */
    public static <T> T parse(String source, Class<T> type, JsonParsingMode mode) throws JsonException {
        return parse(source, mode).toJavaObject(type);
    }

    /**
     * Parses a JSON document from a file into Java object of specified type.
     * @param file File containing JSON document
     * @param type The type descriptor
     * @param mode Parsing mode defining the allowed JSON syntax
     * @return Object containing parsed data or {@code null} the file cannot be read for some reason
     *   or document can't be represented as an object of specified type
     * @param <T> The type
     * @throws JsonException If parsing fails
     */
    public static <T> T parse(File file, Class<T> type, JsonParsingMode mode) throws JsonException {
        final String source = Utils.readFileToString(file);
        if (source == null) {
            return null;
        }
        return parse(source, mode).toJavaObject(type);
    }

    /**
     * Parses a Java object and tries to represent it as string containing JSON document
     * @param obj Java object
     * @return String containing JSON document
     */
    public static String serialize(Object obj) {
        return JsonParser.parseJavaObject(obj).toString();
    }

    /**
     * Parses a Java object and tries to represent it as string containing JSON document,
     *   with line breaks and indentation of nested elements making it easier to read
     * @param obj Java object
     * @param indentation Symbols forming an indentation
     * @return String containing JSON document
     */
    public static String serialize(Object obj, String indentation) {
        return JsonParser.parseJavaObject(obj).toText(indentation);
    }
}
