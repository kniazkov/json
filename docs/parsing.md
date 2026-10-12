<!--
  Copyright (c) 2026 Ivan Kniazkov
-->
# Choose the language your input is allowed to speak

[Guide](README.md) · [Recovery](recovery.md) · [I/O](io.md)

## Standards and modes

| Mode | Contract | Useful for |
| --- | --- | --- |
| `STRICT` | JSON syntax: [ECMA-404, 2nd edition, December 2017](https://ecma-international.org/publications-and-standards/standards/ecma-404/) and [RFC 8259, December 2017](https://www.rfc-editor.org/rfc/rfc8259) | Messages with a strict producer/consumer contract |
| `JSON5` | [JSON5 1.0.0, March 2018](https://spec.json5.org/) | Configuration that people edit |
| `EXTENDED` | JSON5 token syntax plus documented library-specific recovery | Import tools that deliberately accept repairs |

These are syntax contracts, not a promise to preserve every spelling or to validate
an application schema. The default is `EXTENDED`. Warnings are not collected unless
you request them.

```java
JsonElement strict = Json.parse("{\"ready\":true}", JsonParsingMode.STRICT);
JsonElement json5 = Json.parse("{ready:true,}", JsonParsingMode.JSON5);
assert strict.equals(json5);
```

All modes accept any JSON value at the root: an object, array, string, number,
boolean or null. Empty input is not a document. STRICT and JSON5 require exactly
one root followed only by their permitted whitespace/comments.

## Ordinary JSON

Use quoted member names, double-quoted strings, commas between values and complete
closing brackets. Numbers use decimal notation; a fraction and an exponent each
need digits.

```json
{
  "message": "Line one\nLine two",
  "distance": 1.25e6,
  "active": true,
  "note": null,
  "samples": [0, -12, 2.5]
}
```

These are **not** strict JSON:

| Input | Why STRICT rejects it |
| --- | --- |
| `{name: "Vega"}` | An unquoted key |
| `{"name": 'Vega'}` | A single-quoted string |
| `[1,2,]` | A trailing comma |
| `/* note */ 42` | A comment |
| `+1`, `.5`, `1.`, `0x10` | Non-JSON numeric notation |
| `01` | A leading zero |
| `1e+` | An incomplete exponent |
| `NaN`, `Infinity` | Non-finite numbers |
| `true false` | Two root values |

Whitespace is space, tab, CR or LF. Strings reject unescaped U+0000–U+001F,
including literal tabs and line breaks. Legal escapes are `\"`, `\\`, `\/`,
`\b`, `\f`, `\n`, `\r`, `\t` and `\uXXXX` with four hexadecimal digits.
An apostrophe inside a double-quoted string needs no escape; `\'` is not a JSON escape.

```java
try {
    Json.parse("[1,2,]", JsonParsingMode.STRICT);
    throw new AssertionError("A trailing comma must fail");
} catch (JsonException expected) {
    assert expected.error().location().row() == 1;
}
assert Json.parse("\"line\\nbreak\"", JsonParsingMode.STRICT)
        .stringValueOrThrow().equals("line\nbreak");
```

## JSON5 for hand-written files

```json5
{
  // A short observing plan
  target: 'Vega',
  retries: +3,
  threshold: .5,
  mask: 0xFF,
  calibration: [1., 2.,],
}
```

The implementation accepts:

- `//` line comments and closed `/* ... */` comments between tokens. Comments do not nest.
- Single- or double-quoted strings; unquoted IdentifierName keys, including Unicode.
- A single trailing comma in either container.
- Leading plus, leading/trailing decimal points, hexadecimal integers and signed
  `NaN`/`Infinity`.
- JSON5 whitespace, including vertical tab, form feed, Unicode space separators,
  U+FEFF, U+2028 and U+2029.

Unquoted keys are identifiers, not arbitrary words. Quote keys such as `"exposure-ms"`,
`"1"` or `"+gain"`. Reserved words can be keys. Identifier Unicode categories follow
the Java runtime; escaped characters must still be valid in their identifier position.
Escaped spellings of keywords cannot be used as literal values.

JSON5 strings also support `\xNN`, `\v`, and `\0` when no decimal digit follows.
An escaped non-digit such as `\q` yields `q`; `\1`–`\9` are rejected.
A backslash immediately followed by LF, CR, CRLF, U+2028 or U+2029 continues a
string without adding a newline. Literal LF/CR inside a string fail. Literal
U+2028/U+2029 are accepted with `UnescapedLineSeparator` warnings when collected;
the serializer escapes them.

Leading-zero decimal integers and octal/binary notation are not JSON5:
`01`, `0o10`, `0b10` fail. NEL (U+0085) is not JSON5 whitespace.

## Implementation boundaries worth knowing

- Object members should have unique names. The tree stores one value per key;
  repeated names overwrite the earlier value, including in STRICT. Do not use
  duplicates when agreement between different JSON implementations matters.
- Initial BOM handling is a separate [input policy](io.md#bom-policies). The default
  allows one matching BOM, including in STRICT; `FORBID` rejects it.
- Finite numbers use Java arbitrary-precision types, subject to their representable
  sizes and available memory. See [numbers](numbers.md) for exponent/scale limits.
- Unicode escapes preserve UTF-16 code units; parsing is not a Unicode normalization
  or surrogate-pair validation pass.
- The writer quotes keys and strings and removes comments. Non-finite numeric nodes
  serialize as JSON5 literals; there is no “strict output” switch that replaces them.

For recoverable malformed syntax and trailing garbage, see [EXTENDED recovery](recovery.md).
