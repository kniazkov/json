package com.kniazkov.json;

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
     * Keys (in ordered list).
     */
    private final List<String> keys;

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
        elements = new TreeMap<>();
        keys = new ArrayList<>();
    }

    @Override
    public int size() {
        return keys.size();
    }

    @Override
    public boolean isEmpty() {
        return keys.isEmpty();
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
        if (!elements.containsKey(key)) {
            keys.add(key);
        }
        return elements.put(key, value.clone(this));
    }

    @Override
    public JsonElement remove(Object key) {
        JsonElement elem = elements.remove(key);
        if (elem != null) {
            keys.remove(key);
        }
        return elem;
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
        keys.clear();
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
        return elements.entrySet();
    }

    @Override
    JsonElement clone(JsonContainer anotherParent) {
        JsonObject copy = new JsonObject(anotherParent);
        for (String key : keys) {
            copy.addElement(key, elements.get(key));
        }
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append('{');
        boolean flag = false;
        for (String key : keys) {
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
    public Object toObject() {
        Map<String, Object> result = new TreeMap<>();
        for (String key : keys) {
            result.put(key, elements.get(key).toObject());
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public <T> T toObject(Class<T> type) {
        if (type == java.util.Map.class) {
            return (T) toObject();
        }
        if (!type.isPrimitive() && !type.isInterface()) {
            T result = null;
            try {
                result = type.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ignored) {
                return null;
            }
            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                String fieldName = field.getName();
                if (elements.containsKey(fieldName)) {
                    Class<?> fieldType = field.getType();
                    JsonElement child = elements.get(fieldName);
                    try {
                        field.setAccessible(true);
                        if (fieldType == int.class) {
                            field.setInt(result, child.getIntValue());
                        }
                        else if (fieldType == boolean.class) {
                            field.setBoolean(result, child.getBooleanValue());
                        }
                        else if (fieldType == java.lang.String.class) {
                            field.set(result, child.getStringValue());
                        }
                        else if (fieldType == java.lang.Object.class) {
                            field.set(result, child.toObject());
                        }
                        else {
                            field.set(result, child.toObject(fieldType));
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
    public JsonElement getElement(String key) {
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
        addChild(key, new JsonNumber(this, value));
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
        if (!elements.containsKey(key)) {
            keys.add(key);
        }
        elements.put(key, elem);
    }

    @Override
    public JsonObject toJsonObject() {
        return this;
    }
}
