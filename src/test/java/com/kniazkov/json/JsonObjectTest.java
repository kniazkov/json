package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

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
        JsonObject obj = createTestObject();
        Assert.assertEquals(6, obj.size());
        Assert.assertEquals("{\"bool\": true, \"nothing\": null, \"str\": \"hello\", \"number\": 13, \"array\": [1, 2], \"obj\": {\"str\": \"test\"}}", obj.toString());
    }

    @Test
    public void objectToJavaGenericObject() {
        Object obj = createTestObject().toObject();
        Assert.assertTrue(obj instanceof Map);
        Map<String, Object> map = (Map<String, Object>) obj;
        Assert.assertTrue(map.get("array") instanceof List);
    }

    @Test
    public void objectToJavaMap() {
        Map map = createTestObject().toObject(Map.class);
        Assert.assertNotNull(map);
        Assert.assertTrue(map.get("number") instanceof Number);
    }

    public static class TestClass {
        private boolean bool;
        private int number;

        private String str;

        public TestClass() {
        }

        public boolean getBool() {
            return bool;
        }

        public int getNumber() {
            return number;
        }

        public String getString() {
            return str;
        }
    }

    @Test
    public void objectToJavaClass() {
        TestClass obj = createTestObject().toObject(TestClass.class);
        Assert.assertNotNull(obj);
        Assert.assertTrue(obj.getBool());
        Assert.assertEquals(13, obj.getNumber());
        Assert.assertEquals("hello", obj.getString());
    }

    /**
     * Creates a JSON object containing test data.
     * @return JSON object
     */
    private JsonObject createTestObject() {
        JsonObject obj = new JsonObject();
        obj.addBoolean("bool", true);
        obj.addNull("nothing");
        obj.addString("str", "hello");
        obj.addNumber("number", 13);
        JsonArray array = obj.createArray("array");
        array.addNumber(1);
        array.addNumber(2);
        JsonObject childObj = obj.createObject("obj");
        childObj.addString("str", "test");
        return obj;
    };
}
