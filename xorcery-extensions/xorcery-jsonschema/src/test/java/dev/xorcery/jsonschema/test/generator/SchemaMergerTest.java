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
package dev.xorcery.jsonschema.test.generator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.xorcery.jsonschema.JsonSchema;
import dev.xorcery.jsonschema.generator.SchemaMerger;
import dev.xorcery.util.Resources;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SchemaMergerTest {

    @Test
    public void testSchemaMergeGenerated() throws IOException {
        SchemaMerger schemaMerger = new SchemaMerger();

        YAMLMapper mapper = new YAMLMapper();
        JsonSchema existingSchema = new JsonSchema((ObjectNode) mapper.readTree(Resources.getResource("existingschema.json").orElseThrow()));
        JsonSchema generatedSchema = new JsonSchema((ObjectNode) mapper.readTree(Resources.getResource("generatedschema.json").orElseThrow()));
        JsonSchema merged = schemaMerger.mergeGenerated(existingSchema, generatedSchema);
        System.out.println(merged.json().toPrettyString());

        Assertions.assertEquals(mapper.readTree(Resources.getResource("mergedgeneratedschema.json").orElseThrow()).toPrettyString(), merged.json().toPrettyString());
    }

    @Test
    public void testSchemaMergeCombine() throws IOException {
        SchemaMerger schemaMerger = new SchemaMerger();

        YAMLMapper mapper = new YAMLMapper();
        JsonSchema uberSchema = new JsonSchema((ObjectNode) mapper.readTree(Resources.getResource("uberschema.json").orElseThrow()));
        JsonSchema addingSchema = new JsonSchema((ObjectNode) mapper.readTree(Resources.getResource("addingschema.json").orElseThrow()));
        JsonSchema merged = schemaMerger.combine(uberSchema, addingSchema);
        System.out.println(merged.json().toPrettyString());

        Assertions.assertEquals(mapper.readTree(Resources.getResource("mergeduberschema.json").orElseThrow()).toPrettyString(), merged.json().toPrettyString());
    }
}
