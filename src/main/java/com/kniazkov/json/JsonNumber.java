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
     * @param value Value of the number.
     */
    public JsonNumber(double value) {
        this(null, value);
    }

    /**
     * Constructor (for internal use).
     * @param parent The parent of this element
     * @param value Value of the number.
     */
    JsonNumber(JsonContainer parent, double value) {
        super(parent);
        this.value = value;
    }

    @Override
    JsonElement clone(JsonContainer anotherParent) {
        return new JsonNumber(anotherParent, value);
    }

    @Override
    public String toString() {
        return Utils.doubleToString(value);
    }

    @Override
    public Object toJavaObject() {
        return value;
    }

    @Override
    public <T> T toJavaObject(Class<T> type) {
        if (type == java.lang.Byte.class) {
            Byte byteValue = (byte)getIntValue();
            return (T)byteValue;
        }
        if (type == java.lang.Short.class) {
            Short shortValue = (short)getIntValue();
            return (T)shortValue;
        }
        if (type == java.lang.Integer.class) {
            Integer intValue = getIntValue();
            return (T)intValue;
        }
        if (type == java.lang.Long.class) {
            Long longValue = getLongValue();
            return (T)longValue;
        }
        if (type == java.lang.Float.class) {
            Float floatValue = (float)value;
            return (T)floatValue;
        }
        if (type == java.lang.Double.class) {
            Double doubleValue = value;
            return (T)doubleValue;
        }
        return null;
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

    @Override
    public String getStringValue() {
        return Utils.doubleToString(value);
    }
}
