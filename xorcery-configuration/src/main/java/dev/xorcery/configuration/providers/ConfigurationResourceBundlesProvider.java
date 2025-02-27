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
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.configuration.resourcebundle.ResourceBundles;
import dev.xorcery.configuration.resourcebundle.spi.ResourceBundlesProvider;
import dev.xorcery.configuration.spi.ResourceBundleTranslationProvider;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigurationResourceBundlesProvider
        implements ResourceBundlesProvider {
    private final Map<String, Configuration> configurations = new ConcurrentHashMap<>();
    private final Map<Locale, ResourceBundle> resourceBundles = new ConcurrentHashMap<>();
    private final List<ResourceBundleTranslationProvider> resourceBundleTranslationProviders = new ArrayList<>();

    public ConfigurationResourceBundlesProvider(Iterable<ResourceBundleTranslationProvider> resourceBundleTranslationProviderList) {
        Iterator<ResourceBundleTranslationProvider> providerIterator = resourceBundleTranslationProviderList.iterator();
        while (providerIterator.hasNext()) {
            try {
                ResourceBundleTranslationProvider provider = providerIterator.next();
                resourceBundleTranslationProviders.add(provider);
            } catch (Throwable e) {
                LogManager.getLogger().warn("Could not instantiate translator:" + e.getMessage());
            }

        }
        resourceBundleTranslationProviderList.forEach(resourceBundleTranslationProviders::add);
    }

    public ConfigurationResourceBundlesProvider() {
        this(ServiceLoader.load(ResourceBundleTranslationProvider.class));
    }

    @Override
    public ResourceBundle getBundle(String baseName, Locale locale) {
        if (baseName.equals(ResourceBundles.class.getName()))
            return resourceBundles.computeIfAbsent(locale, l -> new ConfigurationResourceBundle(this::getConfigurationByModule, this::getConfigurationByLocaleAndModule, locale, resourceBundleTranslationProviders));
        else
            return null;
    }

    protected Configuration getConfigurationByModule(String moduleName) {
        return configurations.computeIfAbsent(moduleName, name ->
                new ConfigurationBuilder(name)
                        .addModules()
                        .addModuleOverrides()
                        .addApplication()
                        .build());
    }

    protected Configuration getConfigurationByLocaleAndModule(String locale, String moduleName) {
        String configurationKey = locale + "/" + moduleName;
        Configuration configuration = configurations.get(configurationKey);
        if (configuration == null) {
            configuration = new ConfigurationBuilder("locales/" + locale + "/" + moduleName)
                    .addModules()
                    .addModuleOverrides()
                    .addApplication()
                    .build();
            configurations.put(configurationKey, configuration);
        }
        return configuration;
    }

    public void reload() {
        configurations.clear();
        resourceBundles.clear();
    }
}
