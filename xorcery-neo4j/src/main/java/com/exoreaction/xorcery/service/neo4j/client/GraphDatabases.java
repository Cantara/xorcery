package com.exoreaction.xorcery.service.neo4j.client;

import org.jvnet.hk2.annotations.Contract;

import java.util.function.Function;

@Contract
public interface GraphDatabases
        extends Function<String, GraphDatabase> {
}
