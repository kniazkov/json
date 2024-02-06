/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * Some JSON element that can contain other elements.
 */
public abstract class JsonContainer extends JsonElement {
    /**
     * Constructor.
     * @param parent The parent of this element
     */
    public JsonContainer(JsonContainer parent) {
        super(parent);
    }

    /**
     * Returns the number of elements in the container.
     * @return The number of elements
     */
    public abstract int size();
}
