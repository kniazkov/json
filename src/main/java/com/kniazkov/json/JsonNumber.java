package com.kniazkov.json;

/**
 * JSON element representing a number.
 */
public final class JsonNumber extends JsonElement {
    /**
     * Value of the number.
     */
    private final double value;

    /**
     * Constructor.
     * @param parent The parent of this element
     */
    public JsonNumber(JsonContainer parent, double value) {
        super(parent);
        this.value = value;
    }

    @Override
    public String toString() {
        if (isLongInteger()) {
            return String.valueOf((long)value);
        }
        return String.valueOf(value);
    }

    @Override
    public boolean isInteger() {
        return value == (int)value;
    }

    @Override
    public boolean isLongInteger() {
        return value == (long)value;
    }

    @Override
    public boolean isNumber() {
        return true;
    }

    @Override
    public int getIntValue() {
        return (int)value;
    }

    @Override
    public long getLongValue() {
        return (long)value;
    }

    @Override
    public double getDoubleValue() {
        return (double)value;
    }
}
