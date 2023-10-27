package com.kniazkov.json;

/**
 * Specifies the location of a character in the JSON document.
 */
public final class Location {
    /**
     * Row.
     */
    int row;

    /**
     * Column.
     */
    int column;

    /**
     * Constructor.
     * @param row The row number.
     * @param column The column number.
     */
    Location(int row, int column) {
        this.row = row;
        this.column = column;
    }

    /**
     * Constructor.
     * @param loc Other instance.
     */
    Location(Location loc) {
        this.row = loc.row;
        this.column = loc.column;
    }

    /**
     * Returns the row number.
     * @return Row number
     */
    public int getRow() {
        return row;
    }

    /**
     * Returns the column number.
     * @return Column number
     */
    public int getColumn() {
        return column;
    }
}
