/* Copyright (c) 2026 Ivan Kniazkov */
package com.kniazkov.json;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Iterative built-in tree operations, with one continuation per active container. */
final class TreeTraversal {
    private TreeTraversal() {}

    static Iterator<Map.Entry<String, JsonElement>> children(JsonContainer container) {
        if (container instanceof JsonObject object) {
            return object.entrySet().iterator();
        }
        Iterator<JsonElement> iterator = ((JsonArray) container).iterator();
        return new Iterator<>() {
            public boolean hasNext() {
                return iterator.hasNext();
            }

            public Map.Entry<String, JsonElement> next() {
                return new AbstractMap.SimpleImmutableEntry<>(null, iterator.next());
            }
        };
    }

    static void attach(JsonContainer parent, String key, JsonElement child) {
        if (parent instanceof JsonObject object) {
            object.addChild(key, child);
        } else {
            ((JsonArray) parent).addChild(child);
        }
    }

    private static JsonElement shallow(JsonElement value, JsonContainer parent) {
        if (value instanceof JsonArray) {
            return new JsonArray(parent);
        }
        if (value instanceof JsonObject) {
            return new JsonObject(parent);
        }
        return value.clone(parent);
    }

    private record CopyFrame(
            Iterator<Map.Entry<String, JsonElement>> children, JsonContainer target) {}

    static JsonElement copy(JsonContainer value, JsonContainer parent) {
        JsonContainer root = (JsonContainer) shallow(value, parent);
        ArrayDeque<CopyFrame> stack = new ArrayDeque<>();
        stack.push(new CopyFrame(children(value), root));
        while (!stack.isEmpty()) {
            CopyFrame frame = stack.peek();
            if (!frame.children.hasNext()) {
                stack.pop();
                continue;
            }
            var entry = frame.children.next();
            JsonElement child = shallow(entry.getValue(), frame.target);
            attach(frame.target, entry.getKey(), child);
            if (child instanceof JsonContainer nested) {
                stack.push(new CopyFrame(children((JsonContainer) entry.getValue()), nested));
            }
        }
        return root;
    }

    private static final class NaturalFrame {
        final Iterator<Map.Entry<String, JsonElement>> children;
        final List<Object> list;
        final Map<String, Object> map;
        String key;

        NaturalFrame(JsonContainer value) {
            children = children(value);
            list = value instanceof JsonArray ? new ArrayList<>() : null;
            map = list == null ? new TreeMap<>() : null;
        }

        void add(Object value) {
            if (list != null) {
                list.add(value);
            } else {
                map.put(key, value);
            }
        }

        Object result() {
            return list != null
                    ? Collections.unmodifiableList(list)
                    : Collections.unmodifiableMap(map);
        }
    }

    static Object natural(JsonContainer value) {
        ArrayDeque<NaturalFrame> stack = new ArrayDeque<>();
        stack.push(new NaturalFrame(value));
        while (true) {
            NaturalFrame frame = stack.peek();
            if (!frame.children.hasNext()) {
                Object result = frame.result();
                stack.pop();
                if (stack.isEmpty()) {
                    return result;
                }
                stack.peek().add(result);
            } else {
                var entry = frame.children.next();
                frame.key = entry.getKey();
                if (entry.getValue() instanceof JsonContainer nested) {
                    stack.push(new NaturalFrame(nested));
                } else {
                    frame.add(entry.getValue().toJavaObject());
                }
            }
        }
    }

    private static final class WriteFrame {
        final Iterator<Map.Entry<String, JsonElement>> children;
        final char close;
        final int level;
        final boolean multiline;
        boolean first = true;

        WriteFrame(JsonContainer value, String indentation, int level) {
            children = children(value);
            close = value instanceof JsonArray ? ']' : '}';
            this.level = level;
            JsonElement only = value.size() == 1 ? children(value).next().getValue() : null;
            multiline =
                    indentation != null
                            && value.size() > 0
                            && (value.size() != 1 || only instanceof JsonContainer);
        }
    }

    static void write(JsonContainer value, StringBuilder out, String indentation, int level) {
        write(value, out, indentation, level, false);
    }

    static void write(
            JsonContainer value,
            StringBuilder out,
            String indentation,
            int level,
            boolean escapeNonAscii) {
        ArrayDeque<WriteFrame> stack = new ArrayDeque<>();
        stack.push(new WriteFrame(value, indentation, level));
        out.append(value instanceof JsonArray ? '[' : '{');
        String newline = System.lineSeparator();
        while (!stack.isEmpty()) {
            WriteFrame frame = stack.peek();
            if (!frame.children.hasNext()) {
                if (frame.multiline) {
                    out.append(newline);
                    Utils.addRepeatingString(out, indentation, frame.level);
                }
                out.append(frame.close);
                stack.pop();
                continue;
            }
            if (!frame.first) {
                out.append(frame.multiline ? "," : ", ");
            }
            if (frame.multiline) {
                out.append(newline);
                Utils.addRepeatingString(out, indentation, frame.level + 1);
            }
            frame.first = false;
            var entry = frame.children.next();
            if (entry.getKey() != null) {
                out.append('"')
                        .append(Utils.escapeEntities(entry.getKey(), escapeNonAscii))
                        .append("\": ");
            }
            if (entry.getValue() instanceof JsonContainer nested) {
                out.append(nested instanceof JsonArray ? '[' : '{');
                stack.push(new WriteFrame(nested, indentation, frame.level + 1));
            } else {
                JsonElement leaf = entry.getValue();
                if (leaf instanceof JsonString) {
                    out.append('"')
                            .append(Utils.escapeEntities(leaf.stringValue(), escapeNonAscii))
                            .append('"');
                } else {
                    out.append(leaf.toString());
                }
            }
        }
    }
}
