/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

/** Regression tests for exact storage throughout the JSON pipeline. */
public class JsonExactNumberTest {
    @Test
    public void factoryChoosesByJavaType() {
        for (Number value : Arrays.asList((byte) 42, (short) 42, 42, 42L, BigInteger.valueOf(42))) {
            Assert.assertTrue(JsonNumber.of(value) instanceof JsonInteger);
        }
        for (Number value : Arrays.asList(42.0f, 42.0, new BigDecimal("42"))) {
            Assert.assertTrue(JsonNumber.of(value) instanceof JsonReal);
        }
        Assert.assertTrue(JsonNumber.of(42) instanceof JsonInteger);
        Assert.assertTrue(JsonNumber.of(42L) instanceof JsonInteger);
        Assert.assertTrue(JsonNumber.of(42.0) instanceof JsonReal);
        Assert.assertTrue(JsonNumber.of(42.0f) instanceof JsonReal);
        Assert.assertEquals("0.1", JsonNumber.of(0.1f).toString());
        Assert.assertEquals("0.1", JsonNumber.of(0.1).toString());
    }

    @Test
    public void invalidFactoryArgumentsAreRejected() {
        Assert.assertThrows(NullPointerException.class, () -> JsonNumber.of((Number) null));
        Assert.assertThrows(NullPointerException.class, () -> JsonNumber.of((BigInteger) null));
        Assert.assertThrows(NullPointerException.class, () -> JsonNumber.of((BigDecimal) null));
        Assert.assertThrows(
                IllegalArgumentException.class, () -> JsonNumber.of(new AtomicInteger(1)));
    }

    @Test
    public void exactIntegerParsingAndSerialization() throws Exception {
        for (String text :
                new String[] {
                    "0",
                    "-1",
                    "9007199254740993",
                    "9223372036854775807",
                    "-9223372036854775808",
                    "9223372036854775808",
                    "18446744073709551616",
                    "-1234567890123456789012345678901234567890"
                }) {
            JsonElement value = Json.parse(text, JsonParsingMode.STRICT);
            Assert.assertTrue(value instanceof JsonInteger);
            Assert.assertEquals(text, value.toString());
            Assert.assertEquals(new BigInteger(text), value.toJavaObject(BigInteger.class));
            Assert.assertEquals(text, Json.serialize(new BigInteger(text)));
        }
        Assert.assertEquals("9007199254740993", Json.serialize(9007199254740993L));
    }

    @Test
    public void exactRealParsingAndSerialization() throws Exception {
        for (String text :
                new String[] {
                    "0.12345678901234567890",
                    "-42.125000000000000001",
                    "1.0",
                    "1e3",
                    "0e1",
                    "-0E-1",
                    "1e309",
                    "1e-400",
                    "1E+2147483647",
                    "1E+2147483648",
                    "1E-2147483647"
                }) {
            JsonElement value = Json.parse(text, JsonParsingMode.STRICT);
            Assert.assertTrue(value instanceof JsonReal);
            Assert.assertEquals(new BigDecimal(text), value.toJavaObject(BigDecimal.class));
            JsonElement restored = Json.parse(value.toString(), JsonParsingMode.STRICT);
            Assert.assertTrue(restored instanceof JsonReal);
            Assert.assertEquals(
                    value.toJavaObject(BigDecimal.class), restored.toJavaObject(BigDecimal.class));
        }
        Assert.assertEquals("42.0", JsonNumber.of(new BigDecimal("42")).toString());
        Assert.assertTrue(
                Json.parse("[0e1]", JsonParsingMode.STRICT).toJsonArray().get(0)
                        instanceof JsonReal);
    }

    @Test
    public void exponentOverflowIsAnExplicitParsingError() {
        for (String text :
                new String[] {"1e4294967296", "1e-4294967296", "1e2147483649", "1.0e-2147483647"}) {
            Assert.assertThrows(
                    text, JsonException.class, () -> Json.parse(text, JsonParsingMode.STRICT));
        }
    }

