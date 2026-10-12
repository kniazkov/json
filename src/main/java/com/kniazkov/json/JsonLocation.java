/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.util.Optional;

/** Immutable position of a UTF-16 code unit, or the position immediately after input at EOF. */
public final class JsonLocation {
    private final int row;
    private final int column;
    private final String sourceName;

    JsonLocation(int row, int column) {
        this(row, column, null);
    }

    JsonLocation(int row, int column, String sourceName) {
        this.row = row;
        this.column = column;
        this.sourceName = sourceName;
    }

    /**
     * Returns the one-based line number. CRLF is one break; CR, LF, U+2028 and U+2029 are also
     * breaks. This does not imply that the parser accepts these characters.
     *
     * @return Line number
     */
    public int row() {
        return row;
    }

    /**
     * Returns the one-based column in UTF-16 code units, not display cells. A tab counts as one
     * unit and a surrogate pair as two.
     *
     * @return Column number
     */
    public int column() {
        return column;
    }

    /**
     * Returns the diagnostic source name, if supplied.
     *
     * @return Source name, or an empty optional for unnamed input
     */
    public Optional<String> sourceName() {
        return Optional.ofNullable(sourceName);
    }

    @Override
    public String toString() {
        return (sourceName == null ? "" : sourceName + ", ") + row + ":" + column;
    }
}
