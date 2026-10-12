/* Copyright (c) 2026 Ivan Kniazkov */
package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

public class InheritedFieldsTest {
    static class Base {
        private int alpha = 1;

        @JsonProperty(name = "renamed")
        private String label = "initial";

        final int fixed = 17;
    }

    static class Middle extends Base {
        protected long beta = 2;
    }

    static class Derived extends Middle {
        int gamma = 3;
    }

    @Test
    public void threeLevelRoundTripIncludesPrivateAnnotatedAndFinalFields() throws Exception {
        Derived original =
                Json.parse("{alpha:10,renamed:'changed',beta:20,gamma:30}", Derived.class);
        String text = Json.serialize(original);
        JsonObject json = Json.parse(text, JsonParsingMode.STRICT).objectValueOrThrow();
        Assert.assertEquals(5, json.size());
        Assert.assertEquals(10, json.element("alpha").intValue());
        Assert.assertEquals("changed", json.element("renamed").stringValue());
        Assert.assertEquals(20, json.element("beta").longValue());
        Assert.assertEquals(30, json.element("gamma").intValue());
        Assert.assertEquals(17, json.element("fixed").intValue());
        Derived copy = Json.parse(text, Derived.class);
        Assert.assertEquals(10, ((Base) copy).alpha);
        Assert.assertEquals("changed", ((Base) copy).label);
        Assert.assertEquals(20, copy.beta);
        Assert.assertEquals(30, copy.gamma);
        Assert.assertEquals(text, Json.serialize(copy));
    }

    static class HiddenBase {
        @JsonProperty(name = "oldName")
        private int value = 1;
    }

    static class HiddenChild extends HiddenBase {
        @JsonProperty(name = "newName")
        String value = "child";
    }

    @Test
    public void javaNameHidingPrecedesRenamingAndCanChangeType() throws Exception {
        HiddenChild value = Json.parse("{oldName:99,newName:'updated'}", HiddenChild.class);
        Assert.assertEquals(1, ((HiddenBase) value).value);
        Assert.assertEquals("updated", value.value);
        Assert.assertEquals("{\"newName\": \"updated\"}", Json.serialize(value));
    }

    static class ExcludedChild extends HiddenBase {
        @JsonProperty(name = "ignored")
        transient int value = 2;

        @JsonProperty(name = "oldName")
        int replacement = 3;
    }

    @Test
    public void excludedHidingFieldDoesNotExposeOrReserveParentName() throws Exception {
        ExcludedChild value = Json.parse("{oldName:42,ignored:99}", ExcludedChild.class);
        Assert.assertEquals(42, value.replacement);
        Assert.assertEquals(1, ((HiddenBase) value).value);
        Assert.assertEquals(2, value.value);
        Assert.assertEquals("{\"oldName\": 42}", Json.serialize(value));
    }

    static class SameClassCollision {
        int duplicate;

        @JsonProperty(name = "duplicate")
        int other;
    }

    static class CollisionBase {
        @JsonProperty(name = "duplicate")
        int first;
    }

    static class InheritedCollision extends CollisionBase {
        @JsonProperty(name = "duplicate")
        int second;
    }

    static class FinalCollision extends CollisionBase {
        @JsonProperty(name = "duplicate")
        final int second = 7;
    }

    private void assertCollision(Object value, String first, String second) throws Exception {
        IllegalArgumentException serialize =
                Assert.assertThrows(IllegalArgumentException.class, () -> Json.serialize(value));
        IllegalArgumentException parse =
                Assert.assertThrows(
                        IllegalArgumentException.class, () -> Json.parse("{}", value.getClass()));
        Assert.assertEquals(serialize.getMessage(), parse.getMessage());
        Assert.assertTrue(serialize.getMessage().contains("'duplicate'"));
        Assert.assertTrue(serialize.getMessage().contains(first));
        Assert.assertTrue(serialize.getMessage().contains(second));
    }

    @Test
    public void duplicateJsonNamesAreRejectedInBothDirections() throws Exception {
        assertCollision(
                new SameClassCollision(),
                "SameClassCollision.duplicate",
                "SameClassCollision.other");
        assertCollision(
                new InheritedCollision(), "CollisionBase.first", "InheritedCollision.second");
        assertCollision(new FinalCollision(), "CollisionBase.first", "FinalCollision.second");
    }

    static class ConstructorCollision extends CollisionBase {
        @JsonProperty(name = "duplicate")
        int second;

        ConstructorCollision() {
            throw new AssertionError("Must validate before construction");
        }
    }

    @Test
    public void collisionIsDetectedBeforeTargetConstructorRuns() {
        Assert.assertThrows(
                IllegalArgumentException.class, () -> Json.parse("{}", ConstructorCollision.class));
    }

    static class IgnoredCollisions extends Base {
        @JsonProperty(name = "renamed")
        static int global;

        @JsonProperty(name = "renamed")
        transient int temporary;
    }

    @Test
    public void excludedFieldsDoNotCauseJsonNameCollisions() throws Exception {
        IgnoredCollisions copy = Json.parse("{alpha:10,renamed:'new'}", IgnoredCollisions.class);
        JsonObject json = Json.parse(Json.serialize(copy)).objectValueOrThrow();
        Assert.assertEquals(3, json.size());
        Assert.assertEquals("new", json.element("renamed").stringValue());
        Assert.assertEquals(10, json.element("alpha").intValue());
    }
}
