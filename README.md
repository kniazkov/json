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
