/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.translation.api;

import dev.xorcery.translation.spi.TranslationProvider;
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
