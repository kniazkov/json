package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests covering {@link JsonObject} class.
 */
public class JsonObjectTest {
    @Test
    public void emptyObject() {
        JsonObject obj = new JsonObject();
        Assert.assertEquals(0, obj.size());
        Assert.assertEquals("{}", obj.toString());
    }

    @Test
    public void objectWithData() {
        JsonObject obj = new JsonObject();
        obj.addBoolean("bool", true);
        obj.addNull("null");
        obj.addNumber("number", 13);
        JsonArray array = obj.createArray("array");
        array.addNumber(1);
        array.addNumber(2);
        JsonObject childObj = obj.createObject("obj");
        childObj.addString("str", "test");
        Assert.assertEquals(5, obj.size());
        Assert.assertEquals("{\"bool\": true, \"null\": null, \"number\": 13, \"array\": [1, 2], \"obj\": {\"str\": \"test\"}}", obj.toString());
    }
}
