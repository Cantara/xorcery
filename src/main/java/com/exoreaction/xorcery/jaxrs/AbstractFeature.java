package com.exoreaction.xorcery.jaxrs;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.ServiceConfiguration;
import com.exoreaction.xorcery.configuration.StandardConfiguration;
import com.exoreaction.xorcery.server.Xorcery;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.InjectionManagerProvider;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;

import java.util.Objects;

public abstract class AbstractFeature
        extends AbstractBinder
        implements Feature {
    protected FeatureContext context;
    protected InjectionManager injectionManager;

    protected ServiceResourceObject resourceObject;

    @Override
    public boolean configure(FeatureContext context) {
        this.context = context;
        this.injectionManager = InjectionManagerProvider.getInjectionManager(context);
        if (isEnabled()) {
            Configuration configuration = injectionManager.getInstance(Configuration.class);
            String serviceType = Objects.requireNonNull(serviceType(), "Service type may not be null");
            StandardConfiguration.Impl standardConfiguration = new StandardConfiguration.Impl(configuration);
            ServiceResourceObject.Builder builder = new ServiceResourceObject.Builder(standardConfiguration, serviceType);
            builder.attributes().attribute("environment", standardConfiguration.getEnvironment());
            builder.attributes().attribute("tag", standardConfiguration.getTag());
            buildResourceObject(builder);
            resourceObject = builder.build();
            bind(resourceObject).named(serviceType);

            if (!super.getBindings().isEmpty()) {
                injectionManager.register(this);
            }
            return true;
        } else
            return false;
    }

    abstract protected String serviceType();

    protected void buildResourceObject(ServiceResourceObject.Builder builder) {

    }

    protected Xorcery xorcery() {
        return injectionManager.getInstance(Xorcery.class);
    }

    protected Configuration configuration() {
        return injectionManager.getInstance(Configuration.class);
    }

    protected boolean isEnabled() {
        return new ServiceConfiguration(configuration().getConfiguration(serviceType())).isEnabled();
    }
}
