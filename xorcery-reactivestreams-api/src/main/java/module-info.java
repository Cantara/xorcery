open module xorcery.reactivestreams.api {
    exports com.exoreaction.xorcery.service.reactivestreams.api;

    requires transitive xorcery.metadata;
    requires transitive jersey.common;

    requires org.glassfish.hk2.api;
}