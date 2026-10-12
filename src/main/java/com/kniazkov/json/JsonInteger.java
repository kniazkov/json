/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

/** Exact JSON integer. */
public final class JsonInteger extends JsonNumber {
    private final BigInteger value;

    /**
     * Creates a number.
     *
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
    public Optional<Number> numberValue() {
        return Optional.of(value);
    }

    @Override
    public Optional<BigDecimal> decimalValue() {
        return Optional.of(new BigDecimal(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
