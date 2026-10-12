/* Copyright (c) 2026 Ivan Kniazkov */
package com.kniazkov.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

public class ConfigurableIoTest {
    private static final Charset CP1251 = Charset.forName("windows-1251");
    private static final String TEXT = "Привет 😀";

    private byte[] output(Object value, Charset charset, boolean bom) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new JsonSerializer().setCharset(charset).setWriteBom(bom).serialize(value, bytes);
        return bytes.toByteArray();
    }

    private JsonElement parse(byte[] bytes, Charset charset, JsonBomPolicy policy)
            throws Exception {
        return new JsonParser()
                .setMode(JsonParsingMode.STRICT)
                .setCharset(charset)
                .setBomPolicy(policy)
                .parse(new ByteArrayInputStream(bytes));
    }

    @Test
    public void unicodeEncodingsRoundTripWithAndWithoutBom() throws Exception {
        for (String name :
                List.of(
                        "UTF-8",
                        "UTF-16",
                        "UTF-16LE",
                        "UTF-16BE",
                        "UTF-32",
                        "UTF-32LE",
                        "UTF-32BE")) {
            Charset charset = Charset.forName(name);
            for (boolean bom : new boolean[] {false, true}) {
                byte[] bytes = output(TEXT, charset, bom);
                Assert.assertEquals(
                        TEXT,
                        parse(bytes, charset, bom ? JsonBomPolicy.REQUIRE : JsonBomPolicy.FORBID)
                                .stringValue());
                Assert.assertEquals(TEXT, parse(bytes, charset, JsonBomPolicy.ALLOW).stringValue());
                Assert.assertThrows(
                        JsonException.class,
                        () ->
                                parse(
                                        bytes,
                                        charset,
                                        bom ? JsonBomPolicy.FORBID : JsonBomPolicy.REQUIRE));
            }
        }
        Assert.assertEquals(
                TEXT,
                parse(
                                output(TEXT, StandardCharsets.UTF_16LE, true),
                                StandardCharsets.UTF_16,
                                JsonBomPolicy.REQUIRE)
                        .stringValue());
        Assert.assertEquals(
                TEXT,
                parse(
                                output(TEXT, Charset.forName("UTF-32LE"), true),
                                Charset.forName("UTF-32"),
                                JsonBomPolicy.REQUIRE)
                        .stringValue());
    }

    @Test
    public void byteOrderAndMarkBytesAreExplicit() throws Exception {
        Assert.assertArrayEquals(new byte[] {0, 0x31}, output(1, StandardCharsets.UTF_16, false));
        Assert.assertArrayEquals(
                new byte[] {(byte) 0xfe, (byte) 0xff, 0, 0x31},
                output(1, StandardCharsets.UTF_16, true));
        Assert.assertArrayEquals(new byte[] {0x31, 0}, output(1, StandardCharsets.UTF_16LE, false));
        Assert.assertArrayEquals(
                new byte[] {(byte) 0xef, (byte) 0xbb, (byte) 0xbf, 0x31},
                output(1, StandardCharsets.UTF_8, true));
        for (String name : List.of("x-UTF-16LE-BOM", "X-UTF-32LE-BOM", "X-UTF-32BE-BOM")) {
            if (Charset.isSupported(name)) {
                Charset charset = Charset.forName(name);
                Assert.assertEquals(
                        1,
                        parse(output(1, charset, false), charset, JsonBomPolicy.FORBID).intValue());
                Assert.assertEquals(
                        1,
                        parse(output(1, charset, true), charset, JsonBomPolicy.REQUIRE).intValue());
            }
        }
    }

    @Test
    public void conflictingDuplicateMissingAndTruncatedMarksFail() throws Exception {
        byte[] le = output(1, StandardCharsets.UTF_16LE, true);
        Assert.assertThrows(
                JsonException.class,
                () -> parse(le, StandardCharsets.UTF_16BE, JsonBomPolicy.ALLOW));
        Assert.assertThrows(
                JsonException.class, () -> parse(le, StandardCharsets.UTF_8, JsonBomPolicy.ALLOW));
        Assert.assertThrows(JsonException.class, () -> parse(le, CP1251, JsonBomPolicy.ALLOW));
        for (Charset charset :
                List.of(
                        StandardCharsets.UTF_8,
                        StandardCharsets.UTF_16LE,
                        Charset.forName("UTF-32BE"))) {
            byte[] once = output(1, charset, true);
            byte[] mark = JsonEncoding.outputMark(charset);
            ByteArrayOutputStream twice = new ByteArrayOutputStream();
            twice.write(mark);
            twice.write(once);
            Assert.assertThrows(
                    JsonException.class,
                    () -> parse(twice.toByteArray(), charset, JsonBomPolicy.ALLOW));
        }
        JsonException malformed =
                Assert.assertThrows(
                        JsonException.class,
                        () ->
                                parse(
                                        new byte[] {(byte) 0xef, (byte) 0xbb},
                                        StandardCharsets.UTF_8,
                                        JsonBomPolicy.ALLOW));
        Assert.assertTrue(malformed.getCause() instanceof CharacterCodingException);
    }

    @Test
    public void characterBomPolicyWorksInEverySyntaxMode() throws Exception {
        for (JsonParsingMode mode : JsonParsingMode.values()) {
            JsonParser parser = new JsonParser().setMode(mode);
            Assert.assertEquals(1, parser.parse("\ufeff1").intValue());
            Assert.assertThrows(JsonException.class, () -> parser.parse("\ufeff\ufeff1"));
            parser.setBomPolicy(JsonBomPolicy.FORBID);
            Assert.assertThrows(JsonException.class, () -> parser.parse("\ufeff1"));
            parser.setBomPolicy(JsonBomPolicy.REQUIRE);
            Assert.assertThrows(JsonException.class, () -> parser.parse("1"));
            Assert.assertEquals(1, parser.parse(new StringReader("\ufeff1")).intValue());
        }
        Assert.assertEquals(1, Json.parse("\ufeff1", JsonParsingMode.STRICT).intValue());
        Assert.assertThrows(
                JsonException.class, () -> Json.parse(" \ufeff1", JsonParsingMode.STRICT));
        Assert.assertThrows(
                JsonException.class, () -> Json.parse("[\ufeff1]", JsonParsingMode.STRICT));
        Assert.assertEquals(
                "\ufeff", Json.parse("\"\ufeff\"", JsonParsingMode.STRICT).stringValue());
    }

    @Test
    public void legacyEncodingAndAsciiEscapesPreserveKeysAndValues() throws Exception {
        String text = "Привет";
        Assert.assertEquals(
                text,
                parse(output(text, CP1251, false), CP1251, JsonBomPolicy.ALLOW).stringValue());
        JsonElement tree = Json.parse("{\"ключ😀\":\"Привет 😀\"}");
        JsonSerializer serializer =
                new JsonSerializer()
                        .setEscapeNonAscii(true)
                        .setCharset(StandardCharsets.US_ASCII)
                        .setIndentation("  ");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        serializer.serialize(tree, bytes);
        String ascii = bytes.toString(StandardCharsets.US_ASCII);
        Assert.assertTrue(ascii.chars().allMatch(ch -> ch < 128));
        Assert.assertTrue(ascii.contains("\\ud83d\\ude00"));
        Assert.assertEquals(tree, Json.parse(ascii));
        Assert.assertEquals("\"\\u041f\"", serializer.serialize("П"));
    }

    @Test
    public void encodingAndDecodingNeverSilentlyReplaceData() throws Exception {
        JsonSerializer serializer = new JsonSerializer().setCharset(CP1251);
        Assert.assertThrows(
                CharacterCodingException.class,
                () -> serializer.serialize(TEXT, new ByteArrayOutputStream()));
        Assert.assertThrows(
                CharacterCodingException.class,
                () -> new JsonSerializer().serialize("\ud800", new ByteArrayOutputStream()));
        for (byte[] bytes : new byte[][] {{'"', (byte) 0xc3, 0x28, '"'}, {'"', (byte) 0xff, '"'}}) {
            JsonException failure =
                    Assert.assertThrows(
                            JsonException.class,
                            () -> parse(bytes, StandardCharsets.UTF_8, JsonBomPolicy.ALLOW));
            Assert.assertTrue(failure.getCause() instanceof CharacterCodingException);
        }
        Assert.assertThrows(
                JsonException.class,
                () -> parse(new byte[] {0x31}, StandardCharsets.UTF_16LE, JsonBomPolicy.ALLOW));
    }

    @Test
    public void charsetAndOutputBomDoNotAffectCharacterApis() throws Exception {
        JsonSerializer serializer =
                new JsonSerializer().setCharset(StandardCharsets.UTF_16).setWriteBom(true);
        Assert.assertEquals("\"Привет 😀\"", serializer.serialize(TEXT));
        StringWriter writer = new StringWriter();
        serializer.serialize(TEXT, writer);
        Assert.assertEquals(serializer.serialize(TEXT), writer.toString());
        JsonParser parser = new JsonParser().setCharset(StandardCharsets.US_ASCII);
        Assert.assertEquals(TEXT, parser.parse(writer.toString()).stringValue());
    }

    @Test
    public void parserStateAndWarningsArePerInvocation() throws Exception {
        JsonParser parser = new JsonParser().setCollectWarnings(true).setSourceName("wire");
        parser.parse("1 junk");
        List<JsonError> old = parser.warnings();
        Assert.assertEquals(1, old.size());
        Assert.assertThrows(UnsupportedOperationException.class, () -> old.clear());
        parser.parse("2");
        Assert.assertTrue(parser.warnings().isEmpty());
        Assert.assertEquals(1, old.size());
        JsonException failure =
                Assert.assertThrows(JsonException.class, () -> parser.parse("[1,,{"));
        Assert.assertEquals("wire", failure.error().location().sourceName().orElseThrow());
        Assert.assertEquals(1, parser.warnings().size());
        parser.setMode(JsonParsingMode.STRICT);
        Assert.assertThrows(JsonException.class, () -> parser.parse("[1,]"));
        Assert.assertTrue(parser.warnings().isEmpty());
        Assert.assertEquals(3, parser.parse("3").intValue());
        parser.setMode(JsonParsingMode.EXTENDED).setCollectWarnings(false).parse("4 junk");
        Assert.assertTrue(parser.warnings().isEmpty());
        List<JsonError> legacy = new ArrayList<>(old);
        JsonParser.parseString("1 junk", legacy);
        Assert.assertEquals(2, legacy.size());
    }

    static class TrackedInput extends ByteArrayInputStream {
        boolean closed;

        TrackedInput(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    static class TrackedOutput extends ByteArrayOutputStream {
        boolean closed;
        boolean flushed;

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public void flush() {
            flushed = true;
        }
    }

    @Test
    public void borrowedResourcesStayOpenOnSuccessAndFailure() throws Exception {
        TrackedInput input = new TrackedInput("1".getBytes(StandardCharsets.UTF_8));
        new JsonParser().parse(input);
        Assert.assertFalse(input.closed);
        TrackedInput broken = new TrackedInput(new byte[] {(byte) 0xff});
        Assert.assertThrows(JsonException.class, () -> new JsonParser().parse(broken));
        Assert.assertFalse(broken.closed);
        TrackedOutput output = new TrackedOutput();
        new JsonSerializer().serialize(TEXT, output);
        Assert.assertFalse(output.closed);
        Assert.assertTrue(output.flushed);
        TrackedOutput rejected = new TrackedOutput();
        Assert.assertThrows(
                CharacterCodingException.class,
                () -> new JsonSerializer().setCharset(CP1251).serialize(TEXT, rejected));
        Assert.assertFalse(rejected.closed);
        Assert.assertEquals(0, rejected.size());
        StringReader reader = new StringReader("1");
        new JsonParser().parse(reader);
        Assert.assertEquals(-1, reader.read());
    }

    @Test
    public void ioFailuresPropagateWithOriginalCauses() {
        IOException failure = new IOException("broken");
        InputStream input =
                new InputStream() {
                    public int read() throws IOException {
                        throw failure;
                    }
                };
        JsonException parse =
                Assert.assertThrows(
                        JsonException.class,
                        () -> new JsonParser().setSourceName("wire").parse(input));
        Assert.assertSame(failure, parse.getCause());
        OutputStream output =
                new OutputStream() {
                    public void write(int b) throws IOException {
                        throw failure;
                    }
                };
        Assert.assertSame(
                failure,
                Assert.assertThrows(
                        IOException.class, () -> new JsonSerializer().serialize(1, output)));
        Writer writer =
                new Writer() {
                    public void write(char[] b, int o, int n) throws IOException {
                        throw failure;
                    }

                    public void flush() {}

                    public void close() {
                        Assert.fail("closed");
                    }
                };
        Assert.assertSame(
                failure,
                Assert.assertThrows(
                        IOException.class, () -> new JsonSerializer().serialize(1, writer)));
    }

    @Test
    public void filesRoundTripAndFailedEncodingDoesNotTruncate() throws Exception {
        Path path = Files.createTempFile("json-config", ".json");
        try {
            new JsonSerializer().setCharset(CP1251).serialize("Привет", path.toFile());
            Assert.assertEquals(
                    "Привет",
                    new JsonParser().setCharset(CP1251).parse(path.toFile()).stringValue());
            Assert.assertThrows(
                    CharacterCodingException.class,
                    () -> new JsonSerializer().setCharset(CP1251).serialize(TEXT, path.toFile()));
            Assert.assertEquals(
                    "Привет",
                    new JsonParser().setCharset(CP1251).parse(path.toFile()).stringValue());
            new JsonSerializer()
                    .setCharset(StandardCharsets.UTF_16LE)
                    .setWriteBom(true)
                    .serialize(TEXT, path.toFile());
            Assert.assertEquals(
                    TEXT,
                    new JsonParser()
                            .setCharset(StandardCharsets.UTF_16)
                            .setBomPolicy(JsonBomPolicy.REQUIRE)
                            .parse(path.toFile())
                            .stringValue());
            Files.writeString(path, "[", StandardCharsets.UTF_8);
            JsonException error =
                    Assert.assertThrows(
                            JsonException.class,
                            () ->
                                    new JsonParser()
                                            .setMode(JsonParsingMode.STRICT)
                                            .parse(path.toFile()));
            Assert.assertEquals(
                    path.toString(), error.error().location().sourceName().orElseThrow());
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    public void invalidSettingsFailImmediatelyAndKeepPreviousSettings() {
        JsonSerializer serializer = new JsonSerializer().setWriteBom(true);
        Assert.assertThrows(IllegalArgumentException.class, () -> serializer.setCharset(CP1251));
        Assert.assertThrows(
                IllegalArgumentException.class,
                () -> new JsonSerializer().setCharset(CP1251).setWriteBom(true));
        Assert.assertThrows(IllegalArgumentException.class, () -> serializer.setIndentation("x"));
        Assert.assertThrows(NullPointerException.class, () -> serializer.setCharset(null));
        Assert.assertThrows(NullPointerException.class, () -> new JsonParser().setMode(null));
        Assert.assertThrows(NullPointerException.class, () -> new JsonParser().setBomPolicy(null));
        Assert.assertEquals("[1, 2]", serializer.serialize(List.of(1, 2)));
    }

    @Test
    public void staticFacadeUsesTheSameDefaultsAndConfiguredCore() throws Exception {
        JsonElement tree = Json.parse("{a:[1,2],text:'Привет'}");
        Assert.assertEquals(new JsonSerializer().serialize(tree), Json.serialize(tree));
        Assert.assertEquals(
                new JsonSerializer().setIndentation("  ").serialize(tree),
                Json.serialize(tree, "  "));
        Assert.assertEquals(new JsonParser().parse("[1,2]"), Json.parse("[1,2]"));
        byte[] bytes = output(TEXT, StandardCharsets.UTF_16BE, true);
        Assert.assertEquals(
                TEXT,
                Json.parse(
                                new ByteArrayInputStream(bytes),
                                StandardCharsets.UTF_16BE,
                                JsonParsingMode.STRICT)
                        .stringValue());
        new JsonSerializer().setIndentation("  ").setIndentation(null).serialize(tree);
    }

    /**
     * Checks content independently of the console's charset in a subprocess.
     *
     * @param args Path to the UTF-8 input file
     * @throws Exception If parsing or serialization fails
     * @throws AssertionError If the charset or content differs from the expected value
     */
    public static void main(String[] args) throws Exception {
        if (!Charset.defaultCharset().equals(CP1251)) {
            throw new AssertionError("Expected changed default charset");
        }
        if (!TEXT.equals(Json.parse(new File(args[0])).stringValue())) {
            throw new AssertionError("Corrupt UTF-8 input");
        }
        byte[] bytes = outputStatic(TEXT);
        if (!Arrays.equals(bytes, ("\"" + TEXT + "\"").getBytes(StandardCharsets.UTF_8))) {
            throw new AssertionError("Corrupt UTF-8 output");
        }
    }

    private static byte[] outputStatic(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new JsonSerializer().serialize(value, bytes);
        return bytes.toByteArray();
    }

    @Test
    public void utf8DefaultsIgnoreTheJvmDefaultCharset() throws Exception {
        Path path = Files.createTempFile("json-charset", ".json");
        try {
            Files.writeString(path, "\"" + TEXT + "\"", StandardCharsets.UTF_8);
            Process process =
                    new ProcessBuilder(
                                    Path.of(System.getProperty("java.home"), "bin", "java")
                                            .toString(),
                                    "-Dfile.encoding=windows-1251",
                                    "-cp",
                                    System.getProperty("java.class.path"),
                                    getClass().getName(),
                                    path.toString())
                            .redirectErrorStream(true)
                            .start();
            try {
                Assert.assertTrue("Subprocess timed out", process.waitFor(30, TimeUnit.SECONDS));
                Assert.assertEquals(
                        new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                        0,
                        process.exitValue());
            } finally {
                process.destroyForcibly();
            }
        } finally {
            Files.deleteIfExists(path);
        }
    }
}
