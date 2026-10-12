/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TokenEndTest {
    @Test
    public void lexerAlwaysReturnsLocatedEndTokens() throws Exception {
        for (String input : new String[] {"", " \r\n", "42 \r\n", "[] \r\n", "{} \r\n"}) {
            Lexer lexer =
                    new Lexer(
                            new Source(new StringReader(input), "source.json"),
                            JsonParsingMode.STRICT);
            Token end;
            do {
                end = lexer.token();
                Assert.assertNotNull(end);
            } while (!(end instanceof TokenEnd));
            String position = input.isEmpty() ? "source.json, 1:1" : "source.json, 2:1";
            Assert.assertEquals(position, end.location().toString());
            Assert.assertEquals("<EOF>", end.toString());
            for (int i = 0; i < 3; i++) {
                Token repeated = lexer.token();
                Assert.assertTrue(repeated instanceof TokenEnd);
                Assert.assertEquals(position, repeated.location().toString());
                Assert.assertEquals(position, end.location().toString());
            }
        }
    }

    @Test
    public void endAfterJson5CommentsAndRealNulAreDistinct() throws Exception {
        String text = "42 /*end*/ //done";
        Lexer lexer = new Lexer(new Source(text), JsonParsingMode.JSON5);
        Assert.assertTrue(lexer.token() instanceof TokenNumber);
        Token end = lexer.token();
        Assert.assertTrue(end instanceof TokenEnd);
        Assert.assertEquals(text.length() + 1, end.location().column());
        Lexer nul = new Lexer(new Source("\0"), JsonParsingMode.STRICT);
        Assert.assertThrows(JsonException.class, nul::token);
    }

    @Test
    public void structuralEofUsesItsTokenLocationInEveryMode() throws Exception {
        String[] incomplete = {
            "",
            " ",
            "[",
            "{",
            "[1",
            "[1,",
            "{\"a\"",
            "{\"a\":",
            "{\"a\":1",
            "{\"a\":1,",
            "[[",
            "[[1",
            "[[1,",
            "[{",
            "[{\"a\"",
            "[{\"a\":",
            "[{\"a\":1",
            "[{\"a\":1,",
            "{\"a\":[",
            "{\"a\":[1,",
            "[[]"
        };
        List<String> recoverable = List.of("[", "{", "[1", "[1,", "{\"a\":1", "{\"a\":1,", "[[]");
        for (JsonParsingMode mode : JsonParsingMode.values()) {
            for (String prefix : incomplete) {
                String input = prefix + "\r\n  ";
                if (mode == JsonParsingMode.EXTENDED && recoverable.contains(prefix)) {
                    List<JsonError> warnings = new ArrayList<>();
                    Assert.assertNotNull(JsonParser.parseString(input, warnings));
                    Assert.assertEquals(1, warnings.size());
                    Assert.assertTrue(warnings.get(0) instanceof JsonError.MissingClosingBracket);
                    Assert.assertEquals("2:3", warnings.get(0).location().toString());
                } else {
                    JsonException error =
                            Assert.assertThrows(
                                    prefix + " / " + mode,
                                    JsonException.class,
                                    () -> Json.parse(input, "cut.json", mode));
                    Assert.assertEquals("cut.json, 2:3", error.error().location().toString());
                    Assert.assertFalse(error.getMessage().isEmpty());
                }
            }
        }
    }

    @Test
    public void everyTruncationProducesAValueOrJsonExceptionNeverNpe() throws Exception {
        String document = "{\"a\":[true,false,null,-12.5e+2,\"x\\u0041\",{\"b\":[]}] }";
        for (JsonParsingMode mode : JsonParsingMode.values()) {
            for (int length = 0; length <= document.length(); length++) {
                String prefix = document.substring(0, length);
                try {
                    JsonElement value = Json.parse(prefix, mode);
                    Assert.assertNotNull(value);
                    Assert.assertTrue(
                            mode == JsonParsingMode.EXTENDED || length == document.length());
                    // Recovered output must itself remain valid JSON.
                    Assert.assertNotNull(Json.parse(value.toString(), JsonParsingMode.STRICT));
                } catch (JsonException error) {
                    Assert.assertNotNull(error.error());
                    Assert.assertNotNull(error.error().location());
                }
            }
        }
    }
}
