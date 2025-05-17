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
package dev.xorcery.util.test;

import dev.xorcery.util.Streams;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StreamsTest {

    @Test
    public void testConcatenatingStreams()
    {
        Stream<Supplier<Stream<String>>> streams = Stream.of(() -> getStringStream(0,10), () -> getStringStream(10,20), () -> getStringStream(20,30), () -> getStringStream(30,40));

        Stream<String> result = Streams.concatenate(streams);

        List<String> resultList = result.toList();

        Assertions.assertEquals(IntStream.range(0,40).mapToObj(Integer::toString).toList(), resultList);

    }

    private static Stream<String> getStringStream(int from, int to) {
        return IntStream.range(from, to).mapToObj(Integer::toString).onClose(() ->
                System.out.println("Close"));
    }

}
