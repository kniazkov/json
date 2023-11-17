package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Tests covering {@link JsonObject} class, method {@link JsonObject#toObject(Class)}.
 */
public class JsonObjectTransformationTest {
    private static class Numbers {
        byte byteValue;

        short shortValue;

        int intValue;

        long longValue;

        float floatValue;

        double doubleValue;

        public Numbers() {
        }
    };

    @Test
    public void numbers() {
        Numbers obj = null;
        boolean oops = false;
        try {
            obj = Json.parse("{byteValue: 3, shortValue: 5, intValue: 13, longValue: 17, floatValue: 19.01, doubleValue: 23.001}", Numbers.class);
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

        public NumbersAsObjects() {
        }
    };

    @Test
    public void numbersAsObjects() {
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

        public Booleans() {
        }
    }

    @Test
    public void booleans() {
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

    private static class Lists {
        LinkedList<Byte> bytes;
        List<Integer> integers;

        public Lists() {
        }
    }

    @Test
    public void lists() {
        Lists obj = null;
        boolean oops = false;
        try {
            obj = Json.parse("{bytes: [1,2,3], integers: [1,2,3]}", Lists.class);
        } catch (JsonException ignored) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(obj);
        Assert.assertTrue(checkList(Arrays.asList((byte)1,(byte)2,(byte)3), obj.bytes));
        Assert.assertTrue(checkList(Arrays.asList(1,2,3), obj.integers));
    }

    private static <T> boolean checkList(List<T> expected, List<T> actual) {
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
}
