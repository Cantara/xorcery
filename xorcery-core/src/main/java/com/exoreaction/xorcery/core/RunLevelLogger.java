package com.exoreaction.xorcery.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.ChangeableRunLevelFuture;
import org.glassfish.hk2.runlevel.ErrorInformation;
import org.glassfish.hk2.runlevel.RunLevelFuture;
import org.glassfish.hk2.runlevel.RunLevelListener;
import org.jvnet.hk2.annotations.Service;

@Service
public class RunLevelLogger
    implements RunLevelListener
{
    private final Logger logger = LogManager.getLogger(getClass());

    @Override
    public void onProgress(ChangeableRunLevelFuture currentJob, int levelAchieved) {
        logger.debug("Reached run level "+levelAchieved);
    }

    @Override
    public void onCancelled(RunLevelFuture currentJob, int levelAchieved) {
        logger.warn("Cancelled run level "+levelAchieved);

    }

    @Override
    public void onError(RunLevelFuture currentJob, ErrorInformation errorInformation) {
        logger.error("Error at run level from "+errorInformation.getFailedDescriptor().getImplementation(), errorInformation.getError());
    }
}
