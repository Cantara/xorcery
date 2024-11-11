package dev.xorcery.domainevents.neo4jprojection.providers;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.domainevents.api.MetadataEvents;
import dev.xorcery.domainevents.publisher.spi.EventPublisherProvider;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.metadata.WithMetadata;
import dev.xorcery.neo4jprojections.api.Neo4jProjections;
import dev.xorcery.neo4jprojections.api.ProjectionStreamContext;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import static dev.xorcery.configuration.Configuration.missing;

@Service
@ContractsProvided(EventPublisherProvider.class)
public class Neo4jEventPublisherProvider
    implements EventPublisherProvider
{
    private final Neo4jProjections neo4jProjections;
    private final Configuration configuration;

    @Inject
    public Neo4jEventPublisherProvider(Neo4jProjections neo4jProjections, Configuration configuration) {
        this.neo4jProjections = neo4jProjections;

        this.configuration = configuration;
    }

    @Override
    public Publisher<Metadata> apply(Flux<MetadataEvents> metadataEventsFlux, ContextView contextView) {
        return metadataEventsFlux.transformDeferredContextual(neo4jProjections.projection())
                .map(WithMetadata::metadata)
                .contextWrite(Context.of(ProjectionStreamContext.projectionId,
                        configuration.getString("neo4jeventpublisherprovider.projectionId").orElseThrow(missing("projectionId"))));
    }
}
