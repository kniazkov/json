/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.json;

import java.util.ArrayDeque;
import java.util.List;

/** JSON parser. */
final class ParsingSession {
    /** Lexer that splits JSON documents into separate tokens. */
    private final Lexer lexer;

    /** Input shared with the lexer, used to validate or discard the document tail. */
    private final Source source;

    /** Closing token retained for an enclosing container during recovery. */
    private Token pendingToken;

    private int openArrays;
    private int openObjects;

    /** Parsing mode. */
    private final JsonParsingMode mode;

    /**
     * List of errors that occurred during parsing in extended mode and were intentionally ignored.
     */
    private final List<JsonError> warnings;

    /**
     * Constructor.
     *
     * @param src Object containing JSON document for parsing
     */
    ParsingSession(Source src, JsonParsingMode mode, List<JsonError> warnings) {
        this.source = src;
        this.lexer = new Lexer(src, mode, warnings);
        this.mode = mode;
        this.warnings = warnings;
    }

    /**
     * Parses the root element from the first position.
     *
     * @return JSON element
     * @throws JsonException If parsing fails
     */
    JsonElement parseRoot() throws JsonException {
        lexer.skipPadding(true);
        Token first = lexer.token();
        if (first instanceof TokenEnd) {
            throw new JsonException(new JsonError.ExpectedRoot(first.location()));
        }
        JsonElement root = parseJsonElement(null, first);
        if (root instanceof JsonContainer container) {
            parseContainers(container);
        }
        lexer.skipPadding(true);
        if (source.charValue() != -1) {
            JsonError error = new JsonError.TrailingContent(source.location());
            if (mode != JsonParsingMode.EXTENDED) {
                throw new JsonException(error);
            }
            if (warnings != null) {
                warnings.add(error);
            }
            // The suffix need not be valid JSON. Do not tokenize it or retain it in memory.
            while (source.nextChar() != -1) {
                // Still propagate input/decoding failures while discarding the suffix.
            }
        }
        return root;
    }

    /**
     * Parses the next JSON element from the current position.
     *
     * @param parent The container that will contain the parsed element
     * @param firstToken The first token in the token sequence
     * @return JSON element
     * @throws JsonException If parsing fails
     */
    private JsonElement parseJsonElement(JsonContainer parent, Token firstToken)
            throws JsonException {
        if (firstToken instanceof TokenEnd) {
            throw new JsonException(new JsonError.UnexpectedEnd(firstToken.location()));
        }
        if (firstToken instanceof TokenLiteral) {
            return ((TokenLiteral) firstToken).toElement(parent);
        }
        if (firstToken instanceof TokenOpeningSquareBracket) {
            return new JsonArray(parent);
        }
        if (firstToken instanceof TokenOpeningCurlyBracket) {
            return new JsonObject(parent);
        }
        throw new JsonException(new JsonError.UnexpectedToken(firstToken.location(), firstToken));
    }

    /** Returns a token, including a closing bracket left for an enclosing container. */
    private Token nextToken() throws JsonException {
        if (pendingToken != null) {
            Token token = pendingToken;
            pendingToken = null;
            return token;
        }
        return lexer.token();
    }

    /** Reports a recoverable syntax error, or rejects it in the strict modes. */
    private void recover(JsonError error) throws JsonException {
        if (mode != JsonParsingMode.EXTENDED) {
            throw new JsonException(error);
        }
        if (warnings != null) {
            warnings.add(error);
        }
    }

    /** Whether a closing token belongs to this container. */
    private static boolean matches(JsonContainer container, Token token) {
        return container instanceof JsonArray && token instanceof TokenClosingSquareBracket
                || container instanceof JsonObject && token instanceof TokenClosingCurlyBracket;
    }

