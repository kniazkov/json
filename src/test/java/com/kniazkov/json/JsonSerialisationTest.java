/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

/** Tests covering {@link JsonElement} class, method {@link JsonElement#toText(String)}}. */
public class JsonSerialisationTest {
    private static final String SEPARATOR = System.lineSeparator();

    @Test
    public void object() {
        final String json = "{one: 1, two: 2, three: 3}";
        final String expected =
                "{"
                        + SEPARATOR
                        + "  \"one\": 1,"
                        + SEPARATOR
                        + "  \"two\": 2,"
                        + SEPARATOR
                        + "  \"three\": 3"
                        + SEPARATOR
                        + "}";
        Assert.assertTrue(commonTest(json, expected));
    }

    @Test
    public void objectWithNestedObject() {
        final String json = "{one: 1, obj: {two: 2, three: 3}}";
        final String expected =
                "{"
                        + SEPARATOR
                        + "  \"one\": 1,"
                        + SEPARATOR
                        + "  \"obj\": {"
                        + SEPARATOR
                        + "    \"two\": 2,"
                        + SEPARATOR
                        + "    \"three\": 3"
                        + SEPARATOR
                        + "  }"
                        + SEPARATOR
                        + "}";
        Assert.assertTrue(commonTest(json, expected));
    }

    @Test
    public void objectWithNestedEmptyObject() {
        final String json = "{one: 1, empty: { }}";
        final String expected =
                "{" + SEPARATOR + "  \"one\": 1," + SEPARATOR + "  \"empty\": {}" + SEPARATOR + "}";
        Assert.assertTrue(commonTest(json, expected));
    }

    @Test
    public void objectWithNestedObjectContainingOnePair() {
        final String json = "{one: 1, obj: {two: 2}}";
        final String expected =
                "{"
                        + SEPARATOR
                        + "  \"one\": 1,"
                        + SEPARATOR
                        + "  \"obj\": {\"two\": 2}"
                        + SEPARATOR
                        + "}";
        Assert.assertTrue(commonTest(json, expected));
    }

    @Test
    public void array() {
        final String json = "[1, 2, 3]";
        final String expected =
                "[" + SEPARATOR + "  1," + SEPARATOR + "  2," + SEPARATOR + "  3" + SEPARATOR + "]";
        Assert.assertTrue(commonTest(json, expected));
    }

    @Test
    public void arrayWithNestedArray() {
        final String json = "[1, [2, 3]]";
        final String expected =
                "[" + SEPARATOR + "  1," + SEPARATOR + "  [" + SEPARATOR + "    2," + SEPARATOR
                        + "    3" + SEPARATOR + "  ]" + SEPARATOR + "]";
        Assert.assertTrue(commonTest(json, expected));
    }

    @Test
    public void arrayWithNestedEmptyArray() {
        final String json = "[1, 2, []]";
        final String expected =
                "[" + SEPARATOR + "  1," + SEPARATOR + "  2," + SEPARATOR + "  []" + SEPARATOR
                        + "]";
        Assert.assertTrue(commonTest(json, expected));
    }

    @Test
    public void arrayWithNestedArrayContainingOneItem() {
        final String json = "[1, 2, [3]]";
        final String expected =
                "[" + SEPARATOR + "  1," + SEPARATOR + "  2," + SEPARATOR + "  [3]" + SEPARATOR
                        + "]";
        Assert.assertTrue(commonTest(json, expected));
    }

    @Test
    public void complexCase() {
        final String json = "{one: 1, two: 2, obj: {three: [3, [4]], four: [{five: null}]}}";
        final String expected =
                "{"
                        + SEPARATOR
                        + "  \"one\": 1,"
                        + SEPARATOR
                        + "  \"two\": 2,"
                        + SEPARATOR
                        + "  \"obj\": {"
                        + SEPARATOR
                        + "    \"three\": ["
                        + SEPARATOR
                        + "      3,"
                        + SEPARATOR
                        + "      [4]"
                        + SEPARATOR
                        + "    ],"
                        + SEPARATOR
                        + "    \"four\": ["
                        + SEPARATOR
                        + "      {\"five\": null}"
                        + SEPARATOR
                        + "    ]"
                        + SEPARATOR
                        + "  }"
                        + SEPARATOR
                        + "}";
        Assert.assertTrue(commonTest(json, expected));
    }

    /**
     * Common test for testing serialization.
     *
     * @param json Source JSON
     * @param expected Expected JSON after parsing source and serialization.
     * @return Testing result ({@code true} = success)
     */
    private boolean commonTest(String json, String expected) {
        String actual = "";
        boolean oops = false;
        try {
            actual = JsonParser.parseString(json).toText("  ");
        } catch (JsonException ignored) {
            return false;
        }
        return expected.equals(actual);
    }
}
