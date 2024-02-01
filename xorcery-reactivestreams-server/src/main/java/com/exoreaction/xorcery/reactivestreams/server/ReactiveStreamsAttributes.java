package com.exoreaction.xorcery.reactivestreams.server;

import io.opentelemetry.api.common.AttributeKey;

import static io.opentelemetry.api.common.AttributeKey.longKey;

public interface ReactiveStreamsAttributes {

    AttributeKey<Long> PUBLISHER_ITEM_COUNT = longKey("xorcery.reactivestream.publisher.item.count");
}
