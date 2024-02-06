/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests covering {@link JsonArray} class.
 */
public class JsonArrayTest {
    @Test
    public void emptyArray() {
        JsonArray array = new JsonArray();
        Assert.assertEquals(0, array.size());
        Assert.assertEquals("[]", array.toString());
    }

    @Test
    public void arrayOfIntegers() {
        JsonArray array = new JsonArray();
        array.addNumber(1);
        array.addNumber(2);
        array.addNumber(3);
        Assert.assertEquals(3, array.size());
        Assert.assertEquals("[1, 2, 3]", array.toString());
    }

    @Test
    public void getElementByIndex() {
        JsonArray array = new JsonArray();
        array.addNumber(13);
        array.addNumber(666);
        Assert.assertEquals(2, array.size());
        boolean oops = false;
        try {
            JsonElement first = array.getElement(0);
            Assert.assertEquals(13, first.getIntValue());
            array.getElement(7);
        } catch (IndexOutOfBoundsException exception) {
            oops = true;
        }
        Assert.assertTrue(oops);
    }
}
