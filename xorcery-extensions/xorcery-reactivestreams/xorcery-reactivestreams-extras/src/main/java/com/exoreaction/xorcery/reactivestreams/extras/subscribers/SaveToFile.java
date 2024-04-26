package com.exoreaction.xorcery.reactivestreams.extras.subscribers;

import com.exoreaction.xorcery.reactivestreams.api.ContextViewElement;
import com.exoreaction.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Put the filename to be written into the subscriber ContextView with key {@link ResourcePublisherContext#resourceUrl}
 *
 * @param <T>
 */
public class SaveToFile<T>
    implements BiFunction<Flux<T>, ContextView, Publisher<T>> {

    private final BiConsumer<T, SynchronousSink<ByteBuffer>> itemTranslator;

    public SaveToFile(BiConsumer<T, SynchronousSink<ByteBuffer>> itemTranslator) {
        this.itemTranslator = itemTranslator;
    }

    @Override
    public Publisher<T> apply(Flux<T> flux, ContextView contextView) {

        File file = new ContextViewElement(contextView).get(ResourceSubscriberContext.file)
                .map(val ->val instanceof File f ? f : new File(val.toString()))
                .orElse(null);
        if (!file.getParentFile().exists())
        {
            return Flux.error(new FileNotFoundException(file.getParentFile().getAbsolutePath()+" directory does not exist"));
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            return flux.handle(new Handler(fileOutputStream)).doOnComplete(()->
            {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            });
        } catch (FileNotFoundException e) {
            return Flux.error(e);
        }
    }

    class Handler
        implements BiConsumer<T, SynchronousSink<T>>, SynchronousSink<ByteBuffer>
    {
        private final FileOutputStream fileOutputStream;
        private SynchronousSink<T> sink;
        private Throwable error;

        public Handler(FileOutputStream fileOutputStream) {
            this.fileOutputStream = fileOutputStream;
        }

        @Override
        public void accept(T item, SynchronousSink<T> sink) {
            this.sink = sink;
            itemTranslator.accept(item, this);
            if (error != null)
                sink.error(error);
            else
                sink.next(item);
        }

        @Override
        public void complete() {
            sink.complete();
        }

        @Override
        public Context currentContext() {
            return sink.currentContext();
        }

        @Override
        public void error(Throwable e) {
            this.error = e;
        }

        @Override
        public void next(ByteBuffer byteBuffer) {
            try {
                fileOutputStream.getChannel().write(byteBuffer);
            } catch (Throwable e) {
                this.error = e;
            }
        }
    }
}
