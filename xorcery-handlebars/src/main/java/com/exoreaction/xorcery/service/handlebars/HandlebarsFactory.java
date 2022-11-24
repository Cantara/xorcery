package com.exoreaction.xorcery.service.handlebars;


import com.exoreaction.xorcery.service.handlebars.helpers.UtilHelpers;
import com.github.jknack.handlebars.Handlebars;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionManagerSupplier;
import org.jvnet.hk2.annotations.Service;

@Service
public class HandlebarsFactory
        implements Factory<Handlebars> {

    private final Handlebars handlebars;

    @Inject
    public HandlebarsFactory() {
        handlebars = new Handlebars()
                .registerHelpers(new UtilHelpers());

        handlebars.with(
                new SystemClassLoaderTemplateLoader("META-INF/handlebars/templates/", ".html")
        );

        handlebars.getCache().setReload(true);
    }

    @Override
    @Singleton
    public Handlebars provide() {
        return handlebars;
    }

    @Override
    public void dispose(Handlebars instance) {
    }
}
