/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class RootTailTest {
    private static final String[] ROOTS = {
        "true",
        "false",
        "null",
        "42",
        "1.25",
        "\"text\"",
        "[]",
        "{}",
        "[1, {\"x\":2}]",
        "{\"x\":[1]}"
    };

    @Test
    public void strictAndJson5RejectAllKindsOfTail() throws Exception {
        for (JsonParsingMode mode :
                new JsonParsingMode[] {JsonParsingMode.STRICT, JsonParsingMode.JSON5}) {
            for (String root : ROOTS) {
                for (String tail :
                        new String[] {
                            "false", "{}", "[]", "42", "@", "\0", "\"unclosed", "1e+", "]"
                        }) {
                    JsonException error =
                            Assert.assertThrows(
                                    root + " / " + tail,
                                    JsonException.class,
                                    () -> Json.parse(root + " \r\n  " + tail, mode));
                    Assert.assertTrue(error.error() instanceof JsonError.TrailingContent);
                    Assert.assertEquals("2:3", error.error().location().toString());
                }
                Assert.assertEquals(
                        Json.parse(root, mode).toString(),
                        Json.parse(" \t\r\n" + root + " \t\r\n", mode).toString());
            }
        }
        for (String text : new String[] {"{}{}", "[]true", "\"a\"\"b\"", "42@"}) {
            Assert.assertThrows(
                    JsonException.class, () -> Json.parse(text, JsonParsingMode.STRICT));
        }
    }

    @Test
    public void extendedDropsArbitrarySuffixWithOneWarning() throws Exception {
        for (String root : ROOTS) {
            for (String tail :
                    new String[] {
                        "false [] @", "\"unclosed", "\0garbage", "1e+", "/* unclosed", "// comment"
                    }) {
                List<JsonError> warnings = new ArrayList<>();
                String input = "\u00a0\u2003" + root + "\r\n\u2007\t" + tail + "\u2029";
                Assert.assertEquals(
                        Json.parse(root).toString(),
                        JsonParser.parseString(input, warnings).toString());
                Assert.assertEquals(1, warnings.size());
                Assert.assertTrue(warnings.get(0) instanceof JsonError.TrailingContent);
                Assert.assertEquals("2:3", warnings.get(0).location().toString());
                Assert.assertEquals(Json.parse(root).toString(), Json.parse(input).toString());
            }
        }
    }

    @Test
    public void extendedSkipsBroadWhitespaceWithoutWarnings() throws Exception {
        String whitespace =
                " \t\n\r\u000b\f\u001c\u001d\u001e\u001f\u0085\u00a0\u1680"
                        + "\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a"
                        + "\u2028\u2029\u202f\u205f\u3000\ufeff";
        List<JsonError> warnings = new ArrayList<>();
        Assert.assertEquals(
                42,
                JsonParser.parseString(
                                whitespace.repeat(100) + "42" + whitespace.repeat(100), warnings)
                        .intValue());
        Assert.assertTrue(warnings.isEmpty());
        // Neither NUL nor zero-width space is whitespace.
        for (String tail : new String[] {"\0", "\u200b"}) {
            warnings.clear();
            JsonParser.parseString("42 " + tail, warnings);
            Assert.assertEquals(1, warnings.size());
        }
        Assert.assertThrows(JsonException.class, () -> Json.parse("\0 42"));
    }

    @Test
    public void strictWhitespaceIsOnlyTheFourJsonCharacters() {
        for (String space :
                new String[] {"\u000b", "\f", "\u0085", "\u00a0", "\u2003", "\u2028", "\ufeff"}) {
            Assert.assertThrows(
                    JsonException.class,
                    () -> Json.parse(" " + space + "42", JsonParsingMode.STRICT));
            Assert.assertThrows(
                    JsonException.class, () -> Json.parse("42" + space, JsonParsingMode.STRICT));
        }
        Assert.assertThrows(
                JsonException.class, () -> Json.parse("42 // comment", JsonParsingMode.STRICT));
    }

    @Test
    public void json5AllowsWhitespaceAndCommentsButNotAnotherValue() throws Exception {
        String padding = "\u000b\f\u00a0\u2007\ufeff\u2028\u2029 /* block */ // line\r\n";
        Assert.assertEquals(
                42, Json.parse(padding + "42" + padding, JsonParsingMode.JSON5).intValue());
        Assert.assertEquals(42, Json.parse("42 // EOF", JsonParsingMode.JSON5).intValue());
        Assert.assertEquals(42, Json.parse("42 /**/ /* **/", JsonParsingMode.JSON5).intValue());
        Assert.assertEquals(
                "[1, 2]", Json.parse("[1,/* between */2]", JsonParsingMode.JSON5).toString());
        for (String tail : new String[] {"/*", "/*/", "/* unterminated"}) {
            JsonException error =
                    Assert.assertThrows(
                            JsonException.class,
                            () -> Json.parse("42 " + tail, JsonParsingMode.JSON5));
            Assert.assertTrue(error.error() instanceof JsonError.UnclosedComment);
            Assert.assertEquals("1:4", error.error().location().toString());
        }
        for (String tail : new String[] {"/", "/x", "\u0085", "\u001c", "\u200b"}) {
            Assert.assertThrows(
                    JsonException.class, () -> Json.parse("42 " + tail, JsonParsingMode.JSON5));
        }
        JsonException error =
                Assert.assertThrows(
                        JsonException.class,
                        () -> Json.parse("42 /*ok*/\n false", JsonParsingMode.JSON5));
        Assert.assertTrue(error.error() instanceof JsonError.TrailingContent);
        Assert.assertEquals("2:2", error.error().location().toString());
    }

    @Test
    public void emptyDocumentHasAnErrorAtEof() {
        for (JsonParsingMode mode : JsonParsingMode.values()) {
            for (String text : new String[] {"", " \r\n"}) {
                JsonException error =
                        Assert.assertThrows(JsonException.class, () -> Json.parse(text, mode));
                Assert.assertTrue(error.error() instanceof JsonError.ExpectedRoot);
                Assert.assertEquals(
                        text.isEmpty() ? "1:1" : "2:1", error.error().location().toString());
            }
        }
        Assert.assertThrows(
                JsonException.class, () -> Json.parse("/* only comment */", JsonParsingMode.JSON5));
        Assert.assertThrows(JsonException.class, () -> Json.parse("\u00a0\u2029"));
    }

    @Test
    public void tailWarningAppendsToExistingRecoveryWarnings() throws Exception {
        List<JsonError> warnings = new ArrayList<>();
        JsonError existing = new JsonError.MissingComma(new JsonLocation(1, 1));
        warnings.add(existing);
        Assert.assertEquals("[1, 2]", JsonParser.parseString("[1 2] garbage", warnings).toString());
        Assert.assertEquals(3, warnings.size());
        Assert.assertSame(existing, warnings.get(0));
        Assert.assertTrue(warnings.get(1) instanceof JsonError.MissingComma);
        Assert.assertTrue(warnings.get(2) instanceof JsonError.TrailingContent);
        Assert.assertEquals("1:7", warnings.get(2).location().toString());
    }

    @Test
    public void everyInputPathChecksTheTailAndPreservesNames() throws Exception {
        Assert.assertThrows(
                JsonException.class,
                () -> Json.parse("42 false", Integer.class, JsonParsingMode.STRICT).orElseThrow());
        JsonException reader =
                Assert.assertThrows(
                        JsonException.class,
                        () ->
                                Json.parse(
                                        new StringReader("42 false"),
                                        "response",
                                        JsonParsingMode.STRICT));
        Assert.assertEquals("response, 1:4", reader.error().location().toString());
        Assert.assertThrows(
                JsonException.class,
                () ->
                        Json.parse(
                                new ByteArrayInputStream(
                                        "42 false".getBytes(StandardCharsets.UTF_8)),
                                JsonParsingMode.STRICT));
        Path file = Files.createTempFile("json-tail-", ".json");
        try {
            Files.writeString(file, "42 false");
            JsonException error =
                    Assert.assertThrows(
                            JsonException.class,
                            () ->
                                    Json.parse(file.toFile(), Integer.class, JsonParsingMode.STRICT)
                                            .orElseThrow());
            Assert.assertEquals(file + ", 1:4", error.error().location().toString());
            Assert.assertEquals(42, Json.parse(file.toFile()).intValue());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void discardedSuffixIsReadToEofAndIoErrorsAreNotWarnings() throws Exception {
        IOException cause = new IOException("tail read failed");
        Reader reader =
                new Reader() {
                    private final String text = "42 garbage";
                    private int index;

                    @Override
                    public int read(char[] data, int off, int len) throws IOException {
                        if (index == text.length()) {
                            throw cause;
                        }
                        data[off] = text.charAt(index++);
                        return 1;
                    }

                    @Override
                    public void close() {
                        Assert.fail("Caller owns reader");
                    }
                };
        JsonException error = Assert.assertThrows(JsonException.class, () -> Json.parse(reader));
        Assert.assertSame(cause, error.getCause());
        Assert.assertTrue(error.error() instanceof JsonError.InputFailure);
    }
}
