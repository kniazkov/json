/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.io.File;
import org.junit.Assert;
import org.junit.Test;

/** Tests covering {@link Json} class. */
public class JsonApiTest {
    private static final String RESOURCES = "src/test/resources/";

    private static final String SEPARATOR = System.lineSeparator();

    static class Simple {
        String str;

        private Simple() {}
    }

    @Test
    public void parsingStringInDifferentModes() {
        final String json = "{str: \"hello\"}";
        Simple obj = null;
        boolean oops = false;
        try {
            obj = Json.parse(json, Simple.class, JsonParsingMode.JSON5);
        } catch (JsonException ignored) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(obj);
        Assert.assertEquals("hello", obj.str);

        obj = null;
        try {
            obj = Json.parse(json, Simple.class, JsonParsingMode.STRICT);
        } catch (JsonException ignored) {
            oops = true;
        }
        Assert.assertTrue(oops);
        Assert.assertNull(obj);
    }

    @Test
    public void parsingFile() {
        JsonElement root = null;
        boolean oops = false;
        try {
            root = Json.parse(new File(RESOURCES + "simple.json"));
        } catch (JsonException ignored) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(root);
        JsonObject obj = root.toJsonObject();
        Assert.assertNotNull(obj);
        Assert.assertTrue(obj.containsKey("str"));
        Assert.assertEquals("hello", obj.get("str").stringValue());
    }

    @Test
    public void parsingFileToObject() {
        Simple obj = null;
        boolean oops = false;
        try {
            obj = Json.parse(new File(RESOURCES + "simple.json"), Simple.class);
        } catch (JsonException ignored) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(obj);
        Assert.assertEquals("hello", obj.str);
    }

    @Test
    public void parsingFileInDifferentModes() {
        final File file = new File(RESOURCES + "simple.json");
        Simple obj = null;
        boolean oops = false;
        try {
            obj = Json.parse(file, Simple.class, JsonParsingMode.JSON5);
        } catch (JsonException ignored) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(obj);
        Assert.assertEquals("hello", obj.str);

        obj = null;
        try {
            obj = Json.parse(file, Simple.class, JsonParsingMode.STRICT);
        } catch (JsonException ignored) {
            oops = true;
        }
        Assert.assertTrue(oops);
        Assert.assertNull(obj);
    }

    static class Vector {
        double x;

        double y;

        double z;
    }

    @Test
    public void serialization() {
        String expected =
                "{"
                        + SEPARATOR
                        + "  \"x\": 100.0,"
                        + SEPARATOR
                        + "  \"y\": 200.0,"
                        + SEPARATOR
                        + "  \"z\": 300.0"
                        + SEPARATOR
                        + "}";
        Vector v = new Vector();
        v.x = 100;
        v.y = 200;
        v.z = 300;
        String actual = Json.serialize(v, "  ");
        Assert.assertEquals(expected, actual);
    }
}
