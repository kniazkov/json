/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

/** Contracts for fluent builders and their tree ownership. */
public class JsonBuilderTest {
    @Test
    public void fluentConstructionCoversEveryNestingCombination() throws Exception {
        JsonObject result =
                Json.object()
                        .put("name", "Observatory")
                        .put("enabled", true)
                        .putNull("note")
                        .object(
                                "site",
                                site ->
                                        site.put("city", "Москва")
                                                .object(
                                                        "position",
                                                        position ->
                                                                position.put("latitude", 55.75)))
                        .array(
                                "devices",
                                devices ->
                                        devices.object(device -> device.put("name", "Camera"))
                                                .array(filters -> filters.add("red").add("blue")))
                        .build();
        String expected =
                """
                {"name":"Observatory","enabled":true,"note":null,
                 "site":{"city":"Москва","position":{"latitude":55.75}},
                 "devices":[{"name":"Camera"},["red","blue"]]}
                """;
        Assert.assertEquals(Json.parse(expected), result);
        assertParents(result);
        JsonArray array = Json.array().add(true).addNull().build();
        Assert.assertTrue(array.get(0).booleanValueOrThrow());
        Assert.assertTrue(array.get(1).isNull());
    }

    @Test
    public void duplicateKeysReplaceValuesWithoutReordering() {
        JsonObjectBuilder builder = Json.object();
        Assert.assertSame(builder, builder.put("a", 1));
        Assert.assertSame(builder, builder.put("b", "text"));
        Assert.assertSame(builder, builder.put("a", 2));
        Assert.assertEquals(List.of("a", "b"), List.copyOf(builder.build().keySet()));
        Assert.assertEquals(2, builder.build().get("a").intValueOrThrow());
        Assert.assertSame(builder, builder.putNull("a"));
        Assert.assertTrue(builder.build().get("a").isNull());
        JsonArrayBuilder array = Json.array();
        Assert.assertSame(array, array.add("x"));
        Assert.assertSame(array, array.addAll(List.of(1, true)));
    }

    @Test
    public void exactNumbersAndSpecialValuesRetainTheirKinds() {
        for (Number number :
                List.of(
                        (byte) 1,
                        (short) 2,
                        3,
                        4L,
                        0.1f,
                        0.1d,
                        new BigInteger("18446744073709551616"),
                        new BigDecimal("1.2300"),
                        Double.NaN,
                        Double.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY)) {
            JsonNumber expected = JsonNumber.of(number);
            JsonElement objectValue = Json.object().put("n", number).build().get("n");
            JsonElement arrayValue = Json.array().add(number).build().get(0);
            JsonElement bulkValue = Json.array().addAll(List.of(number)).build().get(0);
            for (JsonElement actual : List.of(objectValue, arrayValue, bulkValue)) {
                Assert.assertEquals(expected.getClass(), actual.getClass());
                Assert.assertEquals(expected.numberValueOrThrow(), actual.numberValueOrThrow());
            }
        }
    }

    @Test
    public void buildsInputsAndCapturedChildBuildersAreIndependent() {
        JsonObject original = new JsonObject();
        original.addNumber("n", 1);
        AtomicReference<JsonObjectBuilder> objectChild = new AtomicReference<>();
        AtomicReference<JsonArrayBuilder> arrayChild = new AtomicReference<>();
        JsonObjectBuilder builder =
                Json.object()
                        .put("copied", original)
                        .object(
                                "child",
                                child -> {
                                    objectChild.set(child);
                                    child.put("n", 2);
                                })
                        .array(
                                "array",
                                child -> {
                                    arrayChild.set(child);
                                    child.add(3);
                                });
        JsonObject first = builder.build();
        original.addNumber("n", 100);
        objectChild.get().put("n", 200);
        arrayChild.get().add(300);
        builder.put("later", true);
        first.get("copied").objectValueOrThrow().addNumber("n", 400);
        JsonObject second = builder.build();
        Assert.assertFalse(first.containsKey("later"));
        Assert.assertEquals(1, second.get("copied").objectValueOrThrow().get("n").intValue());
        Assert.assertEquals(2, second.get("child").objectValueOrThrow().get("n").intValue());
        Assert.assertEquals(1, second.get("array").arrayValueOrThrow().size());
        assertParents(first);
        assertParents(second);
        JsonArrayBuilder array = Json.array().add(original);
        JsonArray snapshot = array.build();
        snapshot.clear();
        Assert.assertEquals(1, array.build().size());
        array.add(4);
        Assert.assertTrue(snapshot.isEmpty());
    }

