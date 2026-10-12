<!--
  Copyright (c) 2026 Ivan Kniazkov
-->
# Text, files and the bytes between them

[Guide](README.md) · [Parsing](parsing.md) · [Diagnostics](diagnostics.md)

Keep the simple call for the simple task. Configure an instance when the input or
destination has a particular encoding, diagnostic name or formatting requirement.

## Reusable configuration

| Parser setting | Default | Effect |
| --- | --- | --- |
| `setMode(JsonParsingMode)` | EXTENDED | Accepted syntax |
| `setCharset(Charset)` | UTF-8 | Byte decoding only |
| `setBomPolicy(JsonBomPolicy)` | ALLOW | Initial BOM handling |
| `setSourceName(String)` | null | Override diagnostic name |
| `setCollectWarnings(boolean)` | false | Collect per-call diagnostics |

```java
JsonParser parser = new JsonParser()
        .setMode(JsonParsingMode.JSON5)
        .setCharset(StandardCharsets.UTF_8)
        .setSourceName("observatory.json5")
        .setCollectWarnings(true);
JsonObject first = parser.parse("{target:'M31'}").objectValueOrThrow();
JsonObject second = parser.parse("{target:'M42'}").objectValueOrThrow();
assert first.element("target").orElseThrow().stringValueOrThrow().equals("M31");
assert second.element("target").orElseThrow().stringValueOrThrow().equals("M42");
```

Setters return the same instance. Parser and serializer instances are reusable but
**not thread-safe**. Each parse has its own internal session. Settings are validated
by setters; data errors appear when parsing/writing.

Parser inputs are String, Reader, InputStream and File. With no source-name override,
a File uses its supplied path; strings and borrowed streams are unnamed.
`setSourceName(null)` restores that behavior.

| Serializer setting | Default | Effect |
| --- | --- | --- |
| `setIndentation(String)` | null | Compact output; non-null selects line breaks |
| `setCharset(Charset)` | UTF-8 | Byte encoding only |
| `setWriteBom(boolean)` | false | Emit a byte-order mark |
| `setEscapeNonAscii(boolean)` | false | Escape non-ASCII in keys and string values |

```java
JsonSerializer writer = new JsonSerializer().setIndentation("  ");
JsonElement tree = Json.parse("{station:{name:'Hilltop'},targets:['M31','M42']}");
String text = writer.serialize(tree);
assert Json.parse(text).equals(tree);
```

Indentation may contain only JSON whitespace: space, tab, CR or LF.
An empty string gives line breaks without indentation; null gives single-line output.
Compact output uses spaces after commas and colons; it is not whitespace-minified.
Small containers may remain inline. To keep a conventional layout, use spaces or tabs.

## Files and ownership

```java
Path path = Files.createTempFile("observatory-", ".json");
try {
    File file = path.toFile();
    new JsonSerializer().setIndentation("  ").serialize(Map.of("target", "M31"), file);
    JsonObject value = new JsonParser().setMode(JsonParsingMode.STRICT)
            .parse(file).objectValueOrThrow();
    assert value.element("target").orElseThrow().stringValueOrThrow().equals("M31");
} finally {
    Files.deleteIfExists(path);
}
```

The library closes files it opens, on success and failure. It never closes your
Reader, Writer, InputStream or OutputStream. Successful output flushes the borrowed
destination; the caller controls its lifetime.

File/read/decode failures become `JsonException` with their original cause.
Output/encoding failures are `IOException`, including `CharacterCodingException`.
A missing file is not the same as a document containing JSON null.

## Charset selection

UTF-8 is independent of the JVM's default encoding. Choose any Java charset that
provides the required decoder/encoder:

```java
Charset cp1251 = Charset.forName("windows-1251");
ByteArrayOutputStream bytes = new ByteArrayOutputStream();
new JsonSerializer().setCharset(cp1251).serialize(Map.of("город", "Москва"), bytes);
JsonObject value = new JsonParser().setCharset(cp1251)
        .setMode(JsonParsingMode.STRICT)
        .parse(new ByteArrayInputStream(bytes.toByteArray())).objectValueOrThrow();
assert value.element("город").orElseThrow().stringValueOrThrow().equals("Москва");
```

Charset settings apply only to bytes. They do not reinterpret a String or change
how a caller-supplied Reader/Writer encodes text. Library encoders/decoders report
malformed or unmappable data rather than inserting replacement characters.
An upstream Reader may already have replaced bad bytes; the parser cannot undo that.

## BOM policies

| Policy | Initial marker |
| --- | --- |
| ALLOW | Accept zero or one |
| FORBID | Reject a marker |
| REQUIRE | Require one |

For bytes, UTF-8/UTF-16/UTF-32 signatures are inspected before decoding. A BOM must
match the selected charset; there is no general encoding autodetection. Generic
UTF-16/UTF-32 choose byte order from a matching marker, or big endian without one.
Explicit LE/BE charsets reject an opposite-endian marker. Duplicate initial markers
fail in every syntax mode.

```java
ByteArrayOutputStream bytes = new ByteArrayOutputStream();
new JsonSerializer().setCharset(StandardCharsets.UTF_16LE).setWriteBom(true)
        .serialize(List.of(1, 2), bytes);
JsonElement value = new JsonParser().setCharset(StandardCharsets.UTF_16LE)
        .setBomPolicy(JsonBomPolicy.REQUIRE)
        .parse(new ByteArrayInputStream(bytes.toByteArray()));
assert value.toString().equals("[1, 2]");
```

For String/Reader input the policy applies to the first U+FEFF character. A Reader
that already consumed a byte BOM cannot supply it for REQUIRE. An accepted marker
is removed before location tracking, so the first document character is at 1:1.
Later U+FEFF follows the syntax mode's whitespace rules; inside strings it is data.

`setWriteBom(true)` emits exactly one byte marker, including with JDK charset
aliases that normally emit their own. It has no effect on String/Writer output.
Generic UTF-16/UTF-32 output uses big endian. Unsupported BOM encodings, such as
Windows-1251, are rejected regardless of setter order.

## Unicode escaping

Escaping makes string/key content representable in an ASCII destination:

```java
JsonSerializer writer = new JsonSerializer()
        .setCharset(StandardCharsets.US_ASCII)
        .setEscapeNonAscii(true);
ByteArrayOutputStream bytes = new ByteArrayOutputStream();
writer.serialize(Map.of("target", "Луна 😀"), bytes);
String ascii = bytes.toString(StandardCharsets.US_ASCII);
assert ascii.chars().allMatch(ch -> ch < 128);
assert Json.parse(ascii).objectValueOrThrow().element("target").orElseThrow()
        .stringValueOrThrow().equals("Луна 😀");
```

Escapes operate on UTF-16 code units, including surrogate pairs for supplementary
characters. No Unicode normalization is performed. Unpaired surrogates fail raw
byte encoding unless escaped; escaping preserves code units, not Unicode validity.
The option does not escape indentation.

## Memory and failure boundaries

Parsing is buffered and sequential, without first loading a whole file into a
String. The resulting tree and individual token values still occupy memory.
Buffering can consume beyond a value; this is not a resumable multi-document stream API.

Serialization builds the text in memory. For byte output it fully encodes that text
before opening/truncating a file or writing to a borrowed stream. An encoding failure
therefore preserves an existing file. Later write/flush failures may leave partial
output: file writing is not atomic replacement.
