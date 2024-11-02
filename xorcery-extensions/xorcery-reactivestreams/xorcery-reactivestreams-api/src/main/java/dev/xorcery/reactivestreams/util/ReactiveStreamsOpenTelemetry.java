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
