package com.exoreaction.xorcery.thymeleaf;

import com.exoreaction.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
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
