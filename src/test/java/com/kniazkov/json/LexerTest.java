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
    public void longInteger() {
        Lexer lexer = new Lexer(new Source("666000000000"));
        Token token = lexer.getToken();
        Assert.assertTrue(token instanceof TokenNumber);
        Assert.assertEquals("666000000000", token.toString());
    }

    @Test
    public void negativeInteger() {
        Lexer lexer = new Lexer(new Source("  -13"));
        Token token = lexer.getToken();
        Assert.assertTrue(token instanceof TokenNumber);
        Assert.assertEquals("-13", token.toString());
    }
}
