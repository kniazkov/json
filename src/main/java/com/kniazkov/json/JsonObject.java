/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * JSON object.
 */
public final class JsonObject extends JsonContainer implements Map<String, JsonElement> {
    /**
     * Collection of elements.
     */
    private final Map<String, JsonElement> elements;

    /**
     * Constructor.
     */
    public JsonObject() {
        this(null);
    }

    /**
     * Constructor (for internal use).
     * @param parent The parent of this element
     */
    JsonObject(JsonContainer parent) {
        super(parent);
        elements = new LinkedHashMap<>();
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return elements.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return elements.containsValue(value);
    }

    @Override
    public JsonElement get(Object key) {
        return elements.get(key);
    }

    @Override
    public JsonElement put(String key, JsonElement value) {
        Objects.requireNonNull(key, "key");
        JsonElement child = Objects.requireNonNull(value, "value").clone(this);
        return elements.put(key, child);
    }

    @Override
    public JsonElement remove(Object key) {
        return elements.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends JsonElement> map) {
        for (Map.Entry<? extends String, ? extends JsonElement> pair : map.entrySet()) {
            put(pair.getKey(), pair.getValue());
        }
    }

    @Override
    public void clear() {
        elements.clear();
    }

    @Override
    public Set<String> keySet() {
        return elements.keySet();
    }

    @Override
    public Collection<JsonElement> values() {
        return elements.values();
    }

    @Override
    public Set<Entry<String, JsonElement>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public int size() { return elements.size(); }

            @Override
            public void clear() { elements.clear(); }

