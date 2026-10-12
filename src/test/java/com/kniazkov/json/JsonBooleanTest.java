/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

/** Tests covering {@link JsonBoolean} class. */
public class JsonBooleanTest {
    @Test
    public void booleanToObject() {
        JsonBoolean elem = JsonBoolean.of(false);
        Boolean boolValue = elem.toJavaObject(Boolean.class);
        Assert.assertNotNull(boolValue);
        Assert.assertEquals(false, boolValue);
        Assert.assertThrows(JsonDataException.class, () -> elem.toJavaObject(Integer.class));
    }
}
