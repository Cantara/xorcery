package com.exoreaction.xorcery.translation.deepl;

import com.deepl.api.Language;
import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.secrets.Secrets;
import com.exoreaction.xorcery.translation.spi.TranslationProvider;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Service
@ContractsProvided(TranslationProvider.class)
public class DeepLTranslationProvider
        implements TranslationProvider {

    private final Translator translator;

    @Inject
    public DeepLTranslationProvider(Configuration configuration, Secrets secrets) {
        String authKey = configuration.getString("deepl.authkey").map(secrets::getSecretString).orElseThrow(Configuration.missing("deepl.authkey"));
        translator = new Translator(authKey);
    }

    public Translator getTranslator() {
        return translator;
    }

    @Override
    public CompletableFuture<List<String>> translate(List<String> text, Locale source, Locale target) {

        try {
            return CompletableFuture.completedFuture(translator.translateText(text, source.toLanguageTag(), target.toLanguageTag()).stream().map(TextResult::getText).toList());
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<List<String>> getSourceLanguages() {
        try {
            return CompletableFuture.completedFuture(translator.getSourceLanguages().stream().map(Language::getCode).toList());
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<List<String>> getTargetLanguages() {
        try {
            return CompletableFuture.completedFuture(translator.getTargetLanguages().stream().map(Language::getCode).toList());
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
