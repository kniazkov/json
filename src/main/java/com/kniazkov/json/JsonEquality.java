/* Copyright (c) 2026 Ivan Kniazkov */
package com.kniazkov.json;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Value equality with standard List/Map contracts and depth-independent call-stack usage. */
final class JsonEquality {
    private JsonEquality() {}

    private record Pair(Object left, Object right) {}

    /** A lazy continuation, retaining siblings in their original containers. */
    private interface Pairs extends Iterator<Pair> {}

    static boolean equal(JsonElement left, Object right) {
        ArrayDeque<Pairs> stack = new ArrayDeque<>();
        Object a = left, b = right;
        while (true) {
            if (a != b) {
                if (a instanceof List<?> list) {
                    if (!(b instanceof List<?> other) || list.size() != other.size()) {
                        return false;
                    }
                    Iterator<?> first = list.iterator(), second = other.iterator();
                    stack.push(
                            new Pairs() {
                                public boolean hasNext() {
                                    return first.hasNext();
                                }

                                public Pair next() {
                                    return new Pair(first.next(), second.next());
                                }
                            });
                } else if (a instanceof Map<?, ?> map) {
                    if (!(b instanceof Map<?, ?> other) || map.size() != other.size()) {
                        return false;
                    }
                    Iterator<? extends Map.Entry<?, ?>> entries = map.entrySet().iterator();
                    stack.push(
                            new Pairs() {
                                public boolean hasNext() {
                                    return entries.hasNext();
                                }

                                public Pair next() {
                                    var entry = entries.next();
                                    Object value = other.get(entry.getKey());
                                    // JSON maps never contain Java null; retain Map's general null
                                    // distinction.
                                    if (value == null && !other.containsKey(entry.getKey())) {
                                        return new Pair(Boolean.TRUE, Boolean.FALSE);
                                    }
                                    return new Pair(entry.getValue(), value);
                                }
                            });
                } else if (a instanceof JsonElement element) {
                    if (!(b instanceof JsonElement other) || !scalarEqual(element, other)) {
                        return false;
                    }
                } else if (!Objects.equals(a, b)) {
                    return false;
                }
            }
            while (!stack.isEmpty() && !stack.peek().hasNext()) {
                stack.pop();
            }
            if (stack.isEmpty()) {
                return true;
            }
            Pair next;
            try {
                next = stack.peek().next();
            } catch (ClassCastException | NullPointerException incompatibleMap) {
                return false;
            }
            a = next.left;
            b = next.right;
        }
    }

    private static boolean scalarEqual(JsonElement a, JsonElement b) {
        if (a instanceof JsonNumber && b instanceof JsonNumber) {
            if (a.isNaN() || b.isNaN()) {
                return a.isNaN() && b.isNaN();
            }
            if (a.isInfinite() || b.isInfinite()) {
                return a.isPositiveInfinity() && b.isPositiveInfinity()
                        || a.isNegativeInfinity() && b.isNegativeInfinity();
            }
            return a.decimalValue().compareTo(b.decimalValue()) == 0;
        }
        if (a instanceof JsonString && b instanceof JsonString) {
            return Objects.equals(a.stringValue(), b.stringValue());
        }
        if (a instanceof JsonBoolean && b instanceof JsonBoolean) {
            return a.booleanValue() == b.booleanValue();
        }
        return a instanceof JsonNull && b instanceof JsonNull;
    }

    private static int scalarHash(JsonElement value) {
        if (value instanceof JsonNumber) {
            if (!value.isFinite()) {
                return Double.hashCode(value.doubleValue());
            }
            BigDecimal decimal = value.decimalValue();
            if (decimal.signum() == 0) {
                return 0;
            }
            // Normalize using a long scale: stripTrailingZeros can overflow BigDecimal's int scale.
            String coefficient = decimal.unscaledValue().toString();
            int end = coefficient.length();
            while (coefficient.charAt(end - 1) == '0') {
                end--;
            }
            long scale = (long) decimal.scale() - (coefficient.length() - end);
            int hash = 0;
            for (int i = 0; i < end; i++) {
                hash = 31 * hash + coefficient.charAt(i);
            }
            return 31 * hash + Long.hashCode(scale);
        }
        if (value instanceof JsonString) {
            return Objects.hashCode(value.stringValue());
        }
        if (value instanceof JsonBoolean) {
            return Boolean.hashCode(value.booleanValue());
        }
        return 0;
    }

    private static final class HashFrame {
        final Iterator<Map.Entry<String, JsonElement>> children;
        final boolean array;
        int hash;
        int keyHash;

        HashFrame(JsonContainer value) {
            children = TreeTraversal.children(value);
            array = value instanceof JsonArray;
            hash = array ? 1 : 0;
        }

        void add(int childHash) {
            hash = array ? 31 * hash + childHash : hash + (keyHash ^ childHash);
        }
    }

    static int hash(JsonElement value) {
        if (!(value instanceof JsonContainer root)) {
            return scalarHash(value);
        }
        ArrayDeque<HashFrame> stack = new ArrayDeque<>();
        stack.push(new HashFrame(root));
        while (true) {
            HashFrame frame = stack.peek();
            if (!frame.children.hasNext()) {
                int result = frame.hash;
                stack.pop();
                if (stack.isEmpty()) {
                    return result;
                }
                stack.peek().add(result);
            } else {
                var entry = frame.children.next();
                frame.keyHash = Objects.hashCode(entry.getKey());
                if (entry.getValue() instanceof JsonContainer nested) {
                    stack.push(new HashFrame(nested));
                } else {
                    frame.add(scalarHash(entry.getValue()));
                }
            }
        }
    }
}
