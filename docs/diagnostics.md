<!--
  Copyright (c) 2026 Ivan Kniazkov
-->
# Make failures useful

[Guide](README.md) · [Recovery](recovery.md) · [Java mapping](java-mapping.md)

A useful error answers two questions: what failed, and where? Syntax/input errors
have source locations; mapping errors have paths through the data.

## Different failures, different exceptions

| Failure | Exception | Useful information |
| --- | --- | --- |
| Invalid syntax or unreadable input | Checked `JsonException` | `error()`, location, original I/O cause |
| Wrong value/type/range, mapping or cycle | Unchecked `JsonDataException` | Message; mapping path; underlying cause where available |
| Invalid annotation policy, duplicate mapped names, unsuitable map keys/settings | `IllegalArgumentException` | Configuration/data-contract explanation |
| Output/encoding failure | `IOException` | Original output failure |

Not every caller programming error is wrapped: null arguments, bad array indices
and unsupported view mutations may produce the ordinary Java exceptions.

## Syntax errors and locations

```java
String secondJson = "{\"port\":}";

JsonParser parser = new JsonParser()
        .setMode(JsonParsingMode.STRICT)
        .setSourceName("settings.firstJson");
try {
    parser.parse(secondJson);
    throw new AssertionError("A value is required");
} catch (JsonException failure) {
    JsonError error = failure.error();
    JsonLocation location = error.location();
    assert location.sourceName()
            .orElseThrow()
            .equals("settings.firstJson");
    assert location.row() == 1;
    System.out.println(location + ": " + error.message());
    System.out.println(error.localizedMessage("ru"));
}
```

`JsonError.message()` is English. `localizedMessage("en")` and
`localizedMessage("ru")` provide explicit language selection.
`JsonException.getLocalizedMessage()` uses the JVM's `user.language` property.
Exception messages include the source location.

Locations are immutable snapshots:

- `row()` and `column()` start at one.
- `sourceName()` returns Optional, not null.
- Text form is `settings.json, 1:20`, or `1:20` when unnamed.
- Columns count UTF-16 code units. A tab counts as one; a supplementary character
  represented by a surrogate pair counts as two.
- CRLF is one line break. CR, LF, U+2028 and U+2029 are also line breaks.
  Position tracking does not make those characters legal in every syntax mode.
- EOF is the position immediately after the input; empty input ends at 1:1.

A decoding error may be discovered by buffered input ahead of the current token.
Its location identifies where input could no longer be supplied, not necessarily
the precise byte offset of the malformed sequence.

## Inspect warnings, including after a failure

```java
String firstJson = "[1 2]";
String secondJson = "[3,4]";

JsonParser parser = new JsonParser()
        .setCollectWarnings(true);
parser.parse(firstJson);
List<JsonError> firstWarnings = parser.warnings();
assert firstWarnings.size() == 1;
assert firstWarnings.get(0) instanceof JsonError.MissingComma;

parser.parse(secondJson);
assert parser.warnings()
        .isEmpty();
assert firstWarnings.size() == 1; // An immutable snapshot, not a live list.
```

Warnings belong to the last invocation and are reset at its start. If parsing fails,
warnings collected before the failure are still available. Calling
`setCollectWarnings(false)` disables collection on subsequent invocations.

The static `JsonParser.parseString(text, mode, warnings)` API appends to a caller's
list without clearing existing entries, including on failure:

```java
String firstJson = "[1 2]";
String secondJson = "[3 4]";

List<JsonError> warnings = new ArrayList<>();
JsonParser.parseString(
        firstJson,
        JsonParsingMode.EXTENDED,
        warnings);
JsonParser.parseString(
        secondJson,
        JsonParsingMode.EXTENDED,
        warnings);
assert warnings.size() == 2;
```

## Mapping paths are not source positions

```java
String json = "{north:[1,'oops']}";

try {
    Json.parse(json)
            .toJavaObject(new JsonType<Map<String, List<Integer>>>() {});
    throw new AssertionError("The second element must be numeric");
} catch (JsonDataException failure) {
    assert failure.getMessage()
            .contains("$[\"north\"][1]");
}
```

`$` names the root; brackets contain object keys or zero-based array/list indices.
Typed mapping can run long after the source text has been discarded, so its errors
identify the data path rather than a line/column.

Required tree getters report the expected kind and actual element class.
They do not carry a full mapping path or serialize the value into the error message.

## Do not erase the evidence

Returning null from a catch block makes an unreadable file indistinguishable from
a legitimate JSON null. Prefer handling or propagating the failure and retaining
`getCause()`. Input failures have `JsonError.InputFailure`; opening, decoding,
reading and closing causes are retained.

An application may show a friendly message while recording the structured
`JsonError` and cause for diagnosis. Recovery warnings deserve the same deliberate
treatment: success with repairs is different from an untouched valid document.
