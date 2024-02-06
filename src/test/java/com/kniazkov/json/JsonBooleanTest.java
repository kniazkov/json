/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests covering {@link JsonBoolean} class.
 */
public class JsonBooleanTest {
    @Test
    public void booleanToObject() {
        JsonBoolean elem = new JsonBoolean(false);
        Boolean boolValue = elem.toJavaObject(Boolean.class);
        Assert.assertNotNull(boolValue);
        Assert.assertEquals(false, boolValue);
        Integer intValue = elem.toJavaObject(Integer.class);
        Assert.assertNull(intValue);
    }
}
