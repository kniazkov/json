package com.kniazkov.json;

/**
 * Class that contains the basic methods for parsing and serializing JSON documents.
 */
public class Json {
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
        return JsonParser.parseString(source).toObject(type);
    }
}
