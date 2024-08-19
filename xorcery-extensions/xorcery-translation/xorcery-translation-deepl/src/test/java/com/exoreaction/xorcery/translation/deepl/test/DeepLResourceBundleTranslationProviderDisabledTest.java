package com.exoreaction.xorcery.translation.deepl.test;

import com.exoreaction.xorcery.configuration.resourcebundle.ResourceBundles;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

public class DeepLResourceBundleTranslationProviderDisabledTest
{
    @Test
    public void testProviderDisabled()
    {
        // Ensure getBundle doesn't crash when the translation provider cannot be instantiated because there is no authkey set
        Assertions.assertEquals("development", ResourceBundle.getBundle(ResourceBundles.class.getName(), Locale.UK).getString("xorcery.instance.environment"));
    }
}
