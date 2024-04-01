package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.test;

import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import com.exoreaction.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import com.exoreaction.xorcery.reactivestreams.extras.publishers.YamlPublisher;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.ChangeableRunLevelFuture;
import org.glassfish.hk2.runlevel.ErrorInformation;
import org.glassfish.hk2.runlevel.RunLevelFuture;
import org.glassfish.hk2.runlevel.RunLevelListener;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

import java.util.concurrent.atomic.AtomicInteger;

public final class FilePublisher
        implements RunLevelListener {

    @Inject
    public ServiceLocator serviceLocator;

    @Override
    public void onProgress(ChangeableRunLevelFuture currentJob, int levelAchieved) {
        if (levelAchieved == 10 && currentJob.getProposedLevel() > levelAchieved) {
            Logger logger = LogManager.getLogger();
            logger.info("YAML file publishing");
            try {
                serviceLocator.getService(ReactiveStreamsServer.class).publisher("testevents",
                        cfg -> new JsonYamlPublisher(Resources.getResource("persistentsubscribertestevents.yaml").orElseThrow()), JsonYamlPublisher.class);
                logger.info("YAML file published");
            } catch (Throwable e) {
                logger.error(e);
            }
        }
    }

    @Override
    public void onCancelled(RunLevelFuture currentJob, int levelAchieved) {

    }

    @Override
    public void onError(RunLevelFuture currentJob, ErrorInformation errorInformation) {

    }
}
