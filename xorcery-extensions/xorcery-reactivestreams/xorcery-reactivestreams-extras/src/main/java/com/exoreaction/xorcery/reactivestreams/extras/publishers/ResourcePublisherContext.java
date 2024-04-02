package com.exoreaction.xorcery.reactivestreams.extras.publishers;

import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

/**
 * To be used with {@link Flux#contextWrite(ContextView)}
 */
public enum ResourcePublisherContext {
    resourceUrl, // URL of resource to be published
}
