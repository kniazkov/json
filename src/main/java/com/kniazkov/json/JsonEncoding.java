/* Copyright (c) 2026 Ivan Kniazkov */
package com.kniazkov.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/** Byte-level BOM handling before decoders can consume it. */
final class JsonEncoding {
    private JsonEncoding() {}

    private record Mark(String charset, byte[] bytes) {}

    // UTF-32LE must precede UTF-16LE, whose signature is its prefix.
    private static final Mark[] MARKS = {
        new Mark("UTF-32BE", new byte[] {0, 0, (byte) 0xfe, (byte) 0xff}),
        new Mark("UTF-32LE", new byte[] {(byte) 0xff, (byte) 0xfe, 0, 0}),
        new Mark("UTF-8", new byte[] {(byte) 0xef, (byte) 0xbb, (byte) 0xbf}),
        new Mark("UTF-16BE", new byte[] {(byte) 0xfe, (byte) 0xff}),
        new Mark("UTF-16LE", new byte[] {(byte) 0xff, (byte) 0xfe})
    };

    private static Mark mark(byte[] prefix, int offset, int length) {
        for (Mark mark : MARKS) {
            if (length - offset < mark.bytes.length) {
                continue;
            }
            boolean matches = true;
            for (int i = 0; i < mark.bytes.length; i++) {
                if (prefix[offset + i] != mark.bytes[i]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return mark;
            }
        }
        return null;
    }

    private static String name(Charset charset) {
        String name = charset.name().toUpperCase(Locale.ROOT);
        if (name.startsWith("X-UTF-") && name.endsWith("-BOM")) {
            return name.substring(2, name.length() - 4);
        }
        return name;
    }

    static Charset explicitCharset(Charset charset) {
        String name = name(charset);
        if (name.equals("UTF-16")) {
            return StandardCharsets.UTF_16BE;
        }
        if (name.equals("UTF-32")) {
            return Charset.forName("UTF-32BE");
        }
        return name.equals(charset.name().toUpperCase(Locale.ROOT))
                ? charset
                : Charset.forName(name);
    }

    static byte[] outputMark(Charset charset) {
        String name = explicitCharset(charset).name();
        for (Mark mark : MARKS) {
            if (mark.charset.equalsIgnoreCase(name)) {
                return mark.bytes.clone();
            }
        }
        throw new IllegalArgumentException("BOM is not supported for " + charset.name());
    }

    static Reader decode(InputStream input, Charset charset, JsonBomPolicy policy)
            throws IOException {
        Objects.requireNonNull(input, "stream");
        PushbackInputStream stream = new PushbackInputStream(input, 8);
        byte[] prefix = stream.readNBytes(8);
        Mark found = mark(prefix, 0, prefix.length);
        int consumed = 0;
        Charset actual = explicitCharset(charset);
        if (found != null) {
            if (policy == JsonBomPolicy.FORBID) {
                throw new IOException("Initial BOM is forbidden");
            }
            String selected = name(charset);
            boolean compatible =
                    selected.equals(found.charset)
                            || selected.equals("UTF-16") && found.charset.startsWith("UTF-16")
                            || selected.equals("UTF-32") && found.charset.startsWith("UTF-32");
            if (!compatible) {
                throw new IOException(
                        "BOM " + found.charset + " conflicts with charset " + charset.name());
            }
            actual = Charset.forName(found.charset);
            consumed = found.bytes.length;
            if (mark(prefix, consumed, prefix.length) != null) {
                throw new IOException("Duplicate initial BOM");
            }
        } else if (policy == JsonBomPolicy.REQUIRE) {
            throw new IOException("Initial BOM is required");
        }
        stream.unread(prefix, consumed, prefix.length - consumed);
        return new InputStreamReader(
                stream,
                actual.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT));
    }

    static Reader characters(Reader reader, JsonBomPolicy policy) {
        Objects.requireNonNull(reader, "reader");
        return new Reader() {
            boolean first = true;

            @Override
            public int read() throws IOException {
                int ch = reader.read();
                if (!first) {
                    return ch;
                }
                first = false;
                if (ch == 0xfeff) {
                    if (policy == JsonBomPolicy.FORBID) {
                        throw new IOException("Initial BOM is forbidden");
                    }
                    ch = reader.read();
                    if (ch == 0xfeff) {
                        throw new IOException("Duplicate initial BOM");
                    }
                } else if (policy == JsonBomPolicy.REQUIRE) {
                    throw new IOException("Initial BOM is required");
                }
                return ch;
            }

            @Override
            public int read(char[] buffer, int offset, int length) throws IOException {
                Objects.checkFromIndexSize(offset, length, buffer.length);
                if (length == 0) {
                    return 0;
                }
                if (!first) {
                    return reader.read(buffer, offset, length);
                }
                int ch = read();
                if (ch == -1) {
                    return -1;
                }
                buffer[offset] = (char) ch;
                return 1;
            }

            @Override
            public void close() {
                /* Caller owns the reader. */
            }
        };
    }
}