    @Test
    public void json5HexAndDecimalExtensionsAreExact() throws Exception {
        for (JsonParsingMode mode :
                new JsonParsingMode[] {JsonParsingMode.JSON5, JsonParsingMode.EXTENDED}) {
            Assert.assertEquals(
                    new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFF", 16),
                    Json.parse("0XFFFFFFFFFFFFFFFFFFFFFFFF", mode).toJavaObject(BigInteger.class));
            Assert.assertEquals(
                    new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFF", 16).negate(),
                    Json.parse("-0xffffffffffffffffffffffff", mode).toJavaObject(BigInteger.class));
            for (String text : new String[] {"+.5", "-.5", "0.", "13.", "+1.25", "0.e2"}) {
                Assert.assertTrue(Json.parse(text, mode) instanceof JsonReal);
            }
        }
        Assert.assertEquals("13", Json.parse("0013", JsonParsingMode.EXTENDED).toString());
    }

    @Test
    public void malformedNumbersAreRejected() {
        for (String text :
                new String[] {"01", "-01", "+1", ".1", "1.", "0x10", "0X10", "1e", "1e+", "-"}) {
            Assert.assertThrows(
                    text, JsonException.class, () -> Json.parse(text, JsonParsingMode.STRICT));
        }
        for (String text : new String[] {".", "+.", "0x", "0Xg", "1e-"}) {
            Assert.assertThrows(
                    text, JsonException.class, () -> Json.parse(text, JsonParsingMode.JSON5));
        }
    }

