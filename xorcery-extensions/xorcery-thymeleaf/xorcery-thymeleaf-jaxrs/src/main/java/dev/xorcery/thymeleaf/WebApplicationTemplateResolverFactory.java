package dev.xorcery.thymeleaf;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.hk2.Services;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Handler;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

@Service(name = "thymeleaf.webapplication")
public class WebApplicationTemplateResolverFactory
        extends TemplateResolverFactory {
    @Inject
    public WebApplicationTemplateResolverFactory(Configuration configuration, IterableProvider<Handler> handlers) {
        super(
                new TemplateResolverConfiguration(configuration.getConfiguration("thymeleaf.webapplication")),
                new WebApplicationTemplateResolver(JakartaServletWebApplication.buildApplication(Services.ofType(handlers, ServletContextHandler.class).get().getServletContext()))
        );
    }

    @Override
    @Named("thymeleaf.webapplication")
    public ITemplateResolver provide() {
        return super.provide();
    }
}
