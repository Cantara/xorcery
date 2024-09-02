package com.exoreaction.xorcery.translation.deepl.providers;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.configuration.spi.ResourceBundleTranslationProvider;
import com.exoreaction.xorcery.secrets.Secrets;
import com.exoreaction.xorcery.secrets.providers.EnvSecretsProvider;
import com.exoreaction.xorcery.secrets.providers.SecretSecretsProvider;
import com.exoreaction.xorcery.secrets.providers.SystemPropertiesSecretsProvider;
import com.exoreaction.xorcery.translation.deepl.DeepLTranslationProvider;
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
