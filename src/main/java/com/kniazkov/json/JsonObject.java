package com.kniazkov.json;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
                        if (fieldType == byte.class) {
                            field.setByte(result, (byte)child.getIntValue());
                        }
                        else if (fieldType == java.lang.Byte.class) {
                            Byte obj = (byte)child.getIntValue();
                            field.set(result, obj);
                        }
                        else if (fieldType == short.class) {
                            field.setShort(result, (short)child.getIntValue());
                        }
                        else if (fieldType == java.lang.Short.class) {
                            Short obj = (short)child.getIntValue();
                            field.set(result, obj);
                        }
                        else if (fieldType == int.class) {
                            field.setInt(result, child.getIntValue());
                        }
                        else if (fieldType == java.lang.Integer.class) {
                            Integer obj = child.getIntValue();
                            field.set(result, obj);
                        }
                        else if (fieldType == long.class) {
                            field.setLong(result, child.getLongValue());
                        }
                        else if (fieldType == java.lang.Long.class) {
                            Long obj = child.getLongValue();
                            field.set(result, obj);
                        }
                        else if (fieldType == float.class) {
                            field.setFloat(result, (float)child.getDoubleValue());
                        }
                        else if (fieldType == java.lang.Float.class) {
                            Float obj = (float)child.getDoubleValue();
                            field.set(result, obj);
                        }
                        else if (fieldType == double.class) {
                            field.setDouble(result, child.getDoubleValue());
                        }
                        else if (fieldType == java.lang.Double.class) {
                            Double obj = child.getDoubleValue();
                            field.set(result, obj);
                        }
                        else if (fieldType == boolean.class) {
                            field.setBoolean(result, child.getBooleanValue());
                        }
                        else if (fieldType == java.lang.Boolean.class) {
                            Boolean obj = child.getBooleanValue();
                            field.set(result, obj);
                        }
                        else if (fieldType == java.lang.String.class) {
                            field.set(result, child.getStringValue());
                        }
                        else if (fieldType == java.lang.Object.class) {
                            field.set(result, child.toObject());
                        }
                        else if (fieldType == java.util.List.class || fieldType == java.util.ArrayList.class) {
                            List<Object> list = new ArrayList<>();
                            createListFromJsonArray(field, list, child);
                            field.set(result, list);
                        }
                        else if (fieldType == java.util.LinkedList.class) {
                            List<Object> list = new LinkedList<>();
                            createListFromJsonArray(field, list, child);
                            field.set(result, list);
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

    /**
     * Creates Java list from JSON element that represents an array
     * @param field Field of Java object
     * @param list Resulting list
     * @param elem JSON element
     */
    private static void createListFromJsonArray(Field field, List<Object> list, JsonElement elem) {
        JsonArray array = elem.toJsonArray();
        if (array == null) {
            return;
        }
        Type listType = field.getGenericType();
        if (listType instanceof ParameterizedType) {
            Type[] parameters = ((ParameterizedType) listType).getActualTypeArguments();
            assert(parameters.length == 1);
            if (parameters[0] == java.lang.Byte.class) {
                for (JsonElement jsonElement : array) {
                    Byte obj = (byte)jsonElement.getIntValue();
                    list.add(obj);
                }
            }
            else if (parameters[0] == java.lang.Short.class) {
                for (JsonElement jsonElement : array) {
                    Short obj = (short)jsonElement.getIntValue();
                    list.add(obj);
                }
            }
            else if (parameters[0] == java.lang.Integer.class) {
                for (JsonElement jsonElement : array) {
                    Integer obj = jsonElement.getIntValue();
                    list.add(obj);
                }
            }
            else if (parameters[0] == java.lang.Long.class) {
                for (JsonElement jsonElement : array) {
                    Long obj = jsonElement.getLongValue();
                    list.add(obj);
                }
            }
            else if (parameters[0] == java.lang.Float.class) {
                for (JsonElement jsonElement : array) {
                    Float obj = (float)jsonElement.getDoubleValue();
                    list.add(obj);
                }
            }
            else if (parameters[0] == java.lang.Double.class) {
                for (JsonElement jsonElement : array) {
                    Double obj = jsonElement.getDoubleValue();
                    list.add(obj);
                }
            }
            else if (parameters[0] == java.lang.Boolean.class) {
                for (JsonElement jsonElement : array) {
                    Boolean obj = jsonElement.getBooleanValue();
                    list.add(obj);
                }
            }
            else if (parameters[0] == java.lang.String.class) {
                for (JsonElement jsonElement : array) {
                    String obj = jsonElement.getStringValue();
                    list.add(obj);
                }
            }
        }
    }
}