    @Test
    public void existingIntegerPredicatesCheckExactRanges() {
        Assert.assertTrue(JsonNumber.of(Integer.MIN_VALUE).isInteger());
        Assert.assertTrue(JsonNumber.of(Integer.MAX_VALUE).isInteger());
        Assert.assertFalse(JsonNumber.of((long) Integer.MAX_VALUE + 1).isInteger());
        Assert.assertTrue(JsonNumber.of(Long.MIN_VALUE).isLongInteger());
        Assert.assertTrue(JsonNumber.of(Long.MAX_VALUE).isLongInteger());
        Assert.assertFalse(
                JsonNumber.of(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE))
                        .isLongInteger());
        Assert.assertTrue(JsonNumber.of(new BigDecimal("13.000")).isInteger());
        Assert.assertFalse(JsonNumber.of(new BigDecimal("13.0000000000000000001")).isInteger());
        Assert.assertFalse(JsonNumber.of(new BigDecimal("9223372036854775807.1")).isLongInteger());
        Assert.assertTrue(JsonNumber.of(new BigDecimal("9223372036854775807.0")).isLongInteger());
    }

    @Test
    public void existingGettersTruncateAndSaturateWithoutDoubleRounding() {
        Assert.assertEquals(9007199254740993L, JsonNumber.of(9007199254740993L).longValue());
        Assert.assertEquals(
                9007199254740993L, JsonNumber.of(new BigDecimal("9007199254740993.9")).longValue());
        Assert.assertEquals(-13, JsonNumber.of(new BigDecimal("-13.99")).intValue());
        Assert.assertEquals(
                Long.MAX_VALUE, JsonNumber.of(new BigInteger("18446744073709551616")).longValue());
        Assert.assertEquals(
                Long.MIN_VALUE, JsonNumber.of(new BigInteger("-18446744073709551616")).longValue());
        Assert.assertEquals(Integer.MAX_VALUE, JsonNumber.of(Long.MAX_VALUE).intValue());
        Assert.assertEquals(Integer.MIN_VALUE, JsonNumber.of(Long.MIN_VALUE).intValue());
        Assert.assertEquals(0L, JsonNumber.of(new BigDecimal("1e-400")).longValue());
        Assert.assertEquals(
                Double.POSITIVE_INFINITY, JsonNumber.of(new BigDecimal("1e400")).doubleValue(), 0);
        Assert.assertEquals(0.0, JsonNumber.of(new BigDecimal("1e-400")).doubleValue(), 0);
        Assert.assertEquals(BigInteger.valueOf(13), JsonNumber.of(13).toJavaObject());
        Assert.assertThrows(
                JsonDataException.class,
                () -> JsonNumber.of(new BigDecimal("1.5")).toJavaObject(BigInteger.class));
        Assert.assertEquals(
                BigInteger.TEN,
                JsonNumber.of(new BigDecimal("10.0")).toJavaObject(BigInteger.class));
    }

    @Test
    public void containersPreserveTypeValueAndParentThroughCloning() throws Exception {
        BigInteger integer = new BigInteger("18446744073709551616");
        BigDecimal real = new BigDecimal("0.12345678901234567890");
        JsonArray array = new JsonArray();
        array.addNumber(9007199254740993L);
        array.addNumber(integer);
        array.addNumber(real);
        array.addNumber(0.1f);
        array.addNumber(1.0);
        JsonObject object = new JsonObject();
        object.addNumber("integer", integer);
        object.addNumber("real", real);
        object.addNumber("long", 9007199254740993L);
        object.addNumber("float", 0.1f);
        object.addNumber("double", 1.0);
        array.addElement(object);
        JsonArray copy = (JsonArray) array.clone();
        Assert.assertNull(copy.parent());
        Assert.assertEquals(array.toString(), copy.toString());
        for (int i = 0; i < copy.size(); i++) {
            Assert.assertSame(copy, copy.get(i).parent());
            Assert.assertNotSame(array.get(i), copy.get(i));
        }
        JsonObject nested = copy.get(5).toJsonObject();
        Assert.assertSame(nested, nested.get("integer").parent());
        Assert.assertEquals(integer, nested.get("integer").toJavaObject(BigInteger.class));
        Assert.assertEquals(real, nested.get("real").toJavaObject(BigDecimal.class));
        Assert.assertEquals(9007199254740993L, copy.get(0).longValue());
        Assert.assertEquals(
                array.toString(), Json.parse(array.toString(), JsonParsingMode.STRICT).toString());
        Assert.assertTrue(copy.get(0) instanceof JsonInteger);
        Assert.assertTrue(copy.get(2) instanceof JsonReal);
        Assert.assertNull(copy.get(0).clone().parent());
        Assert.assertNull(copy.get(2).clone().parent());
    }

    static class Values {
        long id;
        Long boxed;
        BigInteger integer;
        BigDecimal real;
        List<BigInteger> integers;
        List<BigDecimal> reals;
    }

    @Test
    public void javaMappingPreservesLongAndBigValues() throws Exception {
        Values before = new Values();
        before.id = 9007199254740993L;
        before.boxed = Long.MAX_VALUE;
        before.integer = new BigInteger("18446744073709551616");
        before.real = new BigDecimal("0.12345678901234567890");
        before.integers = Arrays.asList(before.integer, null);
        before.reals = Arrays.asList(before.real, null);
        Values after = Json.parse(Json.serialize(before), Values.class, JsonParsingMode.STRICT);
        Assert.assertEquals(before.id, after.id);
        Assert.assertEquals(before.boxed, after.boxed);
        Assert.assertEquals(before.integer, after.integer);
        Assert.assertEquals(before.real, after.real);
        Assert.assertEquals(before.integers, after.integers);
        Assert.assertEquals(before.reals, after.reals);
        Assert.assertEquals(
                before.integer, Json.parse(Json.serialize(before.integer), BigInteger.class));
        Assert.assertEquals(before.real, Json.parse(Json.serialize(before.real), BigDecimal.class));
    }

    @Test
    public void randomizedExactRoundTrips() throws Exception {
        Random random = new Random(20261011L);
        for (int i = 0; i < 200; i++) {
            BigInteger integer = new BigInteger(256, random);
            if (random.nextBoolean()) integer = integer.negate();
            BigDecimal real = new BigDecimal(integer, random.nextInt(101) - 50);
            Assert.assertEquals(
                    integer,
                    Json.parse(Json.serialize(integer), BigInteger.class, JsonParsingMode.STRICT));
            Assert.assertEquals(
                    0,
                    real.compareTo(
                            Json.parse(
                                    Json.serialize(real),
                                    BigDecimal.class,
                                    JsonParsingMode.STRICT)));
        }
    }
}
