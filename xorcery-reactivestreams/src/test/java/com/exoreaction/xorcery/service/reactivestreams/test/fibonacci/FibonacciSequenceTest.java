package com.exoreaction.xorcery.service.reactivestreams.test.fibonacci;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FibonacciSequenceTest {

    @Test
    public void thatFibonacciSequenceIsCorrect() {
        assertEquals(Collections.emptyList(), FibonacciSequence.sequenceOf(0));
        assertEquals(List.of(0L), FibonacciSequence.sequenceOf(1));
        assertEquals(List.of(0L, 1L), FibonacciSequence.sequenceOf(2));
        assertEquals(List.of(0L, 1L, 1L), FibonacciSequence.sequenceOf(3));
        assertEquals(List.of(0L, 1L, 1L, 2L), FibonacciSequence.sequenceOf(4));
        assertEquals(List.of(0L, 1L, 1L, 2L, 3L, 5L, 8L, 13L, 21L, 34L, 55L, 89L), FibonacciSequence.sequenceOf(12));
    }
}
