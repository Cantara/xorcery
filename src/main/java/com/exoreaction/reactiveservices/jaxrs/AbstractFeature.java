package com.exoreaction.reactiveservices.jaxrs;

import com.exoreaction.reactiveservices.server.Server;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionManagerSupplier;

public abstract class AbstractFeature
        extends AbstractBinder
    implements Feature
{
    @Override
    public boolean configure(FeatureContext context)
    {
        InjectionManager injectionManager = ((InjectionManagerSupplier) context).getInjectionManager();
        return configure(context, injectionManager, injectionManager.getInstance(Server.class));
    }

    public boolean configure(FeatureContext context, InjectionManager injectionManager, Server server)
    {
        if (!super.getBindings().isEmpty())
        {
            injectionManager.register(this);
        }
        return true;
    }

    @Override
    protected void configure()
    {
    }
}
