<!--
  Copyright (c) 2026 Ivan Kniazkov
-->
# Keep the number you were given

[Guide](README.md) · [Tree access](tree.md) · [Java mapping](java-mapping.md)

An identifier should not change because it was too large for a double, and a price
should not acquire a binary rounding tail just because it passed through JSON.
Finite numeric tokens are read directly into arbitrary-precision Java values.

## Representation

| JSON token | Node | Storage |
| --- | --- | --- |
| `42`, `-7`, JSON5 `0xFF` | `JsonInteger` | `BigInteger` |
| `42.0`, `1e3`, JSON5 `.5` | `JsonReal` | `BigDecimal` |
| JSON5 `NaN` | `JsonNaN` | Special value |
| JSON5 `Infinity`, `-Infinity` | `JsonInfinity` | Special value and sign |

JSON has no prescribed 32-bit or 64-bit integer size. This library's bounds come
from Java's BigInteger/BigDecimal representation and available memory. BigDecimal
has finite scale/exponent bounds; a token outside them fails with `JsonException`.
“Arbitrary precision” does not mean an unlimited exponent.

```java
String integerJson = "9007199254740993";
String priceJson = "0.12345678901234567890";

JsonElement value = Json.parse(integerJson, JsonParsingMode.STRICT);
assert value.numberValueOrThrow()
        .equals(new BigInteger("9007199254740993"));
BigDecimal price = Json.parse(priceJson)
        .decimalValueOrThrow();
assert price.equals(new BigDecimal("0.12345678901234567890"));
```

## Choose a factory input with the precision you intend

```java
JsonNumber count = JsonNumber.of(42L);
JsonNumber huge = JsonNumber.of(new BigInteger("18446744073709551616"));
JsonNumber amount = JsonNumber.of(new BigDecimal("19.99"));
JsonNumber measured = JsonNumber.of(0.25);
assert count instanceof JsonInteger;
assert huge instanceof JsonInteger;
assert amount instanceof JsonReal;
assert measured instanceof JsonReal;
```

`long` and `BigInteger` produce integers. Finite `float`, `double` and
`BigDecimal` produce reals, even for integral values. The `Number` overload
dispatches on Byte, Short, Integer, Long, Float, Double, BigInteger or BigDecimal;
other Number implementations fail with `IllegalArgumentException`.

Double inputs use their canonical decimal representation; float inputs use
`Float.toString` without widening first. Neither can restore precision already lost:

```java
JsonNumber roundedBeforeJson = JsonNumber.of(9007199254740993d); // Bad for an exact ID.
JsonNumber exact = JsonNumber.of(9007199254740993L);            // Good: integer input.
assert !roundedBeforeJson.equals(exact);
JsonNumber exactPrice = JsonNumber.of(new BigDecimal("0.1"));  // Good: decimal text.
assert exactPrice.decimalValueOrThrow()
        .equals(new BigDecimal("0.1"));
```

## Conversion is a choice

| Operation | Rule |
| --- | --- |
| `numberValue()` | Optional containing BigInteger/BigDecimal; boxed Double for special values |
| `toJavaObject()` | Stored BigInteger/BigDecimal; boxed Double for special values |
| `decimalValue()` | Optional containing the exact finite decimal; empty for special values |
| `isInteger()` / `isLongInteger()` | Is mathematically integral and fits the signed 32-/64-bit range |
| `intValue()` / `longValue()` | Truncate toward zero; saturate at range boundaries |
| `doubleValue()` | May round, overflow to infinity or underflow to zero |
| `intValueOrThrow()` / `longValueOrThrow()` | Require exact integral value in range |
| `doubleValueOrThrow()` | Allow rounding, reject non-finite results and nonzero-to-zero underflow |

```java
String json = "12.75";

JsonElement value = Json.parse(json);
assert value.intValue() == 12; // Deliberate loss of the fraction.
try {
    value.intValueOrThrow();
    throw new AssertionError("An exact integer was required");
} catch (JsonDataException expected) {
    assert value.decimalValueOrThrow()
            .equals(new BigDecimal("12.75"));
}
assert JsonNumber.of(13.0)
        .isInteger(); // The predicate describes the value, not its class.
```

Typed Java mapping is strict: it does not use truncation/saturation to make data
fit. Byte, Short, Integer, Long and BigInteger require exact integers; finite
float/double conversion rejects overflow and nonzero-to-zero underflow.

## NaN and infinity

```java
String expectedJson = "Infinity";

JsonNumber missingMeasurement = JsonNumber.of(Double.NaN);
JsonNumber unlimited = new JsonInfinity(false);
assert missingMeasurement.isNaN();
assert unlimited.isPositiveInfinity();
assert unlimited.toString()
        .equals(expectedJson);
assert !unlimited.isFinite();
```

`new JsonInfinity(true)` represents negative infinity. All JsonElement instances
have `isNaN`, `isInfinite`, `isPositiveInfinity`, `isNegativeInfinity` and
`isFinite`; non-number nodes return false for these predicates.

Special values work with `numberValueOrThrow()` and typed Float/Double mapping.
They fail integer/BigDecimal mapping and `doubleValueOrThrow()`, which requires
a finite result. Soft integer access follows Java casts: NaN becomes zero and
infinities saturate. A huge decimal can be finite even when its double conversion is not.

**NaN/infinity output is JSON5, not strict JSON.** The serializer emits their
literals without substituting null. If your consumer requires strict JSON,
reject these values in your model before writing.

## What serialization preserves

Numeric value and integer/real kind are preserved; spelling is not. Reals keep a
decimal point or exponent, including `new BigDecimal("42")` serialized as `42.0`.
Exponents use BigDecimal's textual representation. Hexadecimal numbers become
decimal integers, signed NaN becomes `NaN`, and negative zero loses its sign.
Scale does not affect numeric equality or hashing.
