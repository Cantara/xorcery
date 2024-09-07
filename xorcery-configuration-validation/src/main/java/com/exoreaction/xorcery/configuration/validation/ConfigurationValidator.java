package com.exoreaction.xorcery.configuration.validation;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;

public class ConfigurationValidator {
    final private String schemaName;

    public ConfigurationValidator(String schemaName) {
        this.schemaName = schemaName;
    }

    public ConfigurationValidator() {
        this("xorcery-schema.json");
    }

    public Set<ValidationMessage> validate(Configuration configuration) {
        com.networknt.schema.JsonSchemaFactory factory = com.networknt.schema.JsonSchemaFactory.getInstance(com.networknt.schema.SpecVersion.VersionFlag.V4);
        return Resources.getResource(schemaName).map(url ->
        {
            try {
                JsonSchema jsonSchema = factory.getSchema(url.toURI());
                JsonNode jsonNode = configuration.json();
                return jsonSchema.validate(jsonNode);
            } catch (URISyntaxException e) {
                return Set.of(ValidationMessage.builder().message(e.getMessage()).build());
            }
        }).orElse(Collections.emptySet());
    }
}
