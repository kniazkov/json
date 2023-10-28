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
    private final List<JsonElement> list;

    /**
     * Constructor.
     */
    public JsonArray() {
        this(null);
    }

    /**
     * Constructor (for internal use).
     * @param parent The parent of this element
     */
    JsonArray(JsonContainer parent) {
        super(parent);
        list = new ArrayList<>();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append('[');
        boolean flag = false;
        for (JsonElement element : list) {
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
    public JsonArray toArray() {
        return this;
    }

    @Override
    public int size() {
        return list.size();
    }

    /**
     * Returns a child element by its index.
     * @param index Index
     * @return Child element
     * @throws IndexOutOfBoundsException If the index goes out of bounds
     */
    public JsonElement getElement(int index) throws IndexOutOfBoundsException {
        return list.get(index);
    }

    /**
     * Creates a child element of the 'null' type and adds it to the end of the array.
     */
    public void addNull() {
        list.add(new JsonNull(this));
    }

    /**
     * Creates a child element of the 'number' type and adds it to the end of the array.
     * @param value Value of the number
     */
    public void addNumber(double value) {
        list.add(new JsonNumber(this, value));
    }

    /**
     * Adds a child element (for internal use).
     * @param elem Element
     */
    void addChild(JsonElement elem) {
        assert elem.getParent() == this;
        list.add(elem);
    }
}
