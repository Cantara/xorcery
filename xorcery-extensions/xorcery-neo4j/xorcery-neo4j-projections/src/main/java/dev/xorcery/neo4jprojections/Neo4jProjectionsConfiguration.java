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
package dev.xorcery.neo4jprojections;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.ServiceConfiguration;
import org.neo4j.io.ByteUnit;

public record Neo4jProjectionsConfiguration(Configuration context)
        implements ServiceConfiguration {
    /**
     * Max size of batched MetadataEvents to apply before committing and starting a new transaction.
     */
    public int eventBatchSize() {
        return context.getInteger("eventBatchSize").orElse(1024);
    }

    public int getMaxThreadCount() {
        return context.getInteger("maxThreadCount").orElse(-1);
    }

    public long getMaxTransactionSize()
    {
        return ByteUnit.parse(context.getString("maxTransactionSize").orElse("1G"));
    }

    public long getTransactionMemoryUsageMargin()
    {
        return ByteUnit.parse(context.getString("transactionMemoryUsageMargin").orElse("1M"));
    }
}
