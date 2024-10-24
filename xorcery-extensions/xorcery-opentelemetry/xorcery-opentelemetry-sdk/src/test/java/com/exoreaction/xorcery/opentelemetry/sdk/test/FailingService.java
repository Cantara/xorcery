package com.exoreaction.xorcery.opentelemetry.sdk.test;

import com.exoreaction.xorcery.opentelemetry.exporters.local.LocalSpanExporter;
import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name="failing")
@RunLevel(15)
public class FailingService {

    public static LocalSpanExporter localSpanExporter;

    @Inject
    public FailingService(LocalSpanExporter localSpanExporter) {
        FailingService.localSpanExporter = localSpanExporter;
        throw new IllegalStateException("Service failed");
    }
}
