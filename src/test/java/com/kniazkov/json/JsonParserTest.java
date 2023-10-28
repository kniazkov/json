package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests covering {@link JsonParser} class.
 */
public class JsonParserTest {
    @Test
    public void integer() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("  13");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertTrue(elem.isInteger());
        Assert.assertEquals(13, elem.getIntValue());
    }

    @Test
    public void longInteger() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("666000000000");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertFalse(elem.isInteger());
        Assert.assertTrue(elem.isLongInteger());
        Assert.assertEquals(666000000000L, elem.getLongValue());
    }

    @Test
    public void negativeInteger() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("-13");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertTrue(elem.isInteger());
        Assert.assertEquals(-13, elem.getIntValue());
    }

    @Test
    public void positiveInteger() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("+13");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertTrue(elem.isInteger());
        Assert.assertEquals(13, elem.getIntValue());

        JsonError error = null;
        try {
            JsonParser.parseString("+13", JsonParsingMode.STRICT);
        } catch (JsonException exception) {
            error = exception.getError();
        }
        Assert.assertTrue(error instanceof JsonError.InvalidCharacter);
        String message = error.getMessage();
        Assert.assertEquals("Invalid character: '+'", message);
    }

    @Test
    public void emptyArray() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("[ ]");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        JsonArray array = elem.toArray();
        Assert.assertNotNull(array);
        Assert.assertEquals(0, array.size());
    }

    @Test
    public void arrayOfIntegers() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("[1, 2, 3]");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        JsonArray array = elem.toArray();
        Assert.assertNotNull(array);
        Assert.assertEquals(3, array.size());
        Assert.assertEquals("[1, 2, 3]", array.toString());
    }

    @Test
    public void arrayContainingOtherArrays() {
        Assert.assertTrue(commonTest("[1, [2, 3], []]"));
    }

    @Test
    public void arrayWithExtraComma() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("[1, 2,]");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertEquals("[1, 2]", elem.toString());

        JsonError error = null;
        try {
            JsonParser.parseString("[1, 2,]", JsonParsingMode.STRICT);
        } catch (JsonException exception) {
            error = exception.getError();
        }
        Assert.assertTrue(error instanceof JsonError.ExpectedElementAfterComma);
    }

    @Test
    public void nullElement() {
        Assert.assertTrue(commonTest("null"));
    }

    /**
     * A common test for JSON parser. First parses a JSON document,
     * then converts the resulting element into a string, the results should match.
     * @param json JSON document
     * @return Testing result ({@code true} = success)
     */
    private static boolean commonTest(String json) {
        try {
            JsonElement elem = JsonParser.parseString(json);
            String json2 = elem.toString();
            return json.equals(json2);
        } catch (JsonException ignored) {
        }
        return false;
    }
}
