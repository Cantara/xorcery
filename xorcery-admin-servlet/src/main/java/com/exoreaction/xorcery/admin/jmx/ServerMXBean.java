package com.exoreaction.xorcery.admin.jmx;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.core.Xorcery;

public interface ServerMXBean {

    public record Model(Configuration configuration, Xorcery xorcery) implements ServerMXBean {

        @Override
        public String getId() {
            return new InstanceConfiguration(configuration).getId();
        }

        @Override
        public String getName() {
            return new InstanceConfiguration(configuration).getName();
        }

        @Override
        public void shutdown() {
            xorcery.close();
            System.exit(0);
        }
    }

    String getId();

    String getName();

    void shutdown();
}
