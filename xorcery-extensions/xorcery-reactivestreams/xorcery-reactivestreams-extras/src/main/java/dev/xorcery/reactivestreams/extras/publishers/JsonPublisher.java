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
package dev.xorcery.reactivestreams.extras.publishers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.xorcery.collections.Element;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;

public class JsonPublisher<T>
        implements Publisher<T> {

    private static final ObjectReader jsonReader = new JsonMapper().findAndRegisterModules().reader();
    private final Class<? super T> itemType;

    public JsonPublisher(Class<? super T> itemType) {
        this.itemType = itemType;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        if (s instanceof CoreSubscriber<? super T> coreSubscriber)
        {
            try {
                Object resourceUrl = new ContextViewElement(coreSubscriber.currentContext()).get(ResourcePublisherContext.resourceUrl)
                        .orElseThrow(Element.missing(ResourcePublisherContext.resourceUrl));
                URL jsonResource = resourceUrl instanceof URL url ? url : new URL(resourceUrl.toString());
                InputStream resourceAsStream = new BufferedInputStream(jsonResource.openStream(), 32 * 1024);
                JsonFactory factory = JsonFactory.builder().build();
                coreSubscriber.onSubscribe(new ObjectReaderStreamer<>(coreSubscriber, factory.createParser(resourceAsStream), jsonReader.forType(itemType)));
            } catch (Throwable e) {
                coreSubscriber.onSubscribe(new NoopSubscription());
                coreSubscriber.onError(e);
            }
        } else
        {
            s.onError(new IllegalArgumentException("Subscriber must implement CoreSubscriber"));
        }
    }
}
