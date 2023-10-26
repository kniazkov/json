package com.kniazkov.json;

/**
 * Some auxiliary procedures.
 */
class Utils {
    /**
     * Converts a number to a string.
     * @param value A number
     * @return A string representing number
     */
    static String doubleToString(double value) {
        long intVal = (long)value;
        if (value == intVal) {
            return String.valueOf(intVal);
        }
        return String.valueOf(value);
    }
}
