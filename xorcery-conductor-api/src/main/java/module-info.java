open module xorcery.conductor.api {
    exports com.exoreaction.xorcery.service.conductor.api;
    exports com.exoreaction.xorcery.service.conductor.helpers;

    requires transitive xorcery.service.api;
    requires xorcery.reactivestreams.api;

    requires org.glassfish.hk2.api;
    requires jersey.common;
    requires org.apache.logging.log4j;
    requires jakarta.inject;
}