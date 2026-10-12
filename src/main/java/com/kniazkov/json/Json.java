/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Class that combines all methods for parsing and serialising JSON documents. */
public class Json {
    /** Creates a facade instance; parsing and serialization methods are static. */
    public Json() {}

    /**
     * Parses a JSON document, represented as a string, into a tree.
     *
     * @param source JSON document
     * @return JSON element, root of the tree
     * @throws JsonException If parsing or input fails
     */
    public static JsonElement parse(String source) throws JsonException {
        return JsonParser.parseString(source);
    }

    /**
     * Parses a JSON document from a file into a tree.
     *
     * @param file File containing JSON document
     * @return JSON element, root of the tree
     * @throws JsonException If parsing or input fails
     */
    public static JsonElement parse(File file) throws JsonException {
        return parse(file, JsonParsingMode.EXTENDED);
    }

    /**
     * Parses a JSON document, represented as a string, into a tree.
     *
     * @param source JSON document
     * @param mode Parsing mode defining the allowed JSON syntax
     * @return JSON element, root of the tree
     * @throws JsonException If parsing or input fails
     */
    public static JsonElement parse(String source, JsonParsingMode mode) throws JsonException {
        return JsonParser.parseString(source, mode);
    }

    /**
     * Parses a JSON document from a file into a tree.
     *
     * @param file File containing JSON document
     * @param mode Parsing mode defining the allowed JSON syntax
     * @return JSON element, root of the tree
     * @throws JsonException If parsing or input fails
     */
    public static JsonElement parse(File file, JsonParsingMode mode) throws JsonException {
        return parse(file, StandardCharsets.UTF_8, mode);
    }

    /**
     * Parses a JSON document, represented as a string, into Java object of specified type.
     *
     * @param source JSON document
     * @param type The type descriptor
     * @param <T> The type
     * @return Converted value, or empty for JSON null and a reference target
     * @throws JsonException If parsing or input fails
     */
    public static <T> Optional<T> parse(String source, Class<T> type) throws JsonException {
        java.util.Objects.requireNonNull(type, "type");
        return Optional.ofNullable(parse(source).toJavaObject(type));
    }

    /**
     * Parses a JSON document from a file into Java object of specified type.
     *
     * @param file File containing JSON document
     * @param type The type descriptor
     * @param <T> The type
     * @return Converted value, or empty for JSON null and a reference target
     * @throws JsonException If parsing or input fails
     */
    public static <T> Optional<T> parse(File file, Class<T> type) throws JsonException {
        java.util.Objects.requireNonNull(type, "type");
        return Optional.ofNullable(parse(file).toJavaObject(type));
    }

    /**
     * Parses a JSON document, represented as a string, into Java object of specified type.
     *
     * @param source JSON document
     * @param type The type descriptor
     * @param mode Parsing mode defining the allowed JSON syntax
     * @param <T> The type
     * @return Converted value, or empty for JSON null and a reference target
     * @throws JsonException If parsing or input fails
     */
    public static <T> Optional<T> parse(String source, Class<T> type, JsonParsingMode mode)
            throws JsonException {
        java.util.Objects.requireNonNull(type, "type");
        return Optional.ofNullable(parse(source, mode).toJavaObject(type));
    }

    /**
     * Parses a JSON document from a file into Java object of specified type.
     *
     * @param file File containing JSON document
     * @param type The type descriptor
     * @param mode Parsing mode defining the allowed JSON syntax
     * @param <T> The type
     * @return Converted value, or empty for JSON null and a reference target
     * @throws JsonException If parsing or input fails
     */
    public static <T> Optional<T> parse(File file, Class<T> type, JsonParsingMode mode)
            throws JsonException {
        java.util.Objects.requireNonNull(type, "type");
        return Optional.ofNullable(parse(file, mode).toJavaObject(type));
    }

    /**
     * Parses a named string.
     *
     * @param source JSON document
     * @param sourceName Diagnostic name, or null for unnamed input
     * @param mode Parsing mode
     * @return Root element
     * @throws JsonException If parsing fails
     */
    public static JsonElement parse(String source, String sourceName, JsonParsingMode mode)
            throws JsonException {
        return parse(new StringReader(source), sourceName, mode);
    }

