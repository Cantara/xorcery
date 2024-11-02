package dev.xorcery.configuration.providers;

import dev.xorcery.configuration.resourcebundle.ResourceBundles;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.spi.ResourceBundleControlProvider;

public class ConfigurationResourceBundleControlProvider
        implements ResourceBundleControlProvider {

    private static final ResourceBundle.Control INSTANCE = new ConfigurationControl();

    @Override
    public ResourceBundle.Control getControl(String baseName) {
        if (baseName.equals(ResourceBundles.class.getName())) {
            return INSTANCE;
        } else {
            return null;
        }
    }

    public static class ConfigurationControl
            extends ResourceBundle.Control {

        private final ConfigurationResourceBundlesProvider provider = new ConfigurationResourceBundlesProvider();

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            if (reload)
            {
                provider.reload();
            }
            return provider.getBundle(baseName, locale);
        }
    }
}
