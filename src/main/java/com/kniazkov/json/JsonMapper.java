/* Copyright (c) 2026 Ivan Kniazkov */
package com.kniazkov.json;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/** Shared strict conversion for roots, fields and list elements. */
final class JsonMapper {
    private JsonMapper() { }

    static Object convert(JsonElement value, Type type, String path) {
        if (type instanceof ParameterizedType parameterized) {
            validateList(type, path);
            return list(value, type, (Class<?>) parameterized.getRawType(), path);
        }
        if (!(type instanceof Class<?> cls)) { throw unsupported(type, path); }
        if (List.class.isAssignableFrom(cls)) {
            validateList(type, path);
            return list(value, type, cls, path);
        }
        if (cls.isArray() || cls == void.class) { throw unsupported(type, path); }
        if (value.isNull()) {
            if (cls.isPrimitive()) { throw mismatch(value, type, path); }
            return null;
        }
        if (cls == Object.class) { return value.toJavaObject(); }
        if (cls == String.class) {
            if (!value.isString()) { throw mismatch(value, type, path); }
            return value.stringValue();
        }
        if (cls == char.class || cls == Character.class) {
            if (!value.isString() || value.stringValue().length() != 1) { throw mismatch(value, type, path); }
            return value.stringValue().charAt(0);
        }
        if (cls == boolean.class || cls == Boolean.class) {
            if (!value.isBoolean()) { throw mismatch(value, type, path); }
            return value.booleanValue();
        }
        if (cls.isPrimitive() || Number.class.isAssignableFrom(cls)) {
            return number(value, cls, path);
        }
        if (cls == Map.class) {
            if (!(value instanceof JsonObject)) { throw mismatch(value, type, path); }
            return value.toJavaObject();
        }
        if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers()) || cls.isEnum()
                || Map.class.isAssignableFrom(cls) || Collection.class.isAssignableFrom(cls)) {
            throw unsupported(type, path);
        }
        if (!(value instanceof JsonObject object)) { throw mismatch(value, type, path); }
        return object(object, cls, path);
    }

    private static Object number(JsonElement value, Class<?> type, String path) {
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

    private static void validateList(Type type, String path) {
        Type raw = type instanceof ParameterizedType p ? p.getRawType() : type;
        if (raw != List.class && raw != ArrayList.class && raw != LinkedList.class) {
            throw unsupported(type, path);
        }
        Type element = elementType(type);
        if (element instanceof ParameterizedType || element instanceof Class<?> c && List.class.isAssignableFrom(c)) {
            validateList(element, path + "[]");
        } else if (!(element instanceof Class<?> c) || c.isArray()
                || c.getTypeParameters().length != 0 && c != Map.class) {
            throw unsupported(element, path + "[]");
        }
    }

    private static Type elementType(Type type) {
        return type instanceof ParameterizedType p ? p.getActualTypeArguments()[0] : Object.class;
    }

    private static Object list(JsonElement value, Type type, Class<?> raw, String path) {
        if (value.isNull()) { return null; }
        if (!(value instanceof JsonArray array)) { throw mismatch(value, type, path); }
        List<Object> result = raw == LinkedList.class ? new LinkedList<>() : new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            result.add(convert(array.get(i), elementType(type), path + "[" + i + "]"));
        }
        return result;
    }

    private static Object object(JsonObject value, Class<?> type, String path) {
        Map<String, Field> fields = Utils.mappedFields(type);
        for (Field field : fields.values()) { validatePolicy(field); }
        Object result;
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            result = constructor.newInstance();
        } catch (InvocationTargetException failure) {
            throw new JsonDataException(path + ": constructor failed for " + type.getTypeName(), failure.getCause());
        } catch (ReflectiveOperationException | InaccessibleObjectException | SecurityException failure) {
            throw new JsonDataException(path + ": cannot construct " + type.getTypeName(), failure);
        }
        for (Map.Entry<String, Field> entry : fields.entrySet()) {
            Field field = entry.getValue();
            if (!Utils.isWritableField(field) || !value.containsKey(entry.getKey())) { continue; }
            String childPath = path + "[" + new JsonString(entry.getKey()) + "]";
            JsonElement child = value.get(entry.getKey());
            JsonProperty annotation = field.getAnnotation(JsonProperty.class);
            JsonMappingAction onNull = annotation == null ? JsonMappingAction.DEFAULT : annotation.onNull();
            JsonMappingAction onInvalid = annotation == null ? JsonMappingAction.ERROR : annotation.onInvalid();
            Object converted;
            if (child.isNull()) {
                if (onNull == JsonMappingAction.KEEP_DEFAULT) { continue; }
                if (onNull == JsonMappingAction.ERROR || onNull == JsonMappingAction.DEFAULT && field.getType().isPrimitive()) {
                    throw mismatch(child, field.getGenericType(), childPath);
                }
                // Validate supported types even when the value is null.
                converted = convert(child, field.getGenericType(), childPath);
            } else {
                try {
                    converted = convert(child, field.getGenericType(), childPath);
                } catch (JsonDataException failure) {
                    if (onInvalid == JsonMappingAction.KEEP_DEFAULT) { continue; }
                    if (onInvalid == JsonMappingAction.SET_NULL) { converted = null; }
                    else { throw failure; }
                }
            }
            try {
                field.setAccessible(true);
                field.set(result, converted);
            } catch (IllegalAccessException | InaccessibleObjectException | SecurityException failure) {
                throw new JsonDataException(childPath + ": cannot assign field", failure);
            }
        }
        return result;
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

    private static JsonDataException mismatch(JsonElement value, Type type, String path) {
        String actual = value.isNull() ? "null" : value instanceof JsonArray ? "array" : value instanceof JsonObject ? "object"
                : value.isString() ? "string" : value.isBoolean() ? "boolean" : "number";
        return new JsonDataException(path + ": expected " + type.getTypeName() + ", got " + actual);
    }

    private static JsonDataException unsupported(Type type, String path) {
        return new JsonDataException(path + ": unsupported mapping type " + type.getTypeName());
    }
}
