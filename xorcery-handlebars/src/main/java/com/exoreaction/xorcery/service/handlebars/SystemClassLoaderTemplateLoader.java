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
package com.exoreaction.xorcery.service.handlebars;

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
