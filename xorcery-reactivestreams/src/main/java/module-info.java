open module xorcery.reactivestreams {
    exports com.exoreaction.xorcery.service.reactivestreams;
    exports com.exoreaction.xorcery.service.reactivestreams.spi;
    requires transitive xorcery.reactivestreams.api;

    requires xorcery.service.api;
    requires xorcery.util;
    requires xorcery.metadata;
    requires xorcery.restclient;

    requires org.eclipse.jetty.websocket.jetty.server;
    requires com.lmax.disruptor;
    requires org.eclipse.jetty.servlet;
    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.websocket.jetty.client;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.apache.logging.log4j;
}