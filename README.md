<!---
  Copyright (c) 2024 Ivan Kniazkov
-->
# User-friendly JSON parser/serializer

Version 1.0

## Goal

> Write programs that do one thing and do it well. \
― Unix philosophy

The goal of this project is to create a library that allows you to parse and serialize JSON documents
while writing as little code as possible, supports extended JSON5 syntax, and also processes malformed documents.

## The Maven dependency

To link the library to your Maven project, add the following dependency to `pom.xml`:
```xml
<dependencies>
    <dependency>
        <groupId>com.kniazkov</groupId>
        <artifactId>json</artifactId>
        <version>1.0</version>
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

Let's look at a more complex example. Suppose we have several classes:

```java
class Employee {
    String name;
    int age;
}

class Department {
    String title;
    Employee manager;
    List<Employee> staff;
    List<Float> monthlyExpenses;
}
```

The following JSON document can be converted to the above structure:

```json
{
    "title": "Research lab",
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

- `JsonContainer getParent()`

    Returns a *container*, that is, either a JSON object or a JSON array that owns this element.
    If this element is the root of the tree, the method returns `null`.

    In this way, you can traverse the tree in various directions, including from leaf to root.
    To be honest, I don't know why this is so necessary. I guess we can come up with some algorithms to use this later.


- `JsonElement clone()`

    Each element is [cloneable](https://docs.oracle.com/javase/8/docs/api/java/lang/Cloneable.html).
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
    `String` instance, a number is converted to a `Double` instance, and so on.
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


- `boolean getBooleanValue()`

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


- `int getIntValue()`

    Returns the value of the element converted to a 32-bit integer, or 0 if no such conversion is possible.


- `long getLongValue()`

    Returns the value of the element converted to a 62-bit integer, or 0 if no such conversion is possible.


- `double getDoubleValue()`

    Returns the value of the element represented as a double-precision real number, or 0 if no such conversion is possible.


- `boolean isString()`

    Checks if the element is a string.


- `String getStringValue()`

    Returns the value of the element represented as a string, or empty string if no such conversion
    is possible. Primitive types (numbers, boolean values) can be represented as a string.


- `JsonArray JsonArray()`

    Safely casts an element to the `JsonArray` type. Returns an instance of `JsonArray` or `null` if the element
    is not a JSON array.


- `JsonObject toJsonObject()`

    Safely casts an element to the `JsonObject` type. Returns an instance of `JsonObject` or `null` if the element
    is not a JSON object.

### JsonContainer

A container is an element that can store other elements. Thus, only an element of container type
can be a parent of another element, this intermediate class was declared to support "children-parent"
relation. There are two types of containers: objects and arrays.

A container has the following method (which are respectively inherited by JSON objects and JSON arrays):

- `int size()`

    Returns the number of elements in the container.

### JsonObject

JSON objects are sets of key-value pairs, where key is a string and value is any JSON element.

The class inherits the [java.util.Map](https://docs.oracle.com/javase/8/docs/api/java/util/Map.html)
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

- `JsonElement getElement(String key)`

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

The class inherits the [java.util.List](https://docs.oracle.com/javase/8/docs/api/java/util/List.html)
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

- `JsonElement getElement(int index)`

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
Also, element can be retrieved using the `getInstance` method.

### JsonNumber

Represents a JSON element containing number.

### JsonString

Represents a JSON element containing string.

### Classes for error handling

There are three classes for error handling: `JsonLocation`, `JsonError` and `JsonException`.

The location is determined by the row number and column number of the JSON document in which the error was found.
The actual error is described by the `JsonError` class, which contains a location and has the following methods:

- `String getMessage()`

    Returns the text description of the error (in English).

- `String getLocalizedMessage(String lang)`

    Returns the localized text description of the error. English (`en`) and Russian (`ru`) languages
    are now supported.

The last class, `JsonException` is a wrapper of the `JsonError` class, which inherits from `Exception`
and hence can be thrown by the parser.
