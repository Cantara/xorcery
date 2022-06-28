package com.exoreaction.xorcery.json;

import com.exoreaction.xorcery.configuration.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class VariableResolverTest {
    private static Configuration config;

    @BeforeAll
    public static void setup() throws IOException {

        ObjectNode objectNode = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(VariableResolverTest.class.getResourceAsStream("variableresolvertest.yaml"));
        config = new Configuration(new VariableResolver().apply(objectNode, objectNode));

        try {
            new ObjectMapper(new YAMLFactory()).writeValue(System.out, config.json());
        } catch (IOException e) {
            // Ignore
        }
    }

    @Test
    public void testResolveTextValue() {
        assertThat(config.getString("resolve").orElse(null), equalTo("bar"));
    }

    @Test
    public void testResolvePartialTextValue() {
        assertThat(config.getString("partial1").orElse(null), equalTo("bar-abc"));
        assertThat(config.getString("partial2").orElse(null), equalTo("abc-bar-abc"));
        assertThat(config.getString("partial3").orElse(null), equalTo("abc-bar"));
    }

    @Test
    public void testResolveNumber() {
        assertThat(config.getInteger("resolvenumber").orElse(null), equalTo(5));
    }

    @Test
    public void testResolveHierarchicalName() throws IOException {
        assertThat(config.getString("hierarchy").orElse(null), equalTo("some abc value"));
    }

    @Test
    public void testResolveHierarchicalNameWithDefault() throws IOException {
        assertThat(config.getString("missing").orElse(null), equalTo("some defaultvalue value"));
    }

    @Test
    public void testResolveImportedHierarchicalName() throws IOException {
        assertThat(config.getString("importedhierarchy").orElse(null), equalTo("abc"));
    }

    @Test
    public void testResolveRecursive() throws IOException {
        String resolverecursive = config.getString("resolverecursive").orElse(null);
        assertThat(resolverecursive, equalTo("bar"));
    }

    @Test
    public void testResolveNumberWithDefault() throws IOException {
        Configuration config = new Configuration.Builder()
                .addYaml(getClass().getResourceAsStream("variableresolvertest.yaml"))
                .build();

        assertThat(config.getInteger("defaultnumber").orElse(null), equalTo(5));
    }
}