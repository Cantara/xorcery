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
package com.exoreaction.xorcery.log4jpublisher;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.core.LogEvent;

import java.util.concurrent.atomic.AtomicReference;

public class LoggingMetadataEventHandler
        implements EventHandler<WithMetadata<LogEvent>> {

    public final AtomicReference<Configuration> configuration = new AtomicReference<>();

    @Override
    public void onEvent(WithMetadata<LogEvent> event, long seq, boolean endOfBatch) throws Exception {

        LoggingMetadata.Builder builder = new LoggingMetadata.Builder(event.metadata().toBuilder())
                .timestamp(System.currentTimeMillis());
        Configuration conf = configuration.get();
        if (conf != null)
            builder.configuration(new InstanceConfiguration(conf.getConfiguration("instance")));
    }
}
