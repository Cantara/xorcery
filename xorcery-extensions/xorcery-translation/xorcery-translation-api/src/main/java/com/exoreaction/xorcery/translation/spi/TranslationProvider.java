package com.exoreaction.xorcery.translation.spi;

import java.util.List;
import java.util.Locale;

public interface TranslationProvider {
    List<String> translate(List<String> text, Locale source, Locale target);

    List<String> getSourceLanguages();
    List<String> getTargetLanguages();
}