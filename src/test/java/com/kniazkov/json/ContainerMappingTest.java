/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.junit.Assert;
import org.junit.Test;

public class ContainerMappingTest {
    static class Item {
        int value;
    }

    static class Containers {
        int[] numbers = {9};
        Item[] items;
        List<Map<String, Integer>> maps;
        List<int[]> arrays;
        Map<String, List<Item[]>> mixed;
        List<Integer>[] generic;
        Map<String, Integer> values = new LinkedHashMap<>(Map.of("default", 9));
    }

    @Test
    public void allPrimitiveArraysRoundTrip() throws Exception {
        Object[] arrays = {
            new byte[] {-128, 127},
            new short[] {-32768, 32767},
            new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE},
            new long[] {Long.MIN_VALUE, Long.MAX_VALUE},
            new float[] {1.5f, -2.5f},
            new double[] {1.5, -2.5},
            new boolean[] {true, false},
            new char[] {'a', '\n', '\uD800'}
        };
        for (Object input : arrays) {
            JsonElement tree = JsonParser.parseJavaObject(input);
            Object copy = tree.toJavaObject(input.getClass());
            Assert.assertEquals(input.getClass(), copy.getClass());
            Assert.assertEquals(tree, JsonParser.parseJavaObject(copy));
        }
    }

    @Test
    public void referenceRaggedEmptyAndNullArrays() throws Exception {
        int[][] value = Json.parse("[[1,2],null,[],[3]]", int[][].class);
        Assert.assertArrayEquals(new int[] {1, 2}, value[0]);
        Assert.assertNull(value[1]);
        Assert.assertEquals(0, value[2].length);
        Assert.assertArrayEquals(new int[] {3}, value[3]);
        Assert.assertNull(Json.parse("null", int[].class));
        Assert.assertArrayEquals(
                new Integer[] {1, null, 2}, Json.parse("[1,null,2]", Integer[].class));
        Item[] items = Json.parse("[{value:7},null]", Item[].class);
        Assert.assertEquals(7, items[0].value);
        Assert.assertNull(items[1]);
        Object[] objects = Json.parse("[1,1.5,null,[2],{a:3}]", Object[].class);
        Assert.assertEquals(BigInteger.ONE, objects[0]);
        Assert.assertEquals(new BigDecimal("1.5"), objects[1]);
        Assert.assertNull(objects[2]);
        Assert.assertEquals(List.of(BigInteger.TWO), objects[3]);
        Assert.assertEquals(Map.of("a", BigInteger.valueOf(3)), objects[4]);
    }

    @Test
    public void typedFieldsAndGenericArraysRoundTrip() throws Exception {
        String input =
                "{numbers:[1,2],items:[{value:3},null],maps:[{x:4}],arrays:[[5]],"
                        + "mixed:{a:[[{value:6},null],null]},generic:[[7,null],null,[]],values:{x:8}}";
        Containers value = Json.parse(input, Containers.class);
        Assert.assertEquals(Integer.valueOf(4), value.maps.get(0).get("x"));
        Assert.assertArrayEquals(new int[] {5}, value.arrays.get(0));
        Assert.assertEquals(6, value.mixed.get("a").get(0)[0].value);
        Assert.assertEquals(List[].class, value.generic.getClass());
        Assert.assertEquals(Arrays.asList(7, null), value.generic[0]);
        Assert.assertNull(value.generic[1]);
        Assert.assertTrue(value.generic[2].isEmpty());
        Assert.assertEquals(Json.parse(input), JsonParser.parseJavaObject(value));
        Containers absent = Json.parse("{}", Containers.class);
        Assert.assertArrayEquals(new int[] {9}, absent.numbers);
        Assert.assertEquals(Map.of("default", 9), absent.values);
        Containers nil = Json.parse("{numbers:null,values:null}", Containers.class);
        Assert.assertNull(nil.numbers);
        Assert.assertNull(nil.values);
    }

    @Test
    public void genericRootsKeepNestedTypes() throws Exception {
        var target = new JsonType<Map<String, List<Integer>>>() {};
        Map<String, List<Integer>> value = Json.parse("{b:[1,null],a:[]}").toJavaObject(target);
        Assert.assertEquals(Arrays.asList(1, null), value.get("b"));
        Assert.assertEquals(List.of("b", "a"), new ArrayList<>(value.keySet()));
        value.put("c", new ArrayList<>());
        var rows =
                Json.parse("[{a:[1,2]},null]")
                        .toJavaObject(new JsonType<List<Map<String, int[]>>>() {});
        Assert.assertArrayEquals(new int[] {1, 2}, rows.get(0).get("a"));
        Assert.assertNull(rows.get(1));
        var arrays =
                Json.parse("[[{a:[3]}],null]")
                        .toJavaObject(new JsonType<Map<String, List<Integer>>[][]>() {});
        Assert.assertEquals(Map[][].class, arrays.getClass());
        Assert.assertEquals(List.of(3), arrays[0][0].get("a"));
        Assert.assertNull(arrays[1]);
        Assert.assertEquals(
                Integer.valueOf(3), Json.parse("3").toJavaObject(new JsonType<Integer>() {}));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void mapImplementationsAndNaturalValues() throws Exception {
        for (Class<?> target :
                List.of(
                        Map.class,
                        HashMap.class,
                        LinkedHashMap.class,
                        TreeMap.class,
                        SortedMap.class,
                        NavigableMap.class)) {
            Map value = (Map) Json.parse("{b:1,a:[null,2]}", target);
            Class<?> expected =
                    target == Map.class
                            ? LinkedHashMap.class
                            : target == SortedMap.class || target == NavigableMap.class
                                    ? TreeMap.class
                                    : target;
            Assert.assertEquals(expected, value.getClass());
            Assert.assertEquals(BigInteger.ONE, value.get("b"));
            Assert.assertEquals(Arrays.asList(null, BigInteger.TWO), value.get("a"));
            value.put("c", null);
            Assert.assertTrue(value.containsKey("c"));
            Assert.assertNull(Json.parse("null", target));
            Assert.assertTrue(((Map) Json.parse("{}", target)).isEmpty());
            if (value instanceof SortedMap) {
                Assert.assertEquals(List.of("a", "b", "c"), new ArrayList<>(value.keySet()));
            }
        }
        var sorted =
                Json.parse("{b:2,a:null}")
                        .toJavaObject(new JsonType<NavigableMap<String, Integer>>() {});
        Assert.assertEquals("a", sorted.firstKey());
        Assert.assertNull(sorted.get("a"));
        Assert.assertEquals(Integer.valueOf(2), sorted.get("b"));
        Map natural = (Map) Json.parse("{a:1}").toJavaObject();
        Assert.assertThrows(UnsupportedOperationException.class, () -> natural.put("b", 2));
    }

    @Test
    public void typedConcreteMapsConvertValues() throws Exception {
        List<JsonType<? extends Map<String, Integer>>> targets =
                List.of(
                        new JsonType<HashMap<String, Integer>>() {},
                        new JsonType<LinkedHashMap<String, Integer>>() {},
                        new JsonType<TreeMap<String, Integer>>() {},
                        new JsonType<SortedMap<String, Integer>>() {});
        for (var target : targets) {
            Map<String, Integer> value = Json.parse("{x:7,n:null}").toJavaObject(target);
            Assert.assertEquals(Integer.valueOf(7), value.get("x"));
            Assert.assertNull(value.get("n"));
            Assert.assertThrows(
                    JsonDataException.class, () -> Json.parse("{x:1.5}").toJavaObject(target));
        }
    }

    @Test
    public void exactAndSpecialNumbersFollowScalarPolicy() throws Exception {
        BigInteger huge = BigInteger.ONE.shiftLeft(200);
        Assert.assertArrayEquals(
                new BigInteger[] {huge, null},
                Json.parse("[" + huge + ",null]", BigInteger[].class));
        var decimals =
                Json.parse("{x:1.234567890123456789}")
                        .toJavaObject(new JsonType<Map<String, BigDecimal>>() {});
        Assert.assertEquals(new BigDecimal("1.234567890123456789"), decimals.get("x"));
        Assert.assertArrayEquals(
                new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY},
                Json.parse("[NaN,Infinity,-Infinity]", double[].class),
                0);
        Assert.assertThrows(JsonDataException.class, () -> Json.parse("[NaN]", int[].class));
        Assert.assertThrows(
                JsonDataException.class, () -> Json.parse("[Infinity]", BigDecimal[].class));
    }

    @Test
    public void strictArrayFailuresReportIndex() {
        Object[][] cases = {
            {"[null]", int[].class},
            {"[128]", byte[].class},
            {"[32768]", short[].class},
            {"[2147483648]", int[].class},
            {"[9223372036854775808]", long[].class},
            {"[1.5]", int[].class},
            {"[1e400]", double[].class},
            {"[1e-400]", double[].class},
            {"[1e40]", float[].class},
            {"['12']", int[].class},
            {"[1]", boolean[].class},
            {"['ab']", char[].class},
            {"['😀']", char[].class}
        };
        for (Object[] entry : cases) {
            JsonDataException error =
                    Assert.assertThrows(
                            JsonDataException.class,
                            () -> Json.parse((String) entry[0], (Class<?>) entry[1]));
            Assert.assertTrue(error.getMessage(), error.getMessage().contains("$[0]"));
        }
        Assert.assertThrows(JsonDataException.class, () -> Json.parse("'AQ=='", byte[].class));
        Assert.assertThrows(JsonDataException.class, () -> Json.parse("{}", int[].class));
        Assert.assertThrows(JsonDataException.class, () -> Json.parse("[]", Map.class));
    }

    @Test
    public void mapErrorsCarryKeysAndIndices() {
        var type = new JsonType<Map<String, List<int[]>>>() {};
        JsonDataException error =
                Assert.assertThrows(
                        JsonDataException.class,
                        () -> Json.parse("{'a.b':[[1,null]]}").toJavaObject(type));
        Assert.assertTrue(error.getMessage(), error.getMessage().contains("$[\"a.b\"][0][1]"));
        var numbers = new JsonType<Map<String, Byte>>() {};
        Assert.assertThrows(
                JsonDataException.class, () -> Json.parse("{x:128}").toJavaObject(numbers));
    }

    static class CustomMap extends HashMap<String, Integer> {}

    static class Variable<T> {
        Map<String, T> values;
        T[] array;
    }

    static class Bound extends Variable<Integer> {}

    @Test
    public void unsupportedTypesFailEvenForNullOrEmptyInput() {
        List<JsonType<?>> types =
                List.of(
                        new JsonType<Map<Integer, String>>() {},
                        new JsonType<Map<Object, Integer>>() {},
                        new JsonType<Map<String, ?>>() {},
                        new JsonType<Map<String, List<? extends Number>>>() {},
                        new JsonType<CustomMap>() {},
                        new JsonType<Variable<Integer>>() {});
        for (JsonType<?> type : types) {
            for (String input : List.of("null", "{}")) {
                Assert.assertThrows(
                        JsonDataException.class, () -> Json.parse(input).toJavaObject(type));
            }
        }
        for (String input : List.of("null", "[]")) {
            Assert.assertThrows(
                    JsonDataException.class,
                    () -> Json.parse(input).toJavaObject(new JsonType<List<?>[]>() {}));
        }
        for (String input : List.of("{values:{}}", "{values:null}", "{array:[]}", "{array:null}")) {
            Assert.assertThrows(JsonDataException.class, () -> Json.parse(input, Bound.class));
        }
        Assert.assertThrows(IllegalArgumentException.class, () -> new JsonType() {});
    }

    static class Soft {
        @JsonProperty(
                onInvalid = JsonMappingAction.KEEP_DEFAULT,
                onNull = JsonMappingAction.KEEP_DEFAULT)
        int[] numbers = {9};

        @JsonProperty(onInvalid = JsonMappingAction.KEEP_DEFAULT)
        Map<String, int[]> kept = new LinkedHashMap<>(Map.of("default", new int[] {9}));

        @JsonProperty(onInvalid = JsonMappingAction.SET_NULL)
        Map<String, int[]> discarded = new LinkedHashMap<>();

        @JsonProperty(onNull = JsonMappingAction.ERROR)
        int[] required;
    }

    @Test
    public void softPoliciesApplyToWholeContainer() throws Exception {
        Soft value =
                Json.parse(
                        "{numbers:[1,null],kept:{ok:[1],bad:[null]},discarded:{bad:[null]}}",
                        Soft.class);
        Assert.assertArrayEquals(new int[] {9}, value.numbers);
        Assert.assertEquals(Set.of("default"), value.kept.keySet());
        Assert.assertArrayEquals(new int[] {9}, value.kept.get("default"));
        Assert.assertNull(value.discarded);
        Assert.assertArrayEquals(new int[] {9}, Json.parse("{numbers:null}", Soft.class).numbers);
        Assert.assertThrows(
                JsonDataException.class, () -> Json.parse("{required:null}", Soft.class));
    }

    static class Node {
        Node[] children;
        Map<String, Node> links;
    }

    @Test
    public void deepArrayMapPojoMappingUsesHeapStack() throws Exception {
        int depth = 20000;
        String input = "{children:[{links:{next:".repeat(depth) + "{}" + "}}]}".repeat(depth);
        Node node = Json.parse(input, Node.class);
        for (int i = 0; i < depth; i++) {
            Assert.assertEquals(1, node.children.length);
            node = node.children[0].links.get("next");
            Assert.assertNotNull(node);
        }
        Assert.assertNull(node.children);
    }
}
