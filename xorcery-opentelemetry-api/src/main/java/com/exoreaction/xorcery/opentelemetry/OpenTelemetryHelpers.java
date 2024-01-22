package com.exoreaction.xorcery.opentelemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;

import java.util.concurrent.Callable;

public interface OpenTelemetryHelpers {

    static <T> T time(DoubleHistogram histogram, Attributes attributes, Callable<T> event)
            throws Exception
    {
        long start = System.nanoTime();
        try
        {
            return event.call();
        } finally
        {
            long end = System.nanoTime();
            double duration = end - start;
            double inSeconds = duration / 1000000000D;
            histogram.record(inSeconds, attributes);
        }
    }
}
