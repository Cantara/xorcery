package com.exoreaction.xorcery.configuration.providers;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.resourcebundle.ResourceBundles;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ConfigurationResourceBundle
        extends ResourceBundle {
    private final Function<String, Configuration> configurations;
    private final Map<String, Object> cachedLookups = new ConcurrentHashMap<>();
    private final Locale locale;

    public ConfigurationResourceBundle(Function<String, Configuration> configurations, Locale locale) {
        this.configurations = configurations;
        this.locale = locale;
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
        if (keyConfiguration.object().isEmpty())
        {
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
                return language.get("default");
            } else if (result != null)
                return result;

            // key.country
            result = keyConfiguration.get(locale.getCountry()).orElse(null);
            if (result != null)
                return result;

            // key.default
            return keyConfiguration.get("default").orElseGet(()-> moduleConfiguration.get(key).orElse(null));
        } else {
            // key.language
            result = keyConfiguration.get(locale.getLanguage()).orElse(null);
            if (result instanceof Map<?, ?> language) {
                // key.language.default
                return language.get("default");
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
}
