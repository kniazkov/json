/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/** No recursive test helpers: failures must come from the operation under test. */
public class DeepTraversalTest {
    private static final int DEPTH = 20_000;

    private String arrays(int depth) {
        return "[".repeat(depth) + "0" + "]".repeat(depth);
    }

    private JsonElement arrayLeaf(JsonElement root, int depth) {
        Assert.assertNull(root.parent());
        for (int i = 0; i < depth; i++) {
            JsonArray array = root.arrayValueOrThrow();
            Assert.assertEquals(1, array.size());
            JsonElement child = array.get(0);
            Assert.assertSame(array, child.parent());
            root = child;
        }
        return root;
    }

    @Test
    public void deepArraysParseWriteCloneAndConvertInAllModes() throws Exception {
        String input = arrays(DEPTH);
        for (JsonParsingMode mode : JsonParsingMode.values()) {
            JsonElement root = Json.parse(input, mode);
            Assert.assertEquals(0, arrayLeaf(root, DEPTH).intValue());
            Assert.assertEquals(input, root.toString());
            JsonElement copy = root.clone();
            Assert.assertNotSame(root, copy);
            Assert.assertEquals(0, arrayLeaf(copy, DEPTH).intValue());
            Assert.assertEquals(input, copy.toString());
            Object natural = root.toJavaObject();
            for (int i = 0; i < DEPTH; i++) {
                natural = ((List<?>) natural).get(0);
            }
            Assert.assertEquals(BigInteger.ZERO, natural);
            Object typed = root.toJavaObject(List.class);
            for (int i = 0; i < DEPTH; i++) {
                typed = ((List<?>) typed).get(0);
            }
            Assert.assertEquals(BigInteger.ZERO, typed);
        }
    }

    @Test
    public void mixedObjectsAndArraysRetainOrderParentsAndNaturalMaps() throws Exception {
        int depth = DEPTH / 2;
        String input = "{\"child\":[".repeat(depth) + "true" + "]}".repeat(depth);
        JsonElement root = Json.parse(input, JsonParsingMode.STRICT);
        JsonElement copy = root.clone();
        Assert.assertEquals(input.replace(":", ": "), copy.toString());
        Object natural = root.toJavaObject(Map.class);
        JsonElement current = copy;
        for (int i = 0; i < depth; i++) {
            JsonElement array = current.objectValueOrThrow().get("child");
            Assert.assertSame(current, array.parent());
            current = array.arrayValueOrThrow().get(0);
            Assert.assertSame(array, current.parent());
            natural = ((List<?>) ((Map<?, ?>) natural).get("child")).get(0);
        }
        Assert.assertTrue(current.booleanValue());
        Assert.assertEquals(true, natural);
    }

    @Test
    public void deepFormattingDoesNotUseTheCallStack() throws Exception {
        JsonElement root = Json.parse(arrays(DEPTH));
        String formatted = root.toText("");
        Assert.assertEquals(
                root.toString(), Json.parse(formatted, JsonParsingMode.STRICT).toString());
        // Nonempty indentation inherently has quadratic output on a chain.
        JsonElement smaller = Json.parse(arrays(400));
        String indented = smaller.toText("  ");
        Assert.assertTrue(indented.contains("\n  ["));
        Assert.assertEquals(smaller.toString(), Json.parse(indented).toString());
        String nl = System.lineSeparator();
        Assert.assertEquals(
                "{" + nl + "  \"a\": [1]," + nl + "  \"b\": {\"c\": true}" + nl + "}",
                Json.parse("{a:[1],b:{c:true}}").toText("  "));
    }

    @Test
    public void deepCloneThroughContainerMutationKeepsOwnership() throws Exception {
        JsonElement original = Json.parse(arrays(DEPTH));
        JsonArray array = new JsonArray();
        array.add(original);
        Assert.assertSame(array, array.get(0).parent());
        Assert.assertEquals(original.toString(), array.get(0).toString());
        JsonObject object = new JsonObject();
        object.put("deep", original);
        object.entrySet().iterator().next().setValue(original);
        Assert.assertSame(object, object.get("deep").parent());
        Assert.assertNull(original.parent());
        Assert.assertNotSame(original, object.get("deep"));
    }

