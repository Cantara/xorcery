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
package dev.xorcery.thymeleaf;

import jakarta.inject.Inject;
import org.glassfish.hk2.api.Factory;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

public abstract class TemplateResolverFactory
    implements Factory<ITemplateResolver>
{

    private final AbstractConfigurableTemplateResolver templateResolver;

    @Inject
    public TemplateResolverFactory(TemplateResolverConfiguration resolverConfiguration, AbstractConfigurableTemplateResolver templateResolver) {

        this.templateResolver = templateResolver;

        templateResolver.setCharacterEncoding(resolverConfiguration.getCharacterEncoding());
        templateResolver.setTemplateMode(resolverConfiguration.getTemplateMode());
        // This will convert "home" to "/WEB-INF/thymeleaf/templates/home.html"
        templateResolver.setPrefix(resolverConfiguration.getPrefix());
        templateResolver.setSuffix(resolverConfiguration.getSuffix());

        templateResolver.setCacheable(resolverConfiguration.getCacheable());
        templateResolver.setCacheTTLMs(resolverConfiguration.getCacheTTL().toMillis());

        templateResolver.setCheckExistence(resolverConfiguration.isCheckExistence());
        if (!resolverConfiguration.getCacheablePatterns().isEmpty())
            templateResolver.setCacheablePatterns(resolverConfiguration.getCacheablePatterns());
        if (!resolverConfiguration.getNonCacheablePatterns().isEmpty())
            templateResolver.setNonCacheablePatterns(resolverConfiguration.getNonCacheablePatterns());
        templateResolver.setResolvablePatterns(resolverConfiguration.getResolvablePatterns());
        templateResolver.setTemplateAliases(resolverConfiguration.getTemplateAliases());
        templateResolver.setUseDecoupledLogic(resolverConfiguration.isDecoupledLogic());
    }

    @Override
    public ITemplateResolver provide() {
        return templateResolver;
    }

    @Override
    public void dispose(ITemplateResolver instance) {
    }
}
