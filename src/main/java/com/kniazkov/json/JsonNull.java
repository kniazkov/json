package com.kniazkov.json;

/**
 * JSON element representing a {@code null} literal.
 */
public class JsonNull extends JsonElement {
    /**
     * The instance.
     */
    public static final JsonNull instance = new JsonNull(null);

    /**
     * Constructor (for internal use).
     * @param parent The parent of this element
     */
    JsonNull(JsonContainer parent) {
        super(parent);
    }

    @Override
    JsonElement clone(JsonContainer anotherParent) {
        return new JsonNull(anotherParent);
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public boolean isNull() {
        return true;
    }
}
