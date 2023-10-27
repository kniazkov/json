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
        Token token = lexer.getToken();
        Assert.assertTrue(token instanceof TokenNumber);
        Assert.assertEquals("13", token.toString());
    }

    @Test
    public void location11() {
        Lexer lexer = new Lexer(new Source("0"));
        Token token = lexer.getToken();
        Assert.assertNotNull(token);
        Location loc = token.getLocation();
        Assert.assertEquals(1, loc.getRow());
        Assert.assertEquals(1, loc.getColumn());
    }

    @Test
    public void location14() {
        Lexer lexer = new Lexer(new Source(" \t 0"));
        Token token = lexer.getToken();
        Assert.assertNotNull(token);
        Location loc = token.getLocation();
        Assert.assertEquals(1, loc.getRow());
        Assert.assertEquals(4, loc.getColumn());
    }

    @Test
    public void location32() {
        Lexer lexer = new Lexer(new Source(" \r\n\n 0"));
        Token token = lexer.getToken();
        Assert.assertNotNull(token);
        Location loc = token.getLocation();
        Assert.assertEquals(3, loc.getRow());
        Assert.assertEquals(2, loc.getColumn());
    }
}
