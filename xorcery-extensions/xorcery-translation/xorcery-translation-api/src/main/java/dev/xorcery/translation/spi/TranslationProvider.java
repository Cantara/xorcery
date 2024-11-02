package dev.xorcery.translation.spi;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public interface TranslationProvider {
    CompletableFuture<List<String>> translate(List<String> text, Locale source, Locale target);

    CompletableFuture<List<String>> getSourceLanguages();
    CompletableFuture<List<String>> getTargetLanguages();
}