    @Test
    public void failedCallbacksDoNotInsertOrReplaceTheirChildren() {
        JsonObjectBuilder object = Json.object().put("keep", 7);
        JsonArrayBuilder array = Json.array().add(8);
        IllegalStateException failure = new IllegalStateException("callback failed");
        List<ThrowingRunnable> calls =
                List.of(
                        () ->
                                object.object(
                                        "keep",
                                        child -> {
                                            child.put("x", 1);
                                            throw failure;
                                        }),
                        () ->
                                object.array(
                                        "new",
                                        child -> {
                                            child.add(1);
                                            throw failure;
                                        }),
                        () ->
                                array.object(
                                        child -> {
                                            child.put("x", 1);
                                            throw failure;
                                        }),
                        () ->
                                array.array(
                                        child -> {
                                            child.add(1);
                                            throw failure;
                                        }));
        for (ThrowingRunnable call : calls) {
            Assert.assertSame(failure, Assert.assertThrows(IllegalStateException.class, call));
        }
        Assert.assertEquals(1, object.build().size());
        Assert.assertEquals(7, object.build().get("keep").intValue());
        Assert.assertEquals(1, array.build().size());
    }

    @Test
    public void bulkInputIsStrictAtomicAndCopiesNodes() {
        JsonArrayBuilder builder = Json.array().add("before");
        Assert.assertThrows(
                NullPointerException.class, () -> builder.addAll(Arrays.asList(1, null, 2)));
        Assert.assertThrows(
                IllegalArgumentException.class, () -> builder.addAll(List.of(1, new Object())));
        Assert.assertThrows(
                IllegalArgumentException.class, () -> builder.addAll(List.of(List.of(1))));
        Assert.assertThrows(IllegalArgumentException.class, () -> builder.addAll(List.of('x')));
        Assert.assertEquals(1, builder.build().size());
        JsonObject input = new JsonObject();
        input.addNumber("x", 1);
        builder.addAll(List.of("text", true, input, JsonNull.INSTANCE));
        input.clear();
        JsonArray result = builder.build();
        Assert.assertEquals(5, result.size());
        Assert.assertEquals(1, result.get(3).objectValueOrThrow().get("x").intValue());
        Assert.assertTrue(result.get(4).isNull());
        assertParents(result);
    }

    @Test
    public void nullArgumentsAreRejectedBeforeCallbacksRun() {
        JsonObjectBuilder object = Json.object();
        JsonArrayBuilder array = Json.array();
        List<ThrowingRunnable> calls =
                List.of(
                        () -> object.put(null, "x"),
                        () -> object.put(null, true),
                        () -> object.put(null, 1),
                        () -> object.put(null, JsonNull.INSTANCE),
                        () -> object.put("x", (String) null),
                        () -> object.put("x", (Number) null),
                        () -> object.put("x", (JsonElement) null),
                        () -> object.putNull(null),
                        () -> object.object("x", null),
                        () -> object.array("x", null),
                        () -> object.object(null, child -> Assert.fail("Must not run")),
                        () -> object.array(null, child -> Assert.fail("Must not run")),
                        () -> array.add((String) null),
                        () -> array.add((Number) null),
                        () -> array.add((JsonElement) null),
                        () -> array.addAll(null),
                        () -> array.object(null),
                        () -> array.array(null));
        for (ThrowingRunnable call : calls) {
            Assert.assertThrows(NullPointerException.class, call);
        }
        Assert.assertTrue(object.build().isEmpty());
        Assert.assertTrue(array.build().isEmpty());
    }

    @Test
    public void buildsLargeAndDeepTreesWithoutRecursiveCopying() {
        JsonArrayBuilder builder = Json.array();
        for (int i = 0; i < 10000; i++) {
            builder.object(child -> child.put("value", true));
        }
        Assert.assertEquals(10000, builder.build().size());
        JsonArray root = new JsonArray();
        JsonArray current = root;
        for (int i = 0; i < 5000; i++) {
            current = current.createArray();
        }
        current.addNumber(42);
        JsonObject result = Json.object().put("deep", root).build();
        assertParents(result);
        JsonArray copied = result.get("deep").arrayValueOrThrow();
        for (int i = 0; i < 5000; i++) {
            copied = copied.get(0).arrayValueOrThrow();
        }
        Assert.assertEquals(42, copied.get(0).intValue());
    }

    private static void assertParents(JsonElement root) {
        Assert.assertTrue(root.parent().isEmpty());
        ArrayDeque<JsonElement> pending = new ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            JsonElement node = pending.pop();
            Iterable<JsonElement> children =
                    node instanceof JsonObject object
                            ? object.values()
                            : node instanceof JsonArray array ? array : List.of();
            for (JsonElement child : children) {
                Assert.assertSame(node, child.parent().orElseThrow());
                pending.push(child);
            }
        }
    }
}
