package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

public class JsonNumberTest {
    @Test
    public void integerTest() {
        JsonNumber elem = new JsonNumber(null, 13);
        Assert.assertTrue(elem.isInteger());
        Assert.assertTrue(elem.isLongInteger());
        Assert.assertTrue(elem.isNumber());
        Assert.assertEquals(13, elem.getIntValue());
        Assert.assertEquals(13L, elem.getLongValue());
        Assert.assertEquals(13, elem.getDoubleValue(), 0);
        Assert.assertEquals("13", elem.toString());
    }

    @Test
    public void longIntegerTest() {
        JsonNumber elem = new JsonNumber(null, 10000000000L);
        Assert.assertFalse(elem.isInteger());
        Assert.assertTrue(elem.isLongInteger());
        Assert.assertTrue(elem.isNumber());
        Assert.assertEquals(10000000000L, elem.getLongValue());
        Assert.assertEquals((double)10000000000L, elem.getDoubleValue(), 0);
        Assert.assertEquals("10000000000", elem.toString());
    }

    @Test
    public void realNumberTest() {
        JsonNumber elem = new JsonNumber(null, 13.01);
        Assert.assertFalse(elem.isInteger());
        Assert.assertFalse(elem.isLongInteger());
        Assert.assertTrue(elem.isNumber());
        Assert.assertEquals(13, elem.getIntValue());
        Assert.assertEquals(13L, elem.getLongValue());
        Assert.assertEquals(13.01, elem.getDoubleValue(), 0);
        Assert.assertEquals("13.01", elem.toString());
    }
}
