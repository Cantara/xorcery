package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.test;

import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import com.exoreaction.xorcery.util.Resources;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.ChangeableRunLevelFuture;
import org.glassfish.hk2.runlevel.ErrorInformation;
import org.glassfish.hk2.runlevel.RunLevelFuture;
import org.glassfish.hk2.runlevel.RunLevelListener;

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
                serviceLocator.getService(ServerWebSocketStreams.class).publisher("testevents",
                        MetadataJsonNode.class, new JsonYamlPublisher(Resources.getResource("persistentsubscribertestevents.yaml").orElseThrow()));
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
