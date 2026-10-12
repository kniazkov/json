<!--
  Copyright (c) 2025 Ivan Kniazkov
-->
# JSON that meets you where you are

Read a configuration, explore a document, or turn it into a Java object.
**com.kniazkov.json** gives each task a small, direct API, with exact numbers and
useful diagnostics when the input is less tidy than you hoped.

- **Three parsing modes:** strict JSON, human-friendly JSON5, and explicit recovery.
- **Exact finite numbers:** `BigInteger` and `BigDecimal`, without a detour through `double`.
- **An editable tree:** JSON objects and arrays implement Java's `Map` and `List` interfaces.
- **Java mapping:** objects, primitive/reference arrays, nested lists and typed maps.
- **Configurable I/O:** charsets, BOM policies, Unicode escaping and readable output.
- **Deep documents:** iterative traversal, with no fixed nesting-depth limit.

Requires **Java 21+**. Distributed under the [MIT license](LICENSE.txt).

## Standards

The syntax modes are based on **[ECMA-404, 2nd edition (December 2017)](https://ecma-international.org/publications-and-standards/standards/ecma-404/)**,
**[RFC 8259 (December 2017)](https://www.rfc-editor.org/rfc/rfc8259)** and
**[JSON5 1.0.0 (March 2018)](https://spec.json5.org/)**.
`EXTENDED` is this library's own recovery mode, not a separate standard.
See [syntax and standards](docs/parsing.md) for the precise boundaries.

## Install

```xml
<dependency>
    <groupId>com.kniazkov</groupId>
    <artifactId>json</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Get something done

The Java fragments below belong inside a method that declares `throws JsonException`;
import the named types from `com.kniazkov.json`.

```java
JsonObject settings = Json.parse("""
        {"name": "Observatory", "port": 8080}
        """, JsonParsingMode.STRICT).objectValueOrThrow();
String name = settings.element("name").orElseThrow().stringValueOrThrow();
settings.addBoolean("enabled", true);
System.out.println(Json.serialize(settings, "  "));
assert name.equals("Observatory");
```

Or go straight to a Java object:

```java
class Settings {
    String name;
    int port = 8080;
}
Settings settings = Json.parse("{name: 'Observatory'}", Settings.class).orElseThrow();
assert settings.port == 8080;
```

**`Json.parse(text)` defaults to `EXTENDED`, with warning collection disabled.**
Choose `STRICT` when malformed syntax must fail. Choose a configured
`JsonParser` when you want warnings or I/O options.

## Find your route

Start with the [user guide](docs/README.md), or jump to a task:

| I want to… | Read |
| --- | --- |
| Parse, edit and save my first document | [Getting started](docs/getting-started.md) |
| Choose JSON, JSON5 or recovery | [Parsing](docs/parsing.md) · [Recovery examples](docs/recovery.md) |
| Navigate and change the tree | [Tree API](docs/tree.md) |
| Keep numbers exact | [Numbers](docs/numbers.md) |
| Map arrays, maps and application objects | [Java mapping](docs/java-mapping.md) |
| Read files, choose encodings, write Unicode | [Input and output](docs/io.md) |
| Explain a failure to a user | [Diagnostics](docs/diagnostics.md) |
| See how the public classes fit together | [Public API diagrams](docs/public-api.md) |
| Build, test or contribute | [Development](docs/development.md) |
