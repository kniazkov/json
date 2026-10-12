/* Copyright (c) 2026 Ivan Kniazkov */
package com.kniazkov.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Assert;
import org.junit.Test;

public class JsonSpecialNumberTest {
    @Test
    public void specialLiteralsAndFactoriesUseDedicatedClasses() throws Exception {
        for (String literal :
                new String[] {"NaN", "+NaN", "-NaN", "Infinity", "+Infinity", "-Infinity"}) {
            JsonElement value = Json.parse(literal, JsonParsingMode.JSON5);
            boolean nan = literal.endsWith("NaN");
            Assert.assertEquals(nan, value instanceof JsonNaN);
            Assert.assertEquals(!nan, value instanceof JsonInfinity);
            Assert.assertEquals(nan, value.isNaN());
            Assert.assertEquals(!nan, value.isInfinite());
            Assert.assertEquals(literal.equals("-Infinity"), value.isNegativeInfinity());
            Assert.assertEquals(!nan && !literal.startsWith("-"), value.isPositiveInfinity());
            Assert.assertTrue(value.isNumber());
            Assert.assertFalse(value.isFinite());
            Assert.assertFalse(value.isInteger());
            Assert.assertFalse(value.isLongInteger());
            Assert.assertEquals(value.toString(), JsonNumber.of(value.doubleValue()).toString());
            Assert.assertEquals(
                    value.toString(), JsonNumber.of((float) value.doubleValue()).toString());
            Assert.assertEquals(
                    value.toString(), JsonNumber.of((Number) value.doubleValue()).toString());
            Assert.assertEquals(value.toString(), Json.serialize(value.doubleValue()));
        }
        Assert.assertTrue(JsonNumber.of(new BigDecimal("1e400")).isFinite());
        Assert.assertFalse(JsonNumber.of(new BigDecimal("1e400")).isInfinite());
        Assert.assertFalse(new JsonString("NaN").isNaN());
        Assert.assertFalse(JsonNull.INSTANCE.isFinite());
    }

    @Test
    public void gettersAndJavaConversionHaveDefinedNonfiniteBehavior() {
        for (JsonNumber value :
                new JsonNumber[] {new JsonNaN(), new JsonInfinity(false), new JsonInfinity(true)}) {
            Assert.assertEquals(value.numberValue(), value.numberValueOrThrow());
            Assert.assertEquals(value.numberValue(), value.toJavaObject());
            Assert.assertEquals(value.numberValue(), value.toJavaObject(Number.class));
            Assert.assertEquals(value.numberValue(), value.toJavaObject(Double.class));
            Assert.assertEquals((float) value.doubleValue(), value.toJavaObject(Float.class), 0);
            Assert.assertNull(value.decimalValue());
            Assert.assertThrows(
                    JsonDataException.class, () -> value.toJavaObject(BigDecimal.class));
            Assert.assertThrows(
                    JsonDataException.class, () -> value.toJavaObject(BigInteger.class));
            Assert.assertEquals((int) value.doubleValue(), value.intValue());
            Assert.assertEquals((long) value.doubleValue(), value.longValue());
            Assert.assertThrows(JsonDataException.class, value::intValueOrThrow);
            Assert.assertThrows(JsonDataException.class, value::longValueOrThrow);
            Assert.assertThrows(JsonDataException.class, value::doubleValueOrThrow);
            Assert.assertThrows(JsonDataException.class, value::decimalValueOrThrow);
        }
    }

    @Test
    public void nestedValuesCloneAndRoundTripWithoutLosingParents() throws Exception {
        JsonObject object = new JsonObject();
        object.addNumber("nan", Double.NaN);
        JsonArray array = new JsonArray();
        array.addNumber(Double.POSITIVE_INFINITY);
        array.addNumber(Float.NEGATIVE_INFINITY);
        object.addElement("array", array);
        JsonObject copy = (JsonObject) object.clone();
        Assert.assertTrue(copy.element("nan").isNaN());
        Assert.assertSame(copy, copy.element("nan").parent());
        JsonArray nested = copy.element("array").arrayValueOrThrow();
        Assert.assertSame(copy, nested.parent());
        Assert.assertSame(nested, nested.get(0).parent());
        Assert.assertTrue(nested.get(1).isNegativeInfinity());
        Assert.assertEquals(
                copy.toString(), Json.parse(copy.toString(), JsonParsingMode.JSON5).toString());
        Assert.assertEquals(
                copy.toString(), Json.parse(copy.toText("  "), JsonParsingMode.JSON5).toString());
        java.util.Map<?, ?> javaObject = (java.util.Map<?, ?>) copy.toJavaObject();
        Assert.assertEquals(Double.NaN, javaObject.get("nan"));
        Assert.assertEquals(
                java.util.List.of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY),
                javaObject.get("array"));
    }
}
