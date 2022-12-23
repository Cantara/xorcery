open module xorcery.reactivestreams.api {
    exports com.exoreaction.xorcery.service.reactivestreams.api;
    exports com.exoreaction.xorcery.service.reactivestreams.spi;
    exports com.exoreaction.xorcery.service.reactivestreams.providers;

    requires transitive xorcery.metadata;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
}