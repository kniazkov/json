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
        Boolean boolValue = elem.toObject(Boolean.class);
        Assert.assertNotNull(boolValue);
        Assert.assertEquals(false, boolValue);
        Integer intValue = elem.toObject(Integer.class);
        Assert.assertNull(intValue);
    }
}
