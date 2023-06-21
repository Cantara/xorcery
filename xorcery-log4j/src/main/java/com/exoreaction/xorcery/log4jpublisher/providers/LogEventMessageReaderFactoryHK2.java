package com.exoreaction.xorcery.log4jpublisher.providers;

import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;

@Service
@ContractsProvided({MessageReader.Factory.class})
public class LogEventMessageReaderFactoryHK2
    extends LogEventMessageReaderFactory
{

    @Inject
    public LogEventMessageReaderFactoryHK2() throws IOException {
        super();
    }
}
