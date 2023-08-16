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
package com.exoreaction.xorcery.neo4jprojections;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.ServiceConfiguration;

public record Neo4jProjectionsConfiguration(Configuration context)
    implements ServiceConfiguration
{
    public boolean isEventSubscriberEnabled() {
        return context.getBoolean("eventsubscriber.enabled").orElse(true);
    }

    public boolean isCommitPublisherEnabled() {
        return context.getBoolean("commitpublisher.enabled").orElse(true);
    }

    /**
     * How many individual events to apply before committing and starting a new transaction.
     */
    public int eventBatchSize()
    {
        return context.getInteger("eventBatchSize").orElse(1024);
    }
}
