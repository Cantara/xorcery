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
package dev.xorcery.reactivestreams.persistentsubscriber.providers;

import dev.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberCheckpoint;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
public class FilePersistentSubscriberCheckpointFactory
    implements Factory<PersistentSubscriberCheckpoint>
{
    private final LoggerContext loggerContext;

    @Inject
    public FilePersistentSubscriberCheckpointFactory(LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
    }

    @Override
    @Named("file")
    public PersistentSubscriberCheckpoint provide() {
        return new FilePersistentSubscriberCheckpoint(loggerContext.getLogger(FilePersistentSubscriberCheckpoint.class));
    }

    @Override
    public void dispose(PersistentSubscriberCheckpoint instance) {

    }
}
