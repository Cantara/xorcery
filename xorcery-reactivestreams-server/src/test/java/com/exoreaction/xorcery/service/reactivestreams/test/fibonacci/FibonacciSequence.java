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
package com.exoreaction.xorcery.service.reactivestreams.test.fibonacci;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FibonacciSequence implements Iterable<Long> {

    public static List<Long> sequenceOf(int numbersInFibonacciSequence) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new FibonacciSequence(numbersInFibonacciSequence).iterator(), Spliterator.ORDERED), false)
                .collect(Collectors.toList());
    }

    public static List<byte[]> binarySequenceOf(int numbersInFibonacciSequence) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new FibonacciSequence(numbersInFibonacciSequence).binaryIterator(), Spliterator.ORDERED), false)
                .collect(Collectors.toList());
    }

    public static List<ByteBuffer> binaryNioSequenceOf(int numbersInFibonacciSequence) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new FibonacciSequence(numbersInFibonacciSequence).binaryIterator(), Spliterator.ORDERED), false)
                .map(ByteBuffer::wrap)
                .collect(Collectors.toList());
    }

    private final int maxNumbersInFibonacciSequence;

    public FibonacciSequence(int maxNumbersInFibonacciSequence) {
        this.maxNumbersInFibonacciSequence = maxNumbersInFibonacciSequence;
    }

    public Iterator<byte[]> binaryIterator() {
        return new Iterator<>() {
            private final Iterator<Long> delegate = iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public byte[] next() {
                long next = delegate.next();
                byte[] buf = new byte[8];
                ByteBuffer result = ByteBuffer.wrap(buf);
                result.putLong(next);
                return buf;
            }
        };
    }

    public Iterator<ByteBuffer> binaryNioIterator() {
        return new Iterator<>() {
            private final Iterator<Long> delegate = iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public ByteBuffer next() {
                long next = delegate.next();
                byte[] buf = new byte[8];
                ByteBuffer result = ByteBuffer.wrap(buf);
                result.putLong(next);
                result.flip();
                return result;
            }
        };
    }

    @Override
    public Iterator<Long> iterator() {
        return new Iterator<>() {
            private long previous = 1;
            private long current = 0;
            private int n = 0;

            @Override
            public boolean hasNext() {
                return n < maxNumbersInFibonacciSequence;
            }

            @Override
            public Long next() {
                long result = current;
                long newCurrent = current + previous;
                previous = current;
                current = newCurrent;
                n++;
                return result;
            }
        };
    }
}
