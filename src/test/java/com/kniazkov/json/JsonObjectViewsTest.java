/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.Assert;
import org.junit.Test;

public class JsonObjectViewsTest {
    private JsonObject sample() {
        JsonObject object = new JsonObject();
        object.addNumber("c", 3);
        object.addNumber("a", 1);
        object.addNumber("b", 2);
        return object;
    }

    private void consistent(JsonObject object, String... keys) throws Exception {
        Assert.assertEquals(Arrays.asList(keys), new ArrayList<>(object.keySet()));
        Assert.assertEquals(keys.length, object.size());
        Assert.assertEquals(keys.length == 0, object.isEmpty());
        Assert.assertEquals(keys.length, object.values().size());
        Assert.assertEquals(keys.length, object.entrySet().size());
        int index = 0;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            Assert.assertEquals(keys[index++], entry.getKey());
            Assert.assertSame(object.get(entry.getKey()), entry.getValue());
            Assert.assertSame(object, entry.getValue().parent().orElseThrow());
        }
        JsonObject copy = (JsonObject) object.clone();
        Assert.assertEquals(object.toString(), copy.toString());
        Assert.assertEquals(Arrays.asList(keys), new ArrayList<>(copy.keySet()));
        for (JsonElement value : copy.values()) {
            Assert.assertSame(copy, value.parent().orElseThrow());
        }
        Assert.assertEquals(
                object.toString(),
                Json.parse(object.toString(), JsonParsingMode.STRICT).toString());
        Assert.assertEquals(
                object.toString(),
                Json.parse(object.toText("  "), JsonParsingMode.STRICT).toString());
        Assert.assertEquals(keys.length, ((Map<?, ?>) object.toJavaObject()).size());
    }

    @Test
    public void viewsAreLiveAndShareInsertionOrder() throws Exception {
        JsonObject object = sample();
        Set<String> keys = object.keySet();
        Collection<JsonElement> values = object.values();
        Set<Map.Entry<String, JsonElement>> entries = object.entrySet();
        object.addNumber("d", 4);
        Assert.assertEquals(4, keys.size());
        Assert.assertEquals(4, values.size());
        Assert.assertEquals(4, entries.size());
        object.put("a", JsonNumber.of(10));
        consistent(object, "c", "a", "b", "d");
        object.remove("a");
        object.addNumber("a", 11);
        consistent(object, "c", "b", "d", "a");
    }

    @Test
    public void directAndIteratorRemovalsPreserveTheTree() throws Exception {
        List<Consumer<JsonObject>> removals =
                List.of(
                        o -> o.keySet().remove("a"),
                        o -> o.values().remove(o.get("a")),
                        o -> o.entrySet().remove(new AbstractMap.SimpleEntry<>("a", o.get("a"))),
                        o -> {
                            Iterator<String> i = o.keySet().iterator();
                            i.next();
                            i.next();
                            i.remove();
                        },
                        o -> {
                            Iterator<JsonElement> i = o.values().iterator();
                            i.next();
                            i.next();
                            i.remove();
                        },
                        o -> {
                            Iterator<Map.Entry<String, JsonElement>> i = o.entrySet().iterator();
                            i.next();
                            i.next();
                            i.remove();
                        });
        for (Consumer<JsonObject> remove : removals) {
            JsonObject object = sample();
            remove.accept(object);
            Assert.assertFalse(object.containsKey("a"));
            consistent(object, "c", "b");
        }
    }

    @Test
    public void bulkViewMutationsPreserveTheTree() throws Exception {
        List<Consumer<JsonObject>> removals =
                List.of(
                        o -> o.keySet().removeAll(List.of("a", "b")),
                        o -> o.keySet().retainAll(List.of("c")),
                        o -> o.keySet().removeIf(k -> !k.equals("c")),
                        o -> o.values().removeAll(List.of(o.get("a"), o.get("b"))),
                        o -> o.values().retainAll(List.of(o.get("c"))),
                        o -> o.values().removeIf(v -> v.intValue() != 3),
                        o -> o.entrySet().removeAll(new ArrayList<>(o.entrySet()).subList(1, 3)),
                        o -> o.entrySet().retainAll(List.of(o.entrySet().iterator().next())),
                        o -> o.entrySet().removeIf(e -> !e.getKey().equals("c")));
        for (Consumer<JsonObject> remove : removals) {
            JsonObject object = sample();
            remove.accept(object);
            consistent(object, "c");
        }
        for (Consumer<JsonObject> clear :
                List.<Consumer<JsonObject>>of(
                        o -> o.keySet().clear(),
                        o -> o.values().clear(),
                        o -> o.entrySet().clear(),
                        JsonObject::clear)) {
            JsonObject object = sample();
            clear.accept(object);
            consistent(object);
        }
    }

    @Test
    public void entryReplacementClonesAndCannotCreateCycles() throws Exception {
        JsonObject object = sample();
        Map.Entry<String, JsonElement> entry = object.entrySet().iterator().next();
        JsonElement old = entry.getValue();
        JsonObject supplied = new JsonObject();
        supplied.createArray("array").addNumber(7);
        Assert.assertSame(old, entry.setValue(supplied));
        Assert.assertNotSame(supplied, entry.getValue());
        Assert.assertSame(object, entry.getValue().parent().orElseThrow());
        Assert.assertSame(
                entry.getValue(),
                entry.getValue().objectValueOrThrow().get("array").parent().orElseThrow());
        supplied.addNumber("new", 1);
        Assert.assertFalse(entry.getValue().objectValueOrThrow().containsKey("new"));
        consistent(object, "c", "a", "b");
        String snapshot = object.toString();
        entry.setValue(object);
        Assert.assertEquals(snapshot, object.get("c").toString());
        Assert.assertNotSame(object, object.get("c"));
        consistent(object, "c", "a", "b");
        JsonObject nested = object.get("c").objectValueOrThrow();
        nested.entrySet().iterator().next().setValue(object);
        consistent(object, "c", "a", "b");
    }

    @Test
    public void entriesCannotEscapeThroughArraysStreamsOrSpliterators() throws Exception {
        for (int route = 0; route < 4; route++) {
            JsonObject object = sample();
            Map.Entry<String, JsonElement> entry;
            if (route == 0) {
                entry = (Map.Entry<String, JsonElement>) object.entrySet().toArray()[0];
            } else if (route == 1) {
                entry = object.entrySet().toArray(Map.Entry[]::new)[0];
            } else if (route == 2) {
                entry = object.entrySet().stream().findFirst().orElseThrow();
            } else {
                List<Map.Entry<String, JsonElement>> entries = new ArrayList<>();
                object.entrySet().spliterator().tryAdvance(entries::add);
                entry = entries.get(0);
            }
            entry.setValue(object);
            consistent(object, "c", "a", "b");
        }
    }

    @Test
    public void defaultMapMutatorsUseControlledInsertion() throws Exception {
        JsonObject object = sample();
        object.replaceAll((k, v) -> JsonNumber.of(v.intValue() + 10));
        consistent(object, "c", "a", "b");
        Assert.assertEquals(11, object.get("a").intValue());
        object.compute("a", (k, v) -> JsonNumber.of(20));
        object.computeIfAbsent("d", k -> new JsonObject());
        object.computeIfPresent("b", (k, v) -> JsonNumber.of(30));
        object.merge("c", JsonNumber.of(1), (a, b) -> JsonNumber.of(a.intValue() + b.intValue()));
        object.replace("d", new JsonArray());
        object.replace("a", object.get("a"), JsonNumber.of(40));
        object.putIfAbsent("e", JsonNumber.of(5));
        consistent(object, "c", "a", "b", "d", "e");
        object.compute("b", (k, v) -> null);
        object.remove("e", object.get("e"));
        consistent(object, "c", "a", "d");
        object.putAll(object);
        consistent(object, "c", "a", "d");
    }

    @Test
    public void rejectedNullsDoNotCorruptSizeOrOrder() throws Exception {
        JsonObject object = sample();
        Assert.assertThrows(NullPointerException.class, () -> object.put("new", null));
        Assert.assertThrows(NullPointerException.class, () -> object.put(null, JsonNumber.of(1)));
        Assert.assertThrows(NullPointerException.class, () -> object.addNull(null));
        Assert.assertThrows(
                NullPointerException.class,
                () -> object.entrySet().iterator().next().setValue(null));
        Assert.assertThrows(NullPointerException.class, () -> object.replaceAll((k, v) -> null));
        consistent(object, "c", "a", "b");
        object.put("null", JsonNull.INSTANCE);
        consistent(object, "c", "a", "b", "null");
        Assert.assertTrue(object.get("null").isNull());
    }
}
