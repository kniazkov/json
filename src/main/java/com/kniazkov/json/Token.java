package com.kniazkov.json;

/**
 * A token, i.e. minimum indivisible entity of JSON document.
 */
abstract class Token {
    /**
     * Location of the first character of the token.
     */
    private final Location loc;


    /**
     * Constructor.
     * @param loc Location of the first character of the token
     */
    Token(Location loc) {
        this.loc = loc;
    }

    /**
     * Represents the token as a string.
     * @return String representation of the token
     */
    @Override
    public abstract String toString();

    /**
     * Returns the location of the first character of the token.
     * @return Location
     */
    Location getLocation() {
        return loc;
    }
}
