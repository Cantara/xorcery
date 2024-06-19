package com.exoreaction.xorcery.util;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Streams {

    /**
     * Concatenate a stream of supplied streams into a single stream.
     * Closes each stream as it finishes, and calls close on current stream if caller does not need to iterate all items.
     * @param streams
     * @return concatenated stream of all items in the supplied streams
     * @param <T>
     */
    static <T> Stream<T> concatenate(Stream<Supplier<Stream<T>>> streams) {
        ConcatenatingSpliterator<T> spliterator = new ConcatenatingSpliterator<>(streams);
        return StreamSupport.stream(spliterator, false).onClose(spliterator);
    }

    class ConcatenatingSpliterator<T>
            implements Spliterator<T>, Runnable
    {
        final Spliterator<Supplier<Stream<T>>> spliterator;
        Spliterator<T> current;
        Stream<T> currentStream;

        public ConcatenatingSpliterator(Stream<Supplier<Stream<T>>> streams) {
            spliterator = streams.spliterator();
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {

            if (current == null) {
                if (!spliterator.tryAdvance(supplier -> {
                    currentStream = supplier.get();
                    current = currentStream.spliterator();
                }))
                    return false;
            }

            if (current.tryAdvance(action)) {
                return true;
            } else {
                currentStream.close();
                current = null;
                currentStream = null;
                return tryAdvance(action);
            }
        }

        @Override
        public Spliterator<T> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return 0;
        }

        @Override
        public void run() {
            if (currentStream != null)
            {
                currentStream.close();
            }
        }
    }
}