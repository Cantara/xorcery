package com.exoreaction.xorcery.neo4jprojections.reactor.spi;

import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import org.neo4j.graphdb.Transaction;

import java.util.List;

public record EventsWithTransaction(Transaction transaction, List<MetadataEvents> event) {
}
