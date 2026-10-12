/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Assert;
import org.junit.Test;

public class JsonInputTest {
    private static final JsonParsingMode STRICT = JsonParsingMode.STRICT;

    private static class TrackedReader extends Reader {
        private final String text;
        private final IOException failure;
        private int index;
        private boolean closed;

        TrackedReader(String text, IOException failure) {
            this.text = text;
            this.failure = failure;
        }

        @Override
        public int read(char[] data, int off, int len) throws IOException {
            if (index == text.length()) {
                if (failure != null) {
                    throw failure;
                }
                return -1;
            }
            data[off] = text.charAt(index++);
            return 1;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class TrackedStream extends ByteArrayInputStream {
        private boolean closed;

        TrackedStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    @Test
    public void readerOwnershipAndReadFailures() throws Exception {
        TrackedReader good = new TrackedReader("[1,2]", null);
        Assert.assertEquals("[1, 2]", Json.parse(good).toString());
        Assert.assertFalse(good.closed);
        TrackedReader badSyntax = new TrackedReader("@", null);
        JsonException syntax =
                Assert.assertThrows(
                        JsonException.class, () -> Json.parse(badSyntax, "response", STRICT));
        Assert.assertEquals("response, 1:1", syntax.error().location().toString());
        Assert.assertFalse(badSyntax.closed);
        IOException cause = new IOException("connection lost");
        for (String prefix : new String[] {"", "[1,\r\n"}) {
            TrackedReader broken = new TrackedReader(prefix, cause);
            JsonException error =
                    Assert.assertThrows(
                            JsonException.class, () -> Json.parse(broken, "response", STRICT));
            Assert.assertSame(cause, error.getCause());
            Assert.assertEquals(
                    prefix.isEmpty() ? "response, 1:1" : "response, 2:1",
                    error.error().location().toString());
            Assert.assertFalse(broken.closed);
        }
    }

    @Test
    public void streamsDecodeStrictlyAndRemainOpen() throws Exception {
        TrackedStream utf8 = new TrackedStream("\"Привет😀\"".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals("Привет😀", Json.parse(utf8).stringValue());
        Assert.assertFalse(utf8.closed);
        TrackedStream utf16 = new TrackedStream("42".getBytes(StandardCharsets.UTF_16LE));
        Assert.assertEquals(42, Json.parse(utf16, StandardCharsets.UTF_16LE, STRICT).intValue());
        Assert.assertFalse(utf16.closed);
        for (byte[] bytes :
                new byte[][] {{'"', (byte) 0xc3, '"'}, {'"', (byte) 0xe2, (byte) 0x82}}) {
            TrackedStream broken = new TrackedStream(bytes);
            JsonException error =
                    Assert.assertThrows(
                            JsonException.class,
                            () -> Json.parse(broken, "wire", StandardCharsets.UTF_8, STRICT));
            Assert.assertTrue(error.getCause() instanceof CharacterCodingException);
            Assert.assertEquals("wire", error.error().location().sourceName().orElseThrow());
            Assert.assertFalse(broken.closed);
        }
        TrackedStream syntax = new TrackedStream(new byte[] {'@'});
        Assert.assertThrows(JsonException.class, () -> Json.parse(syntax, STRICT));
        Assert.assertFalse(syntax.closed);
    }

    @Test
    public void filesUseUtf8AndReportPathAndOriginalCause() throws Exception {
        Path path = Files.createTempFile("json-source-", ".json");
        try {
            Files.writeString(path, "\"Привет😀\"", StandardCharsets.UTF_8);
            Assert.assertEquals("Привет😀", Json.parse(path.toFile()).stringValue());
            Assert.assertEquals("Привет😀", Json.parse(path.toFile(), String.class));
            Assert.assertEquals("Привет😀", Json.parse(path.toFile(), String.class, STRICT));
            Files.write(path, "42".getBytes(StandardCharsets.UTF_16LE));
            Assert.assertEquals(
                    42, Json.parse(path.toFile(), StandardCharsets.UTF_16LE, STRICT).intValue());
            Files.writeString(path, "\r\n @");
            JsonException error =
                    Assert.assertThrows(
                            JsonException.class, () -> Json.parse(path.toFile(), STRICT));
            Assert.assertEquals(path + ", 2:2", error.error().location().toString());
            Assert.assertTrue(error.getMessage().startsWith(path + ", 2:2, "));
            Assert.assertTrue(error.getLocalizedMessage().startsWith(path + ", 2:2, "));
            Assert.assertEquals(error.getMessage(), error.error().toString());
            Files.write(path, new byte[] {'"', (byte) 0xff, '"'});
            error = Assert.assertThrows(JsonException.class, () -> Json.parse(path.toFile()));
            Assert.assertTrue(error.getCause() instanceof CharacterCodingException);
            Assert.assertEquals(
                    path.toString(), error.error().location().sourceName().orElseThrow());
        } finally {
            Files.deleteIfExists(path);
        }
        JsonException missing =
                Assert.assertThrows(JsonException.class, () -> Json.parse(path.toFile()));
        Assert.assertTrue(missing.getCause() instanceof java.nio.file.NoSuchFileException);
        Assert.assertEquals(path + ", 1:1", missing.error().location().toString());
        Assert.assertThrows(JsonException.class, () -> Json.parse(path.toFile(), String.class));
        Assert.assertThrows(
                JsonException.class, () -> Json.parse(path.toFile(), String.class, STRICT));
    }

    @Test
    public void namedStringsUseTheSameDiagnostics() {
        JsonException error =
                Assert.assertThrows(
                        JsonException.class, () -> Json.parse("\n@", "source.json", STRICT));
        Assert.assertEquals("source.json, 2:1", error.error().location().toString());
    }

    @Test
    public void inputIsBufferedWithoutReadingTheWholeDocument() throws Exception {
        Reader endless =
                new Reader() {
                    private int count;

                    @Override
                    public int read(char[] data, int off, int len) {
                        Assert.assertTrue(
                                "Source must use bounded read-ahead", count + len <= 16384);
                        String prefix = "[1]";
                        for (int i = 0; i < len; i++, count++) {
                            data[off + i] = count < prefix.length() ? prefix.charAt(count) : ' ';
                        }
                        return len;
                    }

                    @Override
                    public void close() {
                        Assert.fail("Caller owns the reader");
                    }
                };
        Source source = new Source(endless, null);
        Assert.assertEquals('[', source.charValue());
        Assert.assertEquals('1', source.nextChar());
        Assert.assertEquals(']', source.nextChar());
    }
}
