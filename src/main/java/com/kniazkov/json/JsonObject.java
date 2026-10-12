/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** JSON object. */
public final class JsonObject extends JsonContainer implements Map<String, JsonElement> {
    /** Collection of elements. */
    private final Map<String, JsonElement> elements;

    /** Constructor. */
    public JsonObject() {
        this(null);
    }

    /**
     * Constructor (for internal use).
     *
     * @param parent The parent of this element
     */
    JsonObject(JsonContainer parent) {
        super(parent);
        elements = new LinkedHashMap<>();
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return elements.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return elements.containsValue(value);
    }

    @Override
    public JsonElement get(Object key) {
        return elements.get(key);
    }

    @Override
    public JsonElement put(String key, JsonElement value) {
        Objects.requireNonNull(key, "key");
        JsonElement child = Objects.requireNonNull(value, "value").clone(this);
        return elements.put(key, child);
    }

    @Override
    public JsonElement remove(Object key) {
        return elements.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends JsonElement> map) {
        for (Map.Entry<? extends String, ? extends JsonElement> pair : map.entrySet()) {
            put(pair.getKey(), pair.getValue());
        }
    }

    @Override
    public void clear() {
        elements.clear();
    }

    @Override
    public Set<String> keySet() {
        return elements.keySet();
    }

    @Override
    public Collection<JsonElement> values() {
        return elements.values();
    }

    @Override
    public Set<Entry<String, JsonElement>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public int size() {
                return elements.size();
            }

            @Override
            public void clear() {
                elements.clear();
            }

            @Override
            public Iterator<Entry<String, JsonElement>> iterator() {
                Iterator<Entry<String, JsonElement>> iterator = elements.entrySet().iterator();
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public Entry<String, JsonElement> next() {
                        Entry<String, JsonElement> entry = iterator.next();
                        // Never expose the backing entry: setValue must follow put's ownership
                        // rules.
                        return new Entry<>() {
                            @Override
                            public String getKey() {
                                return entry.getKey();
                            }

                            @Override
                            public JsonElement getValue() {
                                return entry.getValue();
                            }

                            @Override
                            public JsonElement setValue(JsonElement value) {
                                JsonElement child =
                                        Objects.requireNonNull(value, "value")
                                                .clone(JsonObject.this);
                                return entry.setValue(child);
                            }

                            @Override
                            public boolean equals(Object other) {
                                return other instanceof Entry<?, ?> pair
                                        && Objects.equals(getKey(), pair.getKey())
                                        && Objects.equals(getValue(), pair.getValue());
                            }

                            @Override
                            public int hashCode() {
                                return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
                            }

                            @Override
                            public String toString() {
                                return getKey() + "=" + getValue();
                            }
                        };
                    }

                    @Override
                    public void remove() {
                        iterator.remove();
                    }
                };
            }
        };
    }

    @Override
    JsonElement clone(JsonContainer anotherParent) {
        return TreeTraversal.copy(this, anotherParent);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        TreeTraversal.write(this, builder, null, 0);
        return builder.toString();
    }

    @Override
    protected void toText(StringBuilder builder, String indentation, int level) {
        TreeTraversal.write(this, builder, indentation, level);
    }

    @Override
    public Object toJavaObject() {
        return TreeTraversal.natural(this);
    }

    /**
     * Returns a child element by its key.
     *
     * @param key Key
     * @return Child element
     */
    public JsonElement element(String key) {
        return get(key);
    }

    /**
     * Makes a copy of a JSON element and adds a key-value pair to the object.
     *
     * @param key Key
     * @param elem Element
     */
    public void addElement(String key, JsonElement elem) {
        put(key, elem);
    }

    /**
     * Creates a child element of the 'null' type and adds a key-value pair to the object.
     *
     * @param key Key
     */
    public void addNull(String key) {
        addChild(key, new JsonNull(this));
    }

    /**
     * Creates a child element of the 'boolean' type and adds a key-value pair to the object.
     *
     * @param key Key
     * @param value Boolean value
     */
    public void addBoolean(String key, boolean value) {
        addChild(key, new JsonBoolean(this, value));
    }

    /**
     * Creates a child element of the 'number' type and adds a key-value pair to the object.
     *
     * @param key Key
     * @param value Value of the number
     */
    public void addNumber(String key, double value) {
        addChild(key, JsonNumber.of(this, value));
    }

    /**
     * Adds an exact number using its Java type.
     *
     * @param key Property key
     * @param value Numeric value
     */
    public void addNumber(String key, float value) {
        addChild(key, JsonNumber.of(this, value));
    }

    /**
     * Adds an exact number using its Java type.
     *
     * @param key Property key
     * @param value Numeric value
     */
    public void addNumber(String key, long value) {
        addChild(key, JsonNumber.of(this, value));
    }

    /**
     * Adds an exact number using its Java type.
     *
     * @param key Property key
     * @param value Numeric value
     */
    public void addNumber(String key, Number value) {
        addChild(key, JsonNumber.of(this, value));
    }

    /**
     * Creates a child element of the 'string' type and adds a key-value pair to the object.
     *
     * @param key Key
     * @param value Value of the string
     */
    public void addString(String key, String value) {
        addChild(key, new JsonString(this, value));
    }

    /**
     * Creates an empty array as a child of this object.
     *
     * @param key Key
     * @return The empty array created, so that it can be filled.
     */
    public JsonArray createArray(String key) {
        JsonArray child = new JsonArray(this);
        addChild(key, child);
        return child;
    }

    /**
     * Creates an empty object as a child of this object.
     *
     * @param key Key
     * @return The empty object created, so that it can be filled.
     */
    public JsonObject createObject(String key) {
        JsonObject child = new JsonObject(this);
        addChild(key, child);
        return child;
    }

    /**
     * Adds a child element (for internal use).
     *
     * @param key Key
     * @param elem Element
     */
    void addChild(String key, JsonElement elem) {
        elements.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(elem, "elem"));
    }

    @Override
    public JsonObject toJsonObject() {
        return this;
    }
}
