/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Base class for exact JSON numbers. Use {@code of(...)} to create a number.
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
        return of(BigInteger.valueOf(value));
    }

    /**
     * Creates an arbitrary-precision integer.
     * @param value Integer value, not null
     * @return An integer element
     */
    public static JsonInteger of(BigInteger value) {
        return new JsonInteger(value);
    }

    /**
     * Creates a real number from the canonical decimal representation of a double.
     * @param value Finite value
     * @return A real element, even when the value is mathematically integral
     * @throws NumberFormatException If the value is NaN or infinite
     */
    public static JsonReal of(double value) {
        return of(BigDecimal.valueOf(value));
    }

    /**
     * Creates a real number without first widening a float to double.
     * @param value Finite value
     * @return A real element
     * @throws NumberFormatException If the value is NaN or infinite
     */
    public static JsonReal of(float value) {
        return of(new BigDecimal(Float.toString(value)));
    }

    /**
     * Creates an exact decimal number.
     * @param value Decimal value, not null
     * @return A real element
     */
    public static JsonReal of(BigDecimal value) {
        return new JsonReal(value);
    }

    /**
     * Creates a number according to its runtime Java type.
     * @param value Byte, Short, Integer, Long, Float, Double, BigInteger or BigDecimal
     * @return The corresponding integer or real element
     * @throws IllegalArgumentException If the Number implementation is unsupported
     */
    public static JsonNumber of(Number value) {
        Objects.requireNonNull(value, "value");
        if (value instanceof BigInteger integer) {
            return of(integer);
        }
        if (value instanceof BigDecimal decimal) {
            return of(decimal);
        }
        if (value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long) {
            return of(value.longValue());
        }
        if (value instanceof Float real) {
            return of(real.floatValue());
        }
        if (value instanceof Double real) {
            return of(real.doubleValue());
        }
        throw new IllegalArgumentException("Unsupported number type: " + value.getClass().getName());
    }

    abstract Number numberValue();

    abstract BigDecimal decimalValue();

    @Override
    public Object toJavaObject() {
        // Preserve the existing untyped conversion contract.
        return getDoubleValue();
    }

    @Override
    public <T> T toJavaObject(Class<T> type) {
        Object result;
        if (type == Byte.class) {
            result = (byte) getIntValue();
        }
        else if (type == Short.class) {
            result = (short) getIntValue();
        }
        else if (type == Integer.class) {
            result = getIntValue();
        }
        else if (type == Long.class) {
            result = getLongValue();
        }
        else if (type == Float.class) {
            result = numberValue().floatValue();
        }
        else if (type == Double.class) {
            result = getDoubleValue();
        }
        else if (type == BigDecimal.class) {
            result = decimalValue();
        }
        else if (type == BigInteger.class) {
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
        return value.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) >= 0
                && value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0
                && value.stripTrailingZeros().scale() <= 0;
    }

    @Override
    public boolean isLongInteger() {
        BigDecimal value = decimalValue();
        return value.compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) >= 0
                && value.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) <= 0
                && value.stripTrailingZeros().scale() <= 0;
    }

    @Override
    public final boolean isNumber() {
        return true;
    }

    @Override
    public int getIntValue() {
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
    public long getLongValue() {
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
    public double getDoubleValue() {
        return numberValue().doubleValue();
    }

    @Override
    public String getStringValue() {
        return toString();
    }
}
