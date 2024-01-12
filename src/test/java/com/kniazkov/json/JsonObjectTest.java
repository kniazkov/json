/*
 * Copyright (c) 2023 Ivan Kniazkov
 */
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
        Object obj = createTestObject().toJavaObject();
        Assert.assertTrue(obj instanceof Map);
        Map<String, Object> map = (Map<String, Object>) obj;
        Assert.assertTrue(map.get("array") instanceof List);
    }

    @Test
    public void objectToJavaMap() {
        Map map = createTestObject().toJavaObject(Map.class);
        Assert.assertNotNull(map);
        Assert.assertTrue(map.get("number") instanceof Number);
    }

    private static class TestNestedClass {
        private String str;

        public TestNestedClass() {
        }

        public String getString() {
            return str;
        }
    }

    private static class TestClass {
        private boolean bool;
        private int number;

        private String str;

        private Object nothing;
        private TestNestedClass obj;

        private List<Integer> array;

        public TestClass() {
            nothing = "something";
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

        public Object getNothing() {
            return nothing;
        }

        public TestNestedClass getNested() {
            return obj;
        }

        public List<Integer> getArray() {
            return array;
        }
    }

    @Test
    public void objectToJavaClass() {
        TestClass obj = createTestObject().toJavaObject(TestClass.class);
        Assert.assertNotNull(obj);
        Assert.assertTrue(obj.getBool());
        Assert.assertEquals(13, obj.getNumber());
        Assert.assertEquals("hello", obj.getString());
        Assert.assertNull(obj.getNothing());
        Assert.assertNotNull(obj.getNested());
        Assert.assertEquals("test", obj.getNested().getString());
        Assert.assertNotNull(obj.getArray());
        Assert.assertEquals(2, obj.getArray().size());
        Assert.assertEquals(1, (int)obj.getArray().get(0));
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
