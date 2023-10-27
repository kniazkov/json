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
     * Checks if the element is a 32-bit integer.
     * @return Checking result
     */
    public boolean isInteger() {
        return false;
    }

    /**
     * Checks if the element is a 64-bit integer.
     * @return Checking result
     */
    public boolean isLongInteger() {
        return false;
    }

    /**
     * Checks if the element is a number.
     * @return Checking result
     */
    public boolean isNumber() {
        return false;
    }

    /**
     * Tries to convert the value of an element to a 32-bit integer.
     * @return The value of the element converted to a 32-bit integer,
     *   or 0 if no such conversion is possible
     */
    public int getIntValue() {
        return 0;
    }

    /**
     * Tries to convert the value of an element to a 64-bit integer.
     * @return The value of the element converted to a 64-bit integer,
     *   or 0 if no such conversion is possible
     */
    public long getLongValue() {
        return 0;
    }

    /**
     * Tries to convert the value of an element to a double-precision real number.
     * @return The value of the element converted to a double-precision real number,
     *   or 0 if no such conversion is possible.
     */
    public double getDoubleValue() {
        return 0;
    }

    /**
     * Safely casts an element to the "array" type.
     * @return JSON array or {@code null} if the element is not an array.
     */
    public JsonArray toArray() {
        return null;
    }
}
