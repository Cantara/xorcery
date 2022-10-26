package com.exoreaction.xorcery.core;

import com.exoreaction.xorcery.configuration.model.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.glassfish.hk2.api.*;
import org.jvnet.hk2.annotations.Service;

import java.util.Optional;

@Service
public class ConfigurationValidationService
        implements ValidationService, Validator, Filter {
    private Provider<Configuration> configurationProvider;
    private Configuration configuration;

    @Inject
    public ConfigurationValidationService(Provider<Configuration> configuration) {
        this.configurationProvider = configuration;
    }

    @Override
    public Filter getLookupFilter() {
        return this;
    }

    @Override
    public Validator getValidator() {
        return this;
    }

    @Override
    public boolean matches(Descriptor d) {
        return true;
    }

    @Override
    public boolean validate(ValidationInformation info) {

        try {
            if (info.getCandidate().getImplementation().equals(Configuration.class.getName()))
                return true;

            if (configuration == null)
            {
                configuration = configurationProvider.get();
            }

            boolean result = Optional.ofNullable(info.getCandidate().getName())
                    .flatMap(name -> configuration.getBoolean(name + ".enabled"))
                    .orElse(true);

//            System.out.println(info.getCandidate().getImplementation()+":"+result);
            return result;
        } catch (Throwable e) {
            return true;
        }
    }
}
