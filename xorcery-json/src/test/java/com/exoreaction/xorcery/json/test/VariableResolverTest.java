package com.exoreaction.xorcery.json.test;

import com.exoreaction.xorcery.json.VariableResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;

class VariableResolverTest {
    private static ObjectNode result;

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
              lvl2: "{{level1.level2}}"

            hierarchy: some {{ level1.level2.level3 }} value
            importedhierarchy: "{{ lvl1.lvl2.level3 }}"
            missing: some {{ level1.level3.missing| "defaultvalue" }} value
            defaultnumber: "{{ somenumber | 5 }}"
            resolvemissing: some {{level1.level3.missing | foo}} value            
            resolvemissingwithdefault: some {{level1.level3.missing | level1.level3.missing | "test"}} value
            
            trueflag: true
            falseflag: false
            sometrueconditional: "{{ trueflag ? foo | \\"abc\\"}}"
            somefalseconditional: "{{ falseflag ? 3 | number}}"
            somefalseconditionalwithliteraldefault: "{{ falseflag ? 3 | \\"foo\\"}}"
            somedefaultsconditional: "{{ falseflag ? 3 | falseflag ? 7 | number}}"
            somedefaultsconditional2: "{{ falseflag ? 3 | trueflag ? 7 | number}}"
            
            somedefaultsconditionaltree: "{{ falseflag ? level1 | trueflag ? lvl1 | importedhierarchy}}"
                        """;

    @BeforeAll
    public static void setup() throws IOException {

        ObjectNode objectNode = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(testYaml);
        result = new VariableResolver().apply(objectNode, objectNode);
    }

    @Test
    public void testResolveTextValue() {
        assertThat(result.get("resolve").asText(), Matchers.equalTo("bar"));
    }

    @Test
    public void testResolvePartialTextValue() {
        assertThat(result.get("partial1").asText(), Matchers.equalTo("bar-abc"));
        assertThat(result.get("partial2").asText(), Matchers.equalTo("abc-bar-abc"));
        assertThat(result.get("partial3").asText(), Matchers.equalTo("abc-bar"));
    }

    @Test
    public void testResolveNumber() {
        assertThat(result.get("resolvenumber").asInt(), Matchers.equalTo(5));
    }

    @Test
    public void testResolveHierarchicalName() throws
            IOException {
        assertThat(result.get("hierarchy").asText(), Matchers.equalTo("some abc value"));
    }

    @Test
    public void testResolveHierarchicalNameWithDefault() throws IOException {
        assertThat(result.get("missing").asText(), Matchers.equalTo("some defaultvalue value"));
        assertThat(result.get("resolvemissing").asText(), Matchers.equalTo("some bar value"));
        assertThat(result.get("resolvemissingwithdefault").asText(), Matchers.equalTo("some test value"));
    }

    @Test
    public void testResolveImportedHierarchicalName() throws IOException {
        assertThat(result.get("importedhierarchy").asText(), Matchers.equalTo("abc"));
    }

    @Test
    public void testResolveRecursive() throws IOException {
        String resolverecursive = result.get("resolverecursive").asText();
        assertThat(resolverecursive, Matchers.equalTo("bar"));

        String resolverecursive2 = result.get("resolverecursive2").asText();
        assertThat(resolverecursive2, Matchers.equalTo("bar/test/abc-bar"));
    }

    @Test
    public void testResolveNumberWithDefault() throws IOException {
        assertThat(result.get("defaultnumber").asInt(), Matchers.equalTo(5));
    }

    @Test
    public void testResolveConditionalWithDefault() throws IOException {
        assertThat(result.get("sometrueconditional").asText(), Matchers.equalTo("bar"));
        assertThat(result.get("somefalseconditional").asInt(), Matchers.equalTo(5));
        assertThat(result.get("somefalseconditionalwithliteraldefault").asText(), Matchers.equalTo("foo"));
        assertThat(result.get("somedefaultsconditional").asInt(), Matchers.equalTo(5));
        assertThat(result.get("somedefaultsconditional2").asInt(), Matchers.equalTo(7));
        assertThat(result.get("somedefaultsconditionaltree").toString(), Matchers.equalTo("{\"lvl2\":{\"level3\":\"abc\"}}"));
    }
}