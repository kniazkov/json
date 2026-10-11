/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

public class SourceTest {
    @Test
    public void nulAndAllUtf16UnitsAreDistinctFromEof() throws Exception {
        Source source = new Source("\0\uffff\ud83d\ude00");
        int[] units = {0, 65535, 0xd83d, 0xde00};
        for (int i = 0; i < units.length; i++) {
            Assert.assertEquals(units[i], source.charValue());
            Assert.assertEquals(units[i], source.charValue());
            Assert.assertEquals(i + 1, source.location().column());
            source.nextChar();
        }
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(-1, source.charValue());
            Assert.assertEquals(-1, source.nextChar());
            Assert.assertEquals("1:5", source.location().toString());
        }
        Source empty = new Source("");
        Assert.assertEquals(-1, empty.charValue());
        Assert.assertEquals(-1, empty.nextChar());
        Assert.assertEquals("1:1", empty.location().toString());
    }

    @Test
    public void coordinatesDoNotDependOnPeeking() throws Exception {
        String text = "a\r\nb\rc\nd\u2028e\u2029\t\ud83d\ude00";
        String[] positions = {"1:1", "1:2", "2:1", "2:1", "2:2", "3:1", "3:2",
            "4:1", "4:2", "5:1", "5:2", "6:1", "6:2", "6:3", "6:4"};
        Source peeked = new Source(text);
        Source advanced = new Source(text);
        for (String position : positions) {
            Assert.assertEquals(position, peeked.location().toString());
            Assert.assertEquals(position, advanced.location().toString());
            peeked.charValue();
            peeked.charValue();
            peeked.nextChar();
            advanced.nextChar();
        }
        Assert.assertEquals(-1, advanced.charValue());
        for (String ending : new String[] {"\r", "\n", "\r\n", "\u2028", "\u2029"}) {
            Source source = new Source(ending);
            while (source.nextChar() != -1) { }
            Assert.assertEquals("2:1", source.location().toString());
        }
    }

    @Test
    public void locationsAreNamedImmutableSnapshots() throws Exception {
        Source source = new Source(new StringReader("a\nb"), "source.json");
        JsonLocation saved = source.location();
        source.nextChar();
        source.nextChar();
        Assert.assertEquals("source.json, 1:1", saved.toString());
        Assert.assertEquals(Optional.of("source.json"), saved.sourceName());
        Assert.assertEquals("source.json, 2:1", source.location().toString());
        Assert.assertEquals(Optional.empty(), new Source("").location().sourceName());
    }

    @Test
    public void readFailureIsNotEofAndRetainsCauseAndPosition() throws Exception {
        IOException cause = new IOException("broken reader");
        Reader reader = new Reader() {
            int reads;
            @Override public int read(char[] data, int off, int len) throws IOException {
                if (reads++ == 0) {
                    data[off] = 'a';
                    return 1;
                }
                throw cause;
            }
            @Override public void close() { Assert.fail("Source must not close its reader"); }
        };
        Source source = new Source(reader, "response");
        JsonException error = Assert.assertThrows(JsonException.class, source::nextChar);
        Assert.assertSame(cause, error.getCause());
        Assert.assertTrue(error.error() instanceof JsonError.InputFailure);
        Assert.assertEquals("response, 1:2", error.error().location().toString());
        Assert.assertSame(error, Assert.assertThrows(JsonException.class, source::nextChar));
        Assert.assertSame(error, Assert.assertThrows(JsonException.class, source::charValue));
        Assert.assertEquals("response, 1:2", source.location().toString());
    }

    @Test
    public void lexerUsesIntAsCharacterNotDecimalText() throws Exception {
        Assert.assertEquals("abc", Json.parse("\"abc\"").stringValue());
        Assert.assertTrue(Json.parse("true").booleanValue());
        Assert.assertEquals(123, Json.parse("123").intValue());
        Assert.assertEquals(255, Json.parse("0xff").intValue());
        Assert.assertEquals("\0\uffff\ud83d\ude00", Json.parse("\"\\u0000\\uffff😀\"").stringValue());
        for (String text : new String[] {"\0", "[1,\0]", "\"a\0b\""}) {
            JsonException error = Assert.assertThrows(JsonException.class,
                    () -> Json.parse(text, JsonParsingMode.STRICT));
            Assert.assertTrue(error.error() instanceof JsonError.InvalidCharacter);
        }
        for (String text : new String[] {"\"abc", "\"\\", "\"\\u", "\"\\u0", "\"\\u00", "\"\\u000"}) {
            Assert.assertThrows(JsonException.class, () -> Json.parse(text));
        }
        JsonException badEscape = Assert.assertThrows(JsonException.class, () -> Json.parse("\"\\q\""));
        Assert.assertTrue(badEscape.getMessage().contains("\\q"));
    }
}
