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
        JsonNumber elem = JsonNumber.of(13);
        Assert.assertTrue(elem.isInteger());
        Assert.assertTrue(elem.isLongInteger());
        Assert.assertTrue(elem.isNumber());
        Assert.assertEquals(13, elem.intValue());
        Assert.assertEquals(13L, elem.longValue());
        Assert.assertEquals(13, elem.doubleValue(), 0);
        Assert.assertEquals("13", elem.toString());
        Assert.assertEquals("13", elem.stringValue());
    }

    @Test
    public void longInteger() {
        JsonNumber elem = JsonNumber.of( 10000000000L);
        Assert.assertFalse(elem.isInteger());
        Assert.assertTrue(elem.isLongInteger());
        Assert.assertTrue(elem.isNumber());
        Assert.assertEquals(10000000000L, elem.longValue());
        Assert.assertEquals((double)10000000000L, elem.doubleValue(), 0);
        Assert.assertEquals("10000000000", elem.toString());
    }

    @Test
    public void realNumber() {
        JsonNumber elem = JsonNumber.of(13.01);
        Assert.assertFalse(elem.isInteger());
        Assert.assertFalse(elem.isLongInteger());
        Assert.assertTrue(elem.isNumber());
        Assert.assertEquals(13, elem.intValue());
        Assert.assertEquals(13L, elem.longValue());
        Assert.assertEquals(13.01, elem.doubleValue(), 0);
        Assert.assertEquals("13.01", elem.toString());
    }

    @Test
    public void numberToObject() {
        JsonNumber elem = JsonNumber.of(13);
        Integer intValue = elem.toJavaObject(Integer.class);
        Assert.assertNotNull(intValue);
        Double doubleValue = elem.toJavaObject(Double.class);
        Assert.assertNotNull(doubleValue);
        Assert.assertThrows(JsonDataException.class, () -> elem.toJavaObject(Boolean.class));
    }
}
