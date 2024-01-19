package com.exoreaction.xorcery.opentelemetry.sdk;

import com.exoreaction.xorcery.configuration.Configuration;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.SemanticAttributes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
public class ResourceFactory
    implements Factory<Resource>
{
    private final Resource resource;

    @Inject
    public ResourceFactory(Configuration configuration) {
        OpenTelemetryConfiguration openTelemetryConfiguration = new OpenTelemetryConfiguration(configuration.getConfiguration("opentelemetry"));
        ResourceBuilder builder = Resource.getDefault().toBuilder();
        builder.setSchemaUrl(SemanticAttributes.SCHEMA_URL);
        openTelemetryConfiguration.getResource().forEach(builder::put);
        resource = builder.build();
    }

    @Override
    @Singleton
    public Resource provide() {
        return resource;
    }

    @Override
    public void dispose(Resource instance) {
    }
}
