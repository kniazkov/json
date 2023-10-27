package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests covering {@link Lexer} class.
 */
public class LexerTest {
    @Test
    public void integer() {
        Lexer lexer = new Lexer(new Source("  13"));
        boolean oops = false;
        Token token = null;
        try {
            token = lexer.getToken(JsonParsingMode.STRICT);
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertTrue(token instanceof TokenNumber);
        Assert.assertEquals("13", token.toString());
    }

    @Test
    public void location11() {
        Lexer lexer = new Lexer(new Source("0"));
        boolean oops = false;
        Token token = null;
        try {
            token = lexer.getToken(JsonParsingMode.STRICT);
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(token);
        JsonLocation loc = token.getLocation();
        Assert.assertEquals(1, loc.getRow());
        Assert.assertEquals(1, loc.getColumn());
    }

    @Test
    public void location14() {
        Lexer lexer = new Lexer(new Source(" \t 0"));
        boolean oops = false;
        Token token = null;
        try {
            token = lexer.getToken(JsonParsingMode.STRICT);
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(token);
        JsonLocation loc = token.getLocation();
        Assert.assertEquals(1, loc.getRow());
        Assert.assertEquals(4, loc.getColumn());
    }

    @Test
    public void location32() {
        Lexer lexer = new Lexer(new Source(" \r\n\n 0"));
        boolean oops = false;
        Token token = null;
        try {
            token = lexer.getToken(JsonParsingMode.STRICT);
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(token);
        JsonLocation loc = token.getLocation();
        Assert.assertEquals(3, loc.getRow());
        Assert.assertEquals(2, loc.getColumn());
    }

    @Test
    public void expectedNumberAfterMinus() {
        Lexer lexer = new Lexer(new Source("\n\n\n\n\n\n\n\n\n\n   -abc"));
        JsonError error = null;
        try {
            lexer.getToken(JsonParsingMode.STRICT);
        } catch (JsonException exception) {
            error = exception.getError();
        }
        Assert.assertTrue(error instanceof JsonError.ExpectedNumberAfterMinus);
        String message = error.getMessage();
        JsonLocation loc = error.getLocation();
        Assert.assertEquals("A number after the minus sign is expected", message);
        Assert.assertEquals(11, loc.getRow());
        Assert.assertEquals(5, loc.getColumn());
    }
}
