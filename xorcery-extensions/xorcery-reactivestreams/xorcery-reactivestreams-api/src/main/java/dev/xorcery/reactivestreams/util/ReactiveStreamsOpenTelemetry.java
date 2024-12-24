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
package dev.xorcery.reactivestreams.util;

public interface ReactiveStreamsOpenTelemetry {

    String NAMESPACE = "xorcery.reactivestreams.";

    String XORCERY_MESSAGING_SYSTEM = "xorcery_reactivestream";
    String SUBSCRIBER_IO = NAMESPACE + "subscriber.io";
    String SUBSCRIBER_ITEM_IO = NAMESPACE + "subscriber.item.io";
    String SUBSCRIBER_REQUESTS = NAMESPACE + "subscriber.requests";

    String PUBLISHER_IO = NAMESPACE + "publisher.io";
    String PUBLISHER_ITEM_IO = NAMESPACE + "publisher.item.io";
    String PUBLISHER_REQUESTS = NAMESPACE + "publisher.requests";
    String PUBLISHER_FLUSH_COUNT = NAMESPACE + "publisher.flush.count";

    String OPEN_CONNECTIONS = NAMESPACE + "open_connections";
}
