package com.exoreaction.reactiveservices.disruptor;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequence;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class EventHandlerResult<T,R>
    implements EventHandler<EventHolderWithResult<T,R>>
{
    private final ArrayBlockingQueue<CompletableFuture<R>> futures = new ArrayBlockingQueue<>(1024);

    public void complete(R result)
    {
        try {
            futures.take().complete(result);
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Could not complete result", e);
        }
    }

    public void exceptionally(Throwable throwable)
    {
        futures.poll().completeExceptionally(throwable);
    }

    @Override
    public void onEvent(EventHolderWithResult<T, R> event, long sequence, boolean endOfBatch) throws Exception
    {
        futures.put(event.result);
        try {
            event.result.join();
        } catch (Exception e)
        {
            // Ignore
            LogManager.getLogger(getClass()).error("Could not wait for result", e);
        }
    }

    @Override
    public void onBatchStart(long batchSize) {
        EventHandler.super.onBatchStart(batchSize);
    }

    @Override
    public void onStart() {
        EventHandler.super.onStart();
    }

    @Override
    public void onShutdown() {
        EventHandler.super.onShutdown();
    }

    @Override
    public void setSequenceCallback(Sequence sequenceCallback) {
        EventHandler.super.setSequenceCallback(sequenceCallback);
    }

    @Override
    public void onTimeout(long sequence) throws Exception {
        EventHandler.super.onTimeout(sequence);
    }
}
