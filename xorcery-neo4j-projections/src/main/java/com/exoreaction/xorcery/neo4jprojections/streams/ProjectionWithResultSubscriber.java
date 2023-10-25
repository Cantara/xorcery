package com.exoreaction.xorcery.neo4jprojections.streams;

import com.exoreaction.xorcery.metadata.CommonMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.neo4jprojections.api.WaitForProjectionCommit;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.api.WithResult;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.TimeUnit;

public class ProjectionWithResultSubscriber
        implements Subscriber<WithResult<WithMetadata<ArrayNode>, Metadata>> {
    private final ProjectionSubscriber projectionSubscriber;
    private final WaitForProjectionCommit waitForProjectionCommit;

    public ProjectionWithResultSubscriber(ProjectionSubscriber projectionSubscriber, WaitForProjectionCommit waitForProjectionCommit) {
        this.projectionSubscriber = projectionSubscriber;
        this.waitForProjectionCommit = waitForProjectionCommit;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        projectionSubscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(WithResult<WithMetadata<ArrayNode>, Metadata> item) {
        WithMetadata<ArrayNode> eventsWithMetadata = item.event();
        projectionSubscriber.onNext(eventsWithMetadata);
        CommonMetadata commonMetadata = eventsWithMetadata::metadata;
        waitForProjectionCommit.waitForTimestamp(commonMetadata.getTimestamp())
                .orTimeout(10, TimeUnit.SECONDS)
                .whenComplete((result, throwable) ->
        {
            if (throwable != null) {
                item.result().completeExceptionally(throwable);
                return;
            }

            item.result().complete(eventsWithMetadata.metadata());
        });
    }

    @Override
    public void onError(Throwable throwable) {
        projectionSubscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        projectionSubscriber.onComplete();
    }
}
