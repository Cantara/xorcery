package com.exoreaction.xorcery.configuration.providers;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.resourcebundle.ResourceBundles;
import com.exoreaction.xorcery.configuration.resourcebundle.spi.ResourceBundlesProvider;
import com.exoreaction.xorcery.configuration.spi.ResourceBundleTranslationProvider;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigurationResourceBundlesProvider
    implements ResourceBundlesProvider
{
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
                LogManager.getLogger().warn("Could not instantiate translator:"+e.getMessage());
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
            return resourceBundles.computeIfAbsent(locale, l -> new ConfigurationResourceBundle(this::getConfiguration, locale, resourceBundleTranslationProviders));
        else
            return null;
    }

    protected Configuration getConfiguration(String moduleName)
    {
        return configurations.computeIfAbsent(moduleName, name ->
                new Configuration.Builder().with(new StandardConfigurationBuilder(moduleName)::addDefaults).build());
    }

    public void reload() {
        configurations.clear();
        resourceBundles.clear();
    }
}
