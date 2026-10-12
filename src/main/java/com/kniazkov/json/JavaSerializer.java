/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Java graph to JSON tree, rejecting only back edges on the active identity path. */
final class JavaSerializer {
    private JavaSerializer() {}

    private record Child(String key, Object value, DataPath path) {}

    private record Frame(Object source, JsonContainer target, Iterator<Child> children) {}

    static JsonElement convert(Object source) {
        JsonElement root = node(source, null);
        if (!(root instanceof JsonContainer container)) {
            return root;
        }
        ArrayDeque<Frame> stack = new ArrayDeque<>();
        IdentityHashMap<Object, DataPath> active = new IdentityHashMap<>();
        active.put(source, DataPath.ROOT);
        stack.push(new Frame(source, container, children(source, DataPath.ROOT)));
        while (!stack.isEmpty()) {
            Frame frame = stack.peek();
            if (!frame.children.hasNext()) {
                active.remove(frame.source);
                stack.pop();
                continue;
            }
            Child entry = frame.children.next();
            DataPath ancestor = active.get(entry.value);
            if (ancestor != null) {
                throw new JsonDataException(entry.path + ": cycle refers to " + ancestor);
            }
            if (frame.target instanceof JsonObject object && object.containsKey(entry.key)) {
                throw new IllegalArgumentException("Duplicate JSON object key: " + entry.key);
            }
            JsonElement child = node(entry.value, frame.target);
            TreeTraversal.attach(frame.target, entry.key, child);
            if (child instanceof JsonContainer nested) {
                active.put(entry.value, entry.path);
                stack.push(new Frame(entry.value, nested, children(entry.value, entry.path)));
            }
        }
        return root;
    }

    private static JsonElement node(Object value, JsonContainer parent) {
        if (value == null) {
            return parent == null ? JsonNull.INSTANCE : new JsonNull(parent);
        }
        if (value instanceof Number number) {
            return JsonNumber.of(parent, number);
        }
        if (value instanceof Boolean bool) {
            return new JsonBoolean(parent, bool);
        }
        if (value instanceof String string) {
            return new JsonString(parent, string);
        }
        if (value instanceof Character character) {
            return new JsonString(parent, character.toString());
        }
        if (value.getClass().isArray() || value instanceof List<?>) {
            return new JsonArray(parent);
        }
        return new JsonObject(parent);
    }

    private static Iterator<Child> children(Object value, DataPath path) {
        if (value.getClass().isArray()) {
            return new Iterator<>() {
                int index;
                final int size = Array.getLength(value);

                public boolean hasNext() {
                    return index < size;
                }

                public Child next() {
                    int i = index++;
                    return new Child(null, Array.get(value, i), path.index(i));
                }
            };
        }
        if (value instanceof List<?> list) {
            Iterator<?> iterator = list.iterator();
            return new Iterator<>() {
                int index;

                public boolean hasNext() {
                    return iterator.hasNext();
                }

                public Child next() {
                    return new Child(null, iterator.next(), path.index(index++));
                }
            };
        }
        if (value instanceof Map<?, ?> map) {
            Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
            return new Iterator<>() {
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                public Child next() {
                    var entry = iterator.next();
                    if (!(entry.getKey() instanceof String key)) {
                        throw new IllegalArgumentException(
                                "JSON object keys must be non-null strings");
                    }
                    return new Child(key, entry.getValue(), path.key(key));
                }
            };
        }
        Iterator<Map.Entry<String, Field>> iterator =
                Utils.mappedFields(value.getClass()).entrySet().iterator();
        return new Iterator<>() {
            public boolean hasNext() {
                return iterator.hasNext();
            }

            public Child next() {
                var entry = iterator.next();
                DataPath childPath = path.key(entry.getKey());
                try {
                    entry.getValue().setAccessible(true);
                    return new Child(entry.getKey(), entry.getValue().get(value), childPath);
                } catch (IllegalAccessException
                        | InaccessibleObjectException
                        | SecurityException failure) {
                    throw new JsonDataException(childPath + ": cannot read field", failure);
                }
            }
        };
    }
}
