package com.exoreaction.xorcery.util;

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
