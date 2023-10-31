package com.kniazkov.json;

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
    }

    @Override
    public int size() {
        return elements.size();
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
