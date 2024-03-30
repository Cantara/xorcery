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
package com.exoreaction.xorcery.neo4jprojections.streams;

import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.neo4jprojections.spi.Neo4jEventProjectionPreProcessor;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequence;
import org.apache.logging.log4j.LogManager;

public class PreProcessorEventHandler
    implements EventHandler<MetadataEvents>
{
    private final Neo4jEventProjectionPreProcessor preProcessor;
    private Sequence sequenceCallback;

    public PreProcessorEventHandler(Neo4jEventProjectionPreProcessor preProcessor) {
        this.preProcessor = preProcessor;
    }

    @Override
    public void setSequenceCallback(Sequence sequenceCallback) {
        this.sequenceCallback = sequenceCallback;
    }

    @Override
    public void onEvent(MetadataEvents event, long sequence, boolean endOfBatch) throws Exception {

        try {
            preProcessor.preProcess(event);
        } catch (EarlyReleaseException e) {
            try {
                // Release processed events
                sequenceCallback.set(sequence-1);
                // Try again
                preProcessor.preProcess(event);

                // Remove the flag
                event.getMetadata().json().remove("earlyrelease");
            } catch (EarlyReleaseException ex) {
                LogManager.getLogger().error("Event migration threw EarlyReleastException again:\n{}", event);
                throw ex;
            }
        }
    }
}
