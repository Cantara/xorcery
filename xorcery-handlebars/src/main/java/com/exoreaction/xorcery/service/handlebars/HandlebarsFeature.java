package com.exoreaction.xorcery.service.handlebars;


import com.exoreaction.xorcery.service.handlebars.helpers.UtilHelpers;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
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

        File file = new File(getClass().getResource("/templates/jsonapi/resourcedocument.html").getFile())
                .getParentFile().getParentFile();
        if (file.exists()) {
            handlebars.with(
                    new FileTemplateLoader(file, ".html"),
                    new ClassPathTemplateLoader("/templates/", ".html")
            );
        } else {
                handlebars.with(
                    new ClassPathTemplateLoader("/templates/", ".html")
            );
        }

        handlebars.getCache().setReload(true);

        bind(handlebars);
    }
}
