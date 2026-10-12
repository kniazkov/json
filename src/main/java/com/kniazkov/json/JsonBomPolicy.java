/* Copyright (c) 2026 Ivan Kniazkov */
package com.kniazkov.json;

/** Policy for a byte-order mark at the beginning of each input document. */
public enum JsonBomPolicy {
    /** Accept one initial BOM if present. */
    ALLOW,
    /** Reject an initial BOM. */
    FORBID,
    /** Require exactly one initial BOM. */
    REQUIRE
}