    @Test
    public void deepExtendedRecoveryKeepsItsContract() throws Exception {
        List<JsonError> warnings = new ArrayList<>();
        JsonElement root = JsonParser.parseString("[" + "{x:".repeat(DEPTH) + "0]", warnings);
        Assert.assertEquals(DEPTH, warnings.size());
        Assert.assertEquals(1, root.arrayValueOrThrow().size());
        warnings.clear();
        JsonParser.parseString("[".repeat(DEPTH) + "0" + "]".repeat(DEPTH - 1), warnings);
        Assert.assertEquals(1, warnings.size());
        Assert.assertThrows(
                JsonException.class,
                () -> Json.parse("[".repeat(DEPTH) + "0" + "]".repeat(DEPTH - 2)));
        Assert.assertThrows(
                JsonException.class,
                () -> Json.parse("[".repeat(DEPTH) + "0}", JsonParsingMode.EXTENDED));
        Assert.assertThrows(
                JsonException.class,
                () -> Json.parse("[" + "{x:".repeat(DEPTH) + "0]", JsonParsingMode.JSON5));
    }

    static class Node {
        Node next;
        int count;
    }

    static class ListNode {
        List<ListNode> children;
    }

    @Test
    public void deepPojoMappingAndSerializationUseHeapContinuations() throws Exception {
        String input = "{\"next\":".repeat(DEPTH) + "null" + "}".repeat(DEPTH);
        Node root = Json.parse(input, Node.class);
        int count = 0;
        for (Node node = root; node != null; node = node.next) {
            count++;
        }
        Assert.assertEquals(DEPTH, count);
        JsonElement tree = JsonParser.parseJavaObject(root);
        Node copy = tree.toJavaObject(Node.class);
        count = 0;
        for (Node node = copy; node != null; node = node.next) {
            count++;
        }
        Assert.assertEquals(DEPTH, count);
        Assert.assertTrue(Json.serialize(copy).startsWith("{\"next\": {"));
    }

    @Test
    public void deepTypedListFieldsMapRecursivelyWithoutRecursion() throws Exception {
        int depth = DEPTH / 2;
        String input = "{\"children\":[".repeat(depth) + "{\"children\":null}" + "]}".repeat(depth);
        ListNode node = Json.parse(input, ListNode.class);
        for (int i = 0; i < depth; i++) {
            Assert.assertEquals(1, node.children.size());
            node = node.children.get(0);
        }
        Assert.assertNull(node.children);
    }

    static class Fallback {
        @JsonProperty(onInvalid = JsonMappingAction.KEEP_DEFAULT)
        Node keep = new Node();

        @JsonProperty(onInvalid = JsonMappingAction.SET_NULL)
        Node clear = new Node();
    }

    @Test
    public void deepErrorsHaveFullPathsAndUnwindToTheCorrectFallback() throws Exception {
        String invalid = "{\"next\":".repeat(DEPTH) + "{\"count\":1.5}" + "}".repeat(DEPTH);
        JsonDataException error =
                Assert.assertThrows(JsonDataException.class, () -> Json.parse(invalid, Node.class));
        Assert.assertTrue(
                error.getMessage().startsWith("$" + "[\"next\"]".repeat(DEPTH) + "[\"count\"]:"));
        Fallback value = Json.parse("{keep:" + invalid + ",clear:" + invalid + "}", Fallback.class);
        Assert.assertNotNull(value.keep);
        Assert.assertNull(value.keep.next);
        Assert.assertNull(value.clear);
    }

    @Test
    public void deepJavaCollectionsAndObjectArraysSerialize() throws Exception {
        Object value = 0;
        for (int i = 0; i < DEPTH; i++) {
            value =
                    i % 3 == 0
                            ? new Object[] {value}
                            : i % 3 == 1 ? Collections.singletonList(value) : Map.of("x", value);
        }
        String text = Json.serialize(value);
        JsonElement tree = Json.parse(text, JsonParsingMode.STRICT);
        for (int i = DEPTH - 1; i >= 0; i--) {
            tree =
                    i % 3 == 2
                            ? tree.objectValueOrThrow().get("x")
                            : tree.arrayValueOrThrow().get(0);
        }
        Assert.assertEquals(0, tree.intValue());
    }

