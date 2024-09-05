package com.exoreaction.xorcery.configuration.validation;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;

import java.net.URISyntaxException;
import java.util.Set;

public class ConfigurationValidator {
    public Set<ValidationMessage> validate(Configuration configuration)
    {
        try {
            com.networknt.schema.JsonSchemaFactory factory = com.networknt.schema.JsonSchemaFactory.getInstance(com.networknt.schema.SpecVersion.VersionFlag.V4);
            JsonSchema jsonSchema = factory.getSchema(Resources.getResource("xorcery-schema.json").orElseThrow().toURI());
            JsonNode jsonNode = configuration.json();
            return jsonSchema.validate(jsonNode);
        } catch (URISyntaxException e) {
            return Set.of(ValidationMessage.builder().message(e.getMessage()).build());
        }
    }
}
