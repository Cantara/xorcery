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
