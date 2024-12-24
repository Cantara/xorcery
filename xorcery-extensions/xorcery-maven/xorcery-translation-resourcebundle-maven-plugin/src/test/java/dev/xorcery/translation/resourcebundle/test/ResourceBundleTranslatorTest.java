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
package dev.xorcery.translation.resourcebundle.test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.secrets.Secrets;
import dev.xorcery.secrets.providers.EnvSecretsProvider;
import dev.xorcery.translation.api.Translation;
import dev.xorcery.translation.deepl.DeepLTranslationProvider;
import dev.xorcery.translation.resourcebundle.ResourceBundleTranslator;
import dev.xorcery.util.Resources;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Disabled("only enabled during development as it requires DeepL auth key as 'deepl' environment variable")
class ResourceBundleTranslatorTest {

    @Test
    public void testResourceBundleTranslator()
    {
        Configuration configuration = new ConfigurationBuilder()
                .addTestDefaults()
                .build();
        ResourceBundleTranslator resourceBundleTranslator = new ResourceBundleTranslator(new Translation(new DeepLTranslationProvider(configuration, new Secrets(Map.of("env", new EnvSecretsProvider())::get, "env"))));

        File resultFile = new File(Resources.getResource("resourcebundle-override.yaml").orElseThrow().getFile());

        ObjectNode result = resourceBundleTranslator.translate("resourcebundle", Locale.ENGLISH, resultFile,
                        List.of(Locale.forLanguageTag("sv"),
                                Locale.forLanguageTag("de"),
                                Locale.forLanguageTag("da"),
                                Locale.forLanguageTag("ja"),
                                Locale.forLanguageTag("ru")))
                .orTimeout(10, TimeUnit.SECONDS).join();

        System.out.println(new Configuration(result));
    }
}