package com.exoreaction.xorcery.thymeleaf;

import com.exoreaction.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.jvnet.hk2.annotations.Service;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

@Service(name = "thymeleaf.webapplication")
public class WebApplicationTemplateResolverFactory
        extends TemplateResolverFactory {
    @Inject
    public WebApplicationTemplateResolverFactory(Configuration configuration, ServletContextHandler servletContextHandler) {
        super(
                new TemplateResolverConfiguration(configuration.getConfiguration("thymeleaf.webapplication")),
                new WebApplicationTemplateResolver(JakartaServletWebApplication.buildApplication(servletContextHandler.getServletContext()))
        );
    }

    @Override
    @Named("thymeleaf.webapplication")
    public ITemplateResolver provide() {
        return super.provide();
    }
}
