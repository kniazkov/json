/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Fluent construction of a JSON object. Each build returns an independent tree. Instances are
 * mutable and are not thread-safe. Null values require an explicit {@code putNull} call; arbitrary
 * Java objects require explicit conversion.
 */
public final class JsonObjectBuilder {
    private final JsonObject value = new JsonObject();

    /** Creates an empty builder. */
    public JsonObjectBuilder() {}

    /**
     * Adds a string.
     *
     * @param key Member name
     * @param item Value to add
     * @return This builder
     */
    public JsonObjectBuilder put(String key, String item) {
        Objects.requireNonNull(key, "key");
        value.addString(key, item);
        return this;
    }

    /**
     * Adds a boolean.
     *
     * @param key Member name
     * @param item Value to add
     * @return This builder
     */
    public JsonObjectBuilder put(String key, boolean item) {
        Objects.requireNonNull(key, "key");
        value.addBoolean(key, item);
        return this;
    }

    /**
     * Adds a number using its exact Java numeric type.
     *
     * @param key Member name
     * @param item Value to add
     * @return This builder
     * @throws IllegalArgumentException If the Number implementation is unsupported
     */
    public JsonObjectBuilder put(String key, Number item) {
        Objects.requireNonNull(key, "key");
        value.addNumber(key, item);
        return this;
    }

    /**
     * Adds a copy of an existing JSON node.
     *
     * @param key Member name
     * @param item Value to add
     * @return This builder
     */
    public JsonObjectBuilder put(String key, JsonElement item) {
        Objects.requireNonNull(key, "key");
        value.addElement(key, item);
        return this;
    }

    /**
     * Adds an explicit JSON null.
     *
     * @param key Member name
     * @return This builder
     */
    public JsonObjectBuilder putNull(String key) {
        Objects.requireNonNull(key, "key");
        value.addNull(key);
        return this;
    }

    /**
     * Adds a nested object after its configuration completes successfully. A retained child builder
     * cannot modify the added tree. If configuration throws, this method does not insert or replace
     * the nested value. Explicit changes to the outer builder from inside the callback are not
     * rolled back.
     *
     * @param key Member name
     * @param configure Callback that fills the nested builder
     * @return This builder
     */
    public JsonObjectBuilder object(String key, Consumer<JsonObjectBuilder> configure) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(configure, "configure");
        JsonObjectBuilder child = new JsonObjectBuilder();
        configure.accept(child);
        value.addElement(key, child.snapshotSource());
        return this;
    }

    /**
     * Adds a nested array after its configuration completes successfully. A retained child builder
     * cannot modify the added tree. If configuration throws, this method does not insert or replace
     * the nested value. Explicit changes to the outer builder from inside the callback are not
     * rolled back.
     *
     * @param key Member name
     * @param configure Callback that fills the nested builder
     * @return This builder
     */
    public JsonObjectBuilder array(String key, Consumer<JsonArrayBuilder> configure) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(configure, "configure");
        JsonArrayBuilder child = new JsonArrayBuilder();
        configure.accept(child);
        value.addElement(key, child.snapshotSource());
        return this;
    }

    /**
     * Copies the current contents into a detached tree. Later builder changes do not affect this
     * result; changes to this result do not affect the builder.
     *
     * @return Independent root object
     */
    public JsonObject build() {
        return (JsonObject) value.clone();
    }

    /** Supplies the internal tree for copying by another builder. */
    JsonObject snapshotSource() {
        return value;
    }
}
