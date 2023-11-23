package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests covering {@link JsonString} class.
 */
public class JsonStringTest {
    @Test
    public void simpleString() {
        JsonString elem = new JsonString("hello");
        Assert.assertTrue(elem.isString());
        Assert.assertEquals("hello", elem.getStringValue());
        Assert.assertEquals("\"hello\"", elem.toString());
    }

    @Test
    public void stringWithEntities() {
        JsonString elem = new JsonString("\rtest\n");
        Assert.assertEquals("\"\\rtest\\n\"", elem.toString());
    }

    @Test
    public void stringWithHex() {
        JsonString elem = new JsonString("\u0001test");
        Assert.assertEquals("\"\\u0001test\"", elem.toString());
    }

    @Test
    public void stringToObject() {
        JsonString elem = new JsonString("test");
        String strValue = elem.toJavaObject(String.class);
        Assert.assertNotNull(strValue);
        Assert.assertEquals("test", strValue);
        Integer intValue = elem.toJavaObject(Integer.class);
        Assert.assertNull(intValue);
    }
}
