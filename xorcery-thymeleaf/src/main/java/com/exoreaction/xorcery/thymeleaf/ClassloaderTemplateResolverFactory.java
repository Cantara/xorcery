package com.exoreaction.xorcery.thymeleaf;

import com.exoreaction.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Service(name="thymeleaf.classloader")
public class ClassloaderTemplateResolverFactory
    extends TemplateResolverFactory
{
    @Inject
    public ClassloaderTemplateResolverFactory(Configuration configuration) {
        super(new TemplateResolverConfiguration(configuration.getConfiguration("thymeleaf.classloader")), new ClassLoaderTemplateResolver());
    }

    @Override
    @Named("thymeleaf.classloader")
    public ITemplateResolver provide() {
        return super.provide();
    }
}
