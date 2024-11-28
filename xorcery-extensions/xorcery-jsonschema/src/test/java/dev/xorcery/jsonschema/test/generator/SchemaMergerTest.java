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
    public void testSchemaMerge() throws IOException {
        SchemaMerger schemaMerger = new SchemaMerger();

        YAMLMapper mapper = new YAMLMapper();
        JsonSchema existingSchema = new JsonSchema((ObjectNode) mapper.readTree(Resources.getResource("existingschema.json").orElseThrow()));
        JsonSchema generatedSchema = new JsonSchema((ObjectNode) mapper.readTree(Resources.getResource("generatedschema.json").orElseThrow()));
        JsonSchema merged = schemaMerger.merge(existingSchema, generatedSchema);
        System.out.println(merged.json().toPrettyString());

        Assertions.assertEquals(mapper.readTree(Resources.getResource("generatedschema.json").orElseThrow()).toPrettyString(), merged.json().toPrettyString());
    }
}
