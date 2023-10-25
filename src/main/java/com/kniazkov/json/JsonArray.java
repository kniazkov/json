package com.kniazkov.json;

import java.util.ArrayList;
import java.util.List;

/**
 * Array of JSON elements.
 */
public final class JsonArray extends JsonContainer {
    /**
     * List of elements.
     */
    private final List<JsonElement> elements;

    /**
     * Constructor.
     * @param parent The parent of this element
     */
    public JsonArray(JsonContainer parent) {
        super(parent);
        elements = new ArrayList<>();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append('[');
        boolean flag = false;
        for (JsonElement element : elements) {
            if (flag) {
                builder.append(", ");
            }
            flag = true;
            builder.append(element.toString());
        }
        builder.append(']');
        return builder.toString();
    }

    @Override
    int size() {
        return elements.size();
    }
}
