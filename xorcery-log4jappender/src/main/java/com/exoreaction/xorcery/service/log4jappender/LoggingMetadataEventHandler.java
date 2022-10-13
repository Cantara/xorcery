package com.exoreaction.xorcery.service.log4jappender;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.core.LogEvent;

import java.util.concurrent.atomic.AtomicReference;

public class LoggingMetadataEventHandler
        implements EventHandler<WithMetadata<LogEvent>> {

    public final AtomicReference<Configuration> configuration = new AtomicReference<>();

    @Override
    public void onEvent(WithMetadata<LogEvent> event, long seq, boolean endOfBatch) throws Exception {

        LoggingMetadata.Builder builder = new LoggingMetadata.Builder(event.metadata().toBuilder())
                .timestamp(System.currentTimeMillis());
        Configuration conf = configuration.get();
        if (conf != null)
            builder.configuration(conf);
    }
}
