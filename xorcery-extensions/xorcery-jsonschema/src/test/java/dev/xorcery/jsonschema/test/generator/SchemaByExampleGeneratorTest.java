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
import dev.xorcery.json.JsonMerger;
import dev.xorcery.jsonschema.JsonSchema;
import dev.xorcery.jsonschema.generator.SchemaByExampleGenerator;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SchemaByExampleGeneratorTest {

        @Test
        void testSchemaByExampleGenerator() throws IOException {

            // Given
            String example = """
                    instance:
                        environment: development
                        listofthings:
                        - name: foo
                          obj:
                            name: value
                            name2: value2
                        - name: bar
                          bar1: foo
                        anotherlistofthings: []
                    """;

            String extensions = """
                    $defs.instance.properties.environment.enum:
                      - development
                      - qa
                      - production
                    """;

            YAMLMapper yamlMapper = new YAMLMapper();

            ObjectNode exampleJson = (ObjectNode) yamlMapper.readTree(example);

            // When
            JsonSchema schema = new SchemaByExampleGenerator()
                    .id("http://xorcery.dev/testschema")
                    .title("Example schema")
                    .generateJsonSchema(exampleJson);

            ObjectNode extensionsJson = (ObjectNode)yamlMapper.readTree(extensions);

            ObjectNode extended = new JsonMerger().merge(schema.json(), extensionsJson);

            System.out.println(extended.toPrettyString());
        }
}
