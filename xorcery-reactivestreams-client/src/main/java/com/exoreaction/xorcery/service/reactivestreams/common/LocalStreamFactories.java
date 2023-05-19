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
package com.exoreaction.xorcery.service.reactivestreams.common;

import com.exoreaction.xorcery.configuration.model.Configuration;

import java.util.concurrent.Flow;
import java.util.function.Function;

/**
 * This is used by the reactive stream client to get access to local stream factories, i.e. when authority is set to null.
 */
public interface LocalStreamFactories {

    WrappedSubscriberFactory getSubscriberFactory(String streamName);

    WrappedPublisherFactory getPublisherFactory(String streamName);

    record WrappedPublisherFactory(Function<Configuration, ? extends Flow.Publisher<Object>> factory,
                                   Class<? extends Flow.Publisher<?>> publisherType) {
    }

    record WrappedSubscriberFactory(Function<Configuration, Flow.Subscriber<Object>> factory,
                                    Class<? extends Flow.Subscriber<?>> subscriberType) {
    }
}
