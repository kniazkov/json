/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ContainerRecoveryTest {
    private List<JsonError> check(String input, String expected, int count) throws Exception {
        List<JsonError> warnings = new ArrayList<>();
        JsonElement root = JsonParser.parseString(input, warnings);
        Assert.assertEquals(
                input, Json.parse(expected, JsonParsingMode.STRICT).toString(), root.toString());
        Assert.assertEquals(input, count, warnings.size());
        Assert.assertEquals(root.toString(), Json.parse(input).toString());
        return warnings;
    }

    private void rejected(String input, JsonParsingMode... modes) {
        for (JsonParsingMode mode : modes) {
            Assert.assertThrows(
                    input + " / " + mode, JsonException.class, () -> Json.parse(input, mode));
        }
    }

    @Test
    public void missingCommasPreserveEveryFieldAndElement() throws Exception {
        for (String[] pair :
                new String[][] {
                    {"[1 2]", "[1,2]"}, {"{\"a\":1 \"b\":2}", "{\"a\":1,\"b\":2}"},
                    {"[{} []]", "[{},[]]"}, {"{a:[1] b:{}}", "{\"a\":[1],\"b\":{}}"}
                }) {
            Assert.assertTrue(check(pair[0], pair[1], 1).get(0) instanceof JsonError.MissingComma);
            rejected(pair[0], JsonParsingMode.STRICT, JsonParsingMode.JSON5);
        }
    }

    @Test
    public void eofCanOnlyCloseTheRoot() throws Exception {
        for (String[] pair :
                new String[][] {
                    {"[", "[]"},
                    {"{", "{}"},
                    {"[1", "[1]"},
                    {"[1,", "[1]"},
                    {"{\"a\":1", "{\"a\":1}"},
                    {"{\"a\":1,", "{\"a\":1}"},
                    {"{\"a\":[1]", "{\"a\":[1]}"},
                    {"[{\"a\":1}", "[{\"a\":1}]"}
                }) {
            JsonError warning = check(pair[0], pair[1], 1).get(0);
            Assert.assertTrue(warning instanceof JsonError.MissingClosingBracket);
            Assert.assertEquals(pair[0].length() + 1, warning.location().column());
            rejected(pair[0], JsonParsingMode.STRICT, JsonParsingMode.JSON5);
        }
        for (String input :
                new String[] {"[[", "{\"a\":[1", "{\"a\":[1,", "[{\"a\":1", "[{\"a\":1,"}) {
            rejected(input, JsonParsingMode.values());
        }
        check("[[]", "[[]]", 1);
    }

    @Test
    public void emptyArraySlotsAreSkippedNotReplacedWithNull() throws Exception {
        for (String[] pair :
                new String[][] {
                    {"[1,,2]", "[1,2]", "1"},
                    {"[1,,,2]", "[1,2]", "2"},
                    {"[,1]", "[1]", "1"},
                    {"[1,,]", "[1]", "1"},
                    {"[,]", "[]", "1"},
                    {"[,,]", "[]", "2"},
                    {"[,,,1,,2,,]", "[1,2]", "5"}
                }) {
            for (JsonError warning : check(pair[0], pair[1], Integer.parseInt(pair[2]))) {
                Assert.assertTrue(warning instanceof JsonError.MissingArrayElement);
            }
            rejected(pair[0], JsonParsingMode.STRICT, JsonParsingMode.JSON5);
        }
        List<JsonError> warnings = check("[1,,,2]", "[1,2]", 2);
        Assert.assertEquals(4, warnings.get(0).location().column());
        Assert.assertEquals(5, warnings.get(1).location().column());
        warnings = check("[1,,", "[1]", 2);
        Assert.assertTrue(warnings.get(1) instanceof JsonError.MissingClosingBracket);
    }

    @Test
    public void ordinaryTrailingCommasDoNotWarn() throws Exception {
        for (String[] pair : new String[][] {{"[1,]", "[1]"}, {"{\"a\":1,}", "{\"a\":1}"}}) {
            check(pair[0], pair[1], 0);
            Assert.assertEquals(
                    Json.parse(pair[1]).toString(),
                    Json.parse(pair[0], JsonParsingMode.JSON5).toString());
            rejected(pair[0], JsonParsingMode.STRICT);
        }
    }

    @Test
    public void foreignCloserIsRetainedForItsAncestor() throws Exception {
        for (String[] pair :
                new String[][] {
                    {"[{\"a\":1]", "[{\"a\":1}]"}, {"{\"a\":[1}", "{\"a\":[1]}"},
                    {"[{]", "[{}]"}, {"{\"a\":[}", "{\"a\":[]}"},
                    {"[{\"a\":1,]", "[{\"a\":1}]"}, {"{\"a\":[1,}", "{\"a\":[1]}"},
                    {"[{\"a\":1],2]", "[{\"a\":1}]"}
                }) {
            check(pair[0], pair[1], pair[0].endsWith(",2]") ? 2 : 1);
            rejected(pair[0], JsonParsingMode.STRICT, JsonParsingMode.JSON5);
        }
        check("{\"a\":[[1}", "{\"a\":[[1]]}", 2);
        // Parsing continues in the enclosing object after a recovered nested object.
        check("{\"a\":[{\"b\":1],\"c\":2}", "{\"a\":[{\"b\":1}],\"c\":2}", 1);
    }

    @Test
    public void incompletePairsAndTokensCannotBeRecovered() {
        for (String input :
                new String[] {
                    "{\"a\"",
                    "{\"a\":",
                    "{\"a\":}",
                    "[{\"a\":]",
                    "{\"a\" 1}",
                    "[\"abc",
                    "[1e",
                    "[1e+",
                    "{\"a\":1 \"b\":}",
                    "{\"a\":1 \"b\"}",
                    "{\"a\":1,\"b\":",
                    "{\"a\":1,\"b\""
                }) {
            rejected(input, JsonParsingMode.values());
        }
    }

    @Test
    public void wrongClosersAndInternalGarbageAreErrors() {
        for (String input :
                new String[] {
                    "{\"a\":1]",
                    "[1}",
                    "[1 @ 2]",
                    "{\"a\"::1}",
                    "{\"a\":1,,\"b\":2}",
                    "{,}",
                    "{\"a\":1,,}",
                    "[1:2]",
                    "[1 hello]"
                }) {
            rejected(input, JsonParsingMode.values());
        }
    }

    @Test
    public void warningsKeepOrderAndCoordinatesAndTailPolicy() throws Exception {
        List<JsonError> warnings = check("[\n1,,2 3", "[1,2,3]", 3);
        Assert.assertTrue(warnings.get(0) instanceof JsonError.MissingArrayElement);
        Assert.assertTrue(warnings.get(1) instanceof JsonError.MissingComma);
        Assert.assertTrue(warnings.get(2) instanceof JsonError.MissingClosingBracket);
        Assert.assertEquals("2:3", warnings.get(0).location().toString());
        Assert.assertEquals("2:6", warnings.get(1).location().toString());
        Assert.assertEquals("2:7", warnings.get(2).location().toString());
        warnings = check("{\"a\":1}]", "{\"a\":1}", 1);
        Assert.assertTrue(warnings.get(0) instanceof JsonError.TrailingContent);
        Assert.assertEquals("1:8", warnings.get(0).location().toString());
    }

    @Test
    public void validNestedValuesKeepTheirParents() throws Exception {
        JsonArray root = Json.parse("[{\"a\":[1,,2] \"b\":3]").arrayValueOrThrow();
        JsonObject obj = root.get(0).objectValueOrThrow();
        Assert.assertSame(root, obj.parent());
        JsonArray nested = obj.element("a").arrayValueOrThrow();
        Assert.assertSame(obj, nested.parent());
        Assert.assertSame(nested, nested.get(1).parent());
        Assert.assertEquals(3, obj.element("b").intValue());
    }
}
