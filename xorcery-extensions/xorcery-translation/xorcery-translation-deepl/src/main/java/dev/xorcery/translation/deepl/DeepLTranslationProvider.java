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
package dev.xorcery.translation.deepl;

import com.deepl.api.Language;
import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.secrets.Secrets;
import dev.xorcery.translation.spi.TranslationProvider;
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
