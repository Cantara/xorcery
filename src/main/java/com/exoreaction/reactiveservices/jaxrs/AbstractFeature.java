package com.exoreaction.reactiveservices.jaxrs;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.configuration.ServiceConfiguration;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionManagerSupplier;

import java.util.Objects;

public abstract class AbstractFeature
        extends AbstractBinder
    implements Feature
{
    protected FeatureContext context;
    protected InjectionManager injectionManager;

    protected ServiceResourceObject resourceObject;

    @Override
    public boolean configure(FeatureContext context)
    {
        this.context = context;
        this.injectionManager = ((InjectionManagerSupplier) context).getInjectionManager();
        if (isEnabled())
        {
            Configuration configuration = injectionManager.getInstance(Configuration.class);
            String serviceType = Objects.requireNonNull(serviceType(), "Service type may not be null");
            ServiceResourceObject.Builder builder = new ServiceResourceObject.Builder(server(), serviceType);
            configuration.getString("environment")
                    .ifPresent(env -> builder.attributes().attribute("environment", env));
            configuration.getString("tag")
                    .ifPresent(tag -> builder.attributes().attribute("tag", tag));
            buildResourceObject(builder);
            resourceObject = builder.build();           ;
            server().addService(resourceObject.resourceObject());
            bind(resourceObject).named(serviceType);

            if (!super.getBindings().isEmpty())
            {
                injectionManager.register(this);
            }
            return true;
        }
        else
            return false;
    }

    abstract protected String serviceType();

    protected void buildResourceObject(ServiceResourceObject.Builder builder)
    {

    }
    protected Server server()
    {
        return injectionManager.getInstance(Server.class);
    }
    protected Configuration configuration()
    {
        return injectionManager.getInstance(Configuration.class);
    }

    protected boolean isEnabled()
    {
        return new ServiceConfiguration(configuration().getConfiguration(serviceType())).isEnabled();
    }
}
