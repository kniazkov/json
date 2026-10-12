/* Copyright (c) 2026 Ivan Kniazkov */
package com.kniazkov.json;

import java.util.ArrayDeque;

/** Persistent path segments: linear memory, formatted only for a diagnostic. */
final class DataPath {
    static final DataPath ROOT = new DataPath(null, "$");
    private final DataPath parent;
    private final String segment;
    private DataPath(DataPath parent, String segment) { this.parent = parent; this.segment = segment; }
    DataPath key(String key) { return new DataPath(this, "[" + new JsonString(key) + "]"); }
    DataPath index(int index) { return new DataPath(this, "[" + index + "]"); }
    DataPath element() { return new DataPath(this, "[]"); }
    @Override public String toString() {
        ArrayDeque<String> parts = new ArrayDeque<>();
        for (DataPath p = this; p != null; p = p.parent) { parts.push(p.segment); }
        StringBuilder result = new StringBuilder();
        while (!parts.isEmpty()) { result.append(parts.pop()); }
        return result.toString();
    }
}
