package com.exoreaction.xorcery.jsonschema.test.generator;

import com.exoreaction.xorcery.json.JsonMerger;
import com.exoreaction.xorcery.jsonschema.JsonSchema;
import com.exoreaction.xorcery.jsonschema.generator.SchemaByExampleGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SchemaByExampleGeneratorTest {

        @Test
        void testSchemaByExampleGenerator() throws IOException {

            // Given
            String example = """
                    instance:
                        environment: development
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
                    .id("http://xorcery.exoreaction.com/testschema")
                    .title("Example schema")
                    .generateJsonSchema(exampleJson);

            ObjectNode extensionsJson = (ObjectNode)yamlMapper.readTree(extensions);

            ObjectNode extended = new JsonMerger().merge(schema.json(), extensionsJson);

            System.out.println(extended.toPrettyString());
        }
}
