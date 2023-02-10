open module xorcery.log4j {
    exports com.exoreaction.xorcery.service.log4jpublisher.log4j;
    exports com.exoreaction.xorcery.service.log4jpublisher;
    exports com.exoreaction.xorcery.service.log4jsubscriber;
    exports com.exoreaction.xorcery.service.requestlogpublisher;
    exports com.exoreaction.xorcery.service.log4jpublisher.providers;

    requires xorcery.util;
    requires xorcery.configuration.api;
    requires xorcery.metadata;
    requires xorcery.disruptor;
    requires xorcery.reactivestreams.api;

    requires com.lmax.disruptor;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires jakarta.inject;
    requires com.fasterxml.jackson.databind;
    requires log4j.layout.template.json;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires org.glassfish.hk2.utilities;
    requires org.eclipse.jetty.server;
}