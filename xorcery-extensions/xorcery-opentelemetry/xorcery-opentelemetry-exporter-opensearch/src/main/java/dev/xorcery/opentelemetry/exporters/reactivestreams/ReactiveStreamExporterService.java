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
package dev.xorcery.opentelemetry.exporters.reactivestreams;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service(name = "opentelemetry.exporters.reactivestreams")
public class ReactiveStreamExporterService {

    private final Sinks.Many<MetadataJsonNode<JsonNode>> collector;

    @Inject
    public ReactiveStreamExporterService() {
        collector = Sinks.many().unicast().onBackpressureBuffer();
    }

    public void send(MetadataJsonNode<JsonNode> item) {
        collector.tryEmitNext(item);
    }

    public Flux<MetadataJsonNode<JsonNode>> getCollector() {
        return collector.asFlux();
    }
}
