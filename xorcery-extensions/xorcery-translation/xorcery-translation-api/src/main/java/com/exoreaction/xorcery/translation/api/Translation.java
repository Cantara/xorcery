package com.exoreaction.xorcery.translation.api;

import com.exoreaction.xorcery.translation.spi.TranslationProvider;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Service
public class Translation {

    private final TranslationProvider translationProvider;

    @Inject
    public Translation(TranslationProvider translationProvider) {
        this.translationProvider = translationProvider;
    }

    public CompletableFuture<String> translate(String text, Locale source, Locale target)
    {
        return translationProvider.translate(List.of(text), source, target).thenApply(translations -> translations.get(0));
    }

    public CompletableFuture<List<String>> translate(List<String> texts, Locale source, Locale target)
    {
        return translationProvider.translate(texts, source, target);
    }

    public CompletableFuture<List<String>> getSourceLanguages()
    {
        return translationProvider.getSourceLanguages();
    }

    public CompletableFuture<List<String>> getTargetLanguages()
    {
        return translationProvider.getTargetLanguages();
    }
}
