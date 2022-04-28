package com.exoreaction.reactiveservices.service.sandbox;


import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

import java.io.File;

@Provider
public class SandboxFeature
    extends AbstractFeature
{
    @Override
    protected String serviceType() {
        return "sandbox";
    }

    @Override
    protected void configure() {
        File file = new File(getClass().getResource("/templates/sandbox/resourcedocument.html").getFile())
                .getParentFile().getParentFile();

        TemplateLoader templateLoader = new FileTemplateLoader(file, ".html");
        Handlebars handlebars = new Handlebars().with(templateLoader);
        handlebars.getCache().setReload(true);

        bind(handlebars);
    }
}
