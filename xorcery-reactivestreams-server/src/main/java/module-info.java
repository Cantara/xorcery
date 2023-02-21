open module xorcery.reactivestreams.server {
    exports com.exoreaction.xorcery.service.reactivestreams.server;

    requires transitive xorcery.reactivestreams.api;

    requires xorcery.reactivestreams.api.hk2;
    requires xorcery.reactivestreams.client.hk2;
    requires xorcery.dns.client.hk2;

    requires xorcery.service.api;
    requires xorcery.util;
    requires xorcery.metadata;
    requires xorcery.dns.client;
    requires xorcery.reactivestreams.client;
    requires xorcery.jetty.server;
    requires xorcery.jetty.client.hk2;

    requires org.eclipse.jetty.websocket.jetty.server;
    requires com.lmax.disruptor;
    requires org.eclipse.jetty.servlet;
    requires org.eclipse.jetty.server;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.runlevel;
}