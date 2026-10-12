<!---
  Copyright (c) 2025 Ivan Kniazkov
-->
# User-friendly JSON parser/serializer

Version 2.0-SNAPSHOT (in development)

## Goal

> Write programs that do one thing and do it well. \
― Unix philosophy

The goal of this project is to create a library that allows you to parse and serialize JSON documents
while writing as little code as possible, supports extended JSON5 syntax, and also processes malformed documents.

## The Maven dependency

The current source requires **Java 21 or newer** to build and run. Building also requires
**Maven 3.6.3 or newer**. This requirement applies to the next release; the published
1.2 dependency shown below still targets Java 8.

To build the library, run all tests, and generate the source and Javadoc JARs without
requiring a release signing key:

```sh
mvn -B -Dgpg.skip=true clean verify
```

Release builds retain GPG signing when `gpg.skip` is not set.

### Code style and Javadoc

The Maven `validate` phase checks **all handwritten Java sources and tests**.
Violations fail the build; checks never rewrite source files. CI runs the same
rules on every pull request and push to master.

- **Spotless 2.43.0 + google-java-format 1.24.0 (AOSP)** enforce deterministic
  formatting, four-space indentation, import order, whitespace and final newlines.
- **Checkstyle 10.21.4**, through Maven Checkstyle Plugin 3.6.0, rejects wildcard,
  unused and redundant imports; invalid Java names; omitted braces; multiple
  declarations/statements per line; redundant modifiers; incorrect array syntax;
  accidental switch fall-through; and equals without hashCode. Lines are limited
  to 120 characters, except package/import declarations and lines containing URLs.
- Public types and public/protected API members require Javadoc. Parameters,
  type parameters, return values and explicitly thrown exceptions must be
  documented. Existing comments at every visibility level are checked for
  sentence endings, nonempty descriptions, HTML, paragraphs and tag order.
  Overrides may inherit documentation.
- The Javadoc tool runs with **`doclint=all`** and treats **warnings as errors**.
  Invalid references, malformed markup and missing API documentation fail CI.
- Test fixtures need not have API comments; test methods may use their JUnit
  annotation instead. This is the only path-based suppression. Existing test
  Javadoc still undergoes style checks. Third-party JSON5 fixtures and generated
  output are outside the formatter's source roots.

Versions are pinned in `pom.xml`; rules and the narrow test-documentation
exception are in `config/checkstyle.xml` and `config/checkstyle-suppressions.xml`.
There is no baseline of ignored violations.

```sh
# Read-only formatting and code/Javadoc style checks
mvn -B -ntp validate

# Also generate and fully validate API documentation
mvn -B -ntp validate javadoc:javadoc

# Explicit opt-in autoformatting (does not invent missing documentation)
mvn -B -ntp spotless:apply

# Complete build, style checks, documentation and tests
mvn -B -ntp -Dgpg.skip=true -DargLine=-Xss256k clean verify
```

CI also runs `python3 scripts/check-style-gate.py`: isolated project copies contain
deliberately broken formatting, a wildcard import in test code, a missing Javadoc
parameter tag and an unresolved Javadoc link. Each probe must fail with its
expected diagnostic, so an unrelated build failure cannot masquerade as success.
The script requires Python 3 and Maven on PATH (or the `MAVEN` environment variable).
These automated checks enforce consistency, not the factual correctness of prose
or the quality of API design.

To link the library to your Maven project, add the following dependency to `pom.xml`:
```xml
<dependencies>
    <dependency>
        <groupId>com.kniazkov</groupId>
        <artifactId>json</artifactId>
        <version>1.2</version>
    </dependency>
</dependencies>
```

## How to use

### Parsing to tree

The main class whose functionality will likely be enough for all your purposes is `Json` class,
so let's look at it first. 

A JSON document can be represented as a tree, suitable for data retrieval and modification.
A tree is also an intermediate structure when converting documents to and from specific
user-defined classes.

If you need to parse a JSON document from a **string** to a tree:

```java
import com.kniazkov.json.Json;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonException;

class MyClass {
    public void parseJson(String document) {
        try {
            JsonElement root = Json.parse(document);
            // do something with the resulting tree
        } catch (JsonException exception) {
            // do something if the parsing fails
        }
    }
}
```

If you need to parse a JSON document from a **file** to a tree:

```java
import com.kniazkov.json.Json;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonException;
import java.io.File;

class MyClass {
    public void parseJson(File file) {
        try {
            JsonElement root = Json.parse(file);
            // do something with the resulting tree
        } catch (JsonException exception) {
            // do something if the parsing fails
        }
    }
}
```

This uses a similar `parse` method, but accepts a file as an argument.
The other methods described below are implemented in pairs according to the same principle:
one parses a string and the other works with a file.

### Specifying the parsing mode

