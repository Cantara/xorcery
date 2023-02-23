package com.exoreaction.xorcery.json.test;

import com.exoreaction.xorcery.json.VariableResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class VariableResolverTest {
    private static ObjectNode result;

    private static final String testYaml = "resolverecursive: \"{{resolve}}\"\n" +
                                           "resolverecursive2: \"{{resolve}}/test/{{ partial3 }}\"\n" +
                                           "foo: bar\n" +
                                           "number: 5\n" +
                                           "resolve: \"{{ foo }}\"\n" +
                                           "partial1: \"{{ foo }}-abc\"\n" +
                                           "partial2: \"abc-{{ foo }}-abc\"\n" +
                                           "partial3: \"abc-{{ foo }}\"\n" +
                                           "resolvenumber: \"{{number}}\"\n" +
                                           "\n" +
                                           "level1:\n" +
                                           "  level2:\n" +
                                           "    level3: abc\n" +
                                           "\n" +
                                           "lvl1:\n" +
                                           "  lvl2: \"{{level1.level2}}\"\n" +
                                           "\n" +
                                           "hierarchy: some {{ level1.level2.level3 }} value\n" +
                                           "importedhierarchy: \"{{ lvl1.lvl2.level3 }}\"\n" +
                                           "missing: some {{ level1.level3.missing| \"defaultvalue\" }} value\n" +
                                           "defaultnumber: \"{{ somenumber | 5 }}\"\n" +
                                           "resolvemissing: some {{level1.level3.missing | foo}} value\n" +
                                           "resolvemissingwithdefault: some {{level1.level3.missing | level1.level3.missing | \"test\"}} value\n" +
                                           "\n" +
                                           "trueflag: true\n" +
                                           "falseflag: false\n" +
                                           "sometrueconditional: \"{{ trueflag ? foo | \\\"abc\\\"}}\"\n" +
                                           "somefalseconditional: \"{{ falseflag ? 3 | number}}\"\n" +
                                           "somedefaultsconditional: \"{{ falseflag ? 3 | falseflag ? 7 | number}}\"\n" +
                                           "somedefaultsconditional2: \"{{ falseflag ? 3 | trueflag ? 7 | number}}\"\n" +
                                           "\n" +
                                           "somedefaultsconditionaltree: \"{{ falseflag ? level1 | trueflag ? lvl1 | importedhierarchy}}\"\n";

    @BeforeAll
    public static void setup() throws IOException {

        ObjectNode objectNode = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(testYaml);
        result = new VariableResolver().apply(objectNode, objectNode);
    }

    @Test
    public void testResolveTextValue() {
        MatcherAssert.assertThat(result.get("resolve").asText(), Matchers.equalTo("bar"));
    }

    @Test
    public void testResolvePartialTextValue() {
        MatcherAssert.assertThat(result.get("partial1").asText(), Matchers.equalTo("bar-abc"));
        MatcherAssert.assertThat(result.get("partial2").asText(), Matchers.equalTo("abc-bar-abc"));
        MatcherAssert.assertThat(result.get("partial3").asText(), Matchers.equalTo("abc-bar"));
    }

    @Test
    public void testResolveNumber() {
        MatcherAssert.assertThat(result.get("resolvenumber").asInt(), Matchers.equalTo(5));
    }

    @Test
    public void testResolveHierarchicalName() throws
            IOException {
        MatcherAssert.assertThat(result.get("hierarchy").asText(), Matchers.equalTo("some abc value"));
    }

    @Test
    public void testResolveHierarchicalNameWithDefault() throws IOException {
        MatcherAssert.assertThat(result.get("missing").asText(), Matchers.equalTo("some defaultvalue value"));
        MatcherAssert.assertThat(result.get("resolvemissing").asText(), Matchers.equalTo("some bar value"));
        MatcherAssert.assertThat(result.get("resolvemissingwithdefault").asText(), Matchers.equalTo("some test value"));
    }

    @Test
    public void testResolveImportedHierarchicalName() throws IOException {
        MatcherAssert.assertThat(result.get("importedhierarchy").asText(), Matchers.equalTo("abc"));
    }

    @Test
    public void testResolveRecursive() throws IOException {
        String resolverecursive = result.get("resolverecursive").asText();
        MatcherAssert.assertThat(resolverecursive, Matchers.equalTo("bar"));

        String resolverecursive2 = result.get("resolverecursive2").asText();
        MatcherAssert.assertThat(resolverecursive2, Matchers.equalTo("bar/test/abc-bar"));
    }

    @Test
    public void testResolveNumberWithDefault() throws IOException {
        MatcherAssert.assertThat(result.get("defaultnumber").asInt(), Matchers.equalTo(5));
    }

    @Test
    public void testResolveConditionalWithDefault() throws IOException {
        MatcherAssert.assertThat(result.get("sometrueconditional").asText(), Matchers.equalTo("bar"));
        MatcherAssert.assertThat(result.get("somefalseconditional").asInt(), Matchers.equalTo(5));
        MatcherAssert.assertThat(result.get("somedefaultsconditional").asInt(), Matchers.equalTo(5));
        MatcherAssert.assertThat(result.get("somedefaultsconditional2").asInt(), Matchers.equalTo(7));
        MatcherAssert.assertThat(result.get("somedefaultsconditionaltree").toString(), Matchers.equalTo("{\"lvl2\":{\"level3\":\"abc\"}}"));
    }
}