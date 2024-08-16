package com.kniazkov.json;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation added to a class field stating that this field can be mapped to a property
 * of some JSON object, with the name being the key of that property.
 * Allows binding properties with keys that are not allowed in Java, such as reserved
 * (“class”, “interface”), and so on.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonProperty {
    /**
     * The name (key) of the property to be bound.
     */
    String name();
}
