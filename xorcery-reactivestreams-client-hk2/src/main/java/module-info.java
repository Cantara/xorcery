open module xorcery.reactivestreams.client.hk2 {
    exports com.exoreaction.xorcery.service.reactivestreams.client.hk2;

    requires xorcery.reactivestreams.client;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.inject;
    requires org.eclipse.jetty.client;
    requires xorcery.dns.client;
}