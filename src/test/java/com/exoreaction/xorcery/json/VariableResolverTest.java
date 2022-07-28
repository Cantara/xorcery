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

    private static final String testYaml = """
            resolverecursive: "{{resolve}}"
            resolverecursive2: "{{resolve}}/test/{{ partial3 }}"
            foo: bar
            number: 5
            resolve: "{{ foo }}"
            partial1: "{{ foo }}-abc"
            partial2: "abc-{{ foo }}-abc"
            partial3: "abc-{{ foo }}"
            resolvenumber: "{{number}}"

            level1:
              level2:
                level3: abc

            lvl1:
              level2: "{{level1.level2}}"

            hierarchy: some {{ level1.level2.level3 }} value
            importedhierarchy: "{{ lvl1.level2.level3 }}"
            missing: some {{ level1.level3.missing| "defaultvalue" }} value
            defaultnumber: "{{ somenumber | 5 }}"
            resolvemissing: some {{level1.level3.missing | foo}} value            
            resolvemissingwithdefault: some {{level1.level3.missing | level1.level3.missing | "test"}} value            
                        """;

    @BeforeAll
    public static void setup() throws IOException {

        ObjectNode objectNode = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(testYaml);
        config = new Configuration(new VariableResolver().apply(objectNode, objectNode));

/*
        try {
            new ObjectMapper(new YAMLFactory()).writeValue(System.out, config.json());
        } catch (IOException e) {
            // Ignore
        }
*/
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
    public void testResolveHierarchicalName() throws
            IOException {
        assertThat(config.getString("hierarchy").orElse(null), equalTo("some abc value"));
    }

    @Test
    public void testResolveHierarchicalNameWithDefault() throws IOException {
        assertThat(config.getString("missing").orElse(null), equalTo("some defaultvalue value"));
        assertThat(config.getString("resolvemissing").orElse(null), equalTo("some bar value"));
        assertThat(config.getString("resolvemissingwithdefault").orElse(null), equalTo("some test value"));
    }

    @Test
    public void testResolveImportedHierarchicalName() throws IOException {
        assertThat(config.getString("importedhierarchy").orElse(null), equalTo("abc"));
    }

    @Test
    public void testResolveRecursive() throws IOException {
        String resolverecursive = config.getString("resolverecursive").orElse(null);
        assertThat(resolverecursive, equalTo("bar"));

        String resolverecursive2 = config.getString("resolverecursive2").orElse(null);
        assertThat(resolverecursive2, equalTo("bar/test/abc-bar"));
    }

    @Test
    public void testResolveNumberWithDefault() throws IOException {
        assertThat(config.getInteger("defaultnumber").orElse(null), equalTo(5));
    }
}