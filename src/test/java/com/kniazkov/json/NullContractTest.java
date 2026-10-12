/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

/** Tests the public distinction between absence, JSON null and invalid arguments. */
public class NullContractTest {
    @Test
    public void optionalAccessDistinguishesAbsenceFromJsonNull() {
        JsonObject object = new JsonObject();
        Assert.assertTrue(object.element("missing").isEmpty());
        Assert.assertNull(object.get("missing"));
        object.addNull("present");
        Assert.assertTrue(object.element("present").orElseThrow().isNull());
        Assert.assertSame(object, object.element("present").orElseThrow().parent().orElseThrow());
        Assert.assertTrue(object.parent().isEmpty());
        Assert.assertTrue(object.clone().parent().isEmpty());
        Assert.assertSame(object, object.toJsonObject().orElseThrow());
        Assert.assertTrue(object.toJsonArray().isEmpty());
        JsonArray array = new JsonArray();
        Assert.assertSame(array, array.toJsonArray().orElseThrow());
        Assert.assertTrue(array.toJsonObject().isEmpty());
        Assert.assertThrows(IndexOutOfBoundsException.class, () -> array.element(0));
    }

    @Test
    public void optionalNumbersCoverEveryValueKind() {
        for (JsonElement value :
                List.of(
                        JsonNull.INSTANCE,
                        JsonBoolean.TRUE,
                        new JsonString("x"),
                        new JsonArray(),
                        new JsonObject())) {
            Assert.assertTrue(value.numberValue().isEmpty());
            Assert.assertTrue(value.decimalValue().isEmpty());
            Assert.assertThrows(JsonDataException.class, value::numberValueOrThrow);
            Assert.assertThrows(JsonDataException.class, value::decimalValueOrThrow);
        }
        for (JsonNumber value :
                List.of(
                        JsonNumber.of(12),
                        JsonNumber.of(new BigDecimal("1.25")),
                        new JsonNaN(),
                        new JsonInfinity(false),
                        new JsonInfinity(true))) {
            Assert.assertEquals(value.toJavaObject(), value.numberValue().orElseThrow());
            Assert.assertEquals(value.isFinite(), value.decimalValue().isPresent());
        }
    }

    @Test
    public void typedParsingUsesOptionalButConversionRetainsNull() throws Exception {
        Assert.assertEquals(Optional.of(12), Json.parse("12", Integer.class));
        Assert.assertTrue(Json.parse("null", Integer.class).isEmpty());
        Assert.assertTrue(Json.parse("null", String.class, JsonParsingMode.STRICT).isEmpty());
        File file = File.createTempFile("json-null-contract", ".json");
        try {
            java.nio.file.Files.writeString(file.toPath(), "null");
            Assert.assertTrue(Json.parse(file, Object.class).isEmpty());
            Assert.assertTrue(Json.parse(file, Object.class, JsonParsingMode.STRICT).isEmpty());
        } finally {
            java.nio.file.Files.deleteIfExists(file.toPath());
        }
        Assert.assertNull(JsonNull.INSTANCE.toJavaObject());
        Assert.assertNull(JsonNull.INSTANCE.toJavaObject(String.class));
        Assert.assertNull(JsonNull.INSTANCE.toJavaObject(new JsonType<List<String>>() {}));
        Assert.assertThrows(JsonDataException.class, () -> Json.parse("null", int.class));
    }

