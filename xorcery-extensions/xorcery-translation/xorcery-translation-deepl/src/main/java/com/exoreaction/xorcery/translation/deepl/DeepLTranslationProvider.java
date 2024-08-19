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

    @Override
    public List<String> translate(List<String> text, Locale source, Locale target) {

        try {
            return translator.translateText(text, source.getLanguage(), target.getLanguage()).stream().map(TextResult::getText).toList();
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public List<String> getSourceLanguages() {
        try {
            return translator.getSourceLanguages().stream().map(Language::getCode).toList();
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public List<String> getTargetLanguages() {
        try {
            return translator.getTargetLanguages().stream().map(Language::getCode).toList();
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
