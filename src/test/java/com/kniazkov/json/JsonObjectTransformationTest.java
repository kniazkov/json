/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Tests covering {@link JsonObject} class, method {@link JsonObject#toJavaObject(Class)}.
 */
public class JsonObjectTransformationTest {
    private static class Numbers {
        byte byteValue;

        short shortValue;

        @JsonProperty(name = "int")
        int intValue;

        long longValue;

        float floatValue;

        double doubleValue;

        private Numbers() {
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Numbers)) {
                return false;
            }
            Numbers nn = (Numbers) other;
            return byteValue == nn.byteValue && shortValue == nn.shortValue && intValue == nn.intValue &&
                    longValue == nn.longValue && floatValue == nn.floatValue && doubleValue == nn.doubleValue;
        }
    };

    @Test
    public void parsingNumbers() {
        Numbers obj = null;
        boolean oops = false;
        try {
            obj = Json.parse("{byteValue: 3, shortValue: 5, int: 13, longValue: 17, floatValue: 19.01, doubleValue: 23.001}", Numbers.class);
        } catch (JsonException ignored) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(obj);
        Assert.assertEquals(3, obj.byteValue);
        Assert.assertEquals(5, obj.shortValue);
        Assert.assertEquals(13, obj.intValue);
        Assert.assertEquals(17, obj.longValue);
        Assert.assertEquals((float)19.01, obj.floatValue, 0);
        Assert.assertEquals(23.001, obj.doubleValue, 0);
    }

    private static class NumbersAsObjects {
        Byte byteValue;

        Short shortValue;

        Integer intValue;

        Long longValue;

        Float floatValue;

        Double doubleValue;


        @Override
        public boolean equals(Object other) {
            if (!(other instanceof NumbersAsObjects)) {
                return false;
            }
            NumbersAsObjects nn = (NumbersAsObjects) other;
            return Objects.equals(byteValue, nn.byteValue) && Objects.equals(shortValue, nn.shortValue) &&
                    Objects.equals(intValue, nn.intValue) && Objects.equals(longValue, nn.longValue) &&
                    Objects.equals(floatValue, nn.floatValue) && Objects.equals(doubleValue, nn.doubleValue);
        }
    };

    @Test
    public void parsingNumbersAsObjects() {
        NumbersAsObjects obj = null;
        boolean oops = false;
        try {
            obj = Json.parse("{byteValue: 3, shortValue: 5, intValue: 13, longValue: 17, floatValue: 19.01, doubleValue: 23.001}", NumbersAsObjects.class);
        } catch (JsonException ignored) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(obj);
        Assert.assertNotNull(obj.byteValue);
        Assert.assertEquals(3, obj.byteValue.byteValue());
        Assert.assertNotNull(obj.shortValue);
        Assert.assertEquals(5, obj.shortValue.shortValue());
        Assert.assertNotNull(obj.intValue);
        Assert.assertEquals(13, obj.intValue.intValue());
        Assert.assertNotNull(obj.longValue);
        Assert.assertEquals(17, obj.longValue.longValue());
        Assert.assertNotNull(obj.floatValue);
        Assert.assertEquals((float)19.01, obj.floatValue.floatValue(), 0);
        Assert.assertNotNull(obj.doubleValue);
        Assert.assertEquals(23.001, obj.doubleValue.doubleValue(), 0);
    }

    private static class Booleans {
        boolean primitive;

        Boolean object;

        Boolean anotherObject;

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Booleans)) {
                return false;
            }
            Booleans bb = (Booleans) other;
            return primitive == bb.primitive && Objects.equals(object, bb.object) &&
                    Objects.equals(anotherObject, bb.anotherObject);
        }
    }

    @Test
    public void parsingBooleans() {
        Booleans obj = null;
        boolean oops = false;
        try {
            obj = Json.parse("{primitive: true, object: false}", Booleans.class);
        } catch (JsonException ignored) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(obj);
        Assert.assertTrue(obj.primitive);
        Assert.assertNotNull(obj.object);
        Assert.assertFalse(obj.object.booleanValue());
        Assert.assertNull(obj.anotherObject);
    }

    private static class Vector {
        int x;

        int y;

        public Vector() {
        }

        public Vector(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Vector)) {
                return false;
            }
            Vector vv = (Vector) other;
            return x == vv.x && y == vv.y;
        }
    }

    private static class Lists {
        LinkedList<Byte> bytes;

        ArrayList<Short> shorts;

        List<Integer> integers;

        List<Long> longs;

        List<Float> floats;

        List<Double> doubles;

        List<Boolean> booleans;

        List<String> strings;

        List generic;

        List<Object> objects;

        List<Vector> vectors;


        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Lists)) {
                return false;
            }
            Lists ll = (Lists) other;
            return compareTwoLists(bytes, ll.bytes) && compareTwoLists(shorts, ll.shorts) &&
                    compareTwoLists(integers, ll.integers) && compareTwoLists(longs, ll.longs) &&
                    compareTwoLists(floats, ll.floats) && compareTwoLists(doubles, ll.doubles) &&
                    compareTwoLists(booleans, ll.booleans) && compareTwoLists(strings, ll.strings) &&
                    compareTwoLists(generic, ll.generic) && compareTwoLists(objects, ll.objects) &&
                    compareTwoLists(vectors, ll.vectors);
        }
    }

    @Test
    public void parsingAndSerializeLists() {
        Lists obj = null;
        boolean oops = false;
        try {
            obj = Json.parse("{bytes: [1,2,3], shorts: [1,2,3], integers: [1,2,3], longs: [1,2,3]," +
                    "floats: [1.01, 2.02, 3.03], doubles: [1.01, 2.02, 3.03], booleans: [true, false]," +
                    "strings: ['one','two','three'], generic: ['test', 1], objects: [false, 2], " +
                    "vectors: [{x: 1, y: 2}, {x: 3, y: 4}] }", Lists.class);
        } catch (JsonException ignored) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(obj);
        Assert.assertTrue(compareTwoLists(Arrays.asList((byte)1,(byte)2,(byte)3), obj.bytes));
        Assert.assertTrue(compareTwoLists(Arrays.asList((short)1,(short)2,(short)3), obj.shorts));
        Assert.assertTrue(compareTwoLists(Arrays.asList(1,2,3), obj.integers));
        Assert.assertTrue(compareTwoLists(Arrays.asList((long)1,(long)2,(long)3), obj.longs));
        Assert.assertTrue(compareTwoLists(Arrays.asList((float)1.01,(float)2.02,(float)3.03), obj.floats));
        Assert.assertTrue(compareTwoLists(Arrays.asList(1.01,2.02,3.03), obj.doubles));
        Assert.assertTrue(compareTwoLists(Arrays.asList(true, false), obj.booleans));
        Assert.assertTrue(compareTwoLists(Arrays.asList("one", "two", "three"), obj.strings));
        Assert.assertTrue(compareTwoLists(Arrays.asList((Object)"test", (Object)1.0), obj.generic));
        Assert.assertTrue(compareTwoLists(Arrays.asList((Object)false, (Object)2.0), obj.objects));
        Assert.assertTrue(serializeAndThenParse(obj));
    }

    private static <T> boolean compareTwoLists(List<T> expected, List<T> actual) {
        if (actual == null) {
            return false;
        }
        if (expected.size() != actual.size()) {
            return false;
        }
        Iterator<T> iterator = actual.iterator();
        for (T item : expected) {
            if (!item.equals(iterator.next())) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void serializePrimitives() {
        Assert.assertTrue(serializeAndThenParse((byte)13));
        Assert.assertTrue(serializeAndThenParse((short)13));
        Assert.assertTrue(serializeAndThenParse(13));
        Assert.assertTrue(serializeAndThenParse((long)13));
        Assert.assertTrue(serializeAndThenParse((float)13.01));
        Assert.assertTrue(serializeAndThenParse(13.01));
        Assert.assertTrue(serializeAndThenParse(true));
        Assert.assertTrue(serializeAndThenParse(false));
        Assert.assertTrue(serializeAndThenParse("test"));
    }

    @Test
    public void serializeLists() {
        final List<Object> objects = new LinkedList<>();
        objects.add(1.01);
        objects.add(true);
        objects.add("test");
        Assert.assertTrue(serializeAndThenParse(objects));
    }

    @Test
    public void serializeNumbers() {
        final Numbers obj = new Numbers();
        obj.byteValue = 3;
        obj.shortValue = -5;
        obj.intValue = 7;
        obj.longValue = 13;
        obj.floatValue = (float)17.01;
        obj.doubleValue = 19.0001;
        Assert.assertTrue(serializeAndThenParse(obj));
    }

    @Test
    public void serializeNumbersAsObjects() {
        final NumbersAsObjects obj = new NumbersAsObjects();
        obj.byteValue = 3;
        obj.shortValue = -5;
        obj.intValue = 7;
        obj.longValue = (long)13;
        obj.floatValue = (float)17.01;
        obj.doubleValue = 19.0001;
        Assert.assertTrue(serializeAndThenParse(obj));
    }

    @Test
    public void serializeBooleans() {
        final Booleans obj = new Booleans();
        obj.primitive = true;
        obj.object = false;
        obj.anotherObject = null;
        Assert.assertTrue(serializeAndThenParse(obj));
    }

    private static boolean serializeAndThenParse(Object obj) {
        return serializeAndThenParse(obj, obj);
    }

    private static boolean serializeAndThenParse(Object before, Object expectedAfter) {
        try {
            String json = Json.serialize(before);
            Object actual = Json.parse(json, expectedAfter.getClass());
            return  expectedAfter.equals(actual);
        } catch (JsonException ignored) {
            return false;
        }
    }

    private static class Base {
        int alpha;
    }

    private static class Derived extends Base {
        int beta;
    }

    @Test
    public void parsingToDerivedClass() {
        Derived obj = null;
        boolean oops = false;
        try {
            obj = Json.parse("{alpha: 10, beta: 20}", Derived.class);
        } catch (JsonException ignored) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(obj);
        Assert.assertEquals(10, obj.alpha);
        Assert.assertEquals(20, obj.beta);
    }
}
