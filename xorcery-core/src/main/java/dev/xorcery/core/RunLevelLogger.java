/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.core;

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
