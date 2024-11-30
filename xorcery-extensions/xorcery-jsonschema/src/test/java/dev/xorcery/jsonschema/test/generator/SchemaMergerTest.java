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
