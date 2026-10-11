/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.Assert;
import org.junit.Test;

/** Contracts for the version 2 value access API. */
public class JsonValueAccessTest {
    @Test
    public void numericAccessIsAvailableThroughJsonElement() {
        BigInteger integer = new BigInteger("18446744073709551616");
        BigDecimal decimal = new BigDecimal("0.123456789012345678901234567890");
        JsonElement first = JsonNumber.of(integer);
        JsonElement second = JsonNumber.of(decimal);
        Assert.assertSame(integer, first.numberValue());
        Assert.assertEquals(new BigDecimal(integer), first.decimalValue());
        Assert.assertSame(decimal, second.numberValue());
        Assert.assertSame(decimal, second.decimalValue());
        Assert.assertSame(integer, first.numberValueOrThrow());
        Assert.assertSame(decimal, second.decimalValueOrThrow());
        Assert.assertEquals(new BigDecimal(integer), first.decimalValueOrThrow());
        for (JsonElement value : new JsonElement[]{new JsonString("1"), JsonBoolean.TRUE,
                JsonNull.INSTANCE, new JsonArray(), new JsonObject()}) {
            Assert.assertNull(value.numberValue());
            Assert.assertNull(value.decimalValue());
            Assert.assertThrows(JsonDataException.class, value::numberValueOrThrow);
            Assert.assertThrows(JsonDataException.class, value::decimalValueOrThrow);
        }
    }

    @Test
    public void softAccessorsKeepDefaultsAndNumericConversions() {
        JsonElement string = new JsonString("42");
        Assert.assertEquals(0, string.intValue());
        Assert.assertEquals(0L, string.longValue());
        Assert.assertEquals(0.0, string.doubleValue(), 0);
        Assert.assertFalse(string.booleanValue());
        Assert.assertEquals("", new JsonObject().stringValue());
        Assert.assertEquals("null", JsonNull.INSTANCE.stringValue());
        Assert.assertEquals("true", JsonBoolean.TRUE.stringValue());
        Assert.assertEquals("42", JsonNumber.of(42).stringValue());
        Assert.assertEquals(-12, JsonNumber.of(new BigDecimal("-12.9")).intValue());
        Assert.assertEquals(Long.MAX_VALUE, JsonNumber.of(new BigInteger("18446744073709551616")).longValue());
    }

    @Test
    public void strictValuesDoNotConfuseDefaultsWithErrors() {
        Assert.assertFalse(JsonBoolean.FALSE.booleanValueOrThrow());
        Assert.assertEquals("", new JsonString("").stringValueOrThrow());
        Assert.assertEquals(0, JsonNumber.of(0).intValueOrThrow());
        Assert.assertEquals(0L, JsonNumber.of(0).longValueOrThrow());
        Assert.assertEquals(0.0, JsonNumber.of(0).doubleValueOrThrow(), 0);
        Assert.assertEquals(BigInteger.ZERO, JsonNumber.of(0).numberValueOrThrow());
        Assert.assertEquals(BigDecimal.ZERO, JsonNumber.of(0).decimalValueOrThrow());
        JsonArray array = new JsonArray();
        JsonObject object = new JsonObject();
        Assert.assertSame(array, array.arrayValueOrThrow());
        Assert.assertSame(object, object.objectValueOrThrow());
    }

    @Test
    public void strictAccessRejectsWrongTypes() {
        JsonElement[] elements = {JsonNull.INSTANCE, JsonBoolean.TRUE, new JsonString("12"),
                JsonNumber.of(12), JsonNumber.of(12.0), new JsonArray(), new JsonObject()};
        List<Consumer<JsonElement>> getters = List.of(
                value -> value.booleanValueOrThrow(), value -> value.stringValueOrThrow(),
                value -> value.intValueOrThrow(), value -> value.longValueOrThrow(),
                value -> value.doubleValueOrThrow(), value -> value.numberValueOrThrow(),
                value -> value.decimalValueOrThrow(), value -> value.arrayValueOrThrow(),
                value -> value.objectValueOrThrow());
        for (JsonElement value : elements) {
            boolean[] accepted = {value.isBoolean(), value.isString(), value.isNumber(),
                    value.isNumber(), value.isNumber(), value.isNumber(), value.isNumber(),
                    value instanceof JsonArray, value instanceof JsonObject};
            for (int i = 0; i < getters.size(); i++) {
                Consumer<JsonElement> getter = getters.get(i);
                if (accepted[i]) {
                    getter.accept(value);
                } else {
                    JsonDataException exception = Assert.assertThrows(JsonDataException.class,
                            () -> getter.accept(value));
                    Assert.assertTrue(exception.getMessage().startsWith("Expected "));
                    Assert.assertTrue(exception.getMessage().contains(value.getClass().getSimpleName()));
                }
            }
        }
        Assert.assertThrows(JsonDataException.class, new JsonString(null)::stringValueOrThrow);
    }

