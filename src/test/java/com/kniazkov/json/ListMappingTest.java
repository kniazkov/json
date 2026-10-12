/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ListMappingTest {
    static class Item {
        int value;
    }

    static class Lists {
        List<Byte> bytes;
        List<Short> shorts;
        List<Integer> ints;
        List<Long> longs;
        List<Float> floats;
        List<Double> doubles;
        List<Boolean> booleans;
        List<String> strings;
        List<Item> items;
        List<List<Integer>> nested;
        ArrayList<LinkedList<List<String>>> mixed;
        List raw;
        List<Object> objects;
        List<Integer> initialized = new ArrayList<>(List.of(9));
    }

    @Test
    public void nullElementsRetainTheirPositionsAndTypes() throws Exception {
        Lists value =
                Json.parse(
                        "{bytes:[null,1],shorts:[null,2],ints:[null,3],longs:[null,4],"
                                + "floats:[null,1.5],doubles:[null,2.5],"
                                + "booleans:[null,true],strings:[null,'x'],items:[null,{value:7}]}",
                        Lists.class);
        List<?>[] lists = {
            value.bytes,
            value.shorts,
            value.ints,
            value.longs,
            value.floats,
            value.doubles,
            value.booleans,
            value.strings,
            value.items
        };
        Object[] expected = {(byte) 1, (short) 2, 3, 4L, 1.5f, 2.5, true, "x"};
        for (int i = 0; i < lists.length; i++) {
            Assert.assertEquals(2, lists[i].size());
            Assert.assertNull(lists[i].get(0));
            if (i < expected.length) {
                Assert.assertEquals(expected[i], lists[i].get(1));
            }
        }
        Assert.assertEquals(7, value.items.get(1).value);
    }

    @Test
    public void nullEmptyAndAbsentFieldsRemainDistinct() throws Exception {
        Lists value =
                Json.parse(
                        "{ints:null,nested:null,mixed:null,raw:null,objects:null,initialized:null}",
                        Lists.class);
        Assert.assertNull(value.ints);
        Assert.assertNull(value.nested);
        Assert.assertNull(value.mixed);
        Assert.assertNull(value.raw);
        Assert.assertNull(value.objects);
        Assert.assertNull(value.initialized);
        Lists empty = Json.parse("{ints:[]}", Lists.class);
        Assert.assertTrue(empty.ints.isEmpty());
        Assert.assertEquals(List.of(9), empty.initialized);
    }

    @Test
    public void nestedListsPreserveNullsShapeAndImplementations() throws Exception {
        Lists value =
                Json.parse(
                        "{nested:[[1,null,2],null,[]],mixed:[[['x',null],null,[]],null]}",
                        Lists.class);
        Assert.assertEquals(
                Arrays.asList(Arrays.asList(1, null, 2), null, List.of()), value.nested);
        Assert.assertEquals(ArrayList.class, value.nested.getClass());
        Assert.assertEquals(ArrayList.class, value.mixed.getClass());
        Assert.assertEquals(LinkedList.class, value.mixed.get(0).getClass());
        Assert.assertEquals(Arrays.asList("x", null), value.mixed.get(0).get(0));
        Assert.assertNull(value.mixed.get(0).get(1));
        Assert.assertTrue(value.mixed.get(0).get(2).isEmpty());
        Assert.assertNull(value.mixed.get(1));
        Lists copy = Json.parse(Json.serialize(value), Lists.class);
        Assert.assertEquals(value.nested, copy.nested);
        Assert.assertEquals(value.mixed, copy.mixed);
    }

    @Test
    public void rawAndObjectListsRetainNaturalValues() throws Exception {
        Lists value =
                Json.parse(
                        "{raw:[null,1,[2,null],{x:3}],objects:[null,1,[2,null],{x:3}]}",
                        Lists.class);
        Assert.assertEquals(value.raw, value.objects);
        Assert.assertNull(value.raw.get(0));
        Assert.assertEquals(BigInteger.ONE, value.raw.get(1));
        Assert.assertEquals(Arrays.asList(BigInteger.TWO, null), value.raw.get(2));
        Assert.assertEquals(Map.of("x", BigInteger.valueOf(3)), value.raw.get(3));
        for (Class<?> type : List.of(List.class, ArrayList.class, LinkedList.class)) {
            Object root = Json.parse("[null,1]", type);
            Assert.assertEquals(Arrays.asList(null, BigInteger.ONE), root);
            Assert.assertEquals(
                    type == LinkedList.class ? LinkedList.class : ArrayList.class, root.getClass());
        }
    }

    static class Wildcard {
        List<?> values;
    }

    static class UpperBound {
        List<? extends Number> values;
    }

    static class LowerBound {
        List<? super Integer> values;
    }

    static class Variable<T> {
        List<T> values;
    }

    static class BoundVariable extends Variable<Integer> {}

    static class NestedMap {
        List<Map<String, Integer>> values;
    }

    static class ArrayElement {
        List<int[]> values;
    }

    static class CustomList extends ArrayList<Integer> {}

    static class Custom {
        CustomList values;
    }

    @Test
    public void unsupportedTypesFailEvenForEmptyOrNullLists() {
        for (Class<?> type :
                List.of(
                        Wildcard.class,
                        UpperBound.class,
                        LowerBound.class,
                        Variable.class,
                        BoundVariable.class,
                        Custom.class)) {
            for (String input : List.of("{values:[]}", "{values:null}", "{values:[null]}")) {
                JsonDataException error =
                        Assert.assertThrows(JsonDataException.class, () -> Json.parse(input, type));
                Assert.assertTrue(error.getMessage(), error.getMessage().contains("values"));
                Assert.assertTrue(error.getMessage(), error.getMessage().contains("unsupported"));
            }
        }
    }

    @Test
    public void wrongListShapeReportsFieldAndIndex() {
        JsonDataException field =
                Assert.assertThrows(
                        JsonDataException.class, () -> Json.parse("{ints:{}}", Lists.class));
        Assert.assertTrue(field.getMessage().contains("$[\"ints\"]"));
        JsonDataException nested =
                Assert.assertThrows(
                        JsonDataException.class,
                        () -> Json.parse("{nested:[[1],42]}", Lists.class));
        Assert.assertTrue(nested.getMessage().contains("$[\"nested\"][1]"));
        JsonDataException item =
                Assert.assertThrows(
                        JsonDataException.class, () -> Json.parse("{items:[5]}", Lists.class));
        Assert.assertTrue(item.getMessage().contains("$[\"items\"][0]"));
    }
}
