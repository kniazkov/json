/* Copyright (c) 2026 Ivan Kniazkov */
package com.kniazkov.json;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

/** Reusable configurable serializer. Instances are not thread-safe. */
public final class JsonSerializer {
    private Charset charset = StandardCharsets.UTF_8;
    private String indentation;
    private boolean writeBom;
    private boolean escapeNonAscii;

    /** Creates a compact UTF-8 serializer without a BOM or non-ASCII escaping. */
    public JsonSerializer() {}

    /**
     * Updates this instance configuration.
     *
     * @param charset Byte-output encoding
     * @return this serializer
     */
    public JsonSerializer setCharset(Charset charset) {
        Objects.requireNonNull(charset, "charset");
        JsonEncoding.explicitCharset(charset).newEncoder();
        if (writeBom) {
            JsonEncoding.outputMark(charset);
        }
        this.charset = charset;
        return this;
    }

    /**
     * Updates this instance configuration.
     *
     * @param indentation JSON whitespace per level, or null for compact output
     * @return this serializer
     */
    public JsonSerializer setIndentation(String indentation) {
        if (indentation != null) {
            for (int i = 0; i < indentation.length(); i++) {
                if (" \t\r\n".indexOf(indentation.charAt(i)) < 0) {
                    throw new IllegalArgumentException(
                            "Indentation must contain JSON whitespace only");
                }
            }
        }
        this.indentation = indentation;
        return this;
    }

    /**
     * Updates this instance configuration.
     *
     * @param enabled Whether byte output starts with one BOM
     * @return this serializer
     */
    public JsonSerializer setWriteBom(boolean enabled) {
        if (enabled) {
            JsonEncoding.outputMark(charset);
        }
        writeBom = enabled;
        return this;
    }

    /**
     * Updates this instance configuration.
     *
     * @param enabled Whether non-ASCII characters in keys/strings use Unicode escapes
     * @return this serializer
     */
    public JsonSerializer setEscapeNonAscii(boolean enabled) {
        escapeNonAscii = enabled;
        return this;
    }

    /**
     * Serializes one value using the selected configuration.
     *
     * @param value Java value or existing JSON tree
     * @return JSON text without a BOM
     */
    public String serialize(Object value) {
        JsonElement tree =
                value instanceof JsonElement element ? element : JavaSerializer.convert(value);
        StringBuilder result = new StringBuilder();
        if (tree instanceof JsonContainer container) {
            TreeTraversal.write(container, result, indentation, 0, escapeNonAscii);
        } else if (tree instanceof JsonString) {
            result.append('"')
                    .append(Utils.escapeEntities(tree.stringValue(), escapeNonAscii))
                    .append('"');
        } else {
            result.append(tree.toString());
        }
        return result.toString();
    }

    /**
     * Serializes one value using the selected configuration.
     *
     * @param value Java value or JSON tree
     * @param writer Destination, flushed but not closed
     * @throws IOException If writing/flushing fails
     */
    public void serialize(Object value, Writer writer) throws IOException {
        Objects.requireNonNull(writer, "writer");
        writer.write(serialize(value));
        writer.flush();
    }

    /**
     * Serializes one value using the selected configuration.
     *
     * @param value Java value or JSON tree
     * @param stream Destination, flushed but not closed
     * @throws IOException If encoding/writing fails
     */
    public void serialize(Object value, OutputStream stream) throws IOException {
        Objects.requireNonNull(stream, "stream");
        write(encode(value), stream);
    }

    /**
     * Serializes one value using the selected configuration.
     *
     * @param value Java value or JSON tree
     * @param file Destination, opened/truncated only after successful encoding
     * @throws IOException If encoding/file output fails
     */
    public void serialize(Object value, File file) throws IOException {
        Objects.requireNonNull(file, "file");
        ByteBuffer bytes = encode(value);
        try (OutputStream stream = Files.newOutputStream(file.toPath())) {
            write(bytes, stream);
        }
    }

    private ByteBuffer encode(Object value) throws CharacterCodingException {
        return JsonEncoding.explicitCharset(charset)
                .newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(serialize(value)));
    }

    private void write(ByteBuffer bytes, OutputStream stream) throws IOException {
        if (writeBom) {
            stream.write(JsonEncoding.outputMark(charset));
        }
        byte[] buffer = new byte[Math.min(8192, bytes.remaining())];
        while (bytes.hasRemaining()) {
            int size = Math.min(buffer.length, bytes.remaining());
            bytes.get(buffer, 0, size);
            stream.write(buffer, 0, size);
        }
        stream.flush();
    }
}
