package com.kniazkov.json;

import java.util.*;

/**
 * JSON object.
 */
public class JsonObject extends JsonContainer implements Map<String, JsonElement> {
    /**
     * Collection of elements.
     */
    private final Map<String, JsonElement> elements;

    /**
     * Keys (in ordered list).
     */
    private final List<String> keys;

    /**
     * Constructor.
     */
    public JsonObject() {
        this(null);
    }

    /**
     * Constructor (for internal use).
     * @param parent The parent of this element
     */
    JsonObject(JsonContainer parent) {
        super(parent);
        elements = new TreeMap<>();
        keys = new ArrayList<>();
    }

    @Override
    public int size() {
        return keys.size();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public JsonElement get(Object key) {
        return null;
    }

    @Override
    public JsonElement put(String key, JsonElement value) {
        return null;
    }

    @Override
    public JsonElement remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends JsonElement> m) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<String> keySet() {
        return null;
    }

    @Override
    public Collection<JsonElement> values() {
        return null;
    }

    @Override
    public Set<Entry<String, JsonElement>> entrySet() {
        return null;
    }

    @Override
    JsonElement clone(JsonContainer anotherParent) {
        return null;
    }

    @Override
    public String toString() {
        return null;
    }
}
