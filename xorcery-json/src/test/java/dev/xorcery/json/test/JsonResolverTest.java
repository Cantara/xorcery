/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.json.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.xorcery.json.JsonResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

class JsonResolverTest {
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
                        
            nullfalsy: "{{ falseflag ? 1 }}"
                        
            nullfalsyarray:
                - "1"
                - "{{ falseflag ? 1 }}"
                - "3"    
                
            namespace:
              missing:
                enabled: "{{ namespace.enabled ? false | true }}"
                
            arrayref:
              - foo
              - bar
            somearray:
              "{{ arrayref }}"
            someotherarray:
              - "xyzzy"
              - "{{ arrayref }}"
              
            namedobjectarray:
            - name: "foo"
              value: "xyzzy"
              bar: "{{ namedobjectarray.foo.value }}"
                        """;

    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void setup() throws IOException {

        objectMapper = new ObjectMapper(new YAMLFactory());
        ObjectNode objectNode = (ObjectNode) objectMapper.readTree(testYaml);
        result = new JsonResolver().apply(objectNode, objectNode);
    }

    @Test
    public void testResolveTextValue() {
        assertThat(result.get("resolve").asText(), equalTo("bar"));
    }

    @Test
    public void testResolvePartialTextValue() {
        assertThat(result.get("partial1").asText(), equalTo("bar-abc"));
        assertThat(result.get("partial2").asText(), equalTo("abc-bar-abc"));
        assertThat(result.get("partial3").asText(), equalTo("abc-bar"));
    }

    @Test
    public void testResolveNumber() {
        assertThat(result.get("resolvenumber").asInt(), equalTo(5));
    }

    @Test
    public void testResolveHierarchicalName() throws
            IOException {
        assertThat(result.get("hierarchy").asText(), equalTo("some abc value"));
    }

    @Test
    public void testResolveHierarchicalNameWithDefault() throws IOException {
        assertThat(result.get("missing").asText(), equalTo("some defaultvalue value"));
        assertThat(result.get("resolvemissing").asText(), equalTo("some bar value"));
        assertThat(result.get("resolvemissingwithdefault").asText(), equalTo("some test value"));
    }

    @Test
    public void testResolveImportedHierarchicalName() throws IOException {
        assertThat(result.get("importedhierarchy").asText(), equalTo("abc"));
    }

    @Test
    public void testResolveRecursive() throws IOException {
        String resolverecursive = result.get("resolverecursive").asText();
        assertThat(resolverecursive, equalTo("bar"));

        String resolverecursive2 = result.get("resolverecursive2").asText();
        assertThat(resolverecursive2, equalTo("bar/test/abc-bar"));
    }

    @Test
    public void testResolveNumberWithDefault() throws IOException {
        assertThat(result.get("defaultnumber").asInt(), equalTo(5));
    }

    @Test
    public void testResolveConditionalWithDefault() throws IOException {
        assertThat(result.get("sometrueconditional").asText(), equalTo("bar"));
        assertThat(result.get("somefalseconditional").asInt(), equalTo(5));
        assertThat(result.get("somefalseconditionalwithliteraldefault").asText(), equalTo("foo"));
        assertThat(result.get("somedefaultsconditional").asInt(), equalTo(5));
        assertThat(result.get("somedefaultsconditional2").asInt(), equalTo(7));
        assertThat(result.get("somedefaultsconditionaltree").toString(), equalTo("{\"lvl2\":{\"level3\":\"abc\"}}"));
    }

    @Test
    public void testFalsyWithNull() {
        assertThat(result.get("nullfalsy"), nullValue());
    }

    @Test
    public void testConditionalWithMissing() {
        assertThat(result.get("namespace").get("missing").get("enabled").asBoolean(), equalTo(true));
    }

    @Test
    public void testArrayReferences() {
        System.out.println(result.toPrettyString());
        assertThat(result.get("somearray").toPrettyString(), equalTo("[ \"foo\", \"bar\" ]"));
        assertThat(result.get("someotherarray").toPrettyString(), equalTo("[ \"xyzzy\", \"foo\", \"bar\" ]"));
    }

    @Test
    public void testNamedObjectArrayReference() {
        assertThat(result.get("namedobjectarray").get(0).get("bar").asText(), equalTo("xyzzy"));
    }

}