open module xorcery.reactivestreams.api.hk2 {
    exports com.exoreaction.xorcery.service.reactivestreams.hk2.providers;
    exports com.exoreaction.xorcery.service.reactivestreams.hk2.spi;

    requires xorcery.reactivestreams.api;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
}