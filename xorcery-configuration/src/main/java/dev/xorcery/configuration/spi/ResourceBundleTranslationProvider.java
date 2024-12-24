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
package dev.xorcery.configuration.spi;

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
