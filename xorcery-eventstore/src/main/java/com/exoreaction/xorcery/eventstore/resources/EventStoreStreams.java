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
package com.exoreaction.xorcery.eventstore.resources;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadStreamOptions;
import com.exoreaction.xorcery.eventstore.EventStoreService;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.CompletionStage;

@Service
public class EventStoreStreams {

    private final EventStoreDBClient client;

    @Inject
    public EventStoreStreams(EventStoreService eventStoreService) {
        client = eventStoreService.getClient();
    }

    public CompletionStage<StreamModel> getStream(String id) {
        return client.getStreamMetadata(id).thenCombine(client.readStream(id, ReadStreamOptions.get().maxCount(1).backwards().fromEnd()),
                (streamMetaData, readResult) ->
                {
                    return new StreamModel(id, readResult.getEvents().stream().findFirst().map(event -> event.getEvent().getRevision()).orElse(-1L));
                });
    }
}
