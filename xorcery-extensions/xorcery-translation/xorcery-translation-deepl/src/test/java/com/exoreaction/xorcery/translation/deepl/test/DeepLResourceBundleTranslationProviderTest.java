package com.exoreaction.xorcery.translation.deepl.test;

import com.exoreaction.xorcery.configuration.spi.ResourceBundleTranslationProvider;
import com.exoreaction.xorcery.translation.deepl.providers.DeepLResourceBundleTranslationProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Disabled("only enabled during development, requires authkey")
public class DeepLResourceBundleTranslationProviderTest
{

    protected static ResourceBundleTranslationProvider translationProvider;

    @BeforeAll
    public static void setup() throws ExecutionException, InterruptedException, TimeoutException {
        translationProvider = new DeepLResourceBundleTranslationProvider();
    }

    @Test
    public void testTranslate()
    {
        String result = translationProvider.translate("This is a test", Locale.GERMAN).orTimeout(10, TimeUnit.SECONDS).join();

        Assertions.assertEquals("Dies ist ein Test", result);
    }

    @Test
    public void testTranslateFailed()
    {
        String result = translationProvider.translate("This is a test", new Locale.Builder().setLanguage("am").build()).orTimeout(10, TimeUnit.SECONDS).join();
        Assertions.assertNull(result);
    }

}
