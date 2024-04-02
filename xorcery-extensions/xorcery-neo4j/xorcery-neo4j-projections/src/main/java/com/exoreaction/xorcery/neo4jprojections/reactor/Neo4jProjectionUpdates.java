package com.exoreaction.xorcery.neo4jprojections.reactor;

import com.exoreaction.xorcery.metadata.Metadata;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

@Service(name="neo4j.projections.updates")
public class Neo4jProjectionUpdates
    implements Publisher<Metadata>
{

    @Inject
    public Neo4jProjectionUpdates() {


    }

    @Override
    public void subscribe(Subscriber<? super Metadata> s) {

    }
}
