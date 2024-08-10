package com.exoreaction.xorcery.configuration.providers;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.resourcebundle.ResourceBundles;
import com.exoreaction.xorcery.configuration.resourcebundle.spi.ResourceBundlesProvider;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigurationResourceBundlesProvider
    implements ResourceBundlesProvider
{
    private final Map<String, Configuration> configurations = new ConcurrentHashMap<>();
    private final Map<Locale, ResourceBundle> resourceBundles = new ConcurrentHashMap<>();

    public ConfigurationResourceBundlesProvider() {
    }

    @Override
    public ResourceBundle getBundle(String baseName, Locale locale) {
        if (baseName.equals(ResourceBundles.class.getName()))
            return resourceBundles.computeIfAbsent(locale, l -> new ConfigurationResourceBundle(this::getConfiguration, locale));
        else
            return null;
    }

    protected Configuration getConfiguration(String moduleName)
    {
        return configurations.computeIfAbsent(moduleName, name ->
                new Configuration.Builder().with(new StandardConfigurationBuilder(moduleName)::addDefaults).build());
    }
}
