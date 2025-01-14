/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests covering {@link JsonParser} class.
 */
public class JsonParserTest {
    @Test
    public void zero() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("0");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertTrue(elem.isInteger());
        Assert.assertEquals(0, elem.getIntValue());
    }

    @Test
    public void integer() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("  13");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertTrue(elem.isInteger());
        Assert.assertEquals(13, elem.getIntValue());
    }

    @Test
    public void longInteger() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("666000000000");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertFalse(elem.isInteger());
        Assert.assertTrue(elem.isLongInteger());
        Assert.assertEquals(666000000000L, elem.getLongValue());
    }

    @Test
    public void negativeInteger() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("-13");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertTrue(elem.isInteger());
        Assert.assertEquals(-13, elem.getIntValue());
    }

    @Test
    public void positiveInteger() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("+13");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertTrue(elem.isInteger());
        Assert.assertEquals(13, elem.getIntValue());

        JsonError error = null;
        try {
            JsonParser.parseString("+13", JsonParsingMode.STRICT);
        } catch (JsonException exception) {
            error = exception.getError();
        }
        Assert.assertTrue(error instanceof JsonError.InvalidCharacter);
        String message = error.getMessage();
        Assert.assertEquals("Invalid character: '+'", message);
    }

    @Test
    public void integerStartedFromZero() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("00013");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertTrue(elem.isInteger());
        Assert.assertEquals(13, elem.getIntValue());
    }

    @Test
    public void realNumber() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("13.001");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertFalse(elem.isInteger());
        Assert.assertTrue(elem.isNumber());
        Assert.assertEquals(13.001, elem.getDoubleValue(), 0);
    }

    @Test
    public void negativeRealNumber() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("-13.001");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertFalse(elem.isInteger());
        Assert.assertTrue(elem.isNumber());
        Assert.assertEquals(-13.001, elem.getDoubleValue(), 0);
    }

    @Test
    public void zeroPointFive() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("0.5");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertEquals(0.5, elem.getDoubleValue(), 0);

        elem = null;
        try {
            elem = JsonParser.parseString(".5");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertEquals(0.5, elem.getDoubleValue(), 0);

        elem = null;
        try {
            elem = JsonParser.parseString("-.5");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertEquals(-0.5, elem.getDoubleValue(), 0);

        JsonError error = null;
        try {
            JsonParser.parseString(".5", JsonParsingMode.STRICT);
        } catch (JsonException exception) {
            error = exception.getError();
        }
        Assert.assertTrue(error instanceof JsonError.InvalidCharacter);
    }

    @Test
    public void trailingPoint() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("13.");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertTrue(elem.isInteger());
        Assert.assertEquals(13, elem.getIntValue());

        JsonError error = null;
        try {
            JsonParser.parseString("13.", JsonParsingMode.STRICT);
        } catch (JsonException exception) {
            error = exception.getError();
        }
        Assert.assertTrue(error instanceof JsonError.ExpectedNumberAfterPoint);

        error = null;
        try {
            JsonParser.parseString(" . ");
        } catch (JsonException exception) {
            error = exception.getError();
        }
        Assert.assertTrue(error instanceof JsonError.IncorrectNumberFormat);
    }

    @Test
    public void hexNumber() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("0xFe");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertTrue(elem.isInteger());
        Assert.assertEquals(254, elem.getIntValue());

        JsonError error = null;
        try {
            JsonParser.parseString("0xFe", JsonParsingMode.STRICT);
        } catch (JsonException exception) {
            error = exception.getError();
        }
        Assert.assertTrue(error instanceof JsonError.InvalidCharacter);
        String message = error.getMessage();
        Assert.assertEquals("Invalid character: 'x'", message);

        error = null;
        try {
            JsonParser.parseString("0xZ1");
        } catch (JsonException exception) {
            error = exception.getError();
        }
        Assert.assertTrue(error instanceof JsonError.ExpectedHexDigit);
    }

    @Test
    public void numberWithExponent() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("1e3");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertEquals(1000, elem.getDoubleValue(), 0);

        elem = null;
        try {
            elem = JsonParser.parseString("1.024e3");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertEquals(1024, elem.getDoubleValue(), 0);

        elem = null;
        try {
            elem = JsonParser.parseString("1.024e+3");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertEquals(1024, elem.getDoubleValue(), 0);

        elem = null;
        try {
            elem = JsonParser.parseString("1.0E-3");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertEquals(0.001, elem.getDoubleValue(), 0);

        elem = null;
        try {
            elem = JsonParser.parseString("5.0e100");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertEquals(5e100, elem.getDoubleValue(), 0);
        Assert.assertEquals(elem.toString(), "5.0E100");

        JsonError error = null;
        try {
            JsonParser.parseString("1e");
        } catch (JsonException exception) {
            error = exception.getError();
        }
        Assert.assertTrue(error instanceof JsonError.IncorrectExponentNotation);
    }

    @Test
    public void emptyArray() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("[ ]");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        JsonArray array = elem.toJsonArray();
        Assert.assertNotNull(array);
        Assert.assertEquals(0, array.size());
    }

    @Test
    public void arrayOfIntegers() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("[1, 2, 3]");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        JsonArray array = elem.toJsonArray();
        Assert.assertNotNull(array);
        Assert.assertEquals(3, array.size());
        Assert.assertEquals("[1, 2, 3]", array.toString());
    }

    @Test
    public void arrayContainingOtherArrays() {
        Assert.assertTrue(commonTest("[1, [2, 3], []]"));
    }

    @Test
    public void arrayWithExtraComma() {
        Assert.assertTrue(commonTestErrorIfStrict(
                "[1, 2,]",
                "[1, 2]",
                JsonError.ExpectedElementAfterComma.class,
                1,
                7
        ));
    }

    @Test
    public void nullElement() {
        Assert.assertTrue(commonTest("null"));
    }

    @Test
    public void trueElement() {
        Assert.assertTrue(commonTest("true"));
    }

    @Test
    public void falseElement() {
        Assert.assertTrue(commonTest("false"));
    }

    @Test
    public void simpleString() {
        Assert.assertTrue(commonTest("\"hello\""));
    }

    @Test
    public void stringWithEntities() {
        Assert.assertTrue(commonTest("\"test\\n\\rtest\\u0001\""));
    }

    @Test
    public void stringInSingleQuotes() {
        Assert.assertTrue(commonTestErrorIfStrict(
                "'hello'",
                "\"hello\"",
                JsonError.InvalidCharacter.class,
                1,
                1
        ));
    }

    @Test
    public void stringWithIncorrectSequence() {
        JsonError error = null;
        try {
            JsonParser.parseString("\"abc\\d\"");
        } catch (JsonException exception) {
            error = exception.getError();
        }
        Assert.assertTrue(error instanceof JsonError.IncorrectStringSequence);
        Assert.assertEquals("Incorrect string sequence: '\\d'", error.getMessage());
    }

    @Test
    public void stringWithIncorrectHexadecimalSequence() {
        JsonError error = null;
        try {
            JsonParser.parseString("\"abc\\ujh\"");
        } catch (JsonException exception) {
            error = exception.getError();
        }
        Assert.assertTrue(error instanceof JsonError.IncorrectStringSequence);
        Assert.assertEquals("Incorrect string sequence: '\\ujh\"?'", error.getMessage());
    }

    @Test
    public void notClosedString() {
        Assert.assertTrue(commonTestError(
                "\n  \"test",
                JsonError.UnclosedString.class,
                2,
                3
        ));
    }

    @Test
    public void lineBreak() {
        Assert.assertTrue(commonTestErrorIfStrict(
                "\"line \\\nbreak\"",
                "\"line break\"",
                JsonError.IncorrectStringSequence.class,
                1,
                8
        ));
        Assert.assertTrue(commonTestErrorIfStrict(
                "\"line \\        \nbreak\"",
                "\"line break\"",
                JsonError.IncorrectStringSequence.class,
                1,
                8
        ));
    }

    @Test
    public void emptyObject() {
        boolean oops = false;
        JsonElement elem = null;
        try {
            elem = JsonParser.parseString("{ }");
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        JsonObject obj = elem.toJsonObject();
        Assert.assertNotNull(obj);
        Assert.assertEquals(0, obj.size());
    }

    @Test
    public void objectWithData() {
        Assert.assertTrue(commonTest("{\"one\": 1, \"two\": 2, \"nestedObject\": {\"data\": \"test\"}}"));
    }

    @Test
    public void objectWithExtraComma() {
        Assert.assertTrue(commonTestErrorIfStrict(
                "{\"data\":\"test\",}",
                "{\"data\": \"test\"}",
                JsonError.ExpectedPairAfterComma.class,
                1,
                16
        ));
    }

    @Test
    public void objectWithIdentifiersAsKeys() {
        Assert.assertTrue(commonTestErrorIfStrict(
                "{data0:\"test\"}",
                "{\"data0\": \"test\"}",
                JsonError.ExpectedKey.class,
                1,
                2
        ));
    }

    @Test
    public void missingComma() {
        Assert.assertTrue(commonTestErrorIfStrict(
                "[{\"x\": 1, \"y\": 2} {\"x\": 3, \"y\": 4}]",
                "[{\"x\": 1, \"y\": 2}, {\"x\": 3, \"y\": 4}]",
                JsonError.MissingComma.class,
                1,
                19
        ));
    }

    @Test
    public void unexpectedIdentifier() {
        Assert.assertTrue(commonTestError(
                "hello.txt",
                JsonError.UnexpectedToken.class,
                1,
                1
        ));
    }

    @Test
    public void listOfWarnings() {
        boolean oops = false;
        JsonElement elem = null;
        List<JsonError> warnings = new ArrayList<>();
        try {
            elem = JsonParser.parseString("[{\"x\": 1, \"y\": 2} {\"x\": 3, \"y\": 4}]", warnings);
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(elem);
        Assert.assertEquals(warnings.size(), 1);
        JsonError warning = warnings.get(0);
        Assert.assertTrue(warning instanceof JsonError.MissingComma);
    }

    /**
     * A common test for JSON parser. First parses a JSON document,
     * then converts the resulting element into a string, the results should match.
     * @param json JSON document
     * @return Testing result ({@code true} = success)
     */
    private static boolean commonTest(String json) {
        try {
            JsonElement elem = JsonParser.parseString(json);
            String json2 = elem.toString();
            return json.equals(json2);
        } catch (JsonException ignored) {
        }
        return false;
    }

    /**
     * A common test for JSON parser where expected an error.
     * @param json JSON document
     * @param errorType Type of object representing an error
     * @param row Expected row where error should be
     * @param column Expected column where error should be
     * @return Testing result ({@code true} = success)
     */
    private static boolean commonTestError(String json, Class<? extends JsonError> errorType,
                                           int row, int column) {
        JsonError error = null;
        try {
            JsonParser.parseString(json);
        } catch (JsonException exception) {
            error = exception.getError();
        }
        if (error != null) {
            JsonLocation loc = error.getLocation();
            return errorType.isInstance(error) &&
                    loc.getRow() == row && loc.getColumn() == column;
        }
        return false;
    }

    /**
     * A common test for JSON parser where expected an error if parsed
     *   in STRICT mode, and success otherwise
     * @param json JSON document
     * @param json2 expected JSON document after transformation
     * @param errorType Type of object representing an error
     * @param row Expected row where error should be
     * @param column Expected column where error should be
     * @return Testing result ({@code true} = success)
     */
    private static boolean commonTestErrorIfStrict(String json, String json2,
               Class<? extends JsonError> errorType,
               int row, int column) {
        try {
            JsonElement elem = JsonParser.parseString(json);
            String str = elem.toString();
            if (!str.equals(json2)) {
                return false;
            }
        } catch (JsonException ignored) {
            return false;
        }

        JsonError error = null;
        try {
            JsonParser.parseString(json, JsonParsingMode.STRICT);
        } catch (JsonException exception) {
            error = exception.getError();
        }
        if (error != null) {
            JsonLocation loc = error.getLocation();
            return errorType.isInstance(error) &&
                    loc.getRow() == row && loc.getColumn() == column;
        }
        return false;
    }
}
