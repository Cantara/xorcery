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
package dev.xorcery.opentelemetry.exporters.websocket;

import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;

@Service(name = "opentelemetry.exporters.websocket")
public class WebsocketExporterService {

    private final Sinks.Many<MetadataByteBuffer> collector;

    @Inject
    public WebsocketExporterService() {
        collector = Sinks.many().unicast().onBackpressureBuffer(Queues.<MetadataByteBuffer>get(4096).get());
    }

    public void send(MetadataByteBuffer metadataByteBuffer) {
        collector.tryEmitNext(metadataByteBuffer);
    }

    public Flux<MetadataByteBuffer> getCollector() {
        return collector.asFlux();
    }
}
