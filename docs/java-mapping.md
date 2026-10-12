<!--
  Copyright (c) 2026 Ivan Kniazkov
-->
# Let Java carry the shape

[Guide](README.md) · [Numbers](numbers.md) · [Diagnostics](diagnostics.md)

Use a tree when the shape is flexible. Use a Java type when the shape is part of
your application. The same mapper handles a root value, a field or a value nested
inside several containers, with a path to the failing element.

## Two directions

| Direction | API |
| --- | --- |
| Java value → JSON text | `Json.serialize(value)` or `JsonSerializer.serialize(value)` |
| Java value → tree | `JsonParser.parseJavaObject(value)` |
| Tree → natural Java values | `tree.toJavaObject()` |
| Tree → explicit class | `tree.toJavaObject(Target.class)` |
| Text → explicit class | `Json.parse(text, Target.class).orElseThrow()`, optionally with a mode |
| Tree → generic root type | `tree.toJavaObject(new JsonType<...>() {})` |

When serializing an existing tree, pass it directly to `Json.serialize` or
`JsonSerializer`. Use `parseJavaObject` for Java application data.

## A model without ceremony

```java
class Telescope {
    @JsonProperty(name = "exposure-ms")
    int exposure = 100;
    String target;
    List<String> filters;
    transient String sessionToken;
}
Telescope telescope = Json.parse("""
        {"exposure-ms":250,"target":"M31","filters":["red","green"]}
        """, Telescope.class, JsonParsingMode.STRICT).orElseThrow();
assert telescope.exposure == 250;
assert telescope.filters.equals(List.of("red", "green"));
JsonObject tree = JsonParser.parseJavaObject(telescope).objectValueOrThrow();
assert tree.containsKey("exposure-ms");
assert !tree.containsKey("sessionToken");
```

Unknown JSON members are ignored. Missing fields keep their constructor/initializer
values. A non-null POJO value requires a usable no-argument constructor; private
constructors and fields are accessed reflectively when the runtime permits it.
Constructor failures preserve their causes. Java module/access restrictions still
apply. Records, constructor-parameter binding and non-static inner-class construction
are not provided.

## Which fields participate?

| Field | Java → JSON | JSON → Java |
| --- | --- | --- |
| Ordinary instance field, including private/inherited | Read | Assign |
| `static`, `transient`, synthetic | Ignore | Ignore |
| `final` instance field | Read | Leave its constructor value |

`@JsonProperty` renames an eligible field; it does not override these exclusions.
An empty annotation name means the Java field name.

The entire superclass hierarchy is considered. A child field hides a parent field
with the same **Java name** before exclusions or renaming. An excluded child does
not reveal the hidden parent. Distinct eligible fields mapping to the same JSON
name cause `IllegalArgumentException` in either direction, even if one is final
or absent from the input. Validation precedes constructor invocation.

Parent fields are visited before new child fields, with a hiding child retaining
the parent's position. Java reflection does not guarantee declared-field order;
do not use POJO field order as a wire-format contract.

## Arrays, including primitive arrays

```java
int[][] rows = Json.parse("[[1,2],null,[],[3]]", int[][].class).orElseThrow();
assert Arrays.equals(rows[0], new int[] {1, 2});
assert rows[1] == null;
assert rows[2].length == 0;
assert Json.serialize(rows).equals("[[1, 2], null, [], [3]]");

Character[] letters = Json.parse("['A',null,'Б']", Character[].class).orElseThrow();
assert letters[1] == null;
byte[] bytes = Json.parse("[-128,0,127]", byte[].class).orElseThrow();
assert bytes[0] == -128;
```

All eight primitive array types and reference arrays are supported. Dimensions,
length and ordering are preserved; ragged and empty arrays work. JSON null maps
to a null array or reference element. Null in a primitive element fails.

`char`/`Character` requires a string containing exactly one UTF-16 code unit.
An emoji requiring a surrogate pair is not one Java char. `char[]` is an array
of one-unit strings, not a single string. `byte[]` is an array of signed numbers,
not implicit Base64.

```java
try {
    Json.parse("[1,null,2]", int[].class).orElseThrow();
    throw new AssertionError("A primitive slot cannot hold null");
} catch (JsonDataException expected) {
    assert expected.getMessage().contains("$[1]");
}
try {
    Json.parse("['😀']", char[].class).orElseThrow();
    throw new AssertionError("A surrogate pair is not one char");
} catch (JsonDataException expected) {
    assert expected.getMessage().contains("$[0]");
}
```

## Lists, maps and generic roots

Class alone cannot describe `Map<String, List<Integer>>`. Capture the type with a
direct anonymous subclass of `JsonType<T>`:

```java
Map<String, List<Integer>> groups = Json.parse("{north:[1,null,2],south:[]}")
        .toJavaObject(new JsonType<Map<String, List<Integer>>>() {});
assert groups.get("north").equals(Arrays.asList(1, null, 2));
groups.put("west", new ArrayList<>());

List<Map<String, int[]>> rows = Json.parse("[{samples:[3,4]}]")
        .toJavaObject(new JsonType<List<Map<String, int[]>>>() {});
assert rows.get(0).get("samples")[1] == 4;
```

| Target | Mutable implementation |
| --- | --- |
| `List<E>`, `ArrayList<E>` | ArrayList |
| `LinkedList<E>` | LinkedList |
| `Map<String,V>`, `LinkedHashMap<String,V>` | LinkedHashMap in JSON member order |
| `HashMap<String,V>` | HashMap |
| `TreeMap<String,V>`, `SortedMap<String,V>`, `NavigableMap<String,V>` | TreeMap in key order |

