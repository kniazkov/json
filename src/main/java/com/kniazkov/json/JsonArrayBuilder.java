/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Fluent construction of a JSON array. Each build returns an independent tree. Instances are
 * mutable and are not thread-safe. Null values require an explicit {@code addNull} call; arbitrary
 * Java objects require explicit conversion.
 */
public final class JsonArrayBuilder {
    private final JsonArray value = new JsonArray();

    /** Creates an empty builder. */
    public JsonArrayBuilder() {}

    /**
     * Adds a string.
     *
     * @param item Value to add
     * @return This builder
     */
    public JsonArrayBuilder add(String item) {
        value.addString(item);
        return this;
    }

    /**
     * Adds a boolean.
     *
     * @param item Value to add
     * @return This builder
     */
    public JsonArrayBuilder add(boolean item) {
        value.addBoolean(item);
        return this;
    }

    /**
     * Adds a number using its exact Java numeric type.
     *
     * @param item Value to add
     * @return This builder
     * @throws IllegalArgumentException If the Number implementation is unsupported
     */
    public JsonArrayBuilder add(Number item) {
        value.addNumber(item);
        return this;
    }

    /**
     * Adds a copy of an existing JSON node.
     *
     * @param item Value to add
     * @return This builder
     */
    public JsonArrayBuilder add(JsonElement item) {
        value.addElement(item);
        return this;
    }

    /**
     * Adds an explicit JSON null.
     *
     * @return This builder
     */
    public JsonArrayBuilder addNull() {
        value.addNull();
        return this;
    }

    /**
     * Adds a nested object after its configuration completes successfully. A retained child builder
     * cannot modify the added tree. If configuration throws, this method does not insert or replace
     * the nested value. Explicit changes to the outer builder from inside the callback are not
     * rolled back.
     *
     * @param configure Callback that fills the nested builder
     * @return This builder
     */
    public JsonArrayBuilder object(Consumer<JsonObjectBuilder> configure) {
        Objects.requireNonNull(configure, "configure");
        JsonObjectBuilder child = new JsonObjectBuilder();
        configure.accept(child);
        value.addElement(child.snapshotSource());
        return this;
    }

    /**
     * Adds a nested array after its configuration completes successfully. A retained child builder
     * cannot modify the added tree. If configuration throws, this method does not insert or replace
     * the nested value. Explicit changes to the outer builder from inside the callback are not
     * rolled back.
     *
     * @param configure Callback that fills the nested builder
     * @return This builder
     */
    public JsonArrayBuilder array(Consumer<JsonArrayBuilder> configure) {
        Objects.requireNonNull(configure, "configure");
        JsonArrayBuilder child = new JsonArrayBuilder();
        configure.accept(child);
        value.addElement(child.snapshotSource());
        return this;
    }

    /**
     * Adds strings, booleans, supported numbers or JSON nodes in iteration order. Null entries,
     * arbitrary Java objects and nested Java collections are rejected. No elements are appended
     * unless the entire input has been converted successfully.
     *
     * @param items Values to add
     * @return This builder
     * @throws IllegalArgumentException If an item has an unsupported type
     */
    public JsonArrayBuilder addAll(Iterable<?> items) {
        Objects.requireNonNull(items, "items");
        JsonArray staged = new JsonArray();
        for (Object item : items) {
            Objects.requireNonNull(item, "item");
            if (item instanceof JsonElement element) {
                staged.addElement(element);
            } else if (item instanceof String text) {
                staged.addString(text);
            } else if (item instanceof Boolean flag) {
                staged.addBoolean(flag);
            } else if (item instanceof Number number) {
                staged.addNumber(number);
            } else {
                throw new IllegalArgumentException(
                        "Unsupported builder value type: " + item.getClass().getName());
            }
        }
        value.addAll(staged);
        return this;
    }

    /**
     * Copies the current contents into a detached tree. Later builder changes do not affect this
     * result; changes to this result do not affect the builder.
     *
     * @return Independent root array
     */
    public JsonArray build() {
        return (JsonArray) value.clone();
    }

    /** Supplies the internal tree for copying by another builder. */
    JsonArray snapshotSource() {
        return value;
    }
}
