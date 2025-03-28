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
package dev.xorcery.configuration.validation;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.InstanceConfiguration;
import dev.xorcery.json.JsonElement;
import dev.xorcery.util.Resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

public class ConfigurationValidator {
    final private ObjectMapper objectMapper = new ObjectMapper();

    public ConfigurationValidator() {
    }

    public Set<ValidationMessage> validate(Configuration configuration) {
        return configuration.getString("$schema").map(schemaReference ->
        {
            String schemaString = null;
            URL schemaUrl = null;
            try {
                URI home = new File(InstanceConfiguration.get(configuration).getHome()).toURI();
                schemaUrl = home.resolve(schemaReference).toURL();
                InputStream schemaInputStream = null;
                int from = schemaReference.length();
                do {
                    try {
                        schemaInputStream = schemaUrl.openStream();
                    } catch (IOException e) {
                        from = schemaReference.lastIndexOf('/', from - 1);
                        if (from != -1)
                        {
                            String classPathResource = schemaReference.substring(from);
                            schemaUrl = Resources.getResource(classPathResource).orElse(schemaUrl);
                        }
                    }
                } while (schemaInputStream == null && from != -1);

                if (schemaInputStream == null)
                {
                    return Collections.<ValidationMessage>emptySet();
                }

                schemaString = new String(schemaInputStream.readAllBytes(), StandardCharsets.UTF_8);

                JsonNode jsonSchema = objectMapper.readTree(schemaString);
                if (jsonSchema.isMissingNode())
                {
                    return Collections.<ValidationMessage>emptySet();
                }

                JsonElement jsonSchemaElement = () -> jsonSchema;
                return jsonSchemaElement.getString("$schema").map(schemaSchemaUrl ->
                {
                    SpecVersion.VersionFlag versionFlag = SpecVersion.VersionFlag.fromId(schemaSchemaUrl).orElse(SpecVersion.VersionFlag.V202012);
                    com.networknt.schema.JsonSchemaFactory factory = com.networknt.schema.JsonSchemaFactory.getInstance(versionFlag);
                    JsonSchema schema = factory.getSchema(jsonSchema);
                    JsonNode jsonNode = configuration.json();
                    return schema.validate(jsonNode);
                }).orElse(Collections.emptySet());
            } catch (JsonParseException e) {
                throw new RuntimeException(String.format("Invalid schema(%s):%s",schemaUrl,schemaString));
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }).orElse(Collections.emptySet());
    }
}
