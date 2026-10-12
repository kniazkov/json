/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.math.BigDecimal;

/**
 * Some JSON element, which can be an object, array, string, number, or literal.
 */
public abstract class JsonElement implements Cloneable {
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
    public JsonContainer parent() {
        return parent;
    }

    /**
     * Makes a clone (copy) of this element with a different parent.
     * @param anotherParent Another parent
     * @return A clone of this element
     */
    abstract JsonElement clone(JsonContainer anotherParent);

    @Override
    public JsonElement clone() {
        return clone(null);
    }

    /**
     * Represents the element as a string.
     * @return String representation of the element
     */
    @Override
    public abstract String toString();

    /**
     * Represents the element as text with line breaks and indentation of nested elements,
     *   making it easier to read.
     * @param indentation Symbols forming an indentation
     * @return Text representation of the element
     */
    public String toText(String indentation) {
        final StringBuilder builder = new StringBuilder();
        toText(builder, indentation, 0);
        return builder.toString();
    }

    /**
     * Represents the element as text with line breaks and indentation of nested elements,
     *   making it easier to read (internal method).
     * @param builder Where to build text representation
     * @param indentation Symbols forming an indentation
     * @param level Current indentation level
     */
    protected void toText(StringBuilder builder, String indentation, int level) {
        builder.append(toString());
    }

    /**
     * Represents the element as a Java object.
     * Finite numbers retain BigInteger/BigDecimal; JSON5 special numbers use Double.
     * @return Object containing element data
     */
    public abstract Object toJavaObject();

    /**
     * Represents the element as a Java object of specified type.
     * @param type The type descriptor
     * @return Object containing element data or {@code null} if element can't be represented
     *   as an object of specified type
     * @param <T> The type
     */
    public abstract <T> T toJavaObject(Class<T> type);

    /**
     * Checks if the element is a null literal.
     * @return Checking result
     */
    public boolean isNull() {
        return false;
    }

    /**
     * Checks if the element contains boolean value.
     * @return Checking result
     */
    public boolean isBoolean() {
        return false;
    }

    /**
     * Tries to represent the value of the element as a boolean.
     * @return The value of the element represented as a boolean,
     *   or {@code false} if the element is not boolean
     */
    public boolean booleanValue() {
        return false;
    }

    /**
     * Checks for the JSON5 not-a-number value.
     * @return Whether this element represents JSON5 NaN
     */
    public boolean isNaN() { return false; }

    /**
     * Checks for either signed JSON5 infinity.
     * @return Whether this element represents either JSON5 infinity
     */
    public boolean isInfinite() { return false; }

    /**
     * Checks for positive JSON5 infinity.
     * @return Whether this element represents positive JSON5 infinity
     */
    public boolean isPositiveInfinity() { return false; }

    /**
     * Checks for negative JSON5 infinity.
     * @return Whether this element represents negative JSON5 infinity
     */
    public boolean isNegativeInfinity() { return false; }

    /**
     * Checks the stored numeric value, independently of double conversion overflow.
     * @return Whether this element is a finite number, regardless of double conversion overflow
     */
    public boolean isFinite() { return isNumber() && !isNaN() && !isInfinite(); }

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
     * Tries to convert the value of the element to a 32-bit integer.
     * Numeric fractions are truncated toward zero and overflow saturates at the range boundaries.
     * @return The value of the element converted to a 32-bit integer,
     *   or 0 if no such conversion is possible
     */
    public int intValue() {
        return 0;
    }

    /**
     * Tries to convert the value of the element to a 64-bit integer.
     * Numeric fractions are truncated toward zero and overflow saturates at the range boundaries.
     * @return The value of the element converted to a 64-bit integer,
     *   or 0 if no such conversion is possible
     */
    public long longValue() {
        return 0;
    }

    /**
     * Tries to represent the value of the element as a double-precision real number.
     * Exact numeric values may round, underflow to zero, or overflow to infinity.
     * @return The value of the element represented as a double-precision real number,
     *   or 0 if no such conversion is possible.
     */
    public double doubleValue() {
        return 0;
    }

    /**
     * Checks if the element is a string.
     * @return Checking result
     */
    public boolean isString() {
        return false;
    }

