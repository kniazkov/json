/*
 * Copyright (c) 2023 Ivan Kniazkov
 */
package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests covering {@link JsonElement} class.
 */
public class JsonElementTest {
    @Test
    public void cloning() {
        JsonArray array = new JsonArray();
        array.addNumber(1);
        JsonArray childArray = array.createArray();
        childArray.addNumber(2);
        JsonElement copy = array.clone();
        childArray.addNumber(3);
        Assert.assertEquals("[1, [2, 3]]", array.toString());
        Assert.assertEquals("[1, [2]]", copy.toString());
    }
}
