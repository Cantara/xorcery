open module xorcery.reactivestreams.client {
    exports com.exoreaction.xorcery.service.reactivestreams.client;
    exports com.exoreaction.xorcery.service.reactivestreams.common;

    requires transitive xorcery.reactivestreams.api;
    requires xorcery.util;
    requires xorcery.metadata;
    requires xorcery.dns.client;

    requires com.lmax.disruptor;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires org.eclipse.jetty.io;
    requires org.eclipse.jetty.websocket.jetty.api;
    requires org.eclipse.jetty.websocket.jetty.client;
    requires jakarta.ws.rs;
}