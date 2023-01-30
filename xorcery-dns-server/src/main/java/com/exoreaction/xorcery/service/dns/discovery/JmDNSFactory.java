package com.exoreaction.xorcery.service.dns.discovery;

import com.exoreaction.xorcery.configuration.model.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

import javax.jmdns.JmDNS;
import java.io.IOException;

@Service
public class JmDNSFactory
        implements Factory<JmDNS> {

    private final JmDNS jmDNS;

    @Inject
    public JmDNSFactory(Configuration configuration) throws IOException {
        jmDNS = JmDNS.create(null, configuration.getString("host").orElse(null));
    }

    @Override
    @Singleton
    public JmDNS provide() {
        return jmDNS;
    }

    @Override
    public void dispose(JmDNS instance) {
        try {
            jmDNS.close();
        } catch (IOException e) {
            LogManager.getLogger(getClass()).error("Exception closing JmDNS", e);
        }
    }
}
