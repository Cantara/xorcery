package com.exoreaction.xorcery.reactivestreams.util;

/**
 * TODO: This should be moved to a separate module and added as an operator
 */
@Deprecated()
public interface ReactiveStreamsOpenTelemetry {

    String NAMESPACE = "xorcery.reactivestreams.";

    String XORCERY_MESSAGING_SYSTEM = "xorcery_reactivestream";
    String SUBSCRIBER_IO = NAMESPACE + "subscriber.io";
    String SUBSCRIBER_REQUESTS = NAMESPACE + "subscriber.requests";

    String PUBLISHER_IO = NAMESPACE + "publisher.io";
    String PUBLISHER_REQUESTS = NAMESPACE + "publisher.requests";
    String PUBLISHER_FLUSH_COUNT = NAMESPACE + "publisher.flush.count";
}
