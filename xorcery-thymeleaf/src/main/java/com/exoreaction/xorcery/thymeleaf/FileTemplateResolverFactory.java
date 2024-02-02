package com.exoreaction.xorcery.thymeleaf;

import com.exoreaction.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jvnet.hk2.annotations.Service;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Service(name="thymeleaf.file")
public class FileTemplateResolverFactory
    extends TemplateResolverFactory
{
    @Inject
    public FileTemplateResolverFactory(Configuration configuration) {
        super(new TemplateResolverConfiguration(configuration.getConfiguration("thymeleaf.file")), new FileTemplateResolver());
    }

    @Override
    @Named("thymeleaf.file")
    public ITemplateResolver provide() {
        return super.provide();
    }
}
