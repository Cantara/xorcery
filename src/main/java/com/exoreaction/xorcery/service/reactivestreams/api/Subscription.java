package com.exoreaction.xorcery.service.reactivestreams.api;

public interface Subscription {
    public void request(long n);

    public void cancel();
}