Values can be scalars, POJOs, arrays, lists or maps in any combination.
Generic arrays such as `List<Integer>[]` also preserve their element type.
Declared generic field types work without a root token:

```java
class Catalog {
    Map<String, List<int[]>> measurements;
    List<Integer>[] batches;
}
Catalog catalog = Json.parse("{measurements:{a:[[1,2]]},batches:[[3,null],[]]}",
        Catalog.class).orElseThrow();
assert catalog.measurements.get("a").get(0)[1] == 2;
assert catalog.batches[0].get(1) == null;
```

Raw List/Map class targets use natural Object values. Object mapping gives Java
null, Boolean, String, exact BigInteger/BigDecimal and immutable nested List/Map
values; natural objects use sorted keys. Explicit Map mapping creates a mutable
outer map. There is no implicit deep conversion to mutable collections for values
declared as Object.

Wildcard types, unresolved type variables (including inherited `T`), parameterized
POJOs, custom List/Map subclasses and non-String map key types are rejected.
Container declarations are checked even for null/empty input; absent fields are
not mapped. Generic type-variable substitution is not implemented.

```java
try {
    Json.parse("{}").toJavaObject(new JsonType<Map<Integer, String>>() {});
    throw new AssertionError("JSON keys must map to String");
} catch (JsonDataException expected) {
    assert expected.getMessage().contains("String");
}
```

## Strict scalar conversion

No mode enables coercion between JSON kinds.

| Java target | Required JSON data |
| --- | --- |
| String | String |
| boolean / Boolean | Boolean |
| char / Character | String of one UTF-16 code unit |
| byte/short/int/long and wrappers, BigInteger | Finite exact integer in range, where applicable |
| BigDecimal | Finite number |
| float/double and wrappers | Number; finite overflow or nonzero-to-zero underflow fails; explicit NaN/infinity is accepted |
| Number | Stored BigInteger, BigDecimal or special Double |

Floating-point rounding is permitted. JSON null maps to Java null for references;
it fails for primitives. Errors include a path such as `$["groups"]["north"][2]`.

## Field policies

A default is useful only when you have deliberately chosen it:

```java
class Preferences {
    @JsonProperty(onNull = JsonMappingAction.KEEP_DEFAULT,
            onInvalid = JsonMappingAction.KEEP_DEFAULT)
    int retries = 3;

    @JsonProperty(onInvalid = JsonMappingAction.SET_NULL)
    List<Integer> samples = new ArrayList<>();
}
Preferences preferences = Json.parse("{retries:'many',samples:[1,'bad',3]}",
        Preferences.class).orElseThrow();
assert preferences.retries == 3;
assert preferences.samples == null; // The whole field, not just the invalid element.
```

| Action | `onNull` | `onInvalid` |
| --- | --- | --- |
| DEFAULT | Null for references, error for primitives | Invalid configuration |
| ERROR | Fail | Fail (default) |
| KEEP_DEFAULT | Keep the constructor/initializer value | Keep the whole field's original value |
| SET_NULL | Assign null to a reference field | Assign null to a reference field |

SET_NULL is invalid for primitive fields. Invalid annotation configuration and
duplicate mapped names raise `IllegalArgumentException` and cannot be hidden by
a fallback. Policies are checked before constructing the target class.

`onInvalid` applies to invalid **non-null** data; it does not catch a root field
null rejected by `onNull`. A nested error belongs to the whole containing field
unless an inner field handles it first. Partial arrays/lists/maps are not installed.
Reflection assignment failures are not suppressed by that field's data fallback.
Constructor side effects are not transactional and cannot be rolled back.

## Serialize Java containers safely

Java arrays and Lists become arrays; any Map implementation is read through its
entries. Map keys must be non-null strings. Keys are never converted with
`toString()`; even distinct String identities with identical text are rejected
if an unusual Map exposes both. Entry iteration order determines output order.
Arbitrary Iterable/Set adapters are not provided.

```java
Map<String, Object> payload = new LinkedHashMap<>();
payload.put("id", new BigInteger("18446744073709551616"));
payload.put("samples", new int[] {3, 5, 8});
payload.put("note", null);
JsonObject tree = JsonParser.parseJavaObject(payload).objectValueOrThrow();
assert tree.element("id").orElseThrow().numberValueOrThrow().equals(payload.get("id"));
assert tree.element("note").orElseThrow().isNull();
```

Cycles are errors, but reusing an object in separate branches is allowed:

```java
List<Object> cycle = new ArrayList<>();
cycle.add(cycle);
try {
    Json.serialize(cycle);
    throw new AssertionError("JSON cannot represent an object cycle");
} catch (JsonDataException expected) {
    assert expected.getMessage().contains("$[0]");
}
List<Integer> shared = List.of(1, 2);
assert Json.serialize(List.of(shared, shared)).equals("[[1, 2], [1, 2]]");
```

Cycle detection uses object identity on the active ancestor path. Mapping and
serialization use explicit work stacks, so ordinary document depth does not
consume a Java call stack per level. Memory, constructors and user-defined
collection implementations retain their own limits.

## Absent root values

Typed `Json.parse(textOrFile, Target.class)` returns `Optional<T>`. JSON `null`
produces an empty optional for a supported reference target; a primitive target
still rejects it. A successful non-null conversion produces a present optional.
The same rules apply to the overloads accepting a syntax mode.

```java
assert Json.parse("null", String.class).isEmpty();
String title = Json.parse("null", String.class).orElse("Untitled");
assert title.equals("Untitled");
assert Json.parse("null").toJavaObject(String.class) == null;
```

`toJavaObject` deliberately returns ordinary Java values, including Java null.
Nulls inside Java arrays, lists, maps and POJO fields remain ordinary nulls;
only the typed parse result is wrapped in an optional.
