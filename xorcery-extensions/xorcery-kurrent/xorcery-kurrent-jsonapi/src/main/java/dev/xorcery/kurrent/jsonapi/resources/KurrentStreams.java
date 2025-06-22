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
package dev.xorcery.kurrent.jsonapi.resources;

import dev.xorcery.kurrent.client.api.KurrentClients;
import io.kurrent.dbclient.KurrentDBClient;
import io.kurrent.dbclient.ReadStreamOptions;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.CompletionStage;

@Service
public class KurrentStreams {

    private final KurrentDBClient client;

    @Inject
    public KurrentStreams(KurrentClients kurrentClients) {
        client = kurrentClients.getDefaultClient().getClient();
    }

    public CompletionStage<StreamModel> getStream(String id) {
        return client.getStreamMetadata(id).thenCombine(client.readStream(id, ReadStreamOptions.get().maxCount(1).backwards().fromEnd()),
                (streamMetaData, readResult) ->
                        readResult.getEvents().stream().findFirst().map(event -> new StreamModel(id, event.getEvent().getRevision(), event.getEvent().getCreated().toEpochMilli()))
                                .orElseGet(()->new StreamModel(id, -1, -1)));
    }
}
