package com.exoreaction.xorcery.configuration.jsonschema;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.json.JsonMerger;
import com.exoreaction.xorcery.jsonschema.JsonSchema;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@Disabled
class ConfigurationSchemaBuilderTest {

    @Test
    void testGenerateXorceryConfigurationSchema() throws IOException {

        // Given
        ConfigurationBuilder configuration = new ConfigurationBuilder().addDefaults();

//        System.out.println(configuration);

        // When
        JsonSchema schema = new ConfigurationSchemaBuilder()
                .id("http://xorcery.exoreaction.com/testschema")
                .title("Configuration schema")
                .generateJsonSchema(configuration);

        YAMLMapper yamlMapper = new YAMLMapper();
        JsonNode extensions = yamlMapper.readTree(Resources.getResource("xorcery-schema.yaml").get().openStream());

        ObjectNode extended = new JsonMerger().merge(schema.json(), (ObjectNode)extensions);

        System.out.println(extended.toPrettyString());
    }
}