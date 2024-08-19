package com.exoreaction.xorcery.translation.api;

import com.exoreaction.xorcery.translation.spi.TranslationProvider;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.Locale;

@Service
public class Translation {

    private final TranslationProvider translationProvider;

    @Inject
    public Translation(TranslationProvider translationProvider) {
        this.translationProvider = translationProvider;
    }

    public String translate(String text, Locale source, Locale target)
    {
        return translationProvider.translate(List.of(text), source, target).get(0);
    }

    public List<String> getSourceLanguages()
    {
        return translationProvider.getSourceLanguages();
    }

    public List<String> getTargetLanguages()
    {
        return translationProvider.getTargetLanguages();
    }
}
