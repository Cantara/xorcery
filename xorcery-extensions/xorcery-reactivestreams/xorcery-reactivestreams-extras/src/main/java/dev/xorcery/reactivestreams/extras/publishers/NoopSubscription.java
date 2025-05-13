package dev.xorcery.reactivestreams.extras.publishers;

import org.reactivestreams.Subscription;

public record NoopSubscription()
    implements Subscription
{
    @Override
    public void request(long n) {
    }

    @Override
    public void cancel() {
    }
}
