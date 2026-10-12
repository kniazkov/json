/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Assert;
import org.junit.Test;

public class ArrayMapSerializationTest {
    private JsonElement serialize(Object value) throws Exception {
        JsonElement tree = JsonParser.parseJavaObject(value);
        assertParents(tree, null);
        String text = Json.serialize(value);
        Assert.assertEquals(tree.toString(), text);
        Assert.assertEquals(text, Json.parse(text, JsonParsingMode.STRICT).toString());
        Assert.assertEquals(
                text, Json.parse(Json.serialize(value, "  "), JsonParsingMode.STRICT).toString());
        return tree;
    }

    private void assertParents(JsonElement value, JsonContainer parent) {
        Assert.assertSame(parent, value.parent().orElse(null));
        if (value instanceof JsonArray array) {
            for (JsonElement item : array) {
                assertParents(item, array);
            }
        } else if (value instanceof JsonObject object) {
            for (JsonElement item : object.values()) {
                assertParents(item, object);
            }
        }
    }

    @Test
    public void allPrimitiveArrayTypesAndEmptyArrays() throws Exception {
        Object[] arrays = {
            new byte[] {-128, 127},
            new short[] {-32768, 32767},
            new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE},
            new long[] {Long.MIN_VALUE, Long.MAX_VALUE},
            new float[] {1.5f, -2.5f},
            new double[] {1.25, -2.25},
            new boolean[] {true, false},
            new char[] {'a', '\n'}
        };
        String[] expected = {
            "[-128, 127]",
            "[-32768, 32767]",
            "[-2147483648, 2147483647]",
            "[-9223372036854775808, 9223372036854775807]",
            "[1.5, -2.5]",
            "[1.25, -2.25]",
            "[true, false]",
            "[\"a\", \"\\n\"]"
        };
        for (int i = 0; i < arrays.length; i++) {
            Assert.assertEquals(expected[i], serialize(arrays[i]).toString());
            Object empty =
                    java.lang.reflect.Array.newInstance(arrays[i].getClass().getComponentType(), 0);
            Assert.assertEquals("[]", serialize(empty).toString());
        }
    }

    @Test
    public void charactersUseStringEscaping() throws Exception {
        Assert.assertEquals("\"x\"", serialize('x').toString());
        Assert.assertEquals(
                "[\"\\\"\", \"\\\\\", \"\\u0000\", null]",
                serialize(new Character[] {'"', '\\', '\0', null}).toString());
    }

    static class Item {
        int value = 7;
    }

    static class Holder {
        int[] numbers = {1, 2};
        Map<String, Object> properties = Map.of("items", new Item[] {new Item(), null});
        char character = 'z';
    }

    @Test
    public void objectArraysAndFieldsAreSerializedRecursively() throws Exception {
        JsonArray array =
                serialize(new Object[] {new Holder(), null, "text", new Item()})
                        .arrayValueOrThrow();
        Assert.assertEquals(4, array.size());
        JsonObject holder = array.get(0).objectValueOrThrow();
        Assert.assertEquals("[1, 2]", holder.element("numbers").orElseThrow().toString());
        Assert.assertEquals(
                "{\"items\": [{\"value\": 7}, null]}",
                holder.element("properties").orElseThrow().toString());
        Assert.assertEquals("z", holder.element("character").orElseThrow().stringValue());
        Assert.assertTrue(array.get(1).isNull());
        Assert.assertEquals("[]", serialize(new String[0]).toString());
        Assert.assertEquals("null", serialize(null).toString());
    }

    @Test
    public void raggedAndMultidimensionalArraysPreserveShape() throws Exception {
        Assert.assertEquals(
                "[[1, 2], [], null, [3]]",
                serialize(new int[][] {{1, 2}, {}, null, {3}}).toString());
        Assert.assertEquals(
                "[[[\"a\", null]], null]",
                serialize(new String[][][] {{{"a", null}}, null}).toString());
    }

    @Test
    public void mapsUseEntriesAndIterationOrder() throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("z", new int[] {1, 2});
        map.put("a\"\n", null);
        map.put("nested", Arrays.asList(Map.of("value", new Item()), new boolean[] {true}));
        JsonObject json = serialize(map).objectValueOrThrow();
        Assert.assertEquals(List.of("z", "a\"\n", "nested"), List.copyOf(json.keySet()));
        Assert.assertEquals("[1, 2]", json.element("z").orElseThrow().toString());
        Assert.assertTrue(json.element("a\"\n").orElseThrow().isNull());
        Assert.assertEquals(
                "[{\"value\": {\"value\": 7}}, [true]]",
                json.element("nested").orElseThrow().toString());
        for (Map<String, Integer> value :
                List.of(
                        new HashMap<String, Integer>(),
                        new TreeMap<String, Integer>(),
                        Map.<String, Integer>of())) {
            Assert.assertEquals("{}", serialize(value).toString());
        }
        Assert.assertEquals("{\"a\": 1}", serialize(new HashMap<>(Map.of("a", 1))).toString());
        Assert.assertEquals("{\"a\": 1}", serialize(Map.of("a", 1)).toString());
        Assert.assertEquals(
                "{\"a\": 1, \"b\": 2}",
                serialize(new TreeMap<>(Map.of("b", 2, "a", 1))).toString());
    }

    @Test
    public void invalidAndDuplicateMapKeysAreRejected() {
        for (Object key : new Object[] {null, 1, true, new Object()}) {
            Map<Object, Object> map = new HashMap<>();
            map.put(key, 42);
            Assert.assertThrows(IllegalArgumentException.class, () -> Json.serialize(map));
            Assert.assertThrows(IllegalArgumentException.class, () -> Json.serialize(List.of(map)));
        }
        Map<String, Integer> identity = new IdentityHashMap<>();
        identity.put(new String("same"), 1);
        identity.put(new String("same"), 2);
        Assert.assertEquals(2, identity.size());
        Assert.assertThrows(IllegalArgumentException.class, () -> Json.serialize(identity));
    }

    @Test
    public void arraysKeepExistingNonFiniteNumberPolicy() throws Exception {
        String text =
                Json.serialize(
                        new double[] {
                            Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
                        });
        Assert.assertEquals("[NaN, Infinity, -Infinity]", text);
        JsonArray values = Json.parse(text, JsonParsingMode.JSON5).arrayValueOrThrow();
        Assert.assertTrue(values.get(0).isNaN());
        Assert.assertTrue(values.get(1).isPositiveInfinity());
        Assert.assertTrue(values.get(2).isNegativeInfinity());
    }
}
