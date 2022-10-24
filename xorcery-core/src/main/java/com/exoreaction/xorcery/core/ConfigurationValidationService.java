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
    private Provider<Configuration> configuration;

    @Inject
    public ConfigurationValidationService(Provider<Configuration> configuration) {
        this.configuration = configuration;
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
        return Optional.ofNullable(info.getCandidate().getName())
                .flatMap(name -> configuration.get().getBoolean(name + ".enabled"))
                .orElse(true);
    }
}
