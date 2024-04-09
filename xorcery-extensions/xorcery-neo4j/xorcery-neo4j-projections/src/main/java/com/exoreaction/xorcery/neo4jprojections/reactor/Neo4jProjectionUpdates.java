package com.exoreaction.xorcery.neo4jprojections.reactor;

import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.metadata.Metadata;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Sinks;

import java.util.function.Function;

@Service(name="neo4j.projections.updates")
public class Neo4jProjectionUpdates
    implements Publisher<Metadata>,
        Function<MetadataEvents, MetadataEvents>,
        PreDestroy
{
    private final Sinks.Many<Metadata> sink;

    @Inject
    public Neo4jProjectionUpdates() {

        sink = Sinks.many().multicast().onBackpressureBuffer();
    }

    @Override
    public void subscribe(Subscriber<? super Metadata> s) {
        sink.asFlux().subscribe(s);
    }

    @Override
    public MetadataEvents apply(MetadataEvents metadataEvents) {
        sink.tryEmitNext(metadataEvents.getMetadata());
        return metadataEvents;
    }

    @Override
    public void preDestroy() {
        sink.tryEmitComplete();
    }
}
