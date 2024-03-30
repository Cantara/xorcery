package com.exoreaction.xorcery.neo4jprojections.reactor.spi;

import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import org.jvnet.hk2.annotations.Contract;
import reactor.core.publisher.SynchronousSink;

import java.util.List;
import java.util.function.BiConsumer;

@Contract
public interface Neo4jProjection
    extends BiConsumer<EventsWithTransaction, SynchronousSink<List<MetadataEvents>>>
{
}
