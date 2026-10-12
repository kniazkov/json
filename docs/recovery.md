<!--
  Copyright (c) 2026 Ivan Kniazkov
-->
# Recover a document without pretending nothing happened

[Guide](README.md) · [Parsing modes](parsing.md) · [Diagnostics](diagnostics.md)

`EXTENDED` is useful when accepting a damaged document is a deliberate product
decision. Turn on warnings, inspect the repair, then decide whether to keep it.
The parser does not invent values to fill holes or guess a missing object key.

## See the repaired tree and its warnings

```java
String secondJson = "[1,,2 3] trailing note";
String expectedJson = "[1, 2, 3]";

JsonParser parser = new JsonParser()
        .setMode(JsonParsingMode.EXTENDED)
        .setSourceName("import.firstJson")
        .setCollectWarnings(true);
JsonElement value = parser.parse(secondJson);
assert value.toString()
        .equals(expectedJson);
assert parser.warnings()
        .size() == 3;
for (JsonError warning : parser.warnings()) {
    System.out.println(warning.location() + ": " + warning.message());
}
```

The repairs are: skip an absent array element, insert a missing comma, and discard
the suffix. Without warning collection, the same repairs occur silently.

## Repair gallery

These inputs fail in STRICT and JSON5 unless the row explicitly says otherwise.

| Damaged input | EXTENDED result | Diagnostic |
| --- | --- | --- |
| `[1 2]` | `[1,2]` | `MissingComma` |
| `{"a":1 "b":2}` | `{"a":1,"b":2}` | `MissingComma` |
| `[1,,2]` | `[1,2]` | One `MissingArrayElement` |
| `[1,,,2]` | `[1,2]` | Two `MissingArrayElement` warnings |
| `[,1]` | `[1]` | One `MissingArrayElement` |
| `[1,,]` | `[1]` | One `MissingArrayElement` |
| `[,]` / `[,,]` | `[]` | One / two `MissingArrayElement` warnings |
| `[` / `{` | `[]` / `{}` | `MissingClosingBracket` |
| `[1` / `[1,` | `[1]` | `MissingClosingBracket` |
| `{"a":1` / `{"a":1,` | `{"a":1}` | `MissingClosingBracket` |
| `{"a":[1]` | `{"a":[1]}` | `MissingClosingBracket` |
| `[{"a":1]` | `[{"a":1}]` | Close the object before its parent's `]` |
| `{"a":[1}` | `{"a":[1]}` | Close the array before its parent's `}` |
| `{"a":1} discarded text` | `{"a":1}` | `TrailingContent` |
| `[1,]` | `[1]` | No warning; also valid JSON5 |

A missing array element does **not** become `null`. If positions matter, use
explicit nulls and a stricter syntax mode:

```java
String json = "[1,null,2]";

JsonArray positions = Json.parse(json, JsonParsingMode.STRICT)
        .arrayValueOrThrow();
assert positions.size() == 3;
assert positions.element(1)
        .isNull();
```

## Where recovery stops

| Input | Why it fails even in EXTENDED |
| --- | --- |
| `{"a":[1` | More than the root container remains open at EOF |
| `[[` | Closing two unfinished containers at EOF would be required |
| `{"a" 1}` | Missing colon |
| `{"a":}` | Missing value |
| `[{"a":]` | A parent bracket cannot supply an unfinished value |
| `{"a":1,,"b":2}` | Missing object member, not an empty array slot |
| `{"a":1]` / `[1}` | No ancestor owns the mismatched bracket |
| `"unfinished` | An unfinished string |
| `42e+` | An unfinished number, not “42 plus a suffix” |
| `garbage {"a":1}` | Leading garbage is not skipped |
| Only whitespace | No root value |

```java
String json = "{\"a\":[1";

JsonParser parser = new JsonParser()
        .setCollectWarnings(true);
try {
    parser.parse(json);
    throw new AssertionError("Nested EOF recovery is not allowed");
} catch (JsonException expected) {
    assert expected.error() instanceof JsonError.UnexpectedEnd;
}
```

## The rules behind the examples

1. Insert a comma only after a complete value, before a token that can begin the
   next array value or object key. The next object member must still have a colon
   and a complete value.
2. At EOF, insert only the root container's closing bracket. Every nested container
   must already be closed. A root may end when empty, after a value, or after a comma.
3. In an array, skip each absent element represented by a comma encountered while
   expecting a value. Emit a warning for each one.
4. A closing bracket belonging to an ancestor can close intervening containers
   only when each is in a closable state. Retain the bracket for its owner and warn
   for each inserted closer. This can unwind multiple levels; it does not relax
   the one-closer rule at EOF.
5. Never repair incomplete keys, values or lexical tokens by discarding them.

Warnings identify the next token for a missing comma, the empty slot's comma, or
the closing position for an inserted bracket. They are collected in repair order.

## Whitespace and suffixes

At root boundaries, EXTENDED silently accepts characters recognized by Java
`Character.isWhitespace` or `Character.isSpaceChar`, plus NEL (U+0085) and BOM
(U+FEFF), subject to the separate initial-BOM policy. NUL and U+200B are not padding.
This broad rule does not turn whitespace inside strings into separators.

Once the root has been recognized and trailing padding skipped, **any remaining
suffix is discarded with one `TrailingContent` warning**, at its first character.
That includes another JSON value, incomplete text and comment-looking text.
The suffix is still read: decoding and I/O failures remain fatal.

EXTENDED also accepts leading-zero numbers and literal line breaks in strings.
It rejects raw NUL in strings but accepts escaped NUL. Its permissive line
continuation accepts spaces after a backslash before the newline. For portable
hand-written configuration, prefer the narrower JSON5 syntax.
