/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.lang.reflect.Field;
import org.junit.Assert;
import org.junit.Test;

public class MappingFieldPolicyTest {
    static class State {
        static boolean enabled = true;
        static final String CONSTANT = "constant";
        transient String secret = "SECRET";
        private int value = 1;
        final int fixed;
        final String label;

        State() {
            fixed = 7;
            label = new String("constructor");
        }
    }

    @Test
    public void serializationIncludesPrivateAndFinalButNotStaticOrTransient() throws Exception {
        JsonObject json =
                Json.parse(Json.serialize(new State()), JsonParsingMode.STRICT)
                        .objectValueOrThrow();
        Assert.assertEquals(3, json.size());
        Assert.assertEquals(1, json.element("value").intValue());
        Assert.assertEquals(7, json.element("fixed").intValue());
        Assert.assertEquals("constructor", json.element("label").stringValue());
        Assert.assertNull(json.element("enabled"));
        Assert.assertNull(json.element("CONSTANT"));
        Assert.assertNull(json.element("secret"));
    }

    @Test
    public void mappingCannotChangeGlobalTransientOrFinalState() throws Exception {
        boolean previous = State.enabled;
        try {
            State.enabled = true;
            State value =
                    Json.parse(
                            "{enabled:false,CONSTANT:'bad',secret:'bad',value:42,fixed:99,label:'bad'}",
                            State.class);
            Assert.assertNotNull(value);
            Assert.assertTrue(State.enabled);
            Assert.assertEquals("SECRET", value.secret);
            Assert.assertEquals(42, value.value);
            Assert.assertEquals(7, value.fixed);
            Assert.assertEquals("constructor", value.label);
        } finally {
            State.enabled = previous;
        }
    }

    static class Annotated {
        @JsonProperty(name = "global")
        static int global = 10;

        @JsonProperty(name = "hidden")
        transient String hidden = "hidden";

        @JsonProperty(name = "immutable")
        final int immutable;

        @JsonProperty(name = "renamed")
        private int field = 1;

        Annotated() {
            immutable = 20;
        }
    }

    @Test
    public void annotationOnlyRenamesEligibleFields() throws Exception {
        int previous = Annotated.global;
        try {
            Annotated.global = 10;
            Annotated value =
                    Json.parse("{global:99,hidden:'bad',immutable:99,renamed:5}", Annotated.class);
            Assert.assertEquals(10, Annotated.global);
            Assert.assertEquals("hidden", value.hidden);
            Assert.assertEquals(20, value.immutable);
            Assert.assertEquals(5, value.field);
            JsonObject json = Json.parse(Json.serialize(value)).objectValueOrThrow();
            Assert.assertEquals(2, json.size());
            Assert.assertEquals(20, json.element("immutable").intValue());
            Assert.assertEquals(5, json.element("renamed").intValue());
        } finally {
            Annotated.global = previous;
        }
    }

    static class Parent {
        int inherited = 1;
        int hiddenTransient = 2;
        int hiddenStatic = 3;
        int hiddenFinal = 4;
        transient int parentTransient = 5;
        static int parentStatic = 6;
    }

    static class Child extends Parent {
        transient int hiddenTransient = 12;
        static int hiddenStatic = 13;
        final int hiddenFinal;

        Child() {
            hiddenFinal = 14;
        }
    }

    @Test
    public void excludedChildFieldsDoNotResurrectParentFields() throws Exception {
        int childStatic = Child.hiddenStatic;
        int parentStatic = Parent.parentStatic;
        try {
            Child value =
                    Json.parse(
                            "{inherited:99,hiddenTransient:99,hiddenStatic:99,"
                                    + "hiddenFinal:99,parentTransient:99,parentStatic:99}",
                            Child.class);
            Assert.assertEquals(99, value.inherited);
            Assert.assertEquals(12, value.hiddenTransient);
            Assert.assertEquals(14, value.hiddenFinal);
            Assert.assertEquals(childStatic, Child.hiddenStatic);
            Assert.assertEquals(parentStatic, Parent.parentStatic);
            Parent base = value;
            Assert.assertEquals(2, base.hiddenTransient);
            Assert.assertEquals(3, base.hiddenStatic);
            Assert.assertEquals(4, base.hiddenFinal);
            Assert.assertEquals(5, base.parentTransient);
            JsonObject json = Json.parse(Json.serialize(value)).objectValueOrThrow();
            Assert.assertNull(json.element("hiddenTransient"));
            Assert.assertNull(json.element("hiddenStatic"));
            Assert.assertEquals(14, json.element("hiddenFinal").intValue());
        } finally {
            Child.hiddenStatic = childStatic;
            Parent.parentStatic = parentStatic;
        }
    }

    static class Outer {
        String secret = "must not escape";

        class Inner {
            int value = 3;

            String outerSecret() {
                return secret;
            }
        }
    }

    @Test
    public void innerClassSyntheticOuterReferenceIsExcludedInBothDirections() throws Exception {
        Outer.Inner inner = new Outer().new Inner();
        Assert.assertEquals("{\"value\": 3}", Json.serialize(inner));
        boolean found = false;
        for (Field field : inner.getClass().getDeclaredFields()) {
            if (field.isSynthetic()) {
                found = true;
                Assert.assertFalse(Utils.isReadableField(field));
                Assert.assertFalse(Utils.isWritableField(field));
            }
        }
        Assert.assertTrue("Fixture must actually contain a synthetic outer reference", found);
        // No enclosing instance can be supplied to the existing no-argument mapper.
        Assert.assertThrows(
                JsonDataException.class, () -> Json.parse("{value:9}", Outer.Inner.class));
    }
}
