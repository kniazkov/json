<!--
  Copyright (c) 2026 Ivan Kniazkov
-->
# From a document to something useful

[Guide](README.md) · Next: [Parsing](parsing.md)

Imagine an observatory application. Its configuration contains a name, a list of
instruments and a budget. Explore the configuration as a tree first; introduce Java
model classes when the shape becomes useful to your application.

## Read a document

This is ordinary JSON:

```json
{
  "name": "Hilltop Observatory",
  "instruments": ["camera", "spectrometer"],
  "budget": 1250.75,
  "enabled": true
}
```

Ask explicitly for the kinds of values you expect:

```java
String document = """
        {
          "name": "Hilltop Observatory",
          "instruments": ["camera", "spectrometer"],
          "budget": 1250.75,
          "enabled": true
        }
        """;
JsonObject root = Json.parse(document, JsonParsingMode.STRICT).objectValueOrThrow();
String name = root.element("name").stringValueOrThrow();
JsonArray instruments = root.element("instruments").arrayValueOrThrow();
BigDecimal budget = root.element("budget").decimalValueOrThrow();
assert name.equals("Hilltop Observatory");
assert instruments.size() == 2;
assert budget.equals(new BigDecimal("1250.75"));
```

`OrThrow` accessors fail on the wrong kind of value. Use them at application
boundaries, where a silent default would conceal a problem.

## Build and edit

Container helpers create children with the correct parent:

```java
JsonObject observation = new JsonObject();
observation.addString("target", "Jupiter");
observation.addNumber("exposureSeconds", new BigDecimal("0.025"));
JsonArray filters = observation.createArray("filters");
filters.addString("red");
filters.addString("blue");
observation.addBoolean("complete", false);
String compact = Json.serialize(observation);
String readable = Json.serialize(observation, "  ");
assert Json.parse(compact, JsonParsingMode.STRICT).equals(observation);
assert readable.contains("Jupiter");
```

Compact and indented output describe the same tree. Comments, whitespace and the
original numeric spelling are not stored, so serialization is not a source-code formatter.

## Work with a Java class

```java
class Observation {
    String target;
    BigDecimal exposureSeconds;
    List<String> filters;
    boolean complete;
}
Observation value = Json.parse("""
        {
          "target": "Jupiter",
          "exposureSeconds": 0.025,
          "filters": ["red", "blue"],
          "complete": false
        }
        """, Observation.class, JsonParsingMode.STRICT);
assert value.target.equals("Jupiter");
assert value.exposureSeconds.equals(new BigDecimal("0.025"));
assert Json.parse(Json.serialize(value)).objectValueOrThrow()
        .element("filters").arrayValueOrThrow().size() == 2;
```

Mapping uses fields, including inherited private fields, and a no-argument constructor.
You do not need annotations when Java and JSON field names agree.

## A bad value should stay visible

This is valid JSON, but the application expects an integer:

```json
{"port": "8080"}
```

```java
class Settings {
    int port;
}
try {
    Json.parse("{\"port\":\"8080\"}", Settings.class, JsonParsingMode.STRICT);
    throw new AssertionError("A string is not an integer");
} catch (JsonDataException expected) {
    assert expected.getMessage().contains("$[\"port\"]");
}
```

Changing the syntax mode does not make that conversion valid. Fix the producer,
choose the right Java type, or deliberately select a [field fallback](java-mapping.md#field-policies).

## When configuration needs comments

Use JSON5 intentionally:

```json5
{
  // Seconds per exposure
  exposureSeconds: .025,
  target: 'Jupiter',
  filters: ['red', 'blue',],
}
```

```java
JsonParser parser = new JsonParser().setMode(JsonParsingMode.JSON5);
JsonElement value = parser.parse("{target:'Jupiter', filters:['red','blue',],}");
assert value.objectValueOrThrow().element("target").stringValueOrThrow().equals("Jupiter");
```

For damaged documents, start with the [recovery gallery](recovery.md).
For files and Unicode, go to [input and output](io.md).
