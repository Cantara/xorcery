package com.exoreaction.xorcery.service.reactivestreams.test.fibonacci;

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

    private final int maxNumbersInFibonacciSequence;

    public FibonacciSequence(int maxNumbersInFibonacciSequence) {
        this.maxNumbersInFibonacciSequence = maxNumbersInFibonacciSequence;
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
