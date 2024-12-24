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
package dev.xorcery.configuration.providers;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.resourcebundle.ResourceBundles;
import dev.xorcery.configuration.spi.ResourceBundleTranslationProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ConfigurationResourceBundle
        extends ResourceBundle {
    private final Function<String, Configuration> configurations;
    private final Map<String, Object> cachedLookups = new ConcurrentHashMap<>();
    private final Locale locale;
    private final List<ResourceBundleTranslationProvider> resourceBundleTranslationProvider;

    public ConfigurationResourceBundle(
            Function<String, Configuration> configurations,
            Locale locale,
            List<ResourceBundleTranslationProvider> resourceBundleTranslationProvider) {
        this.configurations = configurations;
        this.locale = locale;
        this.resourceBundleTranslationProvider = resourceBundleTranslationProvider;
    }

    @Override
    protected Object handleGetObject(String moduleKey) {
        return cachedLookups.computeIfAbsent(moduleKey, this::lookup);
    }

    private Object lookup(String moduleKey) {
        int dotIndex = moduleKey.indexOf('.');
        String module = moduleKey.substring(0, dotIndex);
        String key = moduleKey.substring(dotIndex + 1);
        Configuration moduleConfiguration = configurations.apply(module);
        Configuration keyConfiguration = moduleConfiguration.getConfiguration(key);
        if (keyConfiguration.object().isEmpty()) {
            // This is just a simple configuration
            return moduleConfiguration.get(key).orElse(null);
        }

        Object result = null;
        if (locale.getCountry() != null) {
            // key.language
            result = keyConfiguration.get(locale.getLanguage()).orElse(null);
            if (result instanceof Map<?, ?> language) {
                // key.language.country
                result = language.get(locale.getCountry());
                if (result != null)
                    return result;

                // key.language.default
                result = language.get("default");
                if (result instanceof String strResult) {
                    return translate(strResult);
                } else {
                    return result;
                }
            } else if (result != null)
                return result;

            // key.country
            result = keyConfiguration.get(locale.getCountry()).orElse(null);
            if (result != null)
                return result;

            // key.default
            result = keyConfiguration.get("default").orElseGet(() -> moduleConfiguration.get(key).orElse(null));
            if (result instanceof String strResult)
            {
                return translate(strResult);
            } else
            {
                return result;
            }
        } else {
            // key.language
            result = keyConfiguration.get(locale.getLanguage()).orElse(null);
            if (result instanceof Map<?, ?> language) {
                // key.language.default
                result = language.get("default");
                if (result instanceof String strResult)
                {
                    return translate(strResult);
                } else
                {
                    return result;
                }
            } else if (result != null)
                return result;

            // key.default
            return keyConfiguration.get("default").orElse(null);
        }
    }

    @Override
    public Enumeration<String> getKeys() {
        return Collections.emptyEnumeration();
    }

    @Override
    public String getBaseBundleName() {
        return ResourceBundles.class.getName();
    }

    private String translate(String result)
    {
        for (ResourceBundleTranslationProvider translationProvider : resourceBundleTranslationProvider) {
            try {
                String translatedResult = translationProvider.translate(result, locale).orTimeout(10, TimeUnit.SECONDS).join();
                if (translatedResult != null)
                    return translatedResult;
            } catch (Throwable e) {
                // Translation failed, ignore
            }
        }
        return result;
    }
}
