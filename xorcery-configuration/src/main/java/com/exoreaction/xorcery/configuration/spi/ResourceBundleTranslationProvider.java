package com.exoreaction.xorcery.configuration.spi;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public interface ResourceBundleTranslationProvider {

    /**
     * Translate text from default locale ({@link Locale#getDefault()}) to the given locale
     * @param text to be translated
     * @param locale to be translated to
     * @return translated text or null if no translation is found or translation failed
     */
    CompletableFuture<String> translate(String text, Locale locale);
}
