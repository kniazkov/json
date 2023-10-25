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
     * Constructor.
     * @param data JSON document for parsing
     */
    Source(String data) {
        this.data = data;
        this.index = 0;
        this.lastIndex = data.length();
    }

    /**
     * Returns the character from the current position.
     * @return A symbol
     */
    char getChar() {
        if (index < lastIndex) {
            return data.charAt(index);
        }
        return 0;
    }

    /**
     * Moves the position forward by 1 and returns the character from the new position.
     * @return A symbol
     */
    char nextChar() {
        if (index + 1 < lastIndex) {
            index++;
            return data.charAt(index);
        }
        return 0;
    }
}
