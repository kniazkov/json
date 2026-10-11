/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.json;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests covering {@link Lexer} class.
 */
public class LexerTest {
    @Test
    public void integer() {
        Lexer lexer = new Lexer(new Source("  13"), JsonParsingMode.STRICT);
        boolean oops = false;
        Token token = null;
        try {
            token = lexer.token();
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertTrue(token instanceof TokenNumber);
        Assert.assertEquals("13", token.toString());
    }

    @Test
    public void location11() {
        Lexer lexer = new Lexer(new Source("0"), JsonParsingMode.STRICT);
        boolean oops = false;
        Token token = null;
        try {
            token = lexer.token();
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(token);
        JsonLocation loc = token.location();
        Assert.assertEquals(1, loc.row());
        Assert.assertEquals(1, loc.column());
    }

    @Test
    public void location14() {
        Lexer lexer = new Lexer(new Source(" \t 0"), JsonParsingMode.STRICT);
        boolean oops = false;
        Token token = null;
        try {
            token = lexer.token();
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(token);
        JsonLocation loc = token.location();
        Assert.assertEquals(1, loc.row());
        Assert.assertEquals(4, loc.column());
    }

    @Test
    public void location32() {
        Lexer lexer = new Lexer(new Source(" \r\n\n 0"), JsonParsingMode.STRICT);
        boolean oops = false;
        Token token = null;
        try {
            token = lexer.token();
        } catch (JsonException exception) {
            oops = true;
        }
        Assert.assertFalse(oops);
        Assert.assertNotNull(token);
        JsonLocation loc = token.location();
        Assert.assertEquals(3, loc.row());
        Assert.assertEquals(2, loc.column());
    }

    @Test
    public void expectedNumberAfterMinus() {
        Lexer lexer = new Lexer(new Source("\n\n\n\n\n\n\n\n\n\n   -abc"), JsonParsingMode.STRICT);
        JsonError error = null;
        try {
            lexer.token();
        } catch (JsonException exception) {
            error = exception.error();
        }
        Assert.assertTrue(error instanceof JsonError.ExpectedNumberAfterMinus);
        String message = error.message();
        JsonLocation loc = error.location();
        Assert.assertEquals("A number after the minus sign is expected", message);
        Assert.assertEquals(11, loc.row());
        Assert.assertEquals(5, loc.column());
    }
}