    @Test
    public void invalidNullArgumentsFailAtTheBoundary() {
        JsonObject object = new JsonObject();
        JsonArray array = new JsonArray();
        JsonParser parser = new JsonParser();
        JsonSerializer serializer = new JsonSerializer();
        List<ThrowingRunnable> calls =
                List.of(
                        () -> new JsonString(null),
                        () -> new JsonInteger(null),
                        () -> new JsonReal(null),
                        () -> JsonNumber.of((Number) null),
                        () -> JsonNumber.of((BigInteger) null),
                        () -> JsonNumber.of((BigDecimal) null),
                        () -> new JsonException(null),
                        () -> new JsonException(null, null),
                        () -> new JsonDataException(null),
                        () -> new JsonDataException(null, null),
                        () -> object.element(null),
                        () -> object.addNull(null),
                        () -> object.addBoolean(null, true),
                        () -> object.addNumber(null, 1),
                        () -> object.addString(null, "x"),
                        () -> object.putAll(null),
                        () -> object.replaceAll(null),
                        () -> object.compute("x", null),
                        () -> array.addAll(null),
                        () -> array.addAll(0, null),
                        () -> array.replaceAll(null),
                        () -> array.removeIf(null),
                        () -> object.addElement(null, JsonNull.INSTANCE),
                        () -> object.addElement("x", null),
                        () -> object.addString("x", null),
                        () -> object.addNumber("x", (Number) null),
                        () -> object.createArray(null),
                        () -> object.createObject(null),
                        () -> array.addElement(null),
                        () -> array.addString(null),
                        () -> array.addNumber((Number) null),
                        () -> JsonNull.INSTANCE.toJavaObject((Class<?>) null),
                        () -> JsonNull.INSTANCE.toJavaObject((JsonType<?>) null),
                        () -> parser.setCharset(null),
                        () -> parser.setMode(null),
                        () -> parser.setBomPolicy(null),
                        () -> parser.parse((String) null),
                        () -> parser.parse((Reader) null),
                        () -> parser.parse((InputStream) null),
                        () -> parser.parse((File) null),
                        () -> JsonParser.parseString("{}", (List<JsonError>) null),
                        () -> JsonParser.parseString("{}", JsonParsingMode.STRICT, null),
                        () -> serializer.setCharset(null),
                        () -> serializer.serialize(null, (Writer) null),
                        () -> serializer.serialize(null, (OutputStream) null),
                        () -> serializer.serialize(null, (File) null),
                        () -> Json.parse("invalid", (Class<?>) null),
                        () -> Json.parse(new File("missing"), (Class<?>) null),
                        () -> Json.parse("invalid", (Class<?>) null, JsonParsingMode.STRICT),
                        () ->
                                Json.parse(
                                        new File("missing"),
                                        (Class<?>) null,
                                        JsonParsingMode.STRICT));
        for (int i = 0; i < calls.size(); i++) {
            Assert.assertThrows(
                    "Null argument case " + i, NullPointerException.class, calls.get(i));
        }
        Assert.assertTrue(object.isEmpty());
        Assert.assertTrue(array.isEmpty());
    }

    @Test
    public void inheritedCollectionContractsKeepTheirNullResults() {
        JsonObject object = new JsonObject();
        Assert.assertNull(object.put("x", JsonNull.INSTANCE));
        Assert.assertNull(object.putIfAbsent("y", JsonBoolean.TRUE));
        Assert.assertNull(object.remove("missing"));
        Assert.assertNull(object.replace("missing", JsonBoolean.FALSE));
        Assert.assertNull(object.getOrDefault("missing", null));
        Assert.assertNull(object.computeIfAbsent("missing", key -> null));
        Assert.assertNull(object.compute("x", (key, value) -> null));
        Assert.assertFalse(object.containsKey("x"));
        Assert.assertFalse(object.equals(null));
        JsonArray array = new JsonArray();
        Assert.assertFalse(array.contains(null));
        Assert.assertThrows(NullPointerException.class, () -> array.add(null));
        array.addNull();
        JsonElement[] oversized = new JsonElement[2];
        Assert.assertSame(oversized, array.toArray(oversized));
        Assert.assertTrue(oversized[0].isNull());
        Assert.assertNull(oversized[1]);
    }

    @Test
    public void explicitNullDataAndResetSettingsRemainSupported() throws Exception {
        Assert.assertTrue(JsonParser.parseJavaObject(null).isNull());
        Assert.assertEquals("null", Json.serialize(null));
        JsonSerializer serializer = new JsonSerializer().setIndentation("  ").setIndentation(null);
        StringWriter writer = new StringWriter();
        serializer.serialize(null, writer);
        Assert.assertEquals("null", writer.toString());
        JsonParser parser = new JsonParser().setSourceName("named").setSourceName(null);
        JsonException failure = Assert.assertThrows(JsonException.class, () -> parser.parse(""));
        Assert.assertTrue(failure.error().location().sourceName().isEmpty());
        Assert.assertNull(failure.getCause());
        Assert.assertThrows(
                NullPointerException.class, () -> failure.error().localizedMessage(null));
        Assert.assertNotNull(new JsonDataException("message", null).getMessage());
    }
}
