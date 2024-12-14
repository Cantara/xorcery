package dev.xorcery.thymeleaf;

import dev.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
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
    public ITemplateResolver provide() {
        return super.provide();
    }
}
