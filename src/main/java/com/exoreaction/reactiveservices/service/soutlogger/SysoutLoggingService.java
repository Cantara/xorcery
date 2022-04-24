package com.exoreaction.reactiveservices.service.soutlogger;

import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.configuration.Configuration;
import com.exoreaction.reactiveservices.service.loggingconsumer.LoggingConsumerService;
import com.exoreaction.reactiveservices.service.soutlogger.disruptor.SysoutLogEventHandler;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
public class SysoutLoggingService
        implements ContainerLifecycleListener {

    private LoggingConsumerService loggingConsumerService;

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        public boolean configure(FeatureContext context, InjectionManager injectionManager, Server server) {
            if (injectionManager.getInstance(Configuration.class).getConfiguration("soutlogging").getBoolean("enabled", true)) {
                server.addService(new ResourceObject.Builder("service", "sysoutlogging").build());

                context.register(SysoutLoggingService.class);
            }

            return super.configure(context, injectionManager, server);
        }
    }


    @Inject
    public SysoutLoggingService(LoggingConsumerService loggingConsumerService) {
        this.loggingConsumerService = loggingConsumerService;
    }

    @Override
    public void onStartup(Container container) {
        System.out.println("Sysoutlogger Startup");

        loggingConsumerService.addLogHandler(new SysoutLogEventHandler());
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

        // TODO Close active sessions
        System.out.println("Shutdown");
    }
}
