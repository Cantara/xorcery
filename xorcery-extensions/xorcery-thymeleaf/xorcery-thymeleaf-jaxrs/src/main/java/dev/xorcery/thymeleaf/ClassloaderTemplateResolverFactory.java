package dev.xorcery.thymeleaf;

import dev.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
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
    public ITemplateResolver provide() {
        return super.provide();
    }
}
