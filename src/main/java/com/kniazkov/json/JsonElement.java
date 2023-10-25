package com.kniazkov.json;

/**
 * Some JSON element, which can be an object, array, string, number, or literal.
 */
public abstract class JsonElement {
    /**
     * The parent of this element, that is, the element that contains this element.
     */
    private final JsonContainer parent;

    /**
     * Constructor.
     * @param parent The parent of this element
     */
    public JsonElement(final JsonContainer parent) {
        this.parent = parent;
    }

    /**
     * Returns the parent of this element, that is, the element that contains this element.
     * @return The parent of this element
     */
    public JsonContainer getParent() {
        return parent;
    }

    /**
     * Represents the element as a string.
     * @return String representation of the element
     */
    @Override
    public abstract String toString();

    /**
     * Represents the element as text with line breaks and indentation of nested elements,
     * making it easier to read.
     * @param indentation Symbols forming an indentation
     * @return Text representation of the element
     */
    public String toText(String indentation) {
        return toString();
    }

    /**
     * Checks if the element is a 32-bit number.
     * @return Checking result
     */
    public boolean isInteger() {
        return false;
    }

    /**
     * Checks if the element is a 64-bit number.
     * @return Checking result
     */
    public boolean isLongInteger() {
        return false;
    }
}
