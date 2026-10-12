<!--
  Copyright (c) 2026 Ivan Kniazkov
-->
# The user guide

A JSON document can be a message from another service, a file edited by a person,
or a piece of data your program is building. Those jobs need different degrees of
strictness. This library lets you choose the right one without changing your data model.

## A useful reading order

1. [Getting started](getting-started.md): a complete trip from text to tree to Java.
2. [Parsing](parsing.md): standards, mode selection and valid syntax.
3. [Recovery](recovery.md): damaged inputs, exact repairs and hard failures.
4. [The tree](tree.md): navigation, mutation, ownership and collection contracts.
5. [Numbers](numbers.md): exact storage and deliberate conversions.
6. [Java mapping](java-mapping.md): POJOs, arrays, maps, generics and field policies.
7. [Input and output](io.md): reusable configuration, files, charsets and Unicode.
8. [Diagnostics](diagnostics.md): warnings, exceptions, locations and causes.
9. [Public API](public-api.md): class diagrams and a compact reference.
10. [Development](development.md): builds, examples, style checks and tests.
11. [Publishing](publishing.md): signed releases and Maven Central.

## Three decisions, not dozens

| Decision | A practical starting point |
| --- | --- |
| How strict is the input contract? | `STRICT` for interchange; `JSON5` for hand-written configuration; `EXTENDED` for controlled recovery |
| What do you want to work with? | A `JsonElement` tree for exploration/editing, or a Java target type for application data |
| Where are the bytes going? | UTF-8 by default; configure `JsonParser`/`JsonSerializer` for other encodings |

Recovery does not make a string into an integer or a fraction into a whole number.
**Syntax parsing and Java type conversion are separate operations.** A document may
parse successfully and still be unsuitable for the Java type you requested.

## Reading the examples

Java examples are independent method-body fragments. Put them in a method declaring
`throws Exception`, or handle the narrower exceptions described in each chapter.
Import library classes from `com.kniazkov.json`; standard classes such as
`BigDecimal`, `List`, `Map` and `StandardCharsets` come from their usual Java packages.
Local classes keep examples self-contained; application models can instead be
top-level classes or static nested classes.

Assertions state the expected result; run Java with `-ea` to enable them.
Examples explicitly marked as bad input either catch the expected exception or state
the recovery result. JSON examples are separate documents, not Java string literals.

[Back to the project](../README.md)
