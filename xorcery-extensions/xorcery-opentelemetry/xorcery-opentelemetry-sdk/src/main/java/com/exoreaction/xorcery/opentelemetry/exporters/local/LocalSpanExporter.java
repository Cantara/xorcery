package com.exoreaction.xorcery.opentelemetry.exporters.local;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service(name="opentelemetry.exporters.local")
public class LocalSpanExporter
    implements SpanExporter
{
    private final List<SpanData> spans = new CopyOnWriteArrayList<>();

    @Inject
    public LocalSpanExporter() {
    }

    public List<SpanData> getSpans(String... namePrefixes) {
        if (namePrefixes.length == 0)
            return spans;
        else
        {
            return spans.stream().filter(spanData ->
            {
                for (String name : namePrefixes) {
                    if (spanData.getName().startsWith(name))
                        return true;
                }
                return false;
            }).toList();
        }
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        this.spans.addAll(spans);
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
