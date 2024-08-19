package com.exoreaction.xorcery.translation.deepl.test;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.translation.api.Translation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Locale;

public class DeepLTranslationProviderTest {

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .build();

    @Test
    public void testTranslate(Translation translation)
    {
        System.out.println(translation.translate("Not started", Locale.forLanguageTag("en"), Locale.forLanguageTag("da")));
    }

    @Test
    public void testGetSourceLanguages(Translation translation)
    {
        System.out.println(translation.getSourceLanguages());
    }

    @Test
    public void testGetTargetLanguages(Translation translation)
    {
        System.out.println(translation.getSourceLanguages());
    }
}
