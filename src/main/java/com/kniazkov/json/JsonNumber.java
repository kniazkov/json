/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Base class for exact finite numbers and JSON5 special numbers. Use {@code of(...)} to create a number.
 * Existing primitive getters truncate fractions and saturate integer overflow,
 * as Java floating-point casts did in earlier versions. Double conversion can
 * round, underflow to zero, or overflow to infinity.
 */
public abstract class JsonNumber extends JsonElement {
    JsonNumber(JsonContainer parent) {
        super(parent);
    }

    /**
     * Creates an integer.
     * @param value Integer value
     * @return An integer element
     */
    public static JsonInteger of(long value) {
        return of(null, value);
    }

    /** Creates a number directly with its parent, without a temporary JSON node. */
    static JsonInteger of(JsonContainer parent, long value) {
        return of(parent, BigInteger.valueOf(value));
    }

    /**
     * Creates an arbitrary-precision integer.
     * @param value Integer value, not null
     * @return An integer element
     */
    public static JsonInteger of(BigInteger value) {
        return of(null, value);
    }

    /** Creates a number directly with its parent, without a temporary JSON node. */
    static JsonInteger of(JsonContainer parent, BigInteger value) {
        return new JsonInteger(parent, value);
    }

    /**
     * Creates a real number from the canonical decimal representation of a double.
     * @param value Floating-point value, including NaN and infinities
     * @return A real element for finite input, otherwise JsonNaN or JsonInfinity
     */
    public static JsonNumber of(double value) {
        return of(null, value);
    }

    /** Creates a number directly with its parent, without a temporary JSON node. */
    static JsonNumber of(JsonContainer parent, double value) {
        if (Double.isNaN(value)) { return new JsonNaN(parent); }
        if (Double.isInfinite(value)) { return new JsonInfinity(parent, value < 0); }
        return of(parent, BigDecimal.valueOf(value));
    }

    /**
     * Creates a real number without first widening a float to double.
     * @param value Floating-point value, including NaN and infinities
     * @return A real element for finite input, otherwise JsonNaN or JsonInfinity
     */
    public static JsonNumber of(float value) {
        return of(null, value);
    }

    /** Creates a number directly with its parent, without a temporary JSON node. */
    static JsonNumber of(JsonContainer parent, float value) {
        if (!Float.isFinite(value)) { return of(parent, (double) value); }
        return of(parent, new BigDecimal(Float.toString(value)));
    }

    /**
     * Creates an exact decimal number.
     * @param value Decimal value, not null
     * @return An exact real element
     */
    public static JsonReal of(BigDecimal value) {
        return of(null, value);
    }

    /** Creates a number directly with its parent, without a temporary JSON node. */
    static JsonReal of(JsonContainer parent, BigDecimal value) {
        return new JsonReal(parent, value);
    }

    /**
     * Creates a number according to its runtime Java type.
     * @param value Byte, Short, Integer, Long, Float, Double, BigInteger or BigDecimal
     * @return The corresponding integer, real, NaN or infinity element
     * @throws IllegalArgumentException If the Number implementation is unsupported
     */
    public static JsonNumber of(Number value) {
        return of(null, value);
    }

    /** Creates a number directly with its parent, without a temporary JSON node. */
    static JsonNumber of(JsonContainer parent, Number value) {
        Objects.requireNonNull(value, "value");
        if (value instanceof BigInteger integer) {
            return of(parent, integer);
        }
        if (value instanceof BigDecimal decimal) {
            return of(parent, decimal);
        }
        if (value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long) {
            return of(parent, value.longValue());
        }
        if (value instanceof Float real) {
            return of(parent, real.floatValue());
        }
        if (value instanceof Double real) {
            return of(parent, real.doubleValue());
        }
        throw new IllegalArgumentException("Unsupported number type: " + value.getClass().getName());
    }

    @Override
    public Number toJavaObject() {
        return numberValue();
    }

    @Override
    public <T> T toJavaObject(Class<T> type) {
        Object result;
        if (type == Object.class || type == Number.class) {
            result = numberValue();
        }
        else if (type == Byte.class) {
            result = (byte) intValue();
        }
        else if (type == Short.class) {
            result = (short) intValue();
        }
        else if (type == Integer.class) {
            result = intValue();
        }
        else if (type == Long.class) {
            result = longValue();
        }
        else if (type == Float.class) {
            result = numberValue().floatValue();
        }
        else if (type == Double.class) {
            result = doubleValue();
        }
        else if (type == BigDecimal.class) {
            result = decimalValue();
        }
        else if (type == BigInteger.class) {
            if (!isFinite()) { return null; }
            try {
                result = numberValue() instanceof BigInteger ? numberValue() : decimalValue().toBigIntegerExact();
            } catch (ArithmeticException exception) {
                return null;
            }
        } else {
            return null;
        }
        return type.cast(result);
    }

    @Override
    public boolean isInteger() {
        BigDecimal value = decimalValue();
        return value != null && value.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) >= 0
                && value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0
                && value.stripTrailingZeros().scale() <= 0;
    }

    @Override
    public boolean isLongInteger() {
        BigDecimal value = decimalValue();
        return value != null && value.compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) >= 0
                && value.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) <= 0
                && value.stripTrailingZeros().scale() <= 0;
    }

    @Override
    public final boolean isNumber() {
        return true;
    }

    @Override
    public int intValue() {
        if (!isFinite()) { return (int) doubleValue(); }
        BigDecimal value = decimalValue();
        if (value.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) < 0) {
            return Integer.MIN_VALUE;
        }
        if (value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            return Integer.MAX_VALUE;
        }
        return numberValue().intValue();
    }

    @Override
    public long longValue() {
        if (!isFinite()) { return (long) doubleValue(); }
        BigDecimal value = decimalValue();
        if (value.compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) < 0) {
            return Long.MIN_VALUE;
        }
        if (value.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
            return Long.MAX_VALUE;
        }
        return numberValue().longValue();
    }

    @Override
    public double doubleValue() {
        return numberValue().doubleValue();
    }

    @Override
    public String stringValue() {
        return toString();
    }
}
