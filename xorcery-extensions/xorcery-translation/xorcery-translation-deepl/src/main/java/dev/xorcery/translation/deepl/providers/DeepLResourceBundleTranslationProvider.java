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
package dev.xorcery.translation.deepl.providers;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.configuration.spi.ResourceBundleTranslationProvider;
import dev.xorcery.secrets.Secrets;
import dev.xorcery.secrets.providers.EnvSecretsProvider;
import dev.xorcery.secrets.providers.SecretSecretsProvider;
import dev.xorcery.secrets.providers.SystemPropertiesSecretsProvider;
import dev.xorcery.translation.deepl.DeepLTranslationProvider;
import org.apache.logging.log4j.LogManager;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DeepLResourceBundleTranslationProvider
    implements ResourceBundleTranslationProvider
{
    private final DeepLTranslationProvider translationProvider;
    private final Locale defaultLocale;
    private final List<String> targetLanguages;

    public DeepLResourceBundleTranslationProvider() throws ExecutionException, InterruptedException, TimeoutException {

        Secrets secrets = new Secrets(Map.of(
                "secret", new SecretSecretsProvider(),
                "env", new EnvSecretsProvider(),
                "system", new SystemPropertiesSecretsProvider()
                )::get, "secret");

        Configuration configuration = new ConfigurationBuilder().addDefaults().build();
        defaultLocale = Locale.getDefault();
        translationProvider = new DeepLTranslationProvider(configuration, secrets);
        targetLanguages = translationProvider.getTargetLanguages().get(10, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<String> translate(String result, Locale locale) {

        if (locale.equals(defaultLocale) || !targetLanguages.contains(locale.getLanguage()))
        {
            return null;
        } else
        {
            // Try to translate it
            try {
                return translationProvider.translate(List.of(result), defaultLocale, locale).thenApply(list -> list.get(0));
            } catch (Exception e) {
                LogManager.getLogger().warn("Could not translate '{}' to locale {}:{}", result, locale.toString(), e.getMessage());
                return CompletableFuture.completedFuture(null);
            }
        }
    }
}
