package com.exoreaction.xorcery.configuration.resourcebundle;

import com.exoreaction.xorcery.collections.Element;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

public record ResourceBundles(ResourceBundle resourceBundle, String bundleName)
    implements Element
{
    public static ResourceBundles getBundle(String bundleName, Locale locale)
    {
        return new ResourceBundles(ResourceBundle.getBundle(ResourceBundles.class.getName(), locale), bundleName);
    }

    public static ResourceBundles getBundle(String bundleName)
    {
        return new ResourceBundles(ResourceBundle.getBundle(ResourceBundles.class.getName(), Locale.getDefault()), bundleName);
    }

    @Override
    public <T> Optional<T> get(String name) {
        try {
            return Optional.ofNullable((T)resourceBundle.getObject(bundleName+"."+name));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
