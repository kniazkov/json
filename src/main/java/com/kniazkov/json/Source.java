/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Objects;

/** Sequential UTF-16 input with one current code unit and immutable location snapshots. */
final class Source {
    private final Reader reader;
    private final String sourceName;
    private int current;
    private int row = 1;
    private int column = 1;
    private boolean afterCarriageReturn;
    private JsonException failure;

    /** Creates an unnamed string source. */
    Source(String data) throws JsonException {
        this(new StringReader(data), null);
    }

    /** Creates a source without taking ownership of the reader; reads the first code unit. */
    Source(Reader reader, String sourceName) throws JsonException {
        Objects.requireNonNull(reader, "reader");
        this.reader = reader instanceof BufferedReader ? reader : new BufferedReader(reader);
        this.sourceName = sourceName;
        this.current = read();
    }

    /** Returns the current UTF-16 code unit (0..65535), or -1 at EOF, without advancing. */
    int charValue() throws JsonException {
        if (failure != null) {
            throw failure;
        }
        return current;
    }

    /** Advances once and returns the new code unit; EOF and its coordinates are stable. */
    int nextChar() throws JsonException {
        if (failure != null) {
            throw failure;
        }
        if (current == -1) {
            return -1;
        }
        if (current == '\r') {
            row++;
            column = 1;
        } else if (current == '\n') {
            if (!afterCarriageReturn) {
                row++;
                column = 1;
            }
        } else if (current == 0x2028 || current == 0x2029) {
            row++;
            column = 1;
        } else {
            column++;
        }
        afterCarriageReturn = current == '\r';
        current = read();
        return current;
    }

    /** Returns an immutable snapshot, including at EOF or a read failure. */
    JsonLocation location() {
        return new JsonLocation(row, column, sourceName);
    }

    private int read() throws JsonException {
        try {
            return reader.read();
        } catch (IOException cause) {
            failure = new JsonException(new JsonError.InputFailure(location()), cause);
            throw failure;
        }
    }
}
