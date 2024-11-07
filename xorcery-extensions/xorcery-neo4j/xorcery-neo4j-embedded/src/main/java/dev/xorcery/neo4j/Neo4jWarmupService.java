package dev.xorcery.neo4j;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.neo4j.client.GraphDatabase;
import dev.xorcery.neo4j.client.GraphResult;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.Collections;
import java.util.List;

@Service(name="neo4jdatabase.warmup")
@RunLevel(4)
public class Neo4jWarmupService {

    private final GraphDatabase graphDatabase;
    private final Configuration configuration;
    private final Logger logger;

    @Inject
    public Neo4jWarmupService(GraphDatabase graphDatabase, Configuration configuration, Logger logger) {
        this.graphDatabase = graphDatabase;
        this.configuration = configuration;
        this.logger = logger;
        executeWarmupQueries();
    }

    public void executeWarmupQueries()
    {
        List<String> warmupQueries = configuration.getListAs("neo4jdatabase.warmup.queries", JsonNode::asText).orElse(Collections.emptyList());
        if (!warmupQueries.isEmpty())
        {
            for (String warmupQuery : warmupQueries) {
                try(GraphResult result = graphDatabase.execute(warmupQuery, Collections.emptyMap(), 90).toCompletableFuture().join()) {
                    result.getResult().accept(row -> true);
                } catch (Exception e) {
                    logger.warn("Warmup script failed", e);
                }
            }
            logger.info("{} Neo4j warmup queries performed", warmupQueries.size());
        }
    }
}
