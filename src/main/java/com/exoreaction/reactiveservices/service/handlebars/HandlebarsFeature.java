package com.exoreaction.reactiveservices.service.handlebars;


import com.exoreaction.reactiveservices.service.handlebars.helpers.UtilHelpers;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
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
        File file = new File(getClass().getResource("/templates/jsonapi/resourcedocument.html").getFile())
                .getParentFile().getParentFile();

        TemplateLoader templateLoader = new FileTemplateLoader(file, ".html");
        Handlebars handlebars = new Handlebars()
                .with(templateLoader)
                .registerHelpers(new UtilHelpers());
        handlebars.getCache().setReload(true);

        bind(handlebars);
    }
}
