package com.kniazkov.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * JSON object.
 */
public class JsonObject extends JsonContainer {
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
    JsonElement clone(JsonContainer anotherParent) {
        return null;
    }

    @Override
    public String toString() {
        return null;
    }
}
