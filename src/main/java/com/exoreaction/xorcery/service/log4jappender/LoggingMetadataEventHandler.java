package com.exoreaction.xorcery.service.log4jappender;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.core.LogEvent;

public record LoggingMetadataEventHandler(Configuration configuration)
        implements EventHandler<WithMetadata<LogEvent>> {

    @Override
    public void onEvent(WithMetadata<LogEvent> event, long seq, boolean endOfBatch) throws Exception {
        new LoggingMetadata.Builder(event.metadata().toBuilder())
                .timestamp(System.currentTimeMillis())
                .configuration(configuration);
    }
}
