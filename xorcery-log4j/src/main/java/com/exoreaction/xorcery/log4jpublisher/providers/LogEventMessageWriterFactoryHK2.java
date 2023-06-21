package com.exoreaction.xorcery.log4jpublisher.providers;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;

@Service
@ContractsProvided({MessageWriter.Factory.class})
public class LogEventMessageWriterFactoryHK2
        extends LogEventMessageWriterFactory {

    @Inject
    public LogEventMessageWriterFactoryHK2(Configuration configuration) throws IOException {
        super(InstanceConfiguration.get(configuration));
    }
}
