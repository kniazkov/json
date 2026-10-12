<!--
  Copyright (c) 2026 Ivan Kniazkov
-->
# Build with confidence

[Guide](README.md) · [Public API](public-api.md)

Build requirements are **Java 21+** and **Maven 3.6.3+**.
The artifact coordinates are `com.kniazkov:json:2.0.0`.

## Build and test

```sh
mvn -B -ntp -DargLine=-Xss256k clean verify
```

This checks style, runs tests, and produces the binary, source and Javadoc JARs.
GPG signing and Central upload are enabled only by the `release` profile.
See [Publishing to Maven Central](publishing.md) for release preparation and upload.
The small stack exercises the iterative traversal paths; it is not a runtime
requirement for library users.

Tests cover all three syntax modes, error recovery, exact and special numbers,
mapping, collection contracts, encodings, BOMs, cycles and deep documents.
The 114 JSON5 parser fixtures under `src/test/resources/json5-suite` include pinned
upstream provenance and their own license.

## Code style

The Maven `validate` phase checks handwritten Java source and tests:

- Spotless **2.43.0** with google-java-format **1.24.0**, AOSP style: deterministic
  layout, four-space indentation, imports, whitespace and final newlines.
- Checkstyle **10.21.4** through Maven Checkstyle Plugin **3.6.0**: naming, imports,
  braces, one declaration/statement per line, modifiers, array syntax, fall-through
  and equals/hashCode pairing. Lines are limited to 120 characters except
  package/import declarations and lines containing URLs.
- Public API documentation is required. Javadoc checks cover parameters, type
  parameters, results, explicitly thrown exceptions, tag order, descriptions,
  sentence endings, paragraphs and HTML. Overrides may inherit documentation.
- Javadoc uses `doclint=all`; both errors and warnings fail the build, including
  unresolved references.

Versions live in `pom.xml`; rules live in
[`config/checkstyle.xml`](../config/checkstyle.xml). The narrow
[test suppression](../config/checkstyle-suppressions.xml) exempts test fixtures from
mandatory API comments, not from checking comments that are present. JUnit methods
may use their annotation instead of Javadoc. There is no ignored-violation baseline.
Third-party fixtures and generated files are outside the formatter's source roots.

## Check without changing files

```sh
mvn -B -ntp validate
mvn -B -ntp validate javadoc:javadoc
```

The second command also generates API documentation under `target`.
Formatting is an explicit separate action:

```sh
mvn -B -ntp spotless:apply
```

Keep Java copyright headers in three-line form:

```text
/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
```

Preserve the applicable year when editing an existing file.

## Test the checks themselves

```sh
python3 scripts/check-style-gate.py
```

The script makes isolated project copies containing four deliberate regressions:
bad formatting, a wildcard import in test code, a missing Javadoc parameter tag,
and a link to a nonexistent type. Each must fail with its specific diagnostic.
An unrelated build failure does not count as a successful negative test.
Use the `MAVEN` environment variable if Maven is not named `mvn` on PATH.

## Documentation examples

```sh
mvn -B -ntp package
python3 scripts/check-doc-examples.py
```

The documentation checker verifies local links and compiles/runs every Java code
block in the README and guide as an independent method-body example with assertions
enabled. It checks JSON/JSON5 blocks in their declared syntax modes as well.
Use `JAVA_HOME` to choose the JDK. Examples that demonstrate errors must catch and
assert the expected error so that a failing example cannot pass accidentally.

## CI and contributions

GitHub Actions runs style/Javadoc checks and the full build on pull requests and
pushes to master. It also exercises the negative style probes and documentation
examples. Ordinary checks do not rewrite tracked files.

Keep mechanical formatting changes separate from behavior changes. A useful
contribution includes an example or regression test showing the intended result,
and updates the relevant guide chapter when a public contract changes.
