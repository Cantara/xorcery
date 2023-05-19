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
package com.exoreaction.xorcery.service.domainevents.neo4jprojections;


import com.exoreaction.xorcery.service.neo4j.spi.Neo4jProvider;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.Map;

public class ApplyJsonDomainEvent
    implements Neo4jProvider
{

    @Context
    public Transaction transaction;

    @Description("Apply JsonDomainEvent")
    @Procedure(
            name = "domainevent.jsondomainevent",
            mode = Mode.WRITE
    )
    public void applyJsonDomainEvent(@Name(value = "metadata") Map<String, Object> metadata,
                                     @Name(value = "created",defaultValue = "{}") Map<String, Object> created,
                                     @Name(value = "updated",defaultValue = "{}") Map<String, Object> updated,
                                     @Name(value = "deleted",defaultValue = "{}") Map<String, Object> deleted,
                                     @Name(value = "attributes",defaultValue = "{}") Map<String, Object> attributes,
                                     @Name(value = "addedrelationships",defaultValue = "[]") List<Map<String, Object>> addedrelationships,
                                     @Name(value = "removedrelationships",defaultValue = "[]") List<Map<String, Object>> removedrelationships
                                     ) {

        System.out.println(metadata);
    }

}
