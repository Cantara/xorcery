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
package dev.xorcery.opentelemetry.collectors.websocket;

import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Sinks;

@Service(name = "opentelemetry.collectors.websocket")
public class WebsocketCollectorService
    implements PreDestroy
{

    private final Sinks.Many<MetadataByteBuffer> logs;
    private final Sinks.Many<MetadataByteBuffer> traces;
    private final Sinks.Many<MetadataByteBuffer> metrics;

    public WebsocketCollectorService() {
        this.logs = Sinks.many().multicast().onBackpressureBuffer();
        this.traces = Sinks.many().multicast().onBackpressureBuffer();
        this.metrics = Sinks.many().multicast().onBackpressureBuffer();
    }

    public void subscribeLogs(Subscriber<MetadataByteBuffer> subscriber)
    {
        logs.asFlux().subscribe(subscriber);
    }

    public void subscribeTraces(Subscriber<MetadataByteBuffer> subscriber)
    {
        traces.asFlux().subscribe(subscriber);
    }

    public void subscribeMetrics(Subscriber<MetadataByteBuffer> subscriber)
    {
        metrics.asFlux().subscribe(subscriber);
    }

    public void collect(MetadataByteBuffer metadataByteBuffer) {
        metadataByteBuffer.metadata().getString("type").ifPresent(type ->
        {
            switch (type)
            {
                case "log"-> logs.tryEmitNext(metadataByteBuffer);
                case "trace"-> traces.tryEmitNext(metadataByteBuffer);
                case "metric"-> metrics.tryEmitNext(metadataByteBuffer);
            }
        });
    }

    @Override
    public void preDestroy() {
        logs.tryEmitComplete();
        traces.tryEmitComplete();
        metrics.tryEmitComplete();
    }
}
