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
package dev.xorcery.translation.deepl.test;

import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.junit.XorceryExtension;
import dev.xorcery.translation.api.Translation;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Disabled("only enabled during development, requires authkey")
public class DeepLTranslationProviderTest {

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .addYaml("""
            log4j2:
              Configuration:
                thresholdFilter:
                  level: "info"
            """)
            .build();

    @Test
    public void testTranslate(Translation translation) {
        System.out.println(translation.translate("Not started", Locale.forLanguageTag("en"), Locale.forLanguageTag("da")).orTimeout(10, TimeUnit.SECONDS).join());
        System.out.println(translation.translate("You made %s amount of %s", Locale.forLanguageTag("en"), Locale.forLanguageTag("da")).orTimeout(10, TimeUnit.SECONDS).join());
        System.out.println(translation.translate("There were %s errors", Locale.forLanguageTag("en"), Locale.forLanguageTag("da")).orTimeout(10, TimeUnit.SECONDS).join());
        System.out.println(translation.translate("You made %s amount of %s", Locale.forLanguageTag("en"), Locale.forLanguageTag("sv")).orTimeout(10, TimeUnit.SECONDS).join());
        System.out.println(translation.translate("There were %s errors", Locale.forLanguageTag("en"), Locale.forLanguageTag("sv")).orTimeout(10, TimeUnit.SECONDS).join());
        System.out.println(translation.translate("You made %s amount of %s", Locale.forLanguageTag("en"), Locale.forLanguageTag("de")).orTimeout(10, TimeUnit.SECONDS).join());
        System.out.println(translation.translate("There were %s errors", Locale.forLanguageTag("en"), Locale.forLanguageTag("de")).orTimeout(10, TimeUnit.SECONDS).join());
    }

    @Test
    public void testGetSourceLanguages(Translation translation) {
        System.out.println(translation.getSourceLanguages().orTimeout(10, TimeUnit.SECONDS).join());
    }

    @Test
    public void testGetTargetLanguages(Translation translation) {
        System.out.println(translation.getSourceLanguages().orTimeout(10, TimeUnit.SECONDS).join());
    }
}
