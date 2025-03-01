/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.opentelemetry.exporters.otlphttp.jdk;

import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

/**
 * @author rickardoberg
 * @since 18/01/2024
 */

final class BodyPublisher implements HttpRequest.BodyPublisher {

    private final int length;
    private final byte[] content;
    private final Supplier<ByteBuffer> bufSupplier;

    BodyPublisher(byte[] content, int length, Supplier<ByteBuffer> bufSupplier) {
        this.content = content;
        this.length = length;
        this.bufSupplier = bufSupplier;
    }

    private List<ByteBuffer> copyToBuffers() {
        int offset = 0;
        int length = this.length;

        List<ByteBuffer> buffers = new ArrayList<>();
        while (length > 0) {
            ByteBuffer b = bufSupplier.get();
            b.clear();
            int lengthToCopy = Math.min(b.capacity(), length);
            b.put(content, offset, lengthToCopy);
            offset += lengthToCopy;
            length -= lengthToCopy;
            b.flip();
            buffers.add(b);
        }
        return buffers;
    }

    @Override
    public long contentLength() {
        return length;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        BodyPublisher.Subscription subscription = new Subscription(copyToBuffers(), subscriber);
        subscriber.onSubscribe(subscription);
    }

    private static class Subscription implements Flow.Subscription {

        private volatile boolean isCompleted;
        private final List<ByteBuffer> buffers;
        private final Flow.Subscriber<? super ByteBuffer> subscriber;

        private int offset = 0;

        private Subscription(List<ByteBuffer> buffers, Flow.Subscriber<? super ByteBuffer> subscriber) {
            this.buffers = buffers;
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            if (isCompleted) {
                return;
            }
            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException("Subscription request must be >= 0"));
                isCompleted = true;
            } else {
                run(n);
            }
        }

        @Override
        public void cancel() {
            isCompleted = true;
        }

        private synchronized void run(long requestedItems) {
            if (isCompleted) {
                return;
            }

            long count = 0;
            ByteBuffer next;
            while (count < requestedItems) {
                int nextIndex = offset++;
                if (nextIndex >= buffers.size()) {
                    break;
                }
                next = buffers.get(nextIndex);
                subscriber.onNext(next);
                count++;
            }
            if (offset >= buffers.size()) {
                isCompleted = true;
                subscriber.onComplete();
            }
        }
    }
}
