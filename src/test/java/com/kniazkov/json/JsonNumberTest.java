/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests covering {@link JsonNumber} class.
 */
public class JsonNumberTest {
    @Test
    public void integer() {
        JsonNumber elem = new JsonNumber(13);
        Assert.assertTrue(elem.isInteger());
        Assert.assertTrue(elem.isLongInteger());
        Assert.assertTrue(elem.isNumber());
        Assert.assertEquals(13, elem.getIntValue());
        Assert.assertEquals(13L, elem.getLongValue());
        Assert.assertEquals(13, elem.getDoubleValue(), 0);
        Assert.assertEquals("13", elem.toString());
        Assert.assertEquals("13", elem.getStringValue());
    }

    @Test
    public void longInteger() {
        JsonNumber elem = new JsonNumber( 10000000000L);
        Assert.assertFalse(elem.isInteger());
        Assert.assertTrue(elem.isLongInteger());
        Assert.assertTrue(elem.isNumber());
        Assert.assertEquals(10000000000L, elem.getLongValue());
        Assert.assertEquals((double)10000000000L, elem.getDoubleValue(), 0);
        Assert.assertEquals("10000000000", elem.toString());
    }

    @Test
    public void realNumber() {
        JsonNumber elem = new JsonNumber(13.01);
        Assert.assertFalse(elem.isInteger());
        Assert.assertFalse(elem.isLongInteger());
        Assert.assertTrue(elem.isNumber());
        Assert.assertEquals(13, elem.getIntValue());
        Assert.assertEquals(13L, elem.getLongValue());
        Assert.assertEquals(13.01, elem.getDoubleValue(), 0);
        Assert.assertEquals("13.01", elem.toString());
    }

    @Test
    public void numberToObject() {
        JsonNumber elem = new JsonNumber(13);
        Integer intValue = elem.toJavaObject(Integer.class);
        Assert.assertNotNull(intValue);
        Double doubleValue = elem.toJavaObject(Double.class);
        Assert.assertNotNull(doubleValue);
        Boolean boolValue = elem.toJavaObject(Boolean.class);
        Assert.assertNull(boolValue);
    }
}