    /**
     * Called only at positions where a container may end. At EOF only the root may be completed. A
     * foreign bracket may close an ancestor, but is never swallowed here.
     */
    private boolean closeContainer(JsonContainer container, Token token) throws JsonException {
        if (matches(container, token)) {
            return true;
        }
        char bracket = container instanceof JsonArray ? ']' : '}';
        if (token instanceof TokenEnd) {
            if (container.parent() != null) {
                throw new JsonException(new JsonError.UnexpectedEnd(token.location()));
            }
            recover(new JsonError.MissingClosingBracket(token.location(), bracket));
            return true;
        }
        if (token instanceof TokenClosingSquareBracket
                || token instanceof TokenClosingCurlyBracket) {
            boolean hasAncestor =
                    token instanceof TokenClosingSquareBracket ? openArrays > 0 : openObjects > 0;
            if (hasAncestor) {
                recover(new JsonError.MissingClosingBracket(token.location(), bracket));
                pendingToken = token;
                return true;
            }
            throw new JsonException(new JsonError.UnexpectedToken(token.location(), token));
        }
        return false;
    }

    /** Whether a token can start an array value, without guessing a malformed token's meaning. */
    private static boolean startsValue(Token token) {
        return token instanceof TokenLiteral
                || token instanceof TokenOpeningSquareBracket
                || token instanceof TokenOpeningCurlyBracket;
    }

    /** Heap-backed parser continuation; only complete pairs/values reach this state. */
    private static final class Frame {
        final JsonContainer container;
        boolean expect = true;
        boolean afterComma;

        Frame(JsonContainer container) {
            this.container = container;
        }
    }

    private void parseContainers(JsonContainer root) throws JsonException {
        ArrayDeque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(root));
        if (root instanceof JsonArray) {
            openArrays++;
        } else {
            openObjects++;
        }
        while (!stack.isEmpty()) {
            Frame frame = stack.peek();
            JsonContainer container = frame.container;
            Token token = nextToken();
            if (matches(container, token) && frame.afterComma && mode == JsonParsingMode.STRICT) {
                throw new JsonException(
                        container instanceof JsonArray
                                ? new JsonError.ExpectedElementAfterComma(token.location())
                                : new JsonError.ExpectedPairAfterComma(token.location()));
            }
            if (closeContainer(container, token)) {
                stack.pop();
                if (container instanceof JsonArray) {
                    openArrays--;
                } else {
                    openObjects--;
                }
                continue;
            }
            JsonElement child;
            if (container instanceof JsonArray array) {
                if (token instanceof TokenComma) {
                    if (frame.expect) {
                        recover(new JsonError.MissingArrayElement(token.location()));
                    }
                    frame.expect = true;
                    frame.afterComma = true;
                    continue;
                }
                if (!frame.expect && startsValue(token)) {
                    recover(new JsonError.MissingComma(token.location()));
                }
                child = parseJsonElement(array, token);
                array.addChild(child);
            } else {
                JsonObject object = (JsonObject) container;
                if (!frame.expect && token instanceof TokenComma) {
                    frame.expect = true;
                    frame.afterComma = true;
                    continue;
                }
                String key;
                if (token instanceof TokenString string) {
                    key = string.value();
                } else if (mode != JsonParsingMode.STRICT && token.identifierName() != null) {
                    key = token.identifierName();
                } else {
                    throw new JsonException(new JsonError.ExpectedKey(token.location()));
                }
                if (!frame.expect) {
                    recover(new JsonError.MissingComma(token.location()));
                }
                token = nextToken();
                if (!(token instanceof TokenColon)) {
                    throw new JsonException(new JsonError.ExpectedSeparator(token.location()));
                }
                token = nextToken();
                if (token instanceof TokenEnd) {
                    throw new JsonException(
                            new JsonError.ExpectedElementAfterSeparator(token.location()));
                }
                child = parseJsonElement(object, token);
                object.addChild(key, child);
            }
            frame.expect = false;
            frame.afterComma = false;
            if (child instanceof JsonContainer nested) {
                stack.push(new Frame(nested));
                if (nested instanceof JsonArray) {
                    openArrays++;
                } else {
                    openObjects++;
                }
            }
        }
    }
}
