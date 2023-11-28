package com.kniazkov.json;

import java.io.File;

/**
 * Class that combines all methods for parsing and serialising JSON documents..
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
        return JsonParser.parseString(source);
    }

    /**
     * Parses JSON document into Java object of specified type.
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
     * Parses a Java object and tries to represent it as string containing JSON document
     * @param obj Java object
     * @return String containing JSON document
     */
    public static String serialize(Object obj) {
        return JsonParser.parseJavaObject(obj).toString();
    }
}
