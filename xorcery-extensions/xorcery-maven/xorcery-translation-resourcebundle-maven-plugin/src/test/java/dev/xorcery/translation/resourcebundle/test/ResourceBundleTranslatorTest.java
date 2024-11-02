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