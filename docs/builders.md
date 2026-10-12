<!--
  Copyright (c) 2026 Ivan Kniazkov
-->
# Build a document in one expression

[Guide](README.md) · [The tree](tree.md) · [Numbers](numbers.md)

`Json.object()` and `Json.array()` create mutable builders. Chained calls describe
the document from top to bottom; callbacks delimit nested objects and arrays.
Call `build()` to obtain an independent, ordinary `JsonObject` or `JsonArray`.

## Objects and nested containers

```java
JsonObject document = Json.object()
        .put("name", "Observatory")
        .put("enabled", true)
        .object("location", location -> location
                .put("city", "Москва")
                .put("latitude", 55.75))
        .array("instruments", instruments -> instruments
                .object(camera -> camera
                        .put("name", "Camera")
                        .put("price", new BigDecimal("1250.75")))
                .object(telescope -> telescope
                        .put("name", "Telescope")
                        .put("price", new BigDecimal("4200"))))
        .putNull("comment")
        .build();
assert document.element("instruments")
        .orElseThrow()
        .arrayValueOrThrow()
        .size() == 2;
String text = Json.serialize(document, "  ");
assert Json.parse(text)
        .equals(document);
```

The result is an ordinary tree: it supports navigation, Java conversion, collection
operations and serialization. Builders are separate from `Map` and `List`, whose
`put` and `add` return values have different meanings.

## Arrays and bulk values

```java
JsonArray values = Json.array()
        .add(10)
        .add(20)
        .add("text")
        .addNull()
        .array(nested -> nested
                .add(true)
                .add(false))
        .build();
assert values.size() == 5;

List<String> names = List.of("Camera", "Telescope");
JsonArray instruments = Json.array()
        .addAll(names)
        .build();
assert instruments.element(0)
        .stringValueOrThrow()
        .equals("Camera");
```

`addAll(Iterable<?>)` consumes the input in iteration order. Each entry must be a
String, Boolean, supported Number or JsonElement. A null entry is rejected; use
`JsonNull.INSTANCE` in a bulk input. Nested Java collections, characters and POJOs
are not implicitly converted. An unsupported type throws `IllegalArgumentException`.
The entire input is staged before appending: iteration or conversion failure adds
no elements to the builder.

## Values and exact numbers

| Operation | Accepted values |
| --- | --- |
| `put(key, value)` / `add(value)` | String, boolean, Number, JsonElement |
| `putNull(key)` / `addNull()` | Explicit JSON null |
| `object(key, callback)` / `object(callback)` | Nested object builder |
| `array(key, callback)` / `array(callback)` | Nested array builder |
| `addAll(values)` | Iterable of supported scalar values and JSON nodes |
| `build()` | Independent copy of the current tree |

Primitive numeric arguments are boxed into their corresponding Java numeric types.
Byte, Short, Integer, Long and BigInteger create exact integers; Float, Double and
BigDecimal follow the same rules as `JsonNumber.of`. NaN and infinities create JSON5
special numbers. Unsupported Number subclasses are rejected.

```java
JsonObject invoice = Json.object()
        .put("id", new BigInteger("18446744073709551616"))
        .put("amount", new BigDecimal("19.99"))
        .build();
assert invoice.element("amount")
        .orElseThrow()
        .decimalValueOrThrow()
        .equals(new BigDecimal("19.99"));
```

All mutation methods return the same builder. Repeated object keys replace their
previous value without changing insertion order. Reference arguments, including
keys and callbacks, must be non-null. Null values require the explicit methods.
`new JsonObjectBuilder()` and `new JsonArrayBuilder()` are also available.

To include a POJO or nested Java collection, explicitly call
`JsonParser.parseJavaObject` before adding it. This keeps reflection and Java mapping
an explicit choice:

```java
class Settings {
    int retries = 3;
}
JsonElement settings = JsonParser.parseJavaObject(new Settings());
JsonObject document = Json.object()
        .put("settings", settings)
        .build();
assert document.element("settings")
        .orElseThrow()
        .objectValueOrThrow()
        .element("retries")
        .orElseThrow()
        .intValueOrThrow() == 3;
```

## Ownership and reusable builders

An existing JSON node is copied when it is added. `build()` makes a fresh deep copy
with an empty root parent and correct parent links throughout. Neither subsequent
builder changes nor mutations of a returned tree affect previous or future builds.

```java
JsonObjectBuilder builder = Json.object()
        .put("name", "Camera");
JsonObject first = builder.build();
builder.put("enabled", true);
JsonObject second = builder.build();
first.addString("name", "Edited copy");
assert !first.containsKey("enabled");
assert second.element("name")
        .orElseThrow()
        .stringValueOrThrow()
        .equals("Camera");
```

The child callback runs immediately. Only after it succeeds is its tree copied into
the parent; keeping the child builder and modifying it later cannot change the
parent. If the callback throws, its exception propagates and its target member or
array slot is not inserted or replaced. Explicit mutations of the outer builder
from inside a callback are not rolled back.

```java
JsonObjectBuilder builder = Json.object()
        .put("keep", 7);
try {
    builder.object("keep", child -> {
        child.put("partial", true);
        throw new IllegalStateException("Cannot finish this section");
    });
    throw new AssertionError("The callback must fail");
} catch (IllegalStateException expected) {
    assert builder.build()
            .element("keep")
            .orElseThrow()
            .intValueOrThrow() == 7;
}
```

Builders are not thread-safe. Copying costs time and memory proportional to the
copied subtree: adding a prebuilt node, completing a nested callback and calling
`build()` all copy data. Bulk insertion stages values as well. For large documents,
fill the builder in a loop and build once. Copying uses the library's iterative
traversal, but recursive Java callbacks still use the Java call stack. When copy
cost matters more than fluent syntax, create nested containers directly with
`JsonObject.createObject` / `createArray` and the corresponding array methods.
