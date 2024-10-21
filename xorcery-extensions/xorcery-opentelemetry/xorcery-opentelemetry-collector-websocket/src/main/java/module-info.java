module xorcery.opentelemetry.collector.websocket {
    exports com.exoreaction.xorcery.opentelemetry.collectors.websocket.attach;
    exports com.exoreaction.xorcery.opentelemetry.collectors.websocket.listen;
    exports com.exoreaction.xorcery.opentelemetry.collectors.websocket;

    requires org.apache.logging.log4j;
    requires xorcery.reactivestreams.api;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.eclipse.jetty.websocket.api;
    requires org.glassfish.hk2.runlevel;
}