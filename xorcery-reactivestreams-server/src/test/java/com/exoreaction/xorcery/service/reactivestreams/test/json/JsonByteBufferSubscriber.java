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
package com.exoreaction.xorcery.service.reactivestreams.test.json;

import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.concurrent.*;

public class JsonByteBufferSubscriber implements Flow.Subscriber<WithMetadata<byte[]>> {
    private Flow.Subscription subscription;

    private final BlockingDeque<JsonNode> receivedEvents = new LinkedBlockingDeque<>();

    private final CountDownLatch terminatedLatch = new CountDownLatch(1);

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .build();

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        System.out.printf("received onSubscribe()%n");
        this.subscription = subscription;
        subscription.request(2);
    }

    @Override
    public void onNext(WithMetadata<byte[]> item) {
        JsonNode jsonNode;
        try {
            jsonNode = mapper.readTree(item.event());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ObjectNode root = mapper.createObjectNode();
        root.set("metadata", item.metadata().json());
        root.set("payload", jsonNode);
        System.out.printf("onNext:%s%n", root.toPrettyString());
        receivedEvents.add(root);
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.printf("onError%n");
        throwable.printStackTrace();
        terminatedLatch.countDown();
    }

    @Override
    public void onComplete() {
        System.out.printf("onComplete!%n");
        terminatedLatch.countDown();
    }

    public ArrayList<JsonNode> getAllReceivedEvents() {
        return new ArrayList<>(receivedEvents);
    }

    /**
     * @param timeout
     * @param unit
     * @return true if the subscription has terminated and false if the waiting time elapsed before the subscriber terminated
     * @throws InterruptedException
     */
    public boolean waitForTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return terminatedLatch.await(timeout, unit);
    }
}