    /**
     * Parses a reader in extended mode without closing it.
     *
     * @param reader Character input
     * @return Root element
     * @throws JsonException If parsing or input fails
     */
    public static JsonElement parse(Reader reader) throws JsonException {
        return parse(reader, JsonParsingMode.EXTENDED);
    }

    /**
     * Parses a reader without closing it. Buffering may read ahead.
     *
     * @param reader Character input
     * @param mode Parsing mode
     * @return Root element
     * @throws JsonException If parsing or input fails
     */
    public static JsonElement parse(Reader reader, JsonParsingMode mode) throws JsonException {
        return parse(reader, null, mode);
    }

    /**
     * Parses a named reader without closing it. Buffering may read ahead.
     *
     * @param reader Character input
     * @param sourceName Diagnostic name, or null for unnamed input
     * @param mode Parsing mode
     * @return Root element
     * @throws JsonException If parsing or input fails
     */
    public static JsonElement parse(Reader reader, String sourceName, JsonParsingMode mode)
            throws JsonException {
        return new JsonParser().setMode(mode).setSourceName(sourceName).parse(reader);
    }

    /**
     * Parses UTF-8 input in extended mode without closing it.
     *
     * @param stream Byte input
     * @return Root element
     * @throws JsonException If parsing or input fails
     */
    public static JsonElement parse(InputStream stream) throws JsonException {
        return parse(stream, JsonParsingMode.EXTENDED);
    }

    /**
     * Parses UTF-8 input without closing it. Invalid byte sequences are reported.
     *
     * @param stream Byte input
     * @param mode Parsing mode
     * @return Root element
     * @throws JsonException If parsing or input fails
     */
    public static JsonElement parse(InputStream stream, JsonParsingMode mode) throws JsonException {
        return parse(stream, StandardCharsets.UTF_8, mode);
    }

    /**
     * Parses bytes in an explicit encoding without closing the stream.
     *
     * @param stream Byte input
     * @param charset Input encoding
     * @param mode Parsing mode
     * @return Root element
     * @throws JsonException If parsing or input fails
     */
    public static JsonElement parse(InputStream stream, Charset charset, JsonParsingMode mode)
            throws JsonException {
        return parse(stream, null, charset, mode);
    }

    /**
     * Parses named bytes without closing the stream. Invalid byte sequences are reported; buffering
     * may read ahead beyond the parsed value.
     *
     * @param stream Byte input
     * @param sourceName Diagnostic name, or null for unnamed input
     * @param charset Input encoding
     * @param mode Parsing mode
     * @return Root element
     * @throws JsonException If parsing or input fails
     */
    public static JsonElement parse(
            InputStream stream, String sourceName, Charset charset, JsonParsingMode mode)
            throws JsonException {
        return new JsonParser()
                .setMode(mode)
                .setCharset(charset)
                .setSourceName(sourceName)
                .parse(stream);
    }

    /**
     * Parses a file in an explicit encoding and closes the owned input on every exit path. The
     * diagnostic source name is the supplied file path. Invalid bytes are reported.
     *
     * @param file JSON file
     * @param charset Input encoding
     * @param mode Parsing mode
     * @return Root element
     * @throws JsonException If opening, parsing, reading, decoding or closing fails
     */
    public static JsonElement parse(File file, Charset charset, JsonParsingMode mode)
            throws JsonException {
        return new JsonParser().setMode(mode).setCharset(charset).parse(file);
    }

    /**
     * Parses a Java object and tries to represent it as string containing JSON document.
     *
     * @param obj Java object
     * @return String containing JSON document
     */
    public static String serialize(Object obj) {
        return new JsonSerializer().serialize(obj);
    }

    /**
     * Parses a Java object and tries to represent it as string containing JSON document, with line
     * breaks and indentation of nested elements making it easier to read.
     *
     * @param obj Java object
     * @param indentation Symbols forming an indentation
     * @return String containing JSON document
     */
    public static String serialize(Object obj, String indentation) {
        return new JsonSerializer().setIndentation(indentation).serialize(obj);
    }
}
