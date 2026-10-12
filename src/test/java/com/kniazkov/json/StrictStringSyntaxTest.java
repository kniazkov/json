/* Copyright (c) 2026 Ivan Kniazkov */
package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

public class StrictStringSyntaxTest {
    private static final JsonParsingMode STRICT = JsonParsingMode.STRICT;

    @Test
    public void rejectsEveryRawControlInValuesAndKeys() {
        for (int code = 0; code < 32; code++) {
            String string = "\"a" + (char) code + "b\"";
            for (String prefix : new String[] {"", "[", "{\"key\":", "{"}) {
                String suffix = prefix.equals("[") ? "]" : prefix.equals("{") ? ":0}"
                        : prefix.isEmpty() ? "" : "}";
                JsonException error = Assert.assertThrows(JsonException.class,
                        () -> Json.parse(prefix + string + suffix, "source.json", STRICT));
                Assert.assertTrue(error.error() instanceof JsonError.InvalidCharacter);
                Assert.assertEquals("source.json, 1:" + (prefix.length() + 3), error.error().location().toString());
            }
        }
    }

    @Test
    public void acceptsEveryEscapedControlAndAllStandardShortEscapes() throws Exception {
        for (int code = 0; code < 32; code++) {
            String text = String.format("\"a\\u%04xb\"", code);
            String expected = "a" + (char) code + "b";
            Assert.assertEquals(expected, Json.parse(text, STRICT).stringValue());
            Assert.assertEquals(expected, Json.parse("[" + text + "]", STRICT).arrayValueOrThrow().get(0).stringValue());
            Assert.assertEquals(1, Json.parse("{" + text + ":1}", STRICT).objectValueOrThrow().element(expected).intValue());
        }
        Assert.assertEquals("\"\\/\b\f\n\r\t", Json.parse("\"\\\"\\\\\\/\\b\\f\\n\\r\\t\"", STRICT).stringValue());
        Assert.assertEquals("it's fine", Json.parse("\"it's fine\"", STRICT).stringValue());
        Assert.assertEquals(" \u007f\u0085\u2028\u2029😀", Json.parse("\" \u007f\u0085\u2028\u2029😀\"", STRICT).stringValue());
    }

    @Test
    public void rejectsEscapedApostropheAndOtherNonJsonEscapes() {
        for (String escape : new String[] {"'", "v", "0", "x41", "q", "\n", "\r\n"}) {
            String string = "\"a\\" + escape + "b\"";
            for (String input : new String[] {string, "[" + string + "]", "{" + string + ":0}"}) {
                JsonException error = Assert.assertThrows(JsonException.class, () -> Json.parse(input, STRICT));
                Assert.assertTrue(error.error() instanceof JsonError.IncorrectStringSequence);
            }
        }
        JsonException error = Assert.assertThrows(JsonException.class,
                () -> Json.parse("\n\"a\\'b\"", "escape.json", STRICT));
        Assert.assertEquals("escape.json, 2:4", error.error().location().toString());
    }

    @Test
    public void preservesOtherModesAndRejectsMalformedUnicodeEscapes() throws Exception {
        for (JsonParsingMode mode : new JsonParsingMode[] {JsonParsingMode.JSON5, JsonParsingMode.EXTENDED}) {
            Assert.assertEquals("a'b", Json.parse("\"a\\'b\"", mode).stringValue());
            Assert.assertEquals("a'b", Json.parse("'a\\'b'", mode).stringValue());
            // Preserve existing behavior; complete JSON5 string rules are a separate task.
            Assert.assertEquals("a\nb", Json.parse("\"a\nb\"", mode).stringValue());
        }
        for (String input : new String[] {"\"\\u\"", "\"\\u0\"", "\"\\u00\"", "\"\\u000\"", "\"\\u00XZ\""}) {
            Assert.assertThrows(JsonException.class, () -> Json.parse(input, STRICT));
        }
    }

    @Test
    public void numericIssueSixRegressionsStayFixed() throws Exception {
        for (String number : new String[] {"0e0", "0E+1", "-0e-1"}) {
            Assert.assertEquals(0, Json.parse(number, STRICT).decimalValue().signum());
            Assert.assertEquals(0, Json.parse("[" + number + "]", STRICT).arrayValueOrThrow().get(0).decimalValue().signum());
            Assert.assertEquals(0, Json.parse("{\"x\":" + number + "}", STRICT).objectValueOrThrow().element("x").decimalValue().signum());
        }
        for (String number : new String[] {"01", "-01", "00", "01.2", "01e2"}) {
            for (String input : new String[] {number, "[" + number + "]", "{\"x\":" + number + "}"}) {
                Assert.assertThrows(JsonException.class, () -> Json.parse(input, STRICT));
            }
        }
    }
}
