/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Reusable configurable parser. Instances are not thread-safe. */
public final class JsonParser {
    private Charset charset = StandardCharsets.UTF_8;
    private JsonParsingMode mode = JsonParsingMode.EXTENDED;
    private JsonBomPolicy bomPolicy = JsonBomPolicy.ALLOW;
    private String sourceName;
    private boolean collectWarnings;
    private List<JsonError> warnings = List.of();

    /** Creates a UTF-8, EXTENDED parser accepting an optional initial BOM. */
    public JsonParser() {}

    /**
     * Updates this instance configuration.
     *
     * @param charset Byte-input encoding
     * @return this parser
     */
    public JsonParser setCharset(Charset charset) {
        Objects.requireNonNull(charset, "charset").newDecoder();
        this.charset = charset;
        return this;
    }

    /**
     * Updates this instance configuration.
     *
     * @param mode Syntax mode
     * @return this parser
     */
    public JsonParser setMode(JsonParsingMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
        return this;
    }

    /**
     * Updates this instance configuration.
     *
     * @param policy Initial BOM policy
     * @return this parser
     */
    public JsonParser setBomPolicy(JsonBomPolicy policy) {
        bomPolicy = Objects.requireNonNull(policy, "policy");
        return this;
    }

    /**
     * Updates this instance configuration.
     *
     * @param name Diagnostic name, or null to use file paths/unnamed input
     * @return this parser
     */
    public JsonParser setSourceName(String name) {
        sourceName = name;
        return this;
    }

    /**
     * Updates this instance configuration.
     *
     * @param enabled Whether to collect warnings for each invocation
     * @return this parser
     */
    public JsonParser setCollectWarnings(boolean enabled) {
        collectWarnings = enabled;
        return this;
    }

    /**
     * Returns the diagnostics from the last invocation.
     *
     * @return Immutable snapshot of the last parse's warnings, also after a failed parse
     */
    public List<JsonError> warnings() {
        return warnings;
    }

    /**
     * Parses one document using the selected configuration.
     *
     * @param text JSON text
     * @return Parsed root
     * @throws JsonException If parsing fails
     */
    public JsonElement parse(String text) throws JsonException {
        return parse(new StringReader(text));
    }

    /**
     * Parses one document using the selected configuration.
     *
     * @param reader Character input, not closed
     * @return Parsed root
     * @throws JsonException If reading/parsing fails
     */
    public JsonElement parse(Reader reader) throws JsonException {
        List<JsonError> diagnostics = begin();
        try {
            return read(JsonEncoding.characters(reader, bomPolicy), sourceName, diagnostics);
        } finally {
            finish(diagnostics);
        }
    }

    /**
     * Parses one document using the selected configuration.
     *
     * @param stream Byte input, not closed
     * @return Parsed root
     * @throws JsonException If input/parsing fails
     */
    public JsonElement parse(InputStream stream) throws JsonException {
        List<JsonError> diagnostics = begin();
        try {
            return bytes(stream, sourceName, diagnostics);
        } finally {
            finish(diagnostics);
        }
    }

    /**
     * Parses one document using the selected configuration.
     *
     * @param file Input file, opened and closed by this call
     * @return Parsed root
     * @throws JsonException If input/parsing fails
     */
    public JsonElement parse(File file) throws JsonException {
        List<JsonError> diagnostics = begin();
        String name = sourceName == null ? file.getPath() : sourceName;
        try (InputStream stream = Files.newInputStream(file.toPath())) {
            return bytes(stream, name, diagnostics);
        } catch (IOException cause) {
            throw inputFailure(name, cause);
        } finally {
            finish(diagnostics);
        }
    }

    private List<JsonError> begin() {
        warnings = List.of();
        return collectWarnings ? new ArrayList<>() : null;
    }

    private void finish(List<JsonError> diagnostics) {
        warnings = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    private JsonElement read(Reader reader, String name, List<JsonError> diagnostics)
            throws JsonException {
        return new ParsingSession(new Source(reader, name), mode, diagnostics).parseRoot();
    }

    private JsonElement bytes(InputStream stream, String name, List<JsonError> diagnostics)
            throws JsonException {
        try {
            return read(JsonEncoding.decode(stream, charset, bomPolicy), name, diagnostics);
        } catch (IOException cause) {
            throw inputFailure(name, cause);
        }
    }

    private static JsonException inputFailure(String name, IOException cause) {
        return new JsonException(new JsonError.InputFailure(new JsonLocation(1, 1, name)), cause);
    }

    /**
     * Parses one document using the selected configuration.
     *
     * @param source JSON text
     * @return Parsed root
     * @throws JsonException If parsing fails
     */
    public static JsonElement parseString(String source) throws JsonException {
        return new JsonParser().parse(source);
    }

    /**
     * Parses one document using the selected configuration.
     *
     * @param source JSON text
     * @param mode Syntax mode
     * @return Parsed root
     * @throws JsonException If parsing fails
     */
    public static JsonElement parseString(String source, JsonParsingMode mode)
            throws JsonException {
        return new JsonParser().setMode(mode).parse(source);
    }

    /**
     * Parses one document using the selected configuration.
     *
     * @param source JSON text
     * @param warnings Destination retaining existing entries
     * @return Parsed root
     * @throws JsonException If parsing fails
     */
    public static JsonElement parseString(String source, List<JsonError> warnings)
            throws JsonException {
        return parseString(source, JsonParsingMode.EXTENDED, warnings);
    }

    /**
     * Parses one document using the selected configuration.
     *
     * @param source JSON text
     * @param mode Syntax mode
     * @param warnings Optional destination retaining existing entries
     * @return Parsed root
     * @throws JsonException If parsing fails
     */
    public static JsonElement parseString(
            String source, JsonParsingMode mode, List<JsonError> warnings) throws JsonException {
        JsonParser parser = new JsonParser().setMode(mode).setCollectWarnings(warnings != null);
        try {
            return parser.parse(source);
        } finally {
            if (warnings != null) {
                warnings.addAll(parser.warnings());
            }
        }
    }

    /**
     * Converts a Java value to a JSON tree.
     *
     * @param obj Java value
     * @return JSON tree
     */
    public static JsonElement parseJavaObject(Object obj) {
        return JavaSerializer.convert(obj);
    }
}
