/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
/**
 * JSON trees, parsing, serialization and Java mapping.
 *
 * <p>Reference arguments must be non-null unless explicitly documented otherwise. Invalid null
 * arguments throw {@link java.lang.NullPointerException}. Java data supplied for serialization may
 * be null, representing JSON null. Parent references, source names, indentation and exception
 * causes may be null to denote a root node, default source name, compact output or absent cause.
 * Inherited Object, List and Map contracts are preserved.
 *
 * <p>Own accessors represent absent results with {@link java.util.Optional}. Java conversion
 * methods named {@code toJavaObject} may return null, and converted Java containers and fields may
 * contain null. Typed {@code Json.parse} methods wrap the root conversion in an Optional.
 */
package com.kniazkov.json;
