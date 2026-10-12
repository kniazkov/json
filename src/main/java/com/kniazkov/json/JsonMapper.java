/* Copyright (c) 2026 Ivan Kniazkov */
package com.kniazkov.json;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/** Strict mapping with explicit continuations, including field-level error boundaries. */
final class JsonMapper {
    private JsonMapper() { }

    static Object convert(JsonElement value, Type type) {
        ArrayDeque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(value, type, DataPath.ROOT));
        while (true) {
            try {
                Frame frame = stack.peek();
                Frame child = frame.step();
                if (child != null) { stack.push(child); }
                else {
                    stack.pop();
                    if (stack.isEmpty()) { return frame.result; }
                    stack.peek().accept(frame.result);
                }
            } catch (JsonDataException failure) {
                stack.pop();
                boolean handled = false;
                while (!stack.isEmpty()) {
                    try {
                        if (stack.peek().recover()) { handled = true; break; }
                    } catch (JsonDataException assignmentFailure) { failure = assignmentFailure; }
                    stack.pop();
                }
                if (!handled) { throw failure; }
            }
        }
    }

    private static final class Frame {
        final JsonElement value;
        final Type type;
        final DataPath path;
        boolean initialized;
        Object result;
        JsonArray array;
        Type element;
        int index;
        Iterator<Map.Entry<String, Field>> fields;
        Field pending;
        DataPath pendingPath;
        JsonMappingAction fallback;
        Frame(JsonElement value, Type type, DataPath path) { this.value = value; this.type = type; this.path = path; }

        Frame step() {
            if (!initialized) { initialize(); initialized = true; }
            if (array != null) {
                if (index == array.size()) { return null; }
                int i = index++;
                return new Frame(array.get(i), element, path.index(i));
            }
            if (fields == null) { return null; }
            JsonObject object = (JsonObject) value;
            while (fields.hasNext()) {
                var entry = fields.next();
                Field field = entry.getValue();
                if (!Utils.isWritableField(field) || !object.containsKey(entry.getKey())) { continue; }
                JsonElement child = object.get(entry.getKey());
                JsonProperty annotation = field.getAnnotation(JsonProperty.class);
                JsonMappingAction onNull = annotation == null ? JsonMappingAction.DEFAULT : annotation.onNull();
                pendingPath = path.key(entry.getKey());
                if (child.isNull()) {
                    if (onNull == JsonMappingAction.KEEP_DEFAULT) { continue; }
                    if (onNull == JsonMappingAction.ERROR || onNull == JsonMappingAction.DEFAULT && field.getType().isPrimitive()) {
                        throw mismatch(child, field.getGenericType(), pendingPath);
                    }
                }
                pending = field;
                fallback = child.isNull() || annotation == null ? JsonMappingAction.ERROR : annotation.onInvalid();
                return new Frame(child, field.getGenericType(), pendingPath);
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        void accept(Object converted) {
            if (array != null) { ((List<Object>) result).add(converted); return; }
            Field field = pending;
            pending = null; // Assignment failures do not use this field's data fallback.
            try {
                field.setAccessible(true);
                field.set(result, converted);
            } catch (IllegalAccessException | InaccessibleObjectException | SecurityException failure) {
                throw new JsonDataException(pendingPath + ": cannot assign field", failure);
            }
        }

        boolean recover() {
            if (pending == null || fallback == JsonMappingAction.ERROR) { return false; }
            if (fallback == JsonMappingAction.SET_NULL) { accept(null); }
            else { pending = null; }
            return true;
        }

        void initialize() {
            if (type instanceof ParameterizedType parameterized) {
                initializeList((Class<?>) parameterized.getRawType()); return;
            }
            if (!(type instanceof Class<?> cls)) { throw unsupported(type, path); }
            if (List.class.isAssignableFrom(cls)) { initializeList(cls); return; }
            if (cls.isArray() || cls == void.class) { throw unsupported(type, path); }
            if (value.isNull()) {
                if (cls.isPrimitive()) { throw mismatch(value, type, path); }
                return;
            }
            if (cls == Object.class) { result = value.toJavaObject(); return; }
            if (cls == String.class) {
                if (!value.isString()) { throw mismatch(value, type, path); }
                result = value.stringValue(); return;
            }
            if (cls == char.class || cls == Character.class) {
                if (!value.isString() || value.stringValue().length() != 1) { throw mismatch(value, type, path); }
                result = value.stringValue().charAt(0); return;
            }
            if (cls == boolean.class || cls == Boolean.class) {
                if (!value.isBoolean()) { throw mismatch(value, type, path); }
                result = value.booleanValue(); return;
            }
            if (cls.isPrimitive() || Number.class.isAssignableFrom(cls)) { result = number(value, cls, path); return; }
            if (cls == Map.class) {
                if (!(value instanceof JsonObject)) { throw mismatch(value, type, path); }
                result = value.toJavaObject(); return;
            }
            if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers()) || cls.isEnum()
                    || Map.class.isAssignableFrom(cls) || Collection.class.isAssignableFrom(cls)) { throw unsupported(type, path); }
            if (!(value instanceof JsonObject)) { throw mismatch(value, type, path); }
            Map<String, Field> schema = Utils.mappedFields(cls);
            for (Field field : schema.values()) { validatePolicy(field); }
            try {
                Constructor<?> constructor = cls.getDeclaredConstructor();
                constructor.setAccessible(true);
                result = constructor.newInstance();
            } catch (InvocationTargetException failure) {
                throw new JsonDataException(path + ": constructor failed for " + cls.getTypeName(), failure.getCause());
            } catch (ReflectiveOperationException | InaccessibleObjectException | SecurityException failure) {
                throw new JsonDataException(path + ": cannot construct " + cls.getTypeName(), failure);
            }
            fields = schema.entrySet().iterator();
        }

        void initializeList(Class<?> raw) {
            validateList(type, path);
            if (value.isNull()) { return; }
            if (!(value instanceof JsonArray input)) { throw mismatch(value, type, path); }
            array = input;
            element = elementType(type);
            result = raw == LinkedList.class ? new LinkedList<>() : new ArrayList<>();
        }
    }

    private static void validateList(Type type, DataPath path) {
        while (true) {
            Type raw = type instanceof ParameterizedType p ? p.getRawType() : type;
            if (raw != List.class && raw != ArrayList.class && raw != LinkedList.class) { throw unsupported(type, path); }
            Type element = elementType(type);
            path = path.element();
            if (element instanceof ParameterizedType || element instanceof Class<?> c && List.class.isAssignableFrom(c)) {
                type = element;
            } else {
                if (!(element instanceof Class<?> c) || c.isArray() || c.getTypeParameters().length != 0 && c != Map.class) {
                    throw unsupported(element, path);
                }
                return;
            }
        }
    }

    private static Type elementType(Type type) {
        return type instanceof ParameterizedType p ? p.getActualTypeArguments()[0] : Object.class;
    }

    private static Object number(JsonElement value, Class<?> type, DataPath path) {
        if (!value.isNumber()) { throw mismatch(value, type, path); }
        if (type == Number.class) { return value.numberValue(); }
        if (type == float.class || type == Float.class) {
            float result = value.numberValue().floatValue();
            if (value.isFinite() && (!Float.isFinite(result)
                    || result == 0 && value.decimalValue().signum() != 0)) {
                throw mismatch(value, type, path);
            }
            return result;
        }
        if (type == double.class || type == Double.class) {
            double result = value.doubleValue();
            if (value.isFinite() && (!Double.isFinite(result)
                    || result == 0 && value.decimalValue().signum() != 0)) {
                throw mismatch(value, type, path);
            }
            return result;
        }
        if (!value.isFinite()) { throw mismatch(value, type, path); }
        BigDecimal decimal = value.decimalValue();
        try {
            if (type == BigDecimal.class) { return decimal; }
            if (type == BigInteger.class) { return decimal.toBigIntegerExact(); }
            if (type == byte.class || type == Byte.class) { return decimal.byteValueExact(); }
            if (type == short.class || type == Short.class) { return decimal.shortValueExact(); }
            if (type == int.class || type == Integer.class) { return decimal.intValueExact(); }
            if (type == long.class || type == Long.class) { return decimal.longValueExact(); }
        } catch (ArithmeticException cause) {
            throw new JsonDataException(path + ": value does not fit " + type.getTypeName(), cause);
        }
        throw unsupported(type, path);
    }

    private static void validatePolicy(Field field) {
        JsonProperty annotation = field.getAnnotation(JsonProperty.class);
        if (annotation == null) { return; }
        if (annotation.onInvalid() == JsonMappingAction.DEFAULT
                || field.getType().isPrimitive() && (annotation.onNull() == JsonMappingAction.SET_NULL
                    || annotation.onInvalid() == JsonMappingAction.SET_NULL)) {
            throw new IllegalArgumentException("Invalid JsonProperty mapping policy on "
                    + field.getDeclaringClass().getName() + "." + field.getName());
        }
    }

    private static JsonDataException mismatch(JsonElement value, Type type, DataPath path) {
        String actual = value.isNull() ? "null" : value instanceof JsonArray ? "array" : value instanceof JsonObject ? "object"
                : value.isString() ? "string" : value.isBoolean() ? "boolean" : "number";
        return new JsonDataException(path + ": expected " + type.getTypeName() + ", got " + actual);
    }

    private static JsonDataException unsupported(Type type, DataPath path) {
        return new JsonDataException(path + ": unsupported mapping type " + type.getTypeName());
    }
}
