package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests covering {@link JsonParser} class.
 */
public class JsonParserTest {
    @Test
    public void integer() {
        JsonElement elem = JsonParser.parseString("  13");
        Assert.assertNotNull(elem);
        Assert.assertTrue(elem.isInteger());
        Assert.assertEquals(13, elem.getIntValue());
    }

    @Test
    public void longInteger() {
        JsonElement elem = JsonParser.parseString("666000000000");
        Assert.assertNotNull(elem);
        Assert.assertFalse(elem.isInteger());
        Assert.assertTrue(elem.isLongInteger());
        Assert.assertEquals(666000000000L, elem.getLongValue());
    }

    @Test
    public void negativeInteger() {
        JsonElement elem = JsonParser.parseString("  -13");
        Assert.assertNotNull(elem);
        Assert.assertTrue(elem.isInteger());
        Assert.assertEquals(-13, elem.getIntValue());
    }
}