    /**
     * Tries to represent the value of the element as a string.
     * @return The value of the element represented as a string,
     *   or empty string if no such conversion is possible
     */
    public String stringValue() {
        return "";
    }

    /**
     * Safely casts an element to the "array" type.
     * @return JSON array or {@code null} if the element is not an array.
     */
    public JsonArray toJsonArray() {
        return null;
    }

    /**
     * Safely casts an element to the "object" type.
     * @return JSON object or {@code null} if the element is not an object.
     */
    public JsonObject toJsonObject() {
        return null;
    }

    /**
     * Returns the exact stored number, or null for a non-numeric element.
     * @return BigInteger for an integer, BigDecimal for a real, Double for a special number, or null
     */
    public Number numberValue() {
        return null;
    }

    /**
     * Returns the exact decimal representation, or null for non-numeric or non-finite values.
     * @return The decimal value, or null
     */
    public BigDecimal decimalValue() {
        return null;
    }

    /**
     * Returns a boolean without coercing other JSON types.
     * @return Boolean value
     * @throws JsonDataException If this element is not a boolean
     */
    public final boolean booleanValueOrThrow() {
        if (!isBoolean()) {
            throw invalidData("a boolean");
        }
        return booleanValue();
    }

    /**
     * Returns an exactly representable signed 32-bit integer.
     * @return Integer value
     * @throws JsonDataException If the type, fraction or range does not fit
     */
    public final int intValueOrThrow() {
        if (!isInteger()) {
            throw invalidData("an exact signed 32-bit integer");
        }
        return intValue();
    }

    /**
     * Returns an exactly representable signed 64-bit integer.
     * @return Long value
     * @throws JsonDataException If the type, fraction or range does not fit
     */
    public final long longValueOrThrow() {
        if (!isLongInteger()) {
            throw invalidData("an exact signed 64-bit integer");
        }
        return longValue();
    }

    /**
     * Returns a double, permitting rounding but rejecting overflow and underflow to zero.
     * @return Finite double value
     * @throws JsonDataException If the element is not numeric or its magnitude cannot be represented
     */
    public final double doubleValueOrThrow() {
        if (!isNumber()) {
            throw invalidData("a number representable as a finite double");
        }
        double value = doubleValue();
        if (!Double.isFinite(value) || (value == 0.0 && decimalValue().signum() != 0)) {
            throw invalidData("a number representable as a finite double without underflow to zero");
        }
        return value;
    }

    /**
     * Returns a JSON string without converting numbers, booleans or null to text.
     * @return String value
     * @throws JsonDataException If the element is not a JSON string or has a null string payload
     */
    public final String stringValueOrThrow() {
        if (!isString() || stringValue() == null) {
            throw invalidData("a string with a non-null value");
        }
        return stringValue();
    }

    /**
     * Returns the exact stored number.
     * @return BigInteger or BigDecimal for finite values, Double for NaN or infinities
     * @throws JsonDataException If the element is not numeric
     */
    public final Number numberValueOrThrow() {
        Number value = numberValue();
        if (value == null) {
            throw invalidData("a number");
        }
        return value;
    }

    /**
     * Returns the exact decimal representation of a finite number.
     * @return Decimal value
     * @throws JsonDataException If the element is not a finite number
     */
    public final BigDecimal decimalValueOrThrow() {
        BigDecimal value = decimalValue();
        if (value == null) {
            throw invalidData("a finite number");
        }
        return value;
    }

    /**
     * Returns this element as a JSON array.
     * @return JSON array
     * @throws JsonDataException If the element is not an array
     */
    public final JsonArray arrayValueOrThrow() {
        JsonArray value = toJsonArray();
        if (value == null) {
            throw invalidData("an array");
        }
        return value;
    }

    /**
     * Returns this element as a JSON object.
     * @return JSON object
     * @throws JsonDataException If the element is not an object
     */
    public final JsonObject objectValueOrThrow() {
        JsonObject value = toJsonObject();
        if (value == null) {
            throw invalidData("an object");
        }
        return value;
    }

    private JsonDataException invalidData(String expected) {
        // Do not serialize the value: it may be sensitive, very large, or cyclic.
        return new JsonDataException("Expected " + expected + "; actual element: "
                + getClass().getSimpleName());
    }

}
