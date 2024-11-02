/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.opentelemetry.sdk;

import dev.xorcery.configuration.Configuration;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.SchemaUrls;
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
        builder.setSchemaUrl(SchemaUrls.V1_25_0);
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
