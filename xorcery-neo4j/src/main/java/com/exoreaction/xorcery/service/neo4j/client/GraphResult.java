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
package com.exoreaction.xorcery.service.neo4j.client;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class GraphResult
    implements AutoCloseable
{
    private final Transaction tx;
    private final Result result;
    private AutoCloseable onClose;

    public GraphResult(Transaction tx, Result result, AutoCloseable onClose) {

        this.tx = tx;
        this.result = result;
        this.onClose = onClose;
    }

    public Result getResult() {
        return result;
    }

    @Override
    public void close() throws Exception {
        result.close();
        tx.rollback();

        onClose.close();
    }
}
