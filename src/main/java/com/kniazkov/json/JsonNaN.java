/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.util.Optional;

/** JSON5 not-a-number value. Its textual representation is not strict JSON. */
public final class JsonNaN extends JsonNumber {
    /** Creates a root NaN value. */
    public JsonNaN() {
        this(null);
    }

    JsonNaN(JsonContainer parent) {
        super(parent);
    }

    @Override
    JsonElement clone(JsonContainer parent) {
        return new JsonNaN(parent);
    }

    @Override
    public Optional<Number> numberValue() {
        return Optional.of(Double.NaN);
    }

    @Override
    public boolean isNaN() {
        return true;
    }

    @Override
    public String toString() {
        return "NaN";
    }
}
