package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Tests covering {@link JsonElement} class, method {@link JsonElement#toText(String)}}.
 */
public class JsonSerialisationTest {
    private static final String separator = System.lineSeparator();

    @Test
    public void object() {
        final String json = "{one: 1, two: 2, three: 3}";
        final String expected = "{" + separator +
                "  \"one\": 1," + separator +
                "  \"two\": 2," + separator +
                "  \"three\": 3" + separator +
                "}";
        Assert.assertTrue(commonTest(json, expected));
    }

    @Test
    public void objectWithNestedObject() {
        final String json = "{one: 1, obj: {two: 2, three: 3}}";
        final String expected = "{" + separator +
                "  \"one\": 1," + separator +
                "  \"obj\": {" + separator +
                "    \"two\": 2," + separator +
                "    \"three\": 3" + separator +
                "  }" + separator +
                "}";
        Assert.assertTrue(commonTest(json, expected));
    }

    @Test
    public void objectWithNestedEmptyObject() {
        final String json = "{one: 1, empty: { }}";
        final String expected = "{" + separator +
                "  \"one\": 1," + separator +
                "  \"empty\": {}" + separator +
                "}";
        Assert.assertTrue(commonTest(json, expected));
    }

    @Test
    public void objectWithNestedObjectContainingOnePair() {
        final String json = "{one: 1, obj: {two: 2}}";
        final String expected = "{" + separator +
                "  \"one\": 1," + separator +
                "  \"obj\": {\"two\": 2}" + separator +
                "}";
        Assert.assertTrue(commonTest(json, expected));
    }

    @Test
    public void array() {
        final String json = "[1, 2, 3]";
        final String expected = "[" + separator +
                "  1," + separator +
                "  2," + separator +
                "  3" + separator +
                "]";
        Assert.assertTrue(commonTest(json, expected));
    }

    @Test
    public void arrayWithNestedArray() {
        final String json = "[1, [2, 3]]";
        final String expected = "[" + separator +
                "  1," + separator +
                "  [" + separator +
                "    2," + separator +
                "    3" + separator +
                "  ]" + separator +
                "]";
        Assert.assertTrue(commonTest(json, expected));
    }

    @Test
    public void arrayWithNestedEmptyArray() {
        final String json = "[1, 2, []]";
        final String expected = "[" + separator +
                "  1," + separator +
                "  2," + separator +
                "  []" + separator +
                "]";
        Assert.assertTrue(commonTest(json, expected));
    }

    @Test
    public void arrayWithNestedArrayContainingOneItem() {
        final String json = "[1, 2, [3]]";
        final String expected = "[" + separator +
                "  1," + separator +
                "  2," + separator +
                "  [3]" + separator +
                "]";
        Assert.assertTrue(commonTest(json, expected));
    }

    @Test
    public void complexCase() {
        final String json = "{one: 1, two: 2, obj: {three: [3, 4], four: [{five: null}]}}";
        final String expected = "{" + separator +
                "  \"one\": 1," + separator +
                "  \"two\": 2," + separator +
                "  \"obj\": {" + separator +
                "    \"three\": [" + separator +
                "      3," + separator +
                "      4" + separator +
                "    ]," + separator +
                "    \"four\": [{\"five\": null}]" + separator +
                "  }" + separator +
                "}";
        Assert.assertTrue(commonTest(json, expected));
    }

    /**
     * Common test for testing serialization.
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