    @Test
    public void directAndIndirectCyclesAreRejectedWithIdentityPaths() {
        List<Object> list = new ArrayList<>();
        list.add(list);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("self", map);
        Object[] array = new Object[1];
        array[0] = array;
        Node node = new Node();
        node.next = node;
        for (Object value : new Object[] {list, map, array, node}) {
            JsonDataException error =
                    Assert.assertThrows(JsonDataException.class, () -> Json.serialize(value));
            Assert.assertTrue(error.getMessage(), error.getMessage().contains("cycle refers to $"));
        }
        List<Object> first = new ArrayList<>();
        Map<String, Object> second = new HashMap<>();
        first.add(second);
        second.put("back", first);
        JsonDataException error =
                Assert.assertThrows(JsonDataException.class, () -> Json.serialize(first));
        Assert.assertTrue(error.getMessage().startsWith("$[0][\"back\"]:"));
    }

    @Test
    public void deepCycleDetectionDoesNotBuildQuadraticPaths() {
        Node root = new Node();
        Node tail = root;
        for (int i = 0; i < DEPTH; i++) {
            tail.next = new Node();
            tail = tail.next;
        }
        tail.next = root;
        JsonDataException error =
                Assert.assertThrows(JsonDataException.class, () -> Json.serialize(root));
        Assert.assertTrue(error.getMessage().endsWith("cycle refers to $"));
        Assert.assertTrue(
                error.getMessage().startsWith("$" + "[\"next\"]".repeat(DEPTH + 1) + ":"));
    }

    static class IdentityOnly {
        int value = 7;

        @Override
        public boolean equals(Object other) {
            throw new AssertionError("Must use identity");
        }

        @Override
        public int hashCode() {
            throw new AssertionError("Must use identity");
        }
    }

    @Test
    public void sharedReferencesAreExpandedInEveryIndependentBranch() {
        IdentityOnly shared = new IdentityOnly();
        List<Object> branch = Arrays.asList(shared, shared);
        Assert.assertEquals(
                "[[{\"value\": 7}, {\"value\": 7}], [{\"value\": 7}, {\"value\": 7}]]",
                Json.serialize(Arrays.asList(branch, branch)));
    }

    static class Boundary {
        @JsonProperty(onInvalid = JsonMappingAction.KEEP_DEFAULT)
        List<Node> keep = List.of(new Node());

        @JsonProperty(onInvalid = JsonMappingAction.SET_NULL)
        List<Node> clear = List.of(new Node());

        int after;
    }

    @Test
    public void failedChildrenDoNotLeakIntoSiblingsOrPartialLists() throws Exception {
        Boundary value =
                Json.parse(
                        "{keep:[{count:1},{count:'bad'}],clear:[{count:2},{count:'bad'}],after:9}",
                        Boundary.class);
        Assert.assertEquals(1, value.keep.size());
        Assert.assertEquals(0, value.keep.get(0).count);
        Assert.assertNull(value.clear);
        Assert.assertEquals(9, value.after);
    }

    static class InnerPolicy {
        @JsonProperty(onInvalid = JsonMappingAction.KEEP_DEFAULT)
        int count = 7;

        @JsonProperty(onNull = JsonMappingAction.ERROR, onInvalid = JsonMappingAction.KEEP_DEFAULT)
        String required = "ok";
    }

    static class OuterPolicy {
        @JsonProperty(onInvalid = JsonMappingAction.SET_NULL)
        InnerPolicy child;

        int after;
    }

    @Test
    public void nearestApplicablePolicyHandlesTheError() throws Exception {
        OuterPolicy handled = Json.parse("{child:{count:'bad'},after:3}", OuterPolicy.class);
        Assert.assertEquals(7, handled.child.count);
        Assert.assertEquals(3, handled.after);
        OuterPolicy escalated = Json.parse("{child:{required:null},after:4}", OuterPolicy.class);
        Assert.assertNull(escalated.child);
        Assert.assertEquals(4, escalated.after);
    }

    @Test
    public void cycleDiagnosticsEscapeKeysAndPointToNonRootAncestor() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("quote\"", nested);
        JsonDataException error =
                Assert.assertThrows(JsonDataException.class, () -> Json.serialize(List.of(nested)));
        Assert.assertEquals("$[0][\"quote\\\"\"]: cycle refers to $[0]", error.getMessage());
    }

    @Test
    public void failedSerializationDoesNotPoisonLaterCalls() {
        List<Object> values = new ArrayList<>();
        values.add(values);
        Assert.assertThrows(JsonDataException.class, () -> Json.serialize(values));
        values.clear();
        values.add(1);
        Assert.assertEquals("[1]", Json.serialize(values));
    }
}
