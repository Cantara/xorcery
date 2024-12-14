module xorcery.jetty.server.httptwo {
    exports dev.xorcery.jetty.server.http2.providers;

    requires org.eclipse.jetty.http2.server;
    requires xorcery.jetty.server;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires org.eclipse.jetty.alpn.server;
}