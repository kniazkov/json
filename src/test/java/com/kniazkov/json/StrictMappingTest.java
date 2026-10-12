/* Copyright (c) 2026 Ivan Kniazkov */
package com.kniazkov.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class StrictMappingTest {
    static class Child {
        int count = 42;
    }

    static class State {
        int count = 42;
        Integer boxed = 42;
        String text = "initial";
        boolean flag = true;
        Child child = new Child();
        List<Integer> values = new ArrayList<>(List.of(7, 8));
    }

    @Test
    public void strictFieldsRejectWrongTypesAndPreserveAbsentDefaults() throws Exception {
        for (String input :
                List.of(
                        "{count:'oops'}",
                        "{boxed:true}",
                        "{text:123}",
                        "{flag:1}",
                        "{child:[]}",
                        "{values:{}}",
                        "{values:[1,'2']}")) {
            Assert.assertThrows(JsonDataException.class, () -> Json.parse(input, State.class));
        }
        State absent = Json.parse("{unknown:1}", State.class);
        Assert.assertEquals(42, absent.count);
        Assert.assertEquals(List.of(7, 8), absent.values);
        State nullable = Json.parse("{boxed:null,text:null,child:null,values:null}", State.class);
        Assert.assertNull(nullable.boxed);
        Assert.assertNull(nullable.text);
        Assert.assertNull(nullable.child);
        Assert.assertNull(nullable.values);
        Assert.assertThrows(JsonDataException.class, () -> Json.parse("{count:null}", State.class));
    }

    @Test
    public void rootConversionsFollowTheSameRulesInEverySyntaxMode() throws Exception {
        Assert.assertEquals(1, (int) Json.parse("1.0", int.class));
        Assert.assertEquals('x', (char) Json.parse("\"x\"", char.class));
        Assert.assertNull(Json.parse("null", Integer.class));
        for (JsonParsingMode mode : JsonParsingMode.values()) {
            Assert.assertThrows(
                    JsonDataException.class, () -> Json.parse("\"123\"", Integer.class, mode));
            Assert.assertThrows(JsonDataException.class, () -> Json.parse("null", int.class, mode));
        }
        Assert.assertThrows(JsonDataException.class, () -> Json.parse("123", String.class));
        Assert.assertThrows(JsonDataException.class, () -> Json.parse("\"ab\"", Character.class));
        Assert.assertThrows(JsonDataException.class, () -> Json.parse("true", Integer.class));
        Assert.assertEquals(BigInteger.ONE, Json.parse("1", Object.class));
    }

    @Test
    public void integersRequireExactValueAndRange() throws Exception {
        Class<?>[] types = {Byte.class, Short.class, Integer.class, Long.class};
        String[] minima = {"-128", "-32768", "-2147483648", "-9223372036854775808"};
        String[] maxima = {"127", "32767", "2147483647", "9223372036854775807"};
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            Assert.assertEquals(minima[i], Json.parse(minima[i], type).toString());
            Assert.assertEquals(maxima[i], Json.parse(maxima[i], type).toString());
            String below = new BigInteger(minima[i]).subtract(BigInteger.ONE).toString();
            String above = new BigInteger(maxima[i]).add(BigInteger.ONE).toString();
            for (String input : List.of(below, above, "1.5", "NaN", "Infinity")) {
                Assert.assertThrows(JsonDataException.class, () -> Json.parse(input, type));
            }
            Assert.assertEquals("1", Json.parse("1.00", type).toString());
        }
        Assert.assertEquals(
                new BigInteger("123456789012345678901234567890"),
                Json.parse("123456789012345678901234567890", BigInteger.class));
        Assert.assertThrows(JsonDataException.class, () -> Json.parse("1.5", BigInteger.class));
    }

    @Test
    public void floatingPointAllowsRoundingAndExplicitNonFiniteValues() throws Exception {
        Assert.assertEquals(0.1, Json.parse("0.1", Double.class), 0);
        Assert.assertEquals(0.1f, Json.parse("0.1", Float.class), 0);
        for (String text : List.of("1e400", "1e-400")) {
            Assert.assertThrows(JsonDataException.class, () -> Json.parse(text, Double.class));
        }
        for (String text : List.of("1e40", "1e-50")) {
            Assert.assertThrows(JsonDataException.class, () -> Json.parse(text, Float.class));
        }
        Assert.assertTrue(Json.parse("NaN", Double.class).isNaN());
        Assert.assertEquals(Float.POSITIVE_INFINITY, Json.parse("Infinity", Float.class), 0);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, Json.parse("-Infinity", Double.class), 0);
        Assert.assertThrows(
                JsonDataException.class, () -> Json.parse("Infinity", BigDecimal.class));
        Assert.assertEquals(new BigDecimal("1e400"), Json.parse("1e400", BigDecimal.class));
    }

    static class Soft {
        @JsonProperty(
                onNull = JsonMappingAction.KEEP_DEFAULT,
                onInvalid = JsonMappingAction.KEEP_DEFAULT)
        int count = 42;

        @JsonProperty(onInvalid = JsonMappingAction.KEEP_DEFAULT)
        List<Integer> values = new ArrayList<>(List.of(7, 8));

        @JsonProperty(onInvalid = JsonMappingAction.SET_NULL)
        Integer nullable = 3;

        @JsonProperty(onNull = JsonMappingAction.ERROR)
        String required = "initial";

        @JsonProperty(name = "renamed", onInvalid = JsonMappingAction.KEEP_DEFAULT)
        Child child = new Child();

        @JsonProperty(onNull = JsonMappingAction.SET_NULL)
        String clear = "initial";
    }

    @Test
    public void fieldPoliciesApplyToWholeValuesAndUseOptionalName() throws Exception {
        Soft value =
                Json.parse(
                        "{count:null,values:[1,'bad',3],nullable:'bad',renamed:{count:'bad'},clear:null}",
                        Soft.class);
        Assert.assertEquals(42, value.count);
        Assert.assertEquals(List.of(7, 8), value.values);
        Assert.assertNull(value.nullable);
        Assert.assertNull(value.clear);
        Assert.assertEquals(42, value.child.count);
        Assert.assertEquals(42, Json.parse("{count:1.5}", Soft.class).count);
        Soft valid = Json.parse("{count:2,values:[1,null,3],renamed:{count:9}}", Soft.class);
        Assert.assertEquals(2, valid.count);
        Assert.assertEquals(Arrays.asList(1, null, 3), valid.values);
        Assert.assertEquals(9, valid.child.count);
        Assert.assertThrows(
                JsonDataException.class, () -> Json.parse("{required:null}", Soft.class));
        JsonObject serialized = Json.parse(Json.serialize(valid)).objectValueOrThrow();
        Assert.assertNotNull(serialized.get("count"));
        Assert.assertNotNull(serialized.get("renamed"));
        Assert.assertFalse(serialized.containsKey("child"));
    }

    static class Nested {
        List<Child> orders;
    }

    @Test
    public void errorPathIncludesAllContainersAndRenamedFields() {
        JsonDataException failure =
                Assert.assertThrows(
                        JsonDataException.class,
                        () -> Json.parse("{orders:[{count:1},{count:1.5}]}", Nested.class));
        Assert.assertTrue(
                failure.getMessage(), failure.getMessage().contains("$[\"orders\"][1][\"count\"]"));
        Assert.assertTrue(failure.getCause() instanceof ArithmeticException);
    }

    static class BadNull {
        @JsonProperty(onNull = JsonMappingAction.SET_NULL)
        int count;

        BadNull() {
            throw new AssertionError("Validation must precede construction");
        }
    }

    static class BadInvalid {
        @JsonProperty(onInvalid = JsonMappingAction.SET_NULL)
        int count;
    }

    static class BadDefault {
        @JsonProperty(onInvalid = JsonMappingAction.DEFAULT)
        String text;
    }

    static class BadContainer {
        @JsonProperty(onInvalid = JsonMappingAction.KEEP_DEFAULT)
        BadInvalid child;
    }

    @Test
    public void invalidConfigurationCannotBeSuppressed() {
        for (Class<?> type : List.of(BadNull.class, BadInvalid.class, BadDefault.class)) {
            Assert.assertThrows(IllegalArgumentException.class, () -> Json.parse("{}", type));
        }
        Assert.assertThrows(
                IllegalArgumentException.class, () -> Json.parse("{child:{}}", BadContainer.class));
    }

    static class Broken {
        Broken() {
            throw new IllegalStateException("broken");
        }
    }

    static class NoDefault {
        NoDefault(int x) {}
    }

    @Test
    public void constructionFailuresRetainCause() {
        JsonDataException failure =
                Assert.assertThrows(JsonDataException.class, () -> Json.parse("{}", Broken.class));
        Assert.assertTrue(failure.getCause() instanceof IllegalStateException);
        Assert.assertEquals("broken", failure.getCause().getMessage());
        JsonDataException missing =
                Assert.assertThrows(
                        JsonDataException.class, () -> Json.parse("{}", NoDefault.class));
        Assert.assertTrue(missing.getCause() instanceof NoSuchMethodException);
    }
}
