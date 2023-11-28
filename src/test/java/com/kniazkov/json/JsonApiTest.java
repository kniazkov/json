package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Tests covering {@link Json} class.
 */
public class JsonApiTest {
    private static final String resources = "src/test/resources/";

    @Test
    public void parsingFile() {
        JsonElement root = null;
        boolean oops = false;
        try {
            root = Json.parse(new File(resources + "simple.json"));
        } catch (JsonException ignored) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(root);
        JsonObject obj = root.toJsonObject();
        Assert.assertNotNull(obj);
        Assert.assertTrue(obj.containsKey("str"));
        Assert.assertEquals("hello", obj.get("str").getStringValue());
    }
}