            @Override
            public Iterator<Entry<String, JsonElement>> iterator() {
                Iterator<Entry<String, JsonElement>> iterator = elements.entrySet().iterator();
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() { return iterator.hasNext(); }

                    @Override
                    public Entry<String, JsonElement> next() {
                        Entry<String, JsonElement> entry = iterator.next();
                        // Never expose the backing entry: setValue must follow put's ownership rules.
                        return new Entry<>() {
                            @Override
                            public String getKey() { return entry.getKey(); }

                            @Override
                            public JsonElement getValue() { return entry.getValue(); }

                            @Override
                            public JsonElement setValue(JsonElement value) {
                                JsonElement child = Objects.requireNonNull(value, "value").clone(JsonObject.this);
                                return entry.setValue(child);
                            }

                            @Override
                            public boolean equals(Object other) {
                                return other instanceof Entry<?, ?> pair
                                        && Objects.equals(getKey(), pair.getKey())
                                        && Objects.equals(getValue(), pair.getValue());
                            }

                            @Override
                            public int hashCode() {
                                return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
                            }

                            @Override
                            public String toString() { return getKey() + "=" + getValue(); }
                        };
                    }

                    @Override
                    public void remove() { iterator.remove(); }
                };
            }
        };
    }

    @Override
    JsonElement clone(JsonContainer anotherParent) {
        JsonObject copy = new JsonObject(anotherParent);
        for (String key : elements.keySet()) {
            copy.addElement(key, elements.get(key));
        }
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append('{');
        boolean flag = false;
        for (String key : elements.keySet()) {
            if (flag) {
                result.append(", ");
            }
            flag = true;
            result.append('"').append(Utils.escapeEntities(key)).append("\": ")
                    .append(elements.get(key).toString());
        }
        result.append('}');
        return result.toString();
    }

    @Override
    protected void toText(StringBuilder builder, String indentation, int level) {
        final String separator = System.lineSeparator();
        final int size = elements.size();
        boolean converted = false;
        if (size == 0) {
            builder.append("{}");
            converted = true;
        } else if (size == 1) {
            final String key = elements.keySet().iterator().next();
            final JsonElement child = elements.get(key);
            if (child.toJsonArray() == null && child.toJsonObject() == null) {
                builder.append("{\"").append(Utils.escapeEntities(key)).append("\": ")
                        .append(child.toString()).append('}');
                converted = true;
            }
        }
        if (!converted) {
            builder.append('{').append(separator);
            boolean flag = false;
            for (String key : elements.keySet()) {
                if (flag) {
                    builder.append(',').append(separator);
                }
                flag = true;
                Utils.addRepeatingString(builder, indentation, level + 1);
                builder.append('"').append(Utils.escapeEntities(key)).append("\": ");
                elements.get(key).toText(builder, indentation, level + 1);
            }
            builder.append(separator);
            Utils.addRepeatingString(builder, indentation, level);
            builder.append('}');
        }
    }

    @Override
    public Object toJavaObject() {
        Map<String, Object> result = new TreeMap<>();
        for (String key : elements.keySet()) {
            result.put(key, elements.get(key).toJavaObject());
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public <T> T toJavaObject(Class<T> type) {
        if (type == java.util.Map.class) {
            return (T) toJavaObject();
        }
        if (!type.isPrimitive() && !type.isInterface()) {
            final Map<String, Field> fields = Utils.mappedFields(type);
            T result = null;
            try {
                Constructor<T> constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                result = constructor.newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ignored) {
                return null;
            }
            for (Map.Entry<String, Field> entry : fields.entrySet()) {
                Field field = entry.getValue();
                if (!Utils.isWritableField(field)) {
                    continue;
                }
                String propertyName = entry.getKey();
                if (elements.containsKey(propertyName)) {
                    Class<?> fieldType = field.getType();
                    JsonElement child = elements.get(propertyName);
                    try {
                        field.setAccessible(true);
                        if (fieldType == byte.class) {
                            field.setByte(result, (byte)child.intValue());
                        }
                        else if (fieldType == java.lang.Byte.class) {
                            Byte obj = child.isNull() ? null : (byte)child.intValue();
                            field.set(result, obj);
                        }
                        else if (fieldType == short.class) {
                            field.setShort(result, (short)child.intValue());
                        }
                        else if (fieldType == java.lang.Short.class) {
                            Short obj = child.isNull() ? null : (short)child.intValue();
                            field.set(result, obj);
                        }
                        else if (fieldType == int.class) {
                            field.setInt(result, child.intValue());
                        }
                        else if (fieldType == java.lang.Integer.class) {
                            Integer obj = child.isNull() ? null : child.intValue();
                            field.set(result, obj);
                        }
                        else if (fieldType == long.class) {
                            field.setLong(result, child.longValue());
                        }
                        else if (fieldType == java.lang.Long.class) {
                            Long obj = child.isNull() ? null : child.longValue();
                            field.set(result, obj);
                        }
                        else if (fieldType == float.class) {
                            field.setFloat(result, (float)child.doubleValue());
                        }
                        else if (fieldType == java.lang.Float.class) {
                            Float obj = child.isNull() ? null : (float)child.doubleValue();
                            field.set(result, obj);
                        }
                        else if (fieldType == double.class) {
                            field.setDouble(result, child.doubleValue());
                        }
                        else if (fieldType == java.lang.Double.class) {
                            Double obj = child.isNull() ? null : child.doubleValue();
                            field.set(result, obj);
                        }
                        else if (fieldType == boolean.class) {
                            field.setBoolean(result, child.booleanValue());
                        }
                        else if (fieldType == java.lang.Boolean.class) {
                            Boolean obj =  child.isNull() ? null : child.booleanValue();
                            field.set(result, obj);
                        }
                        else if (fieldType == java.lang.String.class) {
                            field.set(result, child.isNull() ? null : child.stringValue());
                        }
                        else if (fieldType == java.lang.Object.class) {
                            field.set(result, child.toJavaObject());
                        }
                        else if (fieldType == java.util.List.class || fieldType == java.util.ArrayList.class) {
                            List<Object> list = new ArrayList<>();
                            JsonArray.createListFromJsonArray(field.getGenericType(), list, child.toJsonArray());
                            field.set(result, list);
                        }
                        else if (fieldType == java.util.LinkedList.class) {
                            List<Object> list = new LinkedList<>();
                            JsonArray.createListFromJsonArray(field.getGenericType(), list, child.toJsonArray());
                            field.set(result, list);
                        }
                        else {
                            field.set(result, child.toJavaObject(fieldType));
                        }
                    } catch (IllegalAccessException ignored) {
                        return null;
                    }
                }
            }
            return result;
        }
        return null;
    }

    /**
     * Returns a child element by its key.
     * @param key Key
     * @return Child element
     */
    public JsonElement element(String key) {
        return get(key);
    }

    /**
     * Makes a copy of a JSON element and adds a key-value pair to the object.
     * @param key Key
     * @param elem Element
     */
    public void addElement(String key, JsonElement elem) {
        put(key, elem);
    }

    /**
     * Creates a child element of the 'null' type and adds a key-value pair to the object.
     * @param key Key
     */
    public void addNull(String key) {
        addChild(key, new JsonNull(this));
    }

    /**
     * Creates a child element of the 'boolean' type and adds a key-value pair to the object.
     * @param key Key
     @param value Boolean value
     */
    public void addBoolean(String key, boolean value) {
        addChild(key, new JsonBoolean(this, value));
    }

    /**
     * Creates a child element of the 'number' type and adds a key-value pair to the object.
     * @param key Key
     * @param value Value of the number
     */
    public void addNumber(String key, double value) {
        addChild(key, JsonNumber.of(this, value));
    }

    /**
     * Adds an exact number using its Java type.
     * @param key Property key
     * @param value Numeric value
     */
    public void addNumber(String key, float value) {
        addChild(key, JsonNumber.of(this, value));
    }

    /**
     * Adds an exact number using its Java type.
     * @param key Property key
     * @param value Numeric value
     */
    public void addNumber(String key, long value) {
        addChild(key, JsonNumber.of(this, value));
    }

    /**
     * Adds an exact number using its Java type.
     * @param key Property key
     * @param value Numeric value
     */
    public void addNumber(String key, Number value) {
        addChild(key, JsonNumber.of(this, value));
    }

    /**
     * Creates a child element of the 'string' type and adds a key-value pair to the object.
     * @param key Key
     * @param value Value of the string
     */
    public void addString(String key, String value) {
        addChild(key, new JsonString(this, value));
    }

    /**
     * Creates an empty array as a child of this object.
     * @param key Key
     * @return The empty array created, so that it can be filled.
     */
    public JsonArray createArray(String key) {
        JsonArray child = new JsonArray(this);
        addChild(key, child);
        return child;
    }

    /**
     * Creates an empty object as a child of this object.
     * @param key Key
     * @return The empty object created, so that it can be filled.
     */
    public JsonObject createObject(String key) {
        JsonObject child = new JsonObject(this);
        addChild(key, child);
        return child;
    }

    /**
     * Adds a child element (for internal use).
     * @param key Key
     * @param elem Element
     */
    void addChild(String key, JsonElement elem) {
        elements.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(elem, "elem"));
    }

    @Override
    public JsonObject toJsonObject() {
        return this;
    }

}
