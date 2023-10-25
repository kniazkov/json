package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

public class JsonNumberTest {
    @Test
    public void integerSerialization() {
        JsonNumber elem = new JsonNumber(null, 13);
        String serialized = elem.toString();
        Assert.assertEquals("13", serialized);
    }
}
