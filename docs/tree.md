<!--
  Copyright (c) 2026 Ivan Kniazkov
-->
# Work with the tree

[Guide](README.md) · [Numbers](numbers.md) · [Public API](public-api.md)

Every parsed value is a `JsonElement`. An object is a `JsonObject`, an array is a
`JsonArray`, and leaf nodes represent strings, booleans, null and numbers.
The tree is useful when you want to explore a document before choosing a Java
schema, or edit only a few properties.

## Navigate deliberately

```java
JsonObject root = Json.parse("""
        {"station":{"name":"Hilltop"},"readings":[12,13],"note":null}
        """).objectValueOrThrow();
String name = root.element("station").objectValueOrThrow()
        .element("name").stringValueOrThrow();
int second = root.element("readings").arrayValueOrThrow().element(1).intValueOrThrow();
assert name.equals("Hilltop");
assert second == 13;
assert root.element("note").isNull();
assert root.element("missing") == null;
```

**Missing and JSON null are different.** `get(key)` and `element(key)` return Java
null for an absent member. An explicit JSON null is a `JsonNull` node.
Check for absence before calling an accessor; calling `isNull()` on a missing
member is an ordinary Java `NullPointerException`.

```java
JsonObject root = Json.parse("{}").objectValueOrThrow();
JsonElement title = root.element("title");
String displayTitle = title == null || title.isNull()
        ? "Untitled" : title.stringValueOrThrow();
assert displayTitle.equals("Untitled");
```

Array `get(index)` and `element(index)` use normal bounds checking.
`size()` is available on both containers.

## Soft access versus required values

| Soft method | On a different JSON kind |
| --- | --- |
| `booleanValue()` | `false` |
| `intValue()`, `longValue()`, `doubleValue()` | Zero |
| `stringValue()` | Text for numbers/booleans, `"null"` for JSON null, empty string for containers |
| `numberValue()`, `decimalValue()` | Java null; decimal is also null for non-finite numbers |
| `toJsonArray()`, `toJsonObject()` | Java null |

Soft numeric access may lose information; see [numeric conversion](numbers.md).
Use `isString()`, `isNumber()`, `isBoolean()`, `isNull()` or container casts to
branch on type, or require a value with one of these methods:

| Required method | Accepted value |
| --- | --- |
| `booleanValueOrThrow()` | Boolean |
| `stringValueOrThrow()` | String with a non-null payload |
| `intValueOrThrow()`, `longValueOrThrow()` | Exact integer in the target range |
| `doubleValueOrThrow()` | Number convertible to finite double, without nonzero-to-zero underflow |
| `numberValueOrThrow()` | Any number, including NaN/infinity |
| `decimalValueOrThrow()` | Finite number |
| `arrayValueOrThrow()`, `objectValueOrThrow()` | Corresponding container |

Required accessors throw `JsonDataException`. They do not parse numeric strings
or convert arbitrary values to booleans.

## Create values

```java
JsonObject root = new JsonObject();
root.addString("name", "M42");
root.addNumber("frames", 30L);
root.addBoolean("stacked", true);
root.addNull("comment");
JsonArray exposures = root.createArray("exposures");
exposures.addNumber(new BigDecimal("0.1"));
exposures.addNumber(new BigDecimal("0.2"));
JsonObject metadata = root.createObject("metadata");
metadata.addString("filter", "H-alpha");
assert metadata.parent() == root;
assert exposures.parent() == root;
```

`addNumber` has `long`, `float`, `double` and `Number` overloads. Scalars can
also be created with `new JsonString(text)`, `JsonNumber.of(value)`,
`JsonBoolean.of(value)`, `JsonBoolean.TRUE`, `JsonBoolean.FALSE` and
`JsonNull.INSTANCE`. Use a `JsonNull` node rather than a Java null reference
when inserting a JSON null into a container.

## Insertion copies; creation returns the owned child

`addElement`, `put`, `add`, `set` and entry replacement clone the supplied
element with its new parent. They do not attach the original object twice.

```java
JsonArray template = new JsonArray();
template.addNumber(1);
JsonObject root = new JsonObject();
root.addElement("copy", template);
template.addNumber(2);
JsonArray stored = root.element("copy").arrayValueOrThrow();
assert stored.size() == 1;
assert stored != template;
assert stored.parent() == root;
assert template.parent() == null;

JsonElement detachedCopy = root.clone();
assert detachedCopy.equals(root);
assert detachedCopy.parent() == null;
```

For a child you intend to fill immediately, `createArray`/`createObject` avoids
that extra copy. A node's parent reference is immutable. Removing a node from a
container does not rewrite the removed node's parent; clone it for a detached root.

## Java collection behavior

`JsonObject` implements `Map<String, JsonElement>` and preserves insertion order.
`keySet()`, `values()` and `entrySet()` are live views: removal, clearing and
iterator removal change the object. Views cannot add entries. Entry `setValue`
clones the replacement; it never exposes an unguarded insertion path.

```java
JsonObject root = Json.parse("{a:1,b:2}").objectValueOrThrow();
root.entrySet().iterator().next().setValue(JsonNumber.of(10));
root.keySet().remove("b");
assert root.element("a").intValueOrThrow() == 10;
assert root.size() == 1;
```

`JsonArray` implements `List<JsonElement>`. Mutate it through `add`, `set`,
`remove`, `clear`, bulk operations, `removeIf`, `replaceAll` or `sort`.
Its iterators, list iterators and sublists are read-only views; their mutation
methods throw `UnsupportedOperationException`. Replacement through the array
itself clones each new node.

```java
JsonArray values = Json.parse("[3,1,2]").arrayValueOrThrow();
values.sort(Comparator.comparingInt(JsonElement::intValueOrThrow));
values.removeIf(value -> value.intValueOrThrow() == 2);
assert values.toString().equals("[1, 3]");
```

## Equality is about data

Arrays compare in order; objects compare keys and values regardless of member order.
Finite numbers compare mathematically, ignoring integer/real kind and decimal scale:
`1`, `1.0` and `1e0` are equal and have equal hashes. NaN equals NaN within
this model; infinities compare by sign. Parent references do not participate.

```java
assert Json.parse("{a:1,b:[2]}").equals(Json.parse("{b:[2.0],a:1e0}"));
assert JsonNumber.of(1).hashCode() == JsonNumber.of(new BigDecimal("1.00")).hashCode();
assert !JsonNumber.of(1).equals(Integer.valueOf(1));
```

Container equality follows Map/List contracts, including comparison with ordinary
Java containers holding equal **JSON nodes**. JSON scalar wrappers are not equal
to Java strings, booleans or numeric wrappers. Mutable containers must not be changed
while used as hash keys or set members.

Built-in cloning, equality, hashing and writing use iterative traversal. Calls into
third-party collections or user code still follow those implementations' own limits.
