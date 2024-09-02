package com.exoreaction.xorcery.translation.deepl.test;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.translation.api.Translation;
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
