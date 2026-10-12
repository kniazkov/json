/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class Json5ComplianceTest {
    private static final JsonParsingMode JSON5 = JsonParsingMode.JSON5;

    @Test
    public void upstreamParserSuite() throws Exception {
        int count = 0;
        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                getClass().getResourceAsStream("/json5-suite/cases.tsv"),
                                StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null; ) {
                String[] fields = line.split("\t", -1);
                String input =
                        new String(Base64.getDecoder().decode(fields[2]), StandardCharsets.UTF_8);
                if (Boolean.parseBoolean(fields[1])) {
                    try {
                        Assert.assertNotNull(Json.parse(input, JSON5));
                    } catch (JsonException error) {
                        throw new AssertionError(fields[0], error);
                    }
                } else {
                    Assert.assertThrows(
                            fields[0], JsonException.class, () -> Json.parse(input, JSON5));
                }
                count++;
            }
        }
        Assert.assertEquals(114, count);
    }

    @Test
    public void reservedUnicodeAndEscapedKeys() throws Exception {
        JsonObject object =
                Json.parse(
                                "{true:1,false:2,null:3,NaN:4,Infinity:5,$a:6,_b:7,"
                                        + "ключ:8,\\u0061:9,\\u0074rue:10,a\\u200C:11,Ⅳ:12,𐐀:13}",
                                JSON5)
                        .objectValueOrThrow();
        Assert.assertEquals(10, object.element("true").intValue());
        Assert.assertEquals(5, object.element("Infinity").intValue());
        Assert.assertEquals(8, object.element("ключ").intValue());
        Assert.assertEquals(9, object.element("a").intValue());
        Assert.assertEquals(11, object.element("a\u200c").intValue());
        Assert.assertEquals(13, object.element("𐐀").intValue());
        for (String input :
                new String[] {
                    "{1:0}",
                    "{\\u0031a:0}",
                    "{a\\u002D:0}",
                    "{a-b:0}",
                    "{+Infinity:0}",
                    "{-NaN:0}",
                    "{😀:0}",
                    "{\\x61:0}",
                    "\\u0074rue",
                    "InfinitY",
                    "NaNx"
                }) {
            Assert.assertThrows(input, JsonException.class, () -> Json.parse(input, JSON5));
        }
    }

    @Test
    public void stringsAndEveryLineContinuation() throws Exception {
        Assert.assertEquals(
                "A\u000b\0q$ ", Json.parse("'\\x41\\v\\0\\q\\$\\ '", JSON5).stringValue());
        Assert.assertEquals("a b", Json.parse("'a\\ b'", JsonParsingMode.EXTENDED).stringValue());
        for (String end : new String[] {"\n", "\r", "\r\n", "\u2028", "\u2029"}) {
            Assert.assertEquals("ab", Json.parse("'a\\" + end + "b'", JSON5).stringValue());
        }
        for (int ch = 0; ch < 32; ch++) {
            String input = "'a" + (char) ch + "b'";
            if (ch == '\n' || ch == '\r') {
                Assert.assertThrows(JsonException.class, () -> Json.parse(input, JSON5));
            } else {
                Assert.assertEquals("a" + (char) ch + "b", Json.parse(input, JSON5).stringValue());
            }
        }
        for (String escape : new String[] {"1", "8", "9", "01", "09", "x", "x0", "xGG", "u0XYZ"}) {
            Assert.assertThrows(
                    escape, JsonException.class, () -> Json.parse("'\\" + escape + "'", JSON5));
        }
        List<JsonError> warnings = new ArrayList<>();
        JsonElement value = JsonParser.parseString("'a\u2028b\u2029c'", JSON5, warnings);
        Assert.assertEquals(2, warnings.size());
        Assert.assertTrue(warnings.get(0) instanceof JsonError.UnescapedLineSeparator);
        Assert.assertEquals("1:3", warnings.get(0).location().toString());
        Assert.assertEquals("2:2", warnings.get(1).location().toString());
        Assert.assertEquals("\"a\\u2028b\\u2029c\"", value.toString());
    }

    @Test
    public void commentsAndWhitespaceAtEveryBoundary() throws Exception {
        String input = "/*a*/{ /*b*/ a /*c*/ : /*d*/ [/*e*/0XFF/*f*/,/*g*/-.5/*h*/,],} // eof";
        Assert.assertEquals("{\"a\": [255, -0.5]}", Json.parse(input, JSON5).toString());
        Assert.assertEquals(
                1,
                Json.parse("\uFEFF{\u00a0a\u2003:\u20281\u2029}", JSON5)
                        .objectValueOrThrow()
                        .element("a")
                        .intValue());
        // EXTENDED inherits JSON5 inside the root, retaining the established suffix-warning
        // contract.
        List<JsonError> warnings = new ArrayList<>();
        Assert.assertEquals(
                "{\"a\": [255, -0.5]}", JsonParser.parseString(input, warnings).toString());
        Assert.assertEquals(1, warnings.size());
        Assert.assertTrue(warnings.get(0) instanceof JsonError.TrailingContent);
    }

    @Test
    public void strictStillRejectsExtensions() {
        for (String input :
                new String[] {
                    "//c\n{}",
                    "/*c*/{}",
                    "{true:1}",
                    "{$a:1}",
                    "{\\u0061:1}",
                    "{'a':1}",
                    "\"\\x41\"",
                    "\"\\v\"",
                    "\"\\0\"",
                    "\"\\q\"",
                    "0XFF",
                    "+1",
                    ".1",
                    "1.",
                    "NaN",
                    "+NaN",
                    "-NaN",
                    "Infinity",
                    "+Infinity",
                    "-Infinity"
                }) {
            Assert.assertThrows(
                    input, JsonException.class, () -> Json.parse(input, JsonParsingMode.STRICT));
        }
    }
}
