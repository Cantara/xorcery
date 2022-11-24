package com.exoreaction.xorcery.service.handlebars;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.util.Resources;
import com.github.jknack.handlebars.io.URLTemplateLoader;

import java.io.IOException;
import java.net.URL;

/**
 * This template loader uses the SystemClassLoader to allow loading of templates in a JPMS application, where the templates
 * may be in separate modules and access is governed by module restrictions.
 */
public class SystemClassLoaderTemplateLoader extends URLTemplateLoader {

    /**
     * Creates a new {@link SystemClassLoaderTemplateLoader}.
     *
     * @param prefix The view prefix. Required.
     * @param suffix The view suffix. Required.
     */
    public SystemClassLoaderTemplateLoader(final String prefix, final String suffix) {
        setPrefix(prefix);
        setSuffix(suffix);
    }

    /**
     * Creates a new {@link SystemClassLoaderTemplateLoader}.
     *
     * @param prefix The view prefix. Required.
     */
    public SystemClassLoaderTemplateLoader(final String prefix) {
        this(prefix, DEFAULT_SUFFIX);
    }

    /**
     * Creates a new {@link SystemClassLoaderTemplateLoader}. It looks for templates
     * stored in the root of the classpath.
     */
    public SystemClassLoaderTemplateLoader() {
        this("");
    }

    @Override
    protected URL getResource(final String location) throws IOException {
        return Resources.getResource(location)
                .orElseThrow(()->new IOException("Could not find resource "+location));
    }
}
