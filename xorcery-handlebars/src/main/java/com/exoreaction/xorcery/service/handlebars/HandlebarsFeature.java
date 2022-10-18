package com.exoreaction.xorcery.service.handlebars;


import com.exoreaction.xorcery.service.handlebars.helpers.UtilHelpers;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionManagerSupplier;

import java.io.File;

@Provider
public class HandlebarsFeature
        extends AbstractBinder
        implements Feature {
    @Override
    public boolean configure(FeatureContext context) {

        InjectionManager injectionManager = ((InjectionManagerSupplier) context).getInjectionManager();
        injectionManager.register(this);

        return true;
    }

    @Override
    protected void configure() {

        Handlebars handlebars = new Handlebars()
                .registerHelpers(new UtilHelpers());

        handlebars.with(
                new SystemClassLoaderTemplateLoader("META-INF/handlebars/templates/", ".html")
        );

        handlebars.getCache().setReload(true);

        bind(handlebars);
    }
}
