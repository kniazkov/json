/* Copyright (c) 2026 Ivan Kniazkov */
package com.kniazkov.json;

/** Signed JSON5 infinity. Its textual representation is not strict JSON. */
public final class JsonInfinity extends JsonNumber {
    private final boolean negative;

    /**
     * Creates an infinity.
     *
     * @param negative True for negative infinity, false for positive infinity
     */
    public JsonInfinity(boolean negative) {
        this(null, negative);
    }

    JsonInfinity(JsonContainer parent, boolean negative) {
        super(parent);
        this.negative = negative;
    }

    @Override
    JsonElement clone(JsonContainer parent) {
        return new JsonInfinity(parent, negative);
    }

    @Override
    public Double numberValue() {
        return negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    }

    @Override
    public boolean isInfinite() {
        return true;
    }

    @Override
    public boolean isPositiveInfinity() {
        return !negative;
    }

    @Override
    public boolean isNegativeInfinity() {
        return negative;
    }

    @Override
    public String toString() {
        return negative ? "-Infinity" : "Infinity";
    }
}
