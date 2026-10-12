/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.math.BigDecimal;
import java.util.Objects;

/** Exact JSON decimal number. */
public final class JsonReal extends JsonNumber {
    private final BigDecimal value;

    /**
     * Creates a number.
     *
     * @param value Exact value, not null
     */
    public JsonReal(BigDecimal value) {
        this(null, value);
    }

    JsonReal(JsonContainer parent, BigDecimal value) {
        super(parent);
        this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    JsonElement clone(JsonContainer parent) {
        return new JsonReal(parent, value);
    }

    @Override
    public BigDecimal numberValue() {
        return value;
    }

    @Override
    public BigDecimal decimalValue() {
        return value;
    }

    @Override
    public String toString() {
        String text = value.toString();
        // Retain the real kind when this text is parsed again.
        return text.indexOf('.') < 0 && text.indexOf('E') < 0 ? text + ".0" : text;
    }
}
