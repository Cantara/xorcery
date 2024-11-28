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