    @Test
    public void strictIntegralAccessRequiresExactValueAndRange() {
        Assert.assertEquals(Integer.MIN_VALUE, JsonNumber.of(Integer.MIN_VALUE).intValueOrThrow());
        Assert.assertEquals(Integer.MAX_VALUE, JsonNumber.of(Integer.MAX_VALUE).intValueOrThrow());
        Assert.assertEquals(Long.MIN_VALUE, JsonNumber.of(Long.MIN_VALUE).longValueOrThrow());
        Assert.assertEquals(Long.MAX_VALUE, JsonNumber.of(Long.MAX_VALUE).longValueOrThrow());
        Assert.assertEquals(9007199254740993L, JsonNumber.of(9007199254740993L).longValueOrThrow());
        Assert.assertEquals(12, JsonNumber.of(new BigDecimal("12.000")).intValueOrThrow());
        Assert.assertEquals(1000L, JsonNumber.of(new BigDecimal("1e3")).longValueOrThrow());
        for (String text : new String[]{"1.5", "-1.5", "1e-400", "2147483648", "-2147483649"}) {
            Assert.assertThrows(JsonDataException.class, JsonNumber.of(new BigDecimal(text))::intValueOrThrow);
        }
        for (String text : new String[]{"1.5", "-1.5", "1e-400", "9223372036854775808", "-9223372036854775809"}) {
            Assert.assertThrows(JsonDataException.class, JsonNumber.of(new BigDecimal(text))::longValueOrThrow);
        }
    }

    @Test
    public void strictDoubleAllowsRoundingButRejectsRangeLoss() {
        Assert.assertEquals(0.1, JsonNumber.of(new BigDecimal("0.1")).doubleValueOrThrow(), 0);
        Assert.assertEquals(Double.MAX_VALUE, JsonNumber.of(Double.MAX_VALUE).doubleValueOrThrow(), 0);
        Assert.assertEquals(Double.MIN_VALUE, JsonNumber.of(Double.MIN_VALUE).doubleValueOrThrow(), 0);
        for (String text : new String[]{"1e400", "-1e400", "1e-400", "-1e-400"}) {
            Assert.assertThrows(JsonDataException.class, JsonNumber.of(new BigDecimal(text))::doubleValueOrThrow);
        }
    }

    @Test
    public void untypedConversionPreservesExactNumbersRecursively() throws Exception {
        BigInteger integer = new BigInteger("18446744073709551616");
        BigDecimal real = new BigDecimal("0.12345678901234567890");
        String text = "{\"integer\":" + integer + ",\"values\":[" + real + ",null,{\"id\":9007199254740993}]}";
        Map<?, ?> map = (Map<?, ?>) Json.parse(text, JsonParsingMode.STRICT).toJavaObject();
        Assert.assertEquals(integer, map.get("integer"));
        List<?> list = (List<?>) map.get("values");
        Assert.assertEquals(real, list.get(0));
        Assert.assertNull(list.get(1));
        Assert.assertEquals(new BigInteger("9007199254740993"), ((Map<?, ?>) list.get(2)).get("id"));
        Assert.assertEquals(integer, Json.parse(integer.toString()).toJavaObject());
        Assert.assertEquals(real, Json.parse(real.toString()).toJavaObject());
        Assert.assertEquals(integer, Json.parse(integer.toString(), Number.class));
        Assert.assertEquals(real, Json.parse(real.toString(), Object.class));
        Assert.assertEquals(Double.valueOf(0.1), Json.parse("0.1", Double.class));
    }

    static class UntypedFields {
        Object value;
        List<Object> values;
        List raw;
    }

    @Test
    public void untypedPojoFieldsAndListsPreserveNumbers() throws Exception {
        UntypedFields result = Json.parse("{value:9007199254740993,values:[0.12345678901234567890],raw:[42]}", UntypedFields.class);
        Assert.assertEquals(new BigInteger("9007199254740993"), result.value);
        Assert.assertEquals(new BigDecimal("0.12345678901234567890"), result.values.get(0));
        Assert.assertEquals(BigInteger.valueOf(42), result.raw.get(0));
    }

    @Test
    public void renamedMetadataAndSyntaxDiagnosticsRemainUsable() {
        JsonArray array = new JsonArray();
        array.addNumber(1);
        Assert.assertSame(array, array.element(0).parent());
        Assert.assertSame(JsonBoolean.TRUE, JsonBoolean.of(true));
        JsonException exception = Assert.assertThrows(JsonException.class,
                () -> Json.parse("+1", JsonParsingMode.STRICT));
        Assert.assertEquals(1, exception.error().location().row());
        Assert.assertEquals(1, exception.error().location().column());
        Assert.assertEquals("Invalid character: '+'", exception.error().message());
        Assert.assertEquals("Недопустимый символ: '+'", exception.error().localizedMessage("ru"));
        Assert.assertTrue(exception.getMessage().contains("Invalid character"));
        Assert.assertNotNull(exception.getLocalizedMessage());
    }
}
