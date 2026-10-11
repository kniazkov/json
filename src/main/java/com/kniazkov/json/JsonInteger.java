/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/** Exact JSON integer. */
public final class JsonInteger extends JsonNumber {
    private final BigInteger value;

    /**
     * Creates a number.
     * @param value Exact value, not null
     */
    public JsonInteger(BigInteger value) {
        this(null, value);
    }

    JsonInteger(JsonContainer parent, BigInteger value) {
        super(parent);
        this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    JsonElement clone(JsonContainer parent) {
        return new JsonInteger(parent, value);
    }

    @Override
    Number numberValue() {
        return value;
    }

    @Override
    BigDecimal decimalValue() {
        return new BigDecimal(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