The parser can work in one of three modes:
- `JsonParsingMode.STRICT` - Only the syntax defined by the ECMA-404 standard
("The JSON Data Interchange Standard") is allowed. Parsing syntax constructs not defined by this standard 
will raise an exception.
- `JsonParsingMode.JSON5` - JSON5 syntax ([http://json5.org]()) is allowed.
This is an extension to the base format that aims to be easier to write and maintain files by hand.
- `JsonParsingMode.EXTENDED` - Extended mode. JSON5 syntax is allowed. Besides, if a JSON document contains
some errors, the parser will still try to parse such a document.

This parser is positioned as user-oriented, i.e. a person who edits JSON documents manually
and who makes mistakes from time to time.  Therefore, the `EXTENDED` mode is used by default unless
otherwise specified. 

So here we have a couple more methods that allow us to specify the parsing mode:

```
JsonElement Json.parse(String source, JsonParsingMode mode) throws JsonException
JsonElement Json.parse(File file, JsonParsingMode mode) throws JsonException
```

Here's a full example:

```java
import com.kniazkov.json.Json;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonException;
import com.kniazkov.json.JsonParsingMode;

class MyClass {
    public void parseJson(String document) {
        try {
            JsonElement root = Json.parse(document, JsonParsingMode.STRICT);
            // do something with the resulting tree
        } catch (JsonException exception) {
            // do something if the parsing fails
        }
    }
}
```

### Content before and after the root

`STRICT` and `JSON5` require one root value and reject extra content after it.
`STRICT` accepts only space, tab, CR and LF around the root. `JSON5` additionally
accepts its Unicode spaces and `//` and `/* ... */` comments; block comments must
be closed. JSON5 comments and whitespace are also accepted between tokens.

`EXTENDED` silently skips any amount of whitespace before and after the root:
Java `Character.isWhitespace` or `Character.isSpaceChar`, plus U+0085 (NEL) and
U+FEFF (BOM). This includes non-breaking spaces and Unicode line/paragraph separators.
NUL and U+200B (zero-width space) are not whitespace.

After that padding, **any remaining suffix is discarded**. Exactly one
`JsonError.TrailingContent` is appended to the supplied warnings list, at the first
non-whitespace character of the suffix. It may be another value, arbitrary text,
NUL, an incomplete string, or comment-looking text. The suffix is not tokenized
or retained in memory. Errors encountered while reading or decoding it still throw
`JsonException`; they cannot be recovered as syntax warnings.

```java
List<JsonError> warnings = new ArrayList<>();
JsonElement root = JsonParser.parseString("  [1, 2]  garbage", warnings);
// root is [1, 2]; one TrailingContent warning at 1:11.
```

Existing recovery warnings remain in the list; the tail warning is appended.
Overloads without a warnings list apply the same EXTENDED recovery but do not collect
warnings. Broad EXTENDED whitespace skipping applies at the root boundaries, not
inside strings or as a new rule for separators inside containers. Leading garbage
is not discarded. A document containing only padding has no root and throws
`JsonError.ExpectedRoot` wrapped in `JsonException`.

The parser must first recognize the root: for example `42e+` is an invalid number,
not the number `42` followed by a discarded suffix. Container recovery follows the
rules below. JSON5 syntax is described in the following section.

### STRICT strings

String values and object keys reject every unescaped control code unit U+0000–U+001F,
including literal tabs and line breaks. Use the standard JSON escapes instead:
`\"`, `\\`, `\/`, `\b`, `\f`, `\n`, `\r`, `\t`, or `\u` followed by exactly four
hexadecimal digits. Escaped control characters, including `\u0000`, remain valid.
`\'` is not a JSON escape; an ordinary apostrophe needs no escape inside double quotes.
Raw U+007F, U+0085, U+2028 and U+2029 are not part of the forbidden control range.

These restrictions are specific to STRICT. JSON5 string rules are described below;
EXTENDED retains its additional recovery behavior.

### JSON5 syntax

`JSON5` follows the [JSON5 1.0 syntax](https://spec.json5.org/):

- Both comment forms, `//` through a line terminator/EOF and closed `/* ... */`, are
  accepted wherever token-separating whitespace is allowed. Comments do not nest.
- Whitespace includes TAB, VT, FF, space, Unicode Space_Separator characters, BOM,
  LF, CR, U+2028 and U+2029. NEL is an EXTENDED addition, not JSON5 whitespace.
- Unquoted keys are IdentifierNames, including `$`, `_`, Unicode letters/letter numbers,
  combining marks/digits/connector punctuation in subsequent positions, and ZWNJ/ZWJ
  after the first character. `\uXXXX` escapes must decode to a valid character in that
  position. Unicode categories follow the Java runtime's Unicode version. Reserved
  words such as `true`, `null`, `NaN` and `Infinity` may also be keys. Escaped keyword
  spellings are allowed as keys, not as literal values. Numeric/signed keys require quotes.
- Strings may use either quote character. In addition to JSON escapes they accept
  `\xNN`, `\v`, `\0` (only when not followed by a decimal digit), and escaped non-digit
  characters such as `\q`, which yields `q`. `\1` through `\9` and incomplete/invalid
  hexadecimal escapes are rejected.
- Backslash followed immediately by LF, CR, CRLF, U+2028 or U+2029 continues a string
  without inserting a newline. Raw LF/CR inside a JSON5 string are errors. Other raw
  controls are permitted by JSON5. Raw U+2028/U+2029 are accepted with an
  `UnescapedLineSeparator` warning when a warnings list is supplied; serialization escapes them.
- Numbers accept leading `+`, leading/trailing decimal points, `0x`/`0X` hexadecimal
  integers, and signed or unsigned `NaN`/`Infinity`. Leading-zero decimal integers and
  octal/binary literals are rejected. Finite values retain the documented BigInteger /
  BigDecimal representation and implementation limits.

To collect JSON5 diagnostics, use
`JsonParser.parseString(text, JsonParsingMode.JSON5, warnings)`. STRICT continues to
reject JSON5 extensions. EXTENDED supports JSON5 token syntax and additionally retains
raw line breaks in strings, leading-zero numbers, recovery rules above, and its historical
backslash/spaces/newline continuation. It rejects raw NUL in strings (escaped NUL is valid).
Its arbitrary suffix is still discarded with a warning, including comment-looking suffixes.

The offline tests include all 114 parser cases from `json5/json5-tests`, with pinned
provenance and the upstream MIT license in `src/test/resources/json5-suite`.

### End-of-input diagnostics

The internal lexer always returns a token: EOF is represented by `TokenEnd`, never
Java null. Its immutable location identifies the position after the input (including
skipped padding/comments) and retains the source name. Repeated EOF reads keep the
same coordinates; its diagnostic text is `<EOF>`. At the character level, `Source`
still uses -1 for EOF and 0 for an actual NUL.

The parser uses the EOF token's location when reporting an absent root, missing colon
or value, or incomplete container. Unexpected EOF raises `JsonException`, or triggers
only the explicitly allowed EXTENDED recovery below. It is never handled by catching
`NullPointerException`. Unfinished lexical tokens retain their specific syntax errors.

### Container recovery in EXTENDED

Recovery is determined by the parser state, not by guessing intended values. STRICT
and JSON5 reject the malformed containers below. A single trailing comma before a
real closing bracket is allowed in JSON5 and EXTENDED, without a warning.

| Input in EXTENDED | Result | Warnings |
| --- | --- | --- |
| `[1 2]` | `[1,2]` | MissingComma |
| `{"a":1 "b":2}` | `{"a":1,"b":2}` | MissingComma |
| `[` or `{` | `[]` or `{}` | MissingClosingBracket |
| `[1` or `[1,` | `[1]` | MissingClosingBracket |
| `{"a":1` or `{"a":1,` | `{"a":1}` | MissingClosingBracket |
| `{"a":[1]` | `{"a":[1]}` | MissingClosingBracket |
| `{"a":[1` | Error: a nested container is still open at EOF | — |
| `[1,,2]` | `[1,2]` | One MissingArrayElement |
| `[1,,,2]` | `[1,2]` | Two MissingArrayElement warnings |
| `[,1]` | `[1]` | One MissingArrayElement |
| `[1,]` | `[1]` | None |
| `[1,,]` | `[1]` | One MissingArrayElement |
| `[,]` or `[,,]` | `[]` | One or two MissingArrayElement warnings |
| `[{"a":1]` | `[{"a":1}]` | MissingClosingBracket for the object |
| `{"a":[1}` | `{"a":[1]}` | MissingClosingBracket for the array |
| `{"a":1]` or `[1}` | Error: no enclosing container owns that bracket | — |
| `{"a":1}]` | `{"a":1}` | TrailingContent, per the root-tail policy |

The exact rules are:

1. A missing comma is inserted only after a complete value, before a token that can
   start the next array element or object key. The following object pair must still
   contain a colon and a complete value. No fields are silently discarded.
2. At EOF, only **one closing bracket for the root container** may be inserted. All
   nested containers must already have closed. A root may close before its first
   element, after a complete element, or after a trailing comma.
3. An absent array element is skipped, never replaced with null. Each comma encountered
   while expecting an element produces MissingArrayElement. A separator after an actual
   element is not an empty slot, including an ordinary trailing comma.
4. A foreign closing bracket may cause a container to close only if that container is
   in a closable state and an ancestor owns the bracket. The bracket is retained for
   the ancestor, not swallowed or replaced. This can unwind several containers, with
   a MissingClosingBracket warning for each. The EOF restriction in rule 2 still applies.
5. A started key:value pair must be completed. Missing colons or values, unfinished
   strings/numbers, repeated commas in objects, and other internal garbage are errors.
   For example `[{"a":]` cannot be recovered: the object still needs a value.
6. Warning locations identify the next element/key for a missing comma, the comma
   representing an empty slot, or EOF/the ancestor's bracket for an inserted closer.
   Warnings are appended in recovery order. Without a supplied list, the same recovery
   occurs without collecting warnings. Input/decoding failures remain exceptions.

### Deep documents and cyclic Java graphs

There is no fixed nesting-depth limit. Built-in JSON parsing, compact and formatted
writing, cloning (including container insertion), equality/hashing, Java-object serialization, and both
natural and typed Java conversion use iterative traversal with heap-backed frames.
Call-stack usage does not grow with document depth. All syntax-mode recovery rules,
parent ownership, field policies and collection ordering remain in effect.

Java → JSON serialization detects cycles by **object identity on the active ancestor
path** and raises `JsonDataException`, naming both the offending path and its ancestor:

```text
$[0]["back"]: cycle refers to $
```

The same object may occur in separate completed branches: each occurrence produces
its own JSON subtree. Equality/hashCode methods are not used for cycle detection.
Nulls and scalar values remain ordinary leaves. Reflective field-read failures now
raise a path-bearing JsonDataException with the cause instead of returning a partial
object. No cycle placeholders or reference IDs are emitted.

Traversal state and diagnostic paths use linear space in the active depth. Paths are
stored as linked segments and rendered only when reporting errors. Tree output uses
one StringBuilder rather than building and copying a string for every subtree.
This is not an unlimited-resource guarantee: heap availability, JVM string/collection
sizes, numeric representations and output size still apply. Pretty printing a deep
chain with nonempty indentation inherently produces quadratic-size output; expanding
a heavily shared acyclic Java graph can also produce much more data than its input.
The library does not catch OutOfMemoryError or roll back user constructor side effects.
User-supplied constructors, iterators and custom methods remain outside this stack-safety
contract, as do operations performed by callers on returned Java collections.

Regression tests exercise 20,000 levels, mixed containers, deep POJO/list mapping,
formatting, clones, recovery, error-policy unwinding, cycles and shared references.
The depth suite also runs with `-Xss256k` to avoid relying on a large JVM call stack.

### Serializing Java arrays and maps

`Json.serialize` and `JsonParser.parseJavaObject` support these types at the root,
in object fields and recursively inside arrays, lists and maps:

| Java value | JSON representation |
| --- | --- |
| Primitive numeric array | Array of numbers; `byte[]` uses signed numbers, not Base64 |
| `boolean[]` | Array of booleans |
| `char[]` / `Character[]` | Array of one-character strings (one UTF-16 code unit per character) |
| `Character` or a `char` field | One-character string |
| Object array, including user-defined objects | Array of recursively serialized values |
| Multidimensional or ragged array | Nested arrays |
| Empty array / empty map | `[]` / `{}` |
| Null reference or null element/value | `null` |
| `Map` implementation | Object, in the map's entry iteration order |

Map keys must be non-null strings. Null and non-string keys cause
`IllegalArgumentException`; keys are never converted with `toString()`. Maps using
identity or unusual equality rules may contain distinct string keys with identical
text; these are also rejected rather than silently overwriting a JSON property.
Keys and character values use normal JSON string escaping. Arrays and maps are
accessed through their public APIs, without reflective access to JDK internals.
Numeric values retain the existing exact-number and JSON5 non-finite-number policy.

Arbitrary `Iterable`/`Set` types are not supported.

### Mapping JSON to Java lists

List fields support `List<E>`, `ArrayList<E>` and `LinkedList<E>`, recursively nested
in any combination. `List` and `ArrayList` produce mutable ArrayLists; `LinkedList`
produces a mutable LinkedList. The element order and number of elements are preserved.
JSON null becomes Java null for both the field itself and individual elements,
including wrapper numbers, strings, booleans, objects and nested lists. An empty
array remains an empty list; an absent field retains its constructor-provided value.

Raw lists and `List<Object>` use natural Map/List values and exact BigInteger/BigDecimal
numbers. Elements follow the strict scalar conversion rules below. A non-array value where a list is required, or
a non-null element that cannot be mapped, raises `JsonDataException` with the field
and list index rather than becoming an empty list or a substituted null.

### Mapping JSON to Java arrays and maps

Arrays work at the root and in fields: all eight primitive array types, reference
arrays, multidimensional and ragged arrays. Elements use strict conversions; null
is allowed for reference elements and subarrays, but fails for primitive elements.
An empty JSON array produces a zero-length array; JSON null produces a null array.
`char[]` expects an array of single UTF-16-unit strings, and `byte[]` expects signed
numbers (not Base64).

Typed maps require String keys. Supported targets are:

| Target | Mutable result |
| --- | --- |
| `Map<String,V>` | `LinkedHashMap`, preserving JSON member order |
| `HashMap<String,V>` | `HashMap` |
| `LinkedHashMap<String,V>` | `LinkedHashMap` |
| `TreeMap<String,V>`, `SortedMap<String,V>`, `NavigableMap<String,V>` | `TreeMap`, sorted by key |

Map values can be scalars, POJOs, arrays, lists or maps in any combination.
Null values and null containers are preserved. Raw maps use String keys and natural
Object values. Explicit `Map.class` conversion now returns a mutable LinkedHashMap;
no-argument `toJavaObject()` and `Object.class` retain their natural immutable
Map/List conversion with exact BigInteger/BigDecimal numbers.

For generic root types, capture the full target type with a direct anonymous
subclass of `JsonType<T>`:

```java
int[][] matrix = Json.parse("[[1,2],null,[]]").toJavaObject(int[][].class);
Map<String, List<Integer>> groups = Json.parse("{a:[1,null,2]}")
    .toJavaObject(new JsonType<Map<String, List<Integer>>>() {});
List<Map<String, int[]>> rows = Json.parse("[{a:[1,2]}]")
    .toJavaObject(new JsonType<List<Map<String, int[]>>>() {});
```

The same container types work in POJO fields without a type token. Generic arrays
such as `List<Integer>[]` preserve the full element type during conversion.
Class-only list/map conversions remain raw because Class carries no type arguments.

Wildcard types (including `?`), unresolved type variables (including inherited
`T`), non-String map key types, custom List/Map subclasses and parameterized POJOs
are rejected with `JsonDataException`. Container type declarations are checked
even for empty or null input; absent fields are not validated. Generic
type-variable substitution is not implemented. Wrong shapes, out-of-range numbers
and invalid elements report a complete path, for example `$["groups"]["a"][1]`.
Field `onNull` and `onInvalid` policies apply to arrays and maps too: a failed
element triggers the policy for the whole field, never a partially filled result.
Conversion uses an explicit work stack, without a fixed nesting-depth limit.

### Strict typed conversion and field policies

`toJavaObject(Class)`, `toJavaObject(JsonType)` and typed `Json.parse` use the same
strict mapper at the root, in object fields and in container elements, independently
of the syntax parsing mode.
Incompatible data raises `JsonDataException`, including the path such as
`$["orders"][2]["quantity"]`. Reflection/construction failures preserve the underlying
cause. Constructors still run normally; their side effects cannot be rolled back.
A partially mapped result is not returned to the caller.

- Absent fields retain constructor-provided values; unknown JSON properties are ignored.
- Explicit null becomes Java null for reference types and is an error for primitives.
- Strings and booleans require matching JSON types; there is no automatic coercion.
- Character/char requires a string of exactly one UTF-16 code unit.
- byte/short/int/long and BigInteger require a mathematically integral value in range:
  `1.0` is valid, `1.5` is not. BigDecimal accepts finite numbers.
- float/double allow ordinary rounding (including `0.1`), but reject a finite value
  becoming infinity or a nonzero value becoming zero. Explicit JSON5 NaN and signed
  infinities are supported for float/double, but not integer types or BigDecimal.
- Convenience getters such as `intValue()` and the existing `*OrThrow()` getters are
  unchanged. In particular, `doubleValueOrThrow()` still requires a finite result.

`@JsonProperty` can relax handling for a specific field. `name` is optional; an empty
name uses the Java field name. A nonempty name still renames the JSON property in both
directions. The annotation does not make excluded fields writable.

```java
@JsonProperty(onNull = JsonMappingAction.KEEP_DEFAULT,
              onInvalid = JsonMappingAction.KEEP_DEFAULT)
private int count = 42;
```

| Action | Meaning |
| --- | --- |
| `DEFAULT` | Only for `onNull`: assign null to references, reject null for primitives |
| `ERROR` | Throw JsonDataException |
| `KEEP_DEFAULT` | Keep the entire field's constructor-provided value |
| `SET_NULL` | Assign null; valid only for reference fields |

Defaults are `onNull = DEFAULT`, `onInvalid = ERROR`. Null and invalid non-null data
are separate cases: `onInvalid` never catches an explicit null rejected by `onNull`.
A failure within a nested object or list belongs to the entire containing field:
`KEEP_DEFAULT` does not skip elements or keep a partially converted list. A nested
field's own policy may handle its error before it reaches the containing field.
Assignment/access failures are not suppressed by a data fallback.

`onInvalid = DEFAULT` and either `SET_NULL` action on a primitive field are invalid
configuration. These policies are checked for the mapped class before its constructor
runs, including absent fields. Configuration errors and duplicate JSON names raise
`IllegalArgumentException` and cannot be hidden by `onInvalid`. No `coerce` option is
introduced.

### Automatic Java field mapping

Automatic mapping applies the same eligibility policy before accessing a field or
processing `@JsonProperty`:

| Field | Java → JSON | JSON → Java |
| --- | --- | --- |
| Ordinary instance field, including private fields | Read | Write |
| static | Ignore | Ignore |
| transient | Ignore | Ignore |
| Compiler-generated synthetic field | Ignore | Ignore |
| final instance field | Read | Ignore; preserve the constructor's value |

`@JsonProperty` only renames an eligible field; it never overrides these exclusions.
JSON properties matching excluded fields are ignored. The library does not read or
write them through reflection. Constructors still execute normally and may have their
own side effects. Immutable-object constructor mapping is not implemented.

Both directions visit the entire superclass hierarchy, including private and annotated
inherited fields. Fields declared by a child hide fields with the same **Java name**
in its parents before filtering or applying `@JsonProperty`. An excluded child field
therefore does not expose the hidden parent field, even under a different JSON name.
This rule also applies to final fields when reading JSON.

After hiding and excluding static, transient and synthetic fields, every JSON name
must identify exactly one field. Two distinct fields with the same JSON name cause
`IllegalArgumentException` in **both** directions, including collisions involving
final fields. The message identifies the JSON name and both declaring fields.
Validation happens before reading fields or invoking the target constructor; it does
not depend on which properties are present in the input. Final fields are skipped
for assignment only after this common validation. Excluded fields do not reserve names.

Serialization visits parent fields before new child fields; a hiding child field
keeps the parent's position. Within a class, reflection's declared-field order is used
and is not guaranteed by Java. JSON object member order has no semantic significance.

Synthetic enclosing-instance references of inner classes are excluded from serialization.
Deserializing a non-static inner class still requires an enclosing instance and is not
supported by the existing no-argument-constructor mapper.

### Parsing to Java object

Actually, in most cases, you're not interested in the intermediate result as a tree of JSON elements.
It is much more convenient to use data written directly to a Java object of a specific, problem-oriented class.

Suppose you have such a JSON object:

```json
{
    "name": "Johnny",
    "age": 41
}
```

Such an object can be stored in, say, the following Java structure:

```java
class Person {
    String name;
    int age;
}
```

So, we've got a couple more methods here that allow us to parse JSON documents directly into Java objects:

```
<T> T Json.parse(String source, Class<T> type) throws JsonException
<T> T Json.parse(File file, Class<T> type) throws JsonException
```

And a couple other methods that do the same thing, but allow you to specify the parsing mode:

```
<T> T Json.parse(String source, Class<T> type, JsonParsingMode mode) throws JsonException
<T> T Json.parse(File file, Class<T> type, JsonParsingMode mode) throws JsonException
```

Here's a full example. First, you need to create a class to which the data will be written.
For example, it can be the `Person` class described above. In this form, it will work.
Or, we can upgrade this class by adding access modifiers:

```java
class Person {
    private String name;
    private int age;

    private Person() {
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }
}
```

Okay, what have we got? We have a class whose fields cannot be changed
because they are private, and the object itself cannot be instantiated because
the constructor is private. All this is known as an
"[immutable object](https://en.wikipedia.org/wiki/Immutable_object)",
and in many cases it is much better than a mutable one. There's 
[tons of research](https://github.com/objectionary/eo) on this topic.

Now we can parse a JSON document into an object of this class:

```java
import com.kniazkov.json.Json;
import com.kniazkov.json.JsonException;

class MyClass {
    public void parseJson(String document) {
        try {
            Person person = Json.parse(document, Person.class);
            // do something with the resulting object:
            String name = person.getName();
            // ...
        } catch (JsonException exception) {
            // do something if the parsing fails
        }
    }
}
```

Present fields must have compatible values; otherwise mapping throws `JsonDataException`
unless the field explicitly permits a fallback with `@JsonProperty`.<br/>
The correlation between Java and JSON types is as follows:
- booleans can be written to the `boolean` (`Boolean`) type;
- strings can be written to the `String` type;
- numbers can be written in `byte` (`Byte`), `short` (`Short`), `int` (`Integer`),
`long` (`Long`), `float` (`Float`) and `double` (`Double`) types (the value is converted
to the specified type);
- `null` can be written to any object (but not a primitive type);
- objects are converted (recursively) to Java objects of the specified type;
- arrays may be converted to Java lists, with each element of such an array converted
to the type specified as a Java list parameter.

As shown above, the field name of a Java class is usually mapped to the property name of a JSON object.
However, there are cases when the JSON property key name is not allowed in JAVA. For example,
when the name is reserved (`int`, `extends`, and so on) or contains spaces. In such a case, you can add
a special `JsonProperty` annotation that will contain the actual property name:

```java
import com.kniazkov.json.JsonProperty;

class YourClass {
  @JsonProperty(name = "int")
  private int intValue;
}
```

Let's look at a more complex example. Suppose we have several classes:

```java
import com.kniazkov.json.JsonProperty;

class Employee {
  String name;
  int age;
}

class Department {
  String title;
  @JsonProperty(name = "abstract") String description;
  Employee manager;
  List<Employee> staff;
  List<Float> monthlyExpenses;
}
```

The following JSON document can be converted to the above structure:

```json
{
    "title": "Research lab",
    "abstract": "Investigation of material properties",
    "manager": {
        "name": "James Brown",
        "age": 42
    },
    "staff": [
        {
            "name": "Gary Walker",
            "age": 28
        },
        {
            "name": "Jessica Mcconnell",
            "age": 33
        }
    ],
    "monthlyExpenses" : [1200, 1000, 1450.99]
}
```

### Serializing Java objects

Serialization is the inverse procedure to parsing, that is, transforming a Java object
into a JSON document.

The `Json` class has two methods to do this:

```
String Json.serialize(Object obj)
String Json.serialize(Object obj, String indentation)
```

The first method transforms the Java object into a string without line separators (CR+LF or LF),
for example, the `Person` object we described above would be converted to the string
```json
{"name": "Johnny", "age": 41}
```

The second method composes a string divided into lines by system line separator.
There will be an indentation before each new line. For nested objects or arrays, multiple indents
are added accordingly. The indents are formed by copying the pattern passed as an argument.

Here's a full example.

```java

import com.kniazkov.json.Json;
import java.util.List;
import java.util.Arrays;

class MyClass {
    class Employee {
        String name;
        List<Integer> expenses;
    }

    public String generateJson() {
        Employee obj = new Employee();
        obj.name = "John Smith";
        obj.expenses = Arrays.asList(1000, 2000, 1500);
        return Json.serialize(obj, "\t");
    }
}
```

Output:

```json
{
    "name": "John Smith",
    "expenses": [
        1000,
        2000,
        1500
    ]
}
```

### Serializing JSON tree

You can also serialize a JSON tree (i.e., an intermediate data representation).
To do this, call the `toString` method or `toText` method at the root of the tree (i.e., an object
of the `JsonElement` class):

```
String JsonElement.toString()
String JsonElement.toText(String indentation)
```

The first method transforms the JSON tree into a string without line separators (CR+LF or LF),
while the second method transforms it into a string with separators and indents.

## Data structures

Remember you can work with JSON using only the `Json` class, which is described above.

Ok, as mentioned earlier, the library works with an internal data structure in which it stores a JSON tree.

### JsonElement

A base class that describes a JSON element, which can be an object, array, string, number, or literal.
This is an abstract class, and all specified types of elements are inherited from it.

An element of any type can be the root of a JSON tree. If an element contains no children (for example, a number),
then this element is a valid JSON tree consisting of a single element.

An element has the following methods (which are respectively inherited by all entities):

- `JsonContainer parent()`

    Returns a *container*, that is, either a JSON object or a JSON array that owns this element.
    If this element is the root of the tree, the method returns `null`.

    In this way, you can traverse the tree in various directions, including from leaf to root.
    To be honest, I don't know why this is so necessary. I guess we can come up with some algorithms to use this later.


- `JsonElement clone()`

    Each element is [cloneable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Cloneable.html).
    If the element contains other elements, clones of these elements are also created, i.e. "deep" copying
    is performed. Thus, the result is a new (sub-)tree, which is not related to the original in any way.


- `String toString()`

    Represents the element as a string, that is, it performs serialization.

    This name instead of `serialize` was chosen because `toString` method
    is automatically called by debuggers during step-by-step execution and displaying the object's
    contents, which turns out to be quite convenient.


- `String toText(String indentation)`

    It's the same serialization as `toString()`, only indented to make it easier to read.
    You want to specify a string that will represent "one unit" of indentation. This can be, say,
    multiple spaces or a tab.


- `Object toJavaObject()`

    Converts the element to a suitable Java object. For example, a string is converted to a
    `String` instance, an integer is converted to `BigInteger` and a real number to `BigDecimal`, and so on.
    Complex objects are converted into associative arrays (like `Map`).


- `<T> T toJavaObject(Class<T> type)`

    Using this method, you can directly specify the type of Java object, and, accordingly,  will be performed an attempt
    to convert to this type. If this is not possible, the method will return `null`.

    For example,

    ```java
    import com.kniazkov.json.JsonParser;
    import java.util.List;
    import java.util.Arrays;
    
    class MyClass {
        class Vector {
            int x;
            int y;
        }
    
        Vector createVector() throws JsonException {
            JsonElement element = JsonParser.parseString("{x: 10, y: 20}");
            Vector vector = element.toJavaObject(Vector.class);
            return vector;
        }
    }
    ```


- `boolean isNull()`

    Checks if the element is `null` literal(such a special value allowed by JSON syntax).


- `boolean isBoolean()`

    Checks if the element contains boolean value.


- `boolean booleanValue()`

    If the element contains boolean value (`element.isBoolean() == true`), returns this value.
    If not (for example, element is a string), returns `false`.


- `boolean isInteger()`

    Checks if the element is a 32-bit integer.


- `boolean isLongInteger()`

    Checks if the element is a 64-bit integer.

    If `element.isInteger() == true`, then always `element.isLongInteger() == true` (the reverse is not true).


- `boolean isNumber()`

    Checks if the element is a number.

    If `element.isInteger() == true` or `element.isLongInteger() == true`, then always `element.isNumber() == true`
    (the reverse is not true).


- `int intValue()`

    Returns the value of the element converted to a 32-bit integer, or 0 if no such conversion is possible.


- `long longValue()`

    Returns the value of the element converted to a 64-bit integer, or 0 if no such conversion is possible.


- `double doubleValue()`

    Returns the value of the element represented as a double-precision real number, or 0 if no such conversion is possible.


- `boolean isString()`

    Checks if the element is a string.


- `String stringValue()`

    Returns the value of the element represented as a string, or empty string if no such conversion
    is possible. Primitive types (numbers, boolean values) can be represented as a string.


- `JsonArray JsonArray()`

    Safely casts an element to the `JsonArray` type. Returns an instance of `JsonArray` or `null` if the element
    is not a JSON array.


- `JsonObject toJsonObject()`

    Safely casts an element to the `JsonObject` type. Returns an instance of `JsonObject` or `null` if the element
    is not a JSON object.

### Version 2 value access and migration

Library-defined getters no longer use the `get` prefix; old aliases are removed:

| Previous API | Version 2 |
| --- | --- |
| `getIntValue()`, `getLongValue()`, `getDoubleValue()` | `intValue()`, `longValue()`, `doubleValue()` |
| `getBooleanValue()`, `getStringValue()` | `booleanValue()`, `stringValue()` |
| `getParent()` | `parent()` |
| `getElement(index)` / `getElement(key)` | `element(index)` / `element(key)` |
| `JsonLocation.getRow()` / `getColumn()` | `row()` / `column()` |
| `JsonError.getLocation()` / `getMessage()` / `getLocalizedMessage(lang)` | `location()` / `message()` / `localizedMessage(lang)` |
| `JsonException.getError()` | `error()` |
| `JsonBoolean.getInstance(value)` | `JsonBoolean.of(value)` |

Standard Java contracts remain intact: `List.get`, `Map.get`, and Throwable's
`getMessage()` / `getLocalizedMessage()` retain their standard names. In particular,
`JsonException.getMessage()` still includes the syntax-error location.

All `JsonElement` instances now expose `numberValue()` (Number) and `decimalValue()`
(BigDecimal). Finite numeric nodes return their exact value; special numbers return
Double from numberValue() and null from decimalValue(). Other kinds return null.
`JsonInteger.numberValue()` returns BigInteger and `JsonReal.numberValue()` returns
BigDecimal. Converting an integer to a decimal remains exact.

Soft accessors retain their documented defaults and conversions. For callers that
want a value or an exception, these methods are also available on `JsonElement`:

| Strict accessor | Accepted data |
| --- | --- |
| `booleanValueOrThrow()` | JSON boolean |
| `stringValueOrThrow()` | JSON string with a non-null payload; no number/boolean/null-to-text coercion |
| `intValueOrThrow()` | Exact integral numeric value within the signed 32-bit range |
| `longValueOrThrow()` | Exact integral numeric value within the signed 64-bit range |
| `doubleValueOrThrow()` | Numeric value convertible to finite double without underflow to zero; ordinary rounding is allowed |
| `numberValueOrThrow()` | Any number; BigInteger/BigDecimal for finite values, Double for special values |
| `decimalValueOrThrow()` | A finite number; returns an exact BigDecimal |
| `arrayValueOrThrow()` | JSON array |
| `objectValueOrThrow()` | JSON object |

A type or numeric-format mismatch throws `JsonDataException`, a RuntimeException
separate from syntax errors (`JsonException`). Error messages describe the expected
format and actual element class. They do not serialize the payload. Valid zero,
false and empty strings/containers are returned normally; JSON null is rejected.

```java
JsonElement root = Json.parse("{\"id\":9007199254740993}");
long id = root.objectValueOrThrow().element("id").longValueOrThrow();
// A missing key still returns Java null from element(); check it separately.
```

The conversion method remains named `toJavaObject()`. Its untyped result no longer
rounds numbers to Double: both roots and nested Map/List values retain BigInteger or
BigDecimal. The same applies to Object fields, raw lists and List<Object> in typed
mapping. Explicit `toJavaObject(Double.class)` allows rounding but rejects overflow
and underflow to zero. Number.class and Object.class conversions of numeric nodes
return the stored number. Typed mapping uses the strict rules documented above;
convenience getters keep their existing behavior.

### Equality and collection contracts

JSON elements compare by content, never by their parent links. Cloning therefore
preserves equality and hashCode. Scalar JSON nodes compare only with JSON nodes of
the appropriate kind: `new JsonString("x")` is not equal to Java `"x"`, JsonNull is
not Java null, and a JsonNumber is not a Java Integer/BigDecimal.

- Finite numbers use exact mathematical equality across JsonInteger/JsonReal and
  decimal scales: `1`, `1.0` and `1e0` are equal and have the same hash. Zero has one
  equality class. No conversion through double is used.
- All JsonNaN nodes are equal, including to themselves. Positive and negative
  infinities form separate equality classes; neither equals a finite number or NaN.
- Strings compare by their text and booleans by their value. All JsonNull nodes are equal.
- JsonArray follows the List contract: order matters, and it can equal a standard
  Java List containing equal elements, with the same hashCode.
- JsonObject follows the Map contract: member order does not affect equality or hash.
  It can equal a standard Java Map containing the same keys and equal values.

For example, a JsonArray containing JsonNumber(1) equals a List containing an equal
JsonNumber, but does not equal `List.of(1)`. Nested standard Lists/Maps participate
in the same structural comparison. As usual, this assumes the other collection
obeys its own equals/hashCode contract.

Equality and hashing of JSON trees use iterative traversal, including 20,000-level
regression coverage; parent links are excluded. No content hashes are cached:
containers and their descendants are mutable. Do not modify them while they are keys
in a HashMap or members of a HashSet. Stack-safety of methods invoked directly on
arbitrary external Java collections remains the responsibility of those collections.

This makes contains/indexOf/lastIndexOf/remove, bulk List operations, Map value
lookup, conditional remove/replace and entry views work with equal clones.

### JsonContainer

A container is an element that can store other elements. Thus, only an element of container type
can be a parent of another element, this intermediate class was declared to support "children-parent"
relation. There are two types of containers: objects and arrays.

A container has the following method (which are respectively inherited by JSON objects and JSON arrays):

- `int size()`

    Returns the number of elements in the container.

### JsonObject

`JsonObject` stores fields in insertion order in a single map. Replacing a key keeps
its position; removing and reinserting it places it at the end. `keySet()`, `values()`
and `entrySet()` are live, mutable views in that same order (previously these views
iterated in sorted-key order even though serialization used insertion order).

Removing through a view, iterator, removeIf/removeAll/retainAll, or clear updates the
object itself. Views do not support adding entries. `Map.Entry.setValue()` follows
`put()` ownership rules: it deep-clones the supplied value before replacement, assigning
the object as the clone's parent, and returns the previously stored value. Entries
obtained through arrays, streams and spliterators have the same controlled behavior.
`replaceAll()` and the other Map default mutation methods preserve the stored tree.
Inserting the object itself or an ancestor stores a finite snapshot, not a cycle.

Java null keys and values are rejected before modifying the affected mapping; use
`JsonNull.INSTANCE` or `addNull()` for a JSON null. Bulk operations are not transactional:
if a later operation fails, earlier successful changes may remain. As with existing
remove/put operations, previously obtained removed or replaced nodes retain their
original immutable parent reference; clone such a node to obtain an independent root.
The object and its views are not thread-safe.


JSON objects are sets of key-value pairs, where key is a string and value is any JSON element.

The class inherits the [java.util.Map](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html)
interface. Accordingly, it supports methods that can be used to search, add, delete, iterate over a list
of key-value pairs:

- size;
- isEmpty;
- containsKey;
- containsValue;
- get;
- put;
- remove;
- putAll;
- clear;
- keySet;
- values;
- entrySet.

Modern Java development tools allow you to expand a JSON object
in the IDE window as a drop-down list, which is very convenient for debugging.

The class has the following methods, in addition to inherited methods:

- `JsonElement element(String key)`

    Returns a child element by its key.


- `void addElement(String key, JsonElement elem)`

    Makes a clone of a JSON element and adds new key-value pair to the object.
    The element itself remains unchanged and further its modification will not affect the current tree.

    The clone will have the correct relations with the sequence of parent nodes.
    In this way, data consistency is always achieved.

    "Data consistency" is such a clever phrase that should automatically make this project incredibly
    cool.

However, if you need to fill the object with child elements, it is better to create them directly
inside the object, instead of creating them separately and then adding them using the
`addElement` method. This way you will avoid cloning operation.  
Here are the methods to do this:

- `void addNull(String key)`

    Creates a child element of type `null`.

- `void addBoolean(String key, boolean value)`

    Creates a child element of boolean type.

- `void addNumber(String key, double value)`

    Creates a child element of numeric type.

- `void addString(String key, String value)`

    Creates a child element of string type.

- `JsonArray createArray(String key)`

    Creates an empty array as a child of this object.  
    A reference to the created array is returned so that it can be filled in later.

- `JsonObject createObject(String key)`

  Creates an empty object as a child of this object.  
  A reference to the created object is returned so that it can be filled in later.

### JsonArray

A JSON array is an ordered set of JSON elements.

The class inherits the [java.util.List](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html)
interface. Accordingly, it supports methods that can be used to search, add, delete, iterate over a list
of elements:

- size;
- isEmpty;
- toArray;
- add;
- remove;
- containsAll;
- addAll;
- removeAll;
- retainAll;
- clear;
- get;
- set;
- indexOf;
- lastIndexOf;
- contains;
- iterator;
- listIterator.

`JsonArray.removeIf`, `replaceAll` and `sort` are supported on the array itself.
`replaceAll` clones each replacement with the array as parent and rejects Java null;
use JsonNull for a JSON null. `sort` rearranges existing children without cloning;
provide a comparator because JsonElement has no natural Comparable ordering.
Callback/comparator failures do not promise a transactional rollback of earlier work.
`addAll` snapshots and clones the input before insertion, including self-insertion.

`iterator`, `listIterator` and `subList` remain read-only views: their structural
mutation methods (including subList removeIf/replaceAll/sort) are unsupported.
Contained JSON nodes may still be mutable; these views are not deep immutable copies.
Existing non-structural parent changes are visible; structural changes can invalidate
a subList/iterator according to the backing ArrayList's rules.

The class has the following methods, in addition to inherited methods:

- `JsonElement element(int index)`

    Returns a child element by its index.

- `void addElement(JsonElement elem)`
    
    Makes a clone of the JSON element and appends this clone to the end of the array.
    The element itself remains unchanged and further its modification will not affect the current tree.

    The clone will have the correct relations with the sequence of parent nodes.
    In this way, data consistency is always achieved.

However, if you need to fill the array with child elements, it is better to create them directly
inside the array, instead of creating them separately and then adding them using the `addElement` method.
This way you will avoid cloning operation.  
Here are the methods to do this:

- `void addNull()`

    Creates a child element of type `null`.

- `void addBoolean(boolean value)`

    Creates a child element of boolean type.

- `void addNumber(double value)`

    Creates a child element of numeric type.

- `void addString(String value)`

    Creates a child element of string type.

- `JsonArray createArray()`

    Creates an empty array as a child of this array.  
    A reference to the created array is returned so that it can be filled in later.

- `JsonObject createObject()`

    Creates an empty object as a child of this array.  
    A reference to the created object is returned so that it can be filled in later.

### JsonNull

An element representing `null` literal.

Doesn't have a public constructor. An element without parent is a singleton that can be accessed by
`JsonNull.INSTANCE`.

### JsonBoolean

Represents a boolean JSON element, i.e `true` or `false` literals.

Doesn't have a public constructor. An element without a parent that represents `true` is a singleton that
can be accessed by `JsonBoolean.TRUE`, element that represents `false` can be accessed by `JsonBoolean.FALSE`. 
Also, element can be retrieved using the `of` method.

### JsonNumber

An abstract base class for exact finite numbers and JSON5 special values:

- `JsonInteger` stores a `BigInteger` (including small integers; there is no `long` fast path).
- `JsonReal` stores a `BigDecimal`.
- `JsonNaN` represents NaN.
- `JsonInfinity` represents positive or negative infinity.

Create numbers through the factory:

```java
JsonNumber integer = JsonNumber.of(42);
JsonNumber largeId = JsonNumber.of(9007199254740993L);
JsonNumber huge = JsonNumber.of(new BigInteger("18446744073709551616"));
JsonNumber real = JsonNumber.of(42.0);
JsonNumber decimal = JsonNumber.of(new BigDecimal("0.12345678901234567890"));
```

Import `java.math.BigInteger` and `java.math.BigDecimal` for the arbitrary-precision types.
The `long` and `BigInteger` overloads create `JsonInteger`; finite `float`/`double` and
`BigDecimal` inputs create `JsonReal`, even for integral values such as `42.0`.
The float/double overloads return `JsonNumber`, selecting `JsonNaN` or `JsonInfinity`
for special values. This return-type change requires consumers to recompile.
The `Number` overload dispatches by runtime type and supports `Byte`, `Short`, `Integer`,
`Long`, `Float`, `Double`, `BigInteger`, and `BigDecimal`. Other implementations are rejected
with `IllegalArgumentException`; null is rejected with `NullPointerException`.

**Migration:** replace `new JsonNumber(value)` with `JsonNumber.of(value)`.
Making `JsonNumber` abstract is a source and binary compatibility change; consumers must
be recompiled. Version 2 also renames getters and introduces strict accessors described below.

For finite numbers, the existing methods retain these contracts:

| Method | Behavior |
| --- | --- |
| `isInteger()` | True only for an exact integral value in the signed 32-bit range |
| `isLongInteger()` | True only for an exact integral value in the signed 64-bit range |
| `intValue()` / `longValue()` | Truncate toward zero; saturate at the destination range boundaries, without an intermediate `double` |
| `doubleValue()` | Convert to `double`; rounding, underflow to zero and overflow to infinity are possible |
| `stringValue()` / `toString()` | Serialize the stored exact value |
| `toJavaObject()` | Returns the stored `BigInteger` or `BigDecimal`, including recursively inside arrays and objects |
| `toJavaObject(Class)` | Existing wrapper conversions remain supported; `BigInteger` and `BigDecimal` provide exact numeric access through the existing method |

Typed conversion to an integer requires an exact integral value in range; otherwise
it throws `JsonDataException`. Byte/Short mapping no longer silently narrows values.
The predicates describe value/range, not the subclass: `JsonNumber.of(13.0).isInteger()` is true.

Parsing chooses `JsonInteger` for decimal integers and JSON5 hexadecimal integers, and
`JsonReal` for lexemes containing a point or exponent (`1.0`, `1e3`). Numeric lexemes are
converted directly to `BigInteger`/`BigDecimal`, without primitive accumulators. BigDecimal's
scale/exponent limits still apply; exceeding its supported range produces `JsonException`
instead of an overflowing value. STRICT and JSON5 reject leading-zero decimal integers;
EXTENDED retains support for them.

Serialization preserves the numeric value and numeric kind, not the original spelling.
Real output includes a point or exponent (for example `42.0`); exponent formatting follows
`BigDecimal.toString()` (for example `5.0E+100`). A scale-zero BigDecimal is written with
`.0` so it remains real on parsing. Negative zero is normalized, because neither backing
type distinguishes its sign. Double factories use `BigDecimal.valueOf`; float factories
use `Float.toString` without widening first. Precision already lost before the factory
call cannot be recovered. Special floating-point inputs now create dedicated JSON5
nodes, including through container addNumber methods and Java-object serialization.

`JsonArray.addNumber` and `JsonObject.addNumber` retain the `double` overload and add
`long`, `float`, and `Number` overloads so integers and arbitrary-precision values do not
pass through `double`. Java-object serialization and typed mapping also support `BigInteger`
and `BigDecimal` fields and list elements.

### JsonNaN and JsonInfinity

Use `JsonNumber.of(Double.NaN)`, `JsonNumber.of(Double.POSITIVE_INFINITY)` or
`JsonNumber.of(Double.NEGATIVE_INFINITY)`. Explicit constructors are `new JsonNaN()`
and `new JsonInfinity(negative)`. The latter's boolean selects the sign.

All `JsonElement` instances expose `isNaN()`, `isInfinite()`, `isPositiveInfinity()`,
`isNegativeInfinity()` and `isFinite()`. Non-numeric elements return false for all five.
`isFinite()` describes the stored number, not its double conversion: a huge BigDecimal
can be finite even when doubleValue() overflows to infinity.

Special nodes are numbers but are neither 32-bit nor 64-bit integers. numberValue(),
numberValueOrThrow(), doubleValue() and untyped toJavaObject() return the corresponding
IEEE special value (boxed Double where an object is needed). Float/Double typed mapping
is supported. intValue()/longValue() follow Java casts: NaN becomes zero and infinities
saturate at the signed limits. decimalValue() returns null; BigInteger/BigDecimal typed
mapping rejects these values with JsonDataException. intValueOrThrow(), longValueOrThrow(), doubleValueOrThrow() and
decimalValueOrThrow() reject special values with JsonDataException; the existing strict
double getter continues to require a finite result. Cloning preserves the special kind
and assigns the new parent in the same way as other numeric nodes.

**Output containing these nodes is JSON5, not strict JSON.** toString(), toText() and
Json.serialize() emit `NaN`, `Infinity` and `-Infinity`; they never silently replace them
with null or strings. Signed NaN spellings normalize to `NaN`, and `+Infinity` to
`Infinity`. Finite numeric serialization stays exact. A caller requiring strict JSON
must reject special nodes before serialization.

### JsonString

Represents a JSON element containing string.

### Configurable parser and serializer

The simple calls remain: `Json.parse(text)` and `Json.serialize(value)`. Existing
static overloads delegate to the same configurable implementations; advanced options
no longer require another combination of overloads.

```java
JsonParser parser = new JsonParser()
    .setCharset(Charset.forName("windows-1251"))
    .setMode(JsonParsingMode.STRICT)
    .setBomPolicy(JsonBomPolicy.ALLOW)
    .setSourceName("settings")
    .setCollectWarnings(true);
JsonElement tree = parser.parse(file);
MyClass value = tree.toJavaObject(MyClass.class);
List<JsonError> diagnostics = parser.warnings();

JsonSerializer serializer = new JsonSerializer()
    .setCharset(StandardCharsets.UTF_8)
    .setIndentation("  ")
    .setWriteBom(false)
    .setEscapeNonAscii(false);
serializer.serialize(value, file);
String text = serializer.serialize(value);
```

Both objects are reusable and are not thread-safe. Setters return `this` and validate
settings immediately. Each parse creates a fresh internal session. `warnings()` is an
immutable snapshot of the last call, including warnings collected before a failure;
old snapshots remain valid. Collection is disabled by default. The legacy static
`parseString(..., warnings)` still appends to the supplied list without clearing it.
`setSourceName(null)` restores the file path for files and unnamed input otherwise.

Parser input overloads accept String, Reader, InputStream or File. Serializer accepts
a Java value or existing JsonElement tree and returns a String, or writes to Writer,
OutputStream or File. Owned files are closed on success and failure. Borrowed readers,
writers and streams are never closed; successful output calls flush their destination.
Input/decoding errors remain JsonException with their original causes; output and
encoding errors are IOException (including CharacterCodingException). A caller-supplied
Reader/Writer is responsible for its own decoding/encoding policy.

Defaults are UTF-8, EXTENDED parsing, optional initial BOM, compact output without a
BOM, and literal non-ASCII text. `setIndentation(null)` selects compact output; an empty
string selects line breaks without indentation. Non-null indentation may contain only
JSON whitespace (space, tab, CR, LF). Syntax and non-finite-number behavior are unchanged:
NaN/Infinity output is JSON5. No automatic numeric or Unicode normalization is performed.

#### Encodings and BOM

Charset applies **only** to byte input/output. It does not reinterpret String/Reader
input or String/Writer output. All Java-supported decodable/encodable charsets may be
selected with Charset, including Windows-1251. UTF-8 is always the default, independently
of `file.encoding`. Library decoders/encoders report malformed and unmappable data;
they never silently insert replacement characters.

`JsonBomPolicy.ALLOW` accepts one initial BOM, `FORBID` rejects it, and `REQUIRE`
requires it. Byte input checks UTF-8/UTF-16/UTF-32 signatures **before decoding**, so
UTF-16 cannot silently consume a forbidden marker. The marker must match the selected
encoding; there is no general charset auto-detection. Generic UTF-16/UTF-32 select
endianness from a matching BOM, or use big endian without one. Explicit LE/BE charsets
reject a conflicting marker. Duplicate initial markers are rejected in every mode.

For String/Reader the policy applies to the first U+FEFF character. If an upstream
decoder has already consumed a byte BOM, REQUIRE cannot reconstruct it. An accepted
initial marker is removed before location tracking, so the first document character
is at 1:1. A BOM later in the document follows the selected syntax's whitespace rules;
STRICT does not treat U+FEFF as whitespace. U+FEFF inside a quoted string remains data.
The simple static API now accepts one initial BOM even in STRICT mode.

`setWriteBom(true)` writes exactly one byte marker; false writes none. It has no effect
on returned strings or Writer output. Generic UTF-16/UTF-32 output uses big endian;
LE/BE selects the explicit byte order. The library controls BOM emission even for JDK
BOM-emitting charset aliases. BOM emission with an unsupported encoding (such as
Windows-1251 or ASCII) is an invalid configuration, rejected by either setter ordering.

`setEscapeNonAscii(true)` escapes non-ASCII UTF-16 code units **in keys and string
values**, using surrogate-pair escapes for supplementary characters. This permits
lossless ASCII output, for example an emoji becomes `\ud83d\ude00`. It does not change
the tree or escape indentation. Raw unpaired surrogates fail byte encoding unless
escaped; escaping preserves code units and is not Unicode validation.

Serialization currently materializes the JSON text; byte output is fully encoded
before opening/truncating an owned file or writing a borrowed stream. Encoding failure
therefore leaves an existing file intact. Later write/flush failures can still leave
partial output; file output is not an atomic replacement API. Parsing remains buffered
and sequential, with bounded byte probing and no whole-file string allocation.

### Input sources and locations (version 2)

Strings, files, readers and byte streams share the same sequential input implementation.
Existing file overloads use UTF-8 regardless of the platform default. Examples:

```java
JsonElement root = Json.parse(reader, "server response", JsonParsingMode.STRICT);
JsonElement named = Json.parse(jsonText, "settings", JsonParsingMode.STRICT);
JsonElement utf8 = Json.parse(inputStream, JsonParsingMode.STRICT);
JsonElement encoded = Json.parse(inputStream, "response", StandardCharsets.UTF_16LE,
        JsonParsingMode.STRICT);
JsonElement fromFile = Json.parse(file, StandardCharsets.UTF_8, JsonParsingMode.STRICT);
```

`parse(Reader)` and `parse(InputStream)` default to `EXTENDED`, just like the existing
string and file methods. Reader and stream overloads also accept a mode without a name;
byte-stream overloads accept an explicit charset without a name. Use `toJavaObject(type)`
on the result for typed conversion. For a named overload, a null name means unnamed input;
`sourceName()` always returns an `Optional`, never null.

The input contract is:

1. Internal `Source` operates on a `Reader`. Strings use `StringReader`; byte input uses
   an explicit decoder, UTF-8 by default.
2. `charValue()` and `nextChar()` return `int`: 0..65535 is a UTF-16 code unit, -1 is EOF.
   A real NUL is 0, never EOF. These are code units, not Unicode code points.
3. The first code unit is current immediately after construction; an empty source starts
   at EOF. `charValue()` does not advance, read more input, or change coordinates.
4. `nextChar()` skips the current unit and returns the next one. Repeated advancement at
   EOF returns -1 without changing the position.
5. Rows and columns start at 1. Coordinates identify the current unit; EOF is the position
   immediately after the last unit. Empty input has EOF at `1:1`.
6. CRLF counts as one line break. Standalone CR, LF, U+2028 and U+2029 are also line breaks.
   Advancing past CR moves to the next row, column 1; advancing past its following LF
   leaves that position unchanged. Coordinate tracking is independent of syntax mode
   and does not grant permission to use a character as whitespace.
7. Columns count UTF-16 code units, not display width: a tab takes one position and a
   surrogate pair takes two.
8. `location()` produces an immutable `JsonLocation` snapshot with `row()`, `column()` and
   `Optional<String> sourceName()`. Advancing the source never changes previous snapshots.
9. The optional name is diagnostic text. Files use the path supplied by the caller;
   strings and streams may have an arbitrary name. Locations do not need subclasses.
10. Location formatting is `source.json, 1:20`, or `1:20` without a name.
11. Opening, reading, decoding and closing failures propagate through `JsonException`
    with the original cause. Malformed or unmappable bytes are reported, never silently
    replaced. This guarantee applies to decoders created by the library; a supplied
    `Reader` has already performed its own decoding. Buffered decoding may discover an
    error ahead of the current character; its location is the position where the reader
    failed to supply input, not a precise offset of the offending byte. After a source
    read failure, further reads rethrow the same failure without moving the position.
12. The library closes files it opens, on success and failure. It never closes a caller's
    `Reader` or `InputStream`.
13. `Source` only supplies characters and coordinates. The lexer and parser decide which
    characters and syntax are legal.
14. Input is sequential with bounded buffering, without loading the entire document into
    a string. No rewind or concurrent access is supported. Buffering may consume data
    beyond the parsed value; these overloads are not a resumable multi-document API.
    The resulting JSON tree and individual token values still occupy memory.

Root-tail validation, container recovery and JSON5 syntax are described above.

### Classes for error handling

Syntax diagnostics use `JsonLocation`, `JsonError` and the checked `JsonException`.
Strict value access uses the unchecked `JsonDataException`.

`JsonLocation` is immutable and exposes `int row()`, `int column()` and
`Optional<String> sourceName()`. Its text form is `source.json, 1:20`, or `1:20`
for unnamed input. Exceptions and error descriptions use the same location format.
Input failures use `JsonError.InputFailure`; `JsonException.getCause()` preserves the
original I/O or decoding exception. File-reading failures throw instead of returning null.
The actual error is described by the `JsonError` class, which contains a location and has the following methods:

- `String message()`

    Returns the text description of the error (in English).

- `String localizedMessage(String lang)`

    Returns the localized text description of the error. English (`en`) and Russian (`ru`) languages
    are now supported.

The last class, `JsonException` is a wrapper of the `JsonError` class, which inherits from `Exception`
and hence can be thrown by the parser.
