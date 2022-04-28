package com.exoreaction.reactiveservices.jaxrs;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.configuration.ServiceConfiguration;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.helpers.ServiceResourceObjectBuilder;
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

    protected ResourceObject resourceObject;

    @Override
    public boolean configure(FeatureContext context)
    {
        this.context = context;
        this.injectionManager = ((InjectionManagerSupplier) context).getInjectionManager();
        if (isEnabled())
        {
            String serviceType = Objects.requireNonNull(serviceType(), "Service type may not be null");
            ServiceResourceObjectBuilder builder = new ServiceResourceObjectBuilder(server(), serviceType);
            buildResourceObject(builder);
            server().addService(resourceObject = builder.build());

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

    protected void buildResourceObject(ServiceResourceObjectBuilder builder)
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
