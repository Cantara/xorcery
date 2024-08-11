package com.exoreaction.xorcery.configuration.builder.test;

import com.exoreaction.xorcery.configuration.resourcebundle.ResourceBundles;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class ResourceBundlesTest {

    @Test
    public void testGetResourceBundle()
    {
        {
            ResourceBundle bundle = ResourceBundle.getBundle(ResourceBundles.class.getName(), Locale.ENGLISH);
            Assertions.assertEquals(ResourceBundles.class.getName(), bundle.getBaseBundleName());
            Assertions.assertEquals("Title", bundle.getString("foo.title"));
            Assertions.assertEquals("Color", bundle.getString("foo.color"));
            Assertions.assertEquals("yyyy-MM-dd", bundle.getString("foo.dateFormat"));
            Assertions.assertEquals(5, bundle.getObject("foo.setting"));
            Assertions.assertEquals("{abc={list=[item1, item2, item3], bar=test, foo=3}}", bundle.getObject("foo.complexsetting").toString());
        }

        {
            ResourceBundle bundle = ResourceBundle.getBundle(ResourceBundles.class.getName(), Locale.UK);
            Assertions.assertEquals(ResourceBundles.class.getName(), bundle.getBaseBundleName());
            Assertions.assertEquals("Title", bundle.getString("foo.title"));
            Assertions.assertEquals("Colour", bundle.getString("foo.color"));
            Assertions.assertEquals("dd/MM/yyyy", bundle.getString("foo.dateFormat"));
        }

        {
            ResourceBundle bundle = ResourceBundle.getBundle(ResourceBundles.class.getName(), Locale.US);
            Assertions.assertEquals(ResourceBundles.class.getName(), bundle.getBaseBundleName());
            Assertions.assertEquals("Title", bundle.getString("foo.title"));
            Assertions.assertEquals("Color", bundle.getString("foo.color"));
            Assertions.assertEquals("MM-dd-yyyy", bundle.getString("foo.dateFormat"));
        }

        {
            ResourceBundle bundle = ResourceBundle.getBundle(ResourceBundles.class.getName(), new Locale("sv"));
            Assertions.assertEquals(ResourceBundles.class.getName(), bundle.getBaseBundleName());
            Assertions.assertEquals("Titel", bundle.getString("foo.title"));
            Assertions.assertEquals("Farg", bundle.getString("foo.color"));
        }

        {
            ResourceBundle bundle = ResourceBundle.getBundle(ResourceBundles.class.getName(), new Locale("no"));
            Assertions.assertEquals(ResourceBundles.class.getName(), bundle.getBaseBundleName());
            Assertions.assertEquals("Titel", bundle.getString("foo.title"));
        }
    }
    
    @Test
    public void testResourceBundles()
    {
        {
            ResourceBundles bundle = ResourceBundles.getBundle("foo", Locale.ENGLISH);
            Assertions.assertEquals("Title", bundle.getString("title").orElse(null));
            Assertions.assertEquals("Color", bundle.getString("color").orElse(null));
            Assertions.assertEquals("yyyy-MM-dd", bundle.getString("dateFormat").orElse(null));
            Assertions.assertEquals(5, bundle.getInteger("setting").orElse(null));
            Assertions.assertEquals("{abc={list=[item1, item2, item3], bar=test, foo=3}}", bundle.<Map<String,Object>>get("complexsetting").orElse(Collections.emptyMap()).toString());
        }

        {
            ResourceBundles bundle = ResourceBundles.getBundle("foo", Locale.UK);
            Assertions.assertEquals("Title", bundle.getString("title").orElse(null));
            Assertions.assertEquals("Colour", bundle.getString("color").orElse(null));
            Assertions.assertEquals("dd/MM/yyyy", bundle.getString("dateFormat").orElse(null));
        }

        {
            ResourceBundles bundle = ResourceBundles.getBundle("foo", Locale.US);
            Assertions.assertEquals("Title", bundle.getString("title").orElse(null));
            Assertions.assertEquals("Color", bundle.getString("color").orElse(null));
            Assertions.assertEquals("MM-dd-yyyy", bundle.getString("dateFormat").orElse(null));
        }

        {
            ResourceBundles bundle = ResourceBundles.getBundle("foo", new Locale("sv"));
            Assertions.assertEquals("Titel", bundle.getString("title").orElse(null));
            Assertions.assertEquals("Farg", bundle.getString("color").orElse(null));
        }

        {
            ResourceBundles bundle = ResourceBundles.getBundle("foo", new Locale("no"));
            Assertions.assertEquals("Titel", bundle.getString("title").orElse(null));
        }
    }
}
