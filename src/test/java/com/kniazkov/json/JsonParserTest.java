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
}
