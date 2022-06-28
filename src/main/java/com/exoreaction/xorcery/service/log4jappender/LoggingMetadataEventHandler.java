package com.exoreaction.xorcery.service.log4jappender;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.disruptor.Event;
import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.core.LogEvent;

public record LoggingMetadataEventHandler(Configuration configuration)
        implements EventHandler<Event<LogEvent>> {

    @Override
    public void onEvent(Event<LogEvent> event, long seq, boolean endOfBatch) throws Exception {
        new LoggingMetadata.Builder(event.metadata.toBuilder())
                .timestamp(System.currentTimeMillis())
                .configuration(configuration);
    }
}
