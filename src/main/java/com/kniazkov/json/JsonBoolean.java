package com.kniazkov.json;

/**
 * JSON element representing a boolean value.
 */
public class JsonBoolean extends JsonElement {
    /**
     * Boolean value.
     */
    private final boolean value;

    /**
     * Constructor.
     * @param value Boolean value
     */
    public JsonBoolean(boolean value) {
        this(null, value);
    }

    /**
     * Constructor (for internal use).
     * @param parent The parent of this element
     * @param value Boolean value
     */
    JsonBoolean(JsonContainer parent, boolean value) {
        super(parent);
        this.value = value;
    }

    @Override
    JsonElement clone(JsonContainer anotherParent) {
        return new JsonBoolean(anotherParent, value);
    }

    @Override
    public String toString() {
        return value ? "true" : "false";
    }

    @Override
    public boolean isBoolean() {
        return true;
    }

    @Override
    public boolean getBooleanValue() {
        return value;
    }
}
