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
package dev.xorcery.opentelemetry;

import dev.xorcery.configuration.Configuration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DescriptorType;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.*;
import org.jvnet.hk2.annotations.Service;

import java.util.List;

@Service
public class XorceryLifecycleTracerService
        implements RunLevelListener {

    private final ServiceLocator serviceLocator;
    private final Configuration configuration;
    private final Tracer tracer;
    private Span startup;
    private Span shutdown;

    @Inject
    public XorceryLifecycleTracerService(ServiceLocator serviceLocator, Configuration configuration, OpenTelemetry openTelemetry) {
        this.serviceLocator = serviceLocator;
        this.configuration = configuration;
        this.tracer = openTelemetry.getTracer(getClass().getName(), getClass().getPackage().getImplementationVersion());
    }

    @Override
    public void onProgress(ChangeableRunLevelFuture currentJob, int levelAchieved) {
        try {

            if (currentJob.isUp()) {
                if (levelAchieved == -1) {
                    startup = tracer.spanBuilder("instance started")
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAttribute(AttributeKey.stringKey("server.configuration"), configuration.toString())
                            .startSpan();
                }

                getNamedRunLevelServices().forEach(service ->
                {
                    ActiveDescriptor<?> activeDescriptor = service.getActiveDescriptor();
                    Class<?> implementationClass = activeDescriptor.getImplementationClass();
                    if (implementationClass.getAnnotation(RunLevel.class).value() == levelAchieved) {
                        startup.addEvent("service started " + implementationClass.getAnnotation(Service.class).name());
                    }
                });

                if (levelAchieved == 20) {
                    startup.end();
                }
            } else if (currentJob.isDown()) {
                if (shutdown == null) {
                    shutdown = tracer.spanBuilder("instance stopped")
                            .setSpanKind(SpanKind.INTERNAL)
                            .startSpan();
                }

                getNamedRunLevelServices().forEach(service ->
                {
                    ActiveDescriptor<?> activeDescriptor = service.getActiveDescriptor();
                    Class<?> implementationClass = activeDescriptor.getImplementationClass();
                    if (implementationClass.getAnnotation(RunLevel.class).value() == levelAchieved) {
                        shutdown.addEvent("service stopped " + implementationClass.getAnnotation(Service.class).name());
                    }
                });

                if (levelAchieved == -1) {
                    shutdown.end();
                }
            }
        } catch (Throwable e) {
            System.out.println(e);
        }
    }

    @Override
    public void onCancelled(RunLevelFuture currentJob, int levelAchieved) {
    }

    @Override
    public void onError(RunLevelFuture currentJob, ErrorInformation errorInformation) {
        Span startupFailed = tracer.spanBuilder("instance failed")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute(AttributeKey.stringKey("server.configuration"), configuration.toString())
                .startSpan();
        startupFailed.recordException(errorInformation.getError());
        if (errorInformation.getFailedDescriptor() != null) {
            startupFailed.setAttribute("service.name", errorInformation.getFailedDescriptor().getName());
        }
        startupFailed.end();
    }

    private List<ServiceHandle<?>> getNamedRunLevelServices() {
        return serviceLocator.getAllServiceHandles(f -> f.getName() != null
                        && f.getDescriptorType().equals(DescriptorType.CLASS))
                .stream()
                .filter(s -> s.getActiveDescriptor().isReified()
                        && s.getActiveDescriptor().getImplementationClass().getAnnotation(Service.class) != null
                        && s.getActiveDescriptor().getImplementationClass().getAnnotation(RunLevel.class) != null)
                .toList();
    }
}
