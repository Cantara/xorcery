package dev.xorcery.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ResourcesTest {

    @Test
    public void testResourceURLStreamHandlerProvider() throws IOException {
        try (InputStream in = new URL("resource://foo.txt").openStream())
        {
            System.out.println(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }

        try (InputStream in = new URL("resource://test/foo.txt").openStream())
        {
            System.out.println(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
