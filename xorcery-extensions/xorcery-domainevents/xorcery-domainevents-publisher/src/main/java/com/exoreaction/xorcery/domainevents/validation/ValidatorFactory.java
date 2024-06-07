package com.exoreaction.xorcery.domainevents.validation;

import jakarta.inject.Inject;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;

@Service(name="validator")
public class ValidatorFactory
    implements Factory<Validator>, PreDestroy
{
    private final jakarta.validation.ValidatorFactory factory;

    @Inject
    public ValidatorFactory() {
        factory = Validation.buildDefaultValidatorFactory();
    }

    public jakarta.validation.ValidatorFactory getFactory() {
        return factory;
    }

    @Override
    public Validator provide() {
        return factory.getValidator();
    }

    @Override
    public void dispose(Validator instance) {
    }

    @Override
    public void preDestroy() {
        factory.close();
    }
}
