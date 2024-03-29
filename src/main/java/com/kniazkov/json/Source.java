/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

/**
 * Object containing JSON document for parsing.
 */
final class Source {
    /**
     * JSON document for parsing.
     */
    private final String data;

    /**
     * Index indicating the current position.
     */
    private int index;

    /**
     * Index indicating the last position.
     */
    private final int lastIndex;

    /**
     * Specifies the location of the current character in the JSON document.
     */
    private final JsonLocation loc;

    /**
     * Flag that indicates that the lexer has encountered a newline character.
     */
    private boolean newLine;

    /**
     * Constructor.
     * @param data JSON document for parsing
     */
    Source(String data) {
        this.data = data;
        this.index = 0;
        this.lastIndex = data.length();
        this.loc = new JsonLocation(1, 1);
        this.newLine = false;
    }

    /**
     * Returns the character from the current position.
     * @return A symbol
     */
    char getChar() {
        if (index < lastIndex) {
            char ch = data.charAt(index);
            if (ch == '\n') {
                newLine = true;
            }
            return ch;
        }
        return 0;
    }

    /**
     * Moves the position forward by 1 and returns the character from the new position.
     * @return A symbol
     */
    char nextChar() {
        if (index < lastIndex) {
            index++;
            if (newLine) {
                loc.row++;
                loc.column = 1;
                newLine = false;
            } else {
                loc.column++;
            }
            if (index < lastIndex) {
                char ch = data.charAt(index);
                if (ch == '\n') {
                    newLine = true;
                }
                return ch;
            }
        }
        return 0;
    }

    /**
     * Returns the location of the current character in the JSON document.
     * @return Location instance
     */
    JsonLocation getLocation() {
        return new JsonLocation(loc);
    }
}
