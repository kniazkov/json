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

Object fields are filled only if they have a suitable type, otherwise such a field is ignored.<br/>
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
(BigDecimal). Numeric nodes return their exact value; other kinds return null.
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
| `numberValueOrThrow()` | Any JSON number; returns BigInteger or BigDecimal |
| `decimalValueOrThrow()` | Any JSON number; returns an exact BigDecimal |
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
mapping. Explicit `toJavaObject(Double.class)` still requests a potentially lossy
conversion. Number.class and Object.class conversions of numeric nodes return the
exact stored number. Typed DTO mapping otherwise retains its existing conversion
policy; strict getters do not silently change that separate policy.

### JsonContainer

A container is an element that can store other elements. Thus, only an element of container type
can be a parent of another element, this intermediate class was declared to support "children-parent"
relation. There are two types of containers: objects and arrays.

A container has the following method (which are respectively inherited by JSON objects and JSON arrays):

- `int size()`

    Returns the number of elements in the container.

### JsonObject

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

An abstract base class for two exact numeric representations:

- `JsonInteger` stores a `BigInteger` (including small integers; there is no `long` fast path).
- `JsonReal` stores a `BigDecimal`.

Create numbers through the factory:

```java
JsonNumber integer = JsonNumber.of(42);
JsonNumber largeId = JsonNumber.of(9007199254740993L);
JsonNumber huge = JsonNumber.of(new BigInteger("18446744073709551616"));
JsonNumber real = JsonNumber.of(42.0);
JsonNumber decimal = JsonNumber.of(new BigDecimal("0.12345678901234567890"));
```

Import `java.math.BigInteger` and `java.math.BigDecimal` for the arbitrary-precision types.
The `long` and `BigInteger` overloads create `JsonInteger`; the `float`, `double`, and
`BigDecimal` overloads create `JsonReal`, even for integral values such as `42.0`.
The `Number` overload dispatches by runtime type and supports `Byte`, `Short`, `Integer`,
`Long`, `Float`, `Double`, `BigInteger`, and `BigDecimal`. Other implementations are rejected
with `IllegalArgumentException`; null is rejected with `NullPointerException`.

**Migration:** replace `new JsonNumber(value)` with `JsonNumber.of(value)`.
Making `JsonNumber` abstract is a source and binary compatibility change; consumers must
be recompiled. Version 2 also renames getters and introduces strict accessors described below.

The existing methods retain these contracts:

| Method | Behavior |
| --- | --- |
| `isInteger()` | True only for an exact integral value in the signed 32-bit range |
| `isLongInteger()` | True only for an exact integral value in the signed 64-bit range |
| `intValue()` / `longValue()` | Truncate toward zero; saturate at the destination range boundaries, without an intermediate `double` |
| `doubleValue()` | Convert to `double`; rounding, underflow to zero and overflow to infinity are possible |
| `stringValue()` / `toString()` | Serialize the stored exact value |
| `toJavaObject()` | Returns the stored `BigInteger` or `BigDecimal`, including recursively inside arrays and objects |
| `toJavaObject(Class)` | Existing wrapper conversions remain supported; `BigInteger` and `BigDecimal` provide exact numeric access through the existing method |

Conversion to `BigInteger` returns null for a fractional real or a value that cannot be
materialized as a `BigInteger`. Byte/Short conversion retains the existing narrowing behavior.
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
call cannot be recovered. NaN and infinities are rejected with `NumberFormatException`,
including during Java-object serialization; no non-finite JSON5 representation is added.

`JsonArray.addNumber` and `JsonObject.addNumber` retain the `double` overload and add
`long`, `float`, and `Number` overloads so integers and arbitrary-precision values do not
pass through `double`. Java-object serialization and typed mapping also support `BigInteger`
and `BigDecimal` fields and list elements.

### JsonString

Represents a JSON element containing string.

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

This refactoring prepares input handling for full-document validation. Rejecting trailing
content after the root (issue #3), incomplete syntax (issues #4/#5), and completing JSON5
support (issue #7) remain separate parser changes.

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
