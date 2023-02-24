package com.exoreaction.xorcery.service.dns.client.discovery;

import com.exoreaction.xorcery.configuration.model.Configuration;
import org.apache.logging.log4j.LogManager;

import javax.jmdns.JmDNS;
import java.io.IOException;

public class JmDNSFactory {

    private final JmDNS jmDNS;

    public JmDNSFactory(Configuration configuration) throws IOException {
        jmDNS = JmDNS.create(null, configuration.getString("host").orElse(null));
    }

    public JmDNS provide() {
        return jmDNS;
    }

    public void dispose(JmDNS instance) {
        try {
            jmDNS.close();
        } catch (IOException e) {
            LogManager.getLogger(getClass()).error("Exception closing JmDNS", e);
        }
    }
}
