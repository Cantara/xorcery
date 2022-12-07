open module xorcery.log4jappender {
    exports com.exoreaction.xorcery.service.log4jappender.jaxrs;
    exports com.exoreaction.xorcery.service.log4jappender.jaxrs.providers;
    exports com.exoreaction.xorcery.service.log4jappender.log4j;
    exports com.exoreaction.xorcery.service.log4jappender;
    exports com.exoreaction.xorcery.service.requestlog;

    requires xorcery.util;
    requires xorcery.configuration.api;
    requires xorcery.metadata;
    requires xorcery.disruptor;
    requires xorcery.reactivestreams.api;
    requires xorcery.service.api;
    requires xorcery.jsonapi;

    requires com.lmax.disruptor;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires jersey.server;
    requires com.fasterxml.jackson.databind;
    requires org.eclipse.jetty.http;
    requires org.eclipse.jetty.server;
    requires log4j.layout.template.json;
    requires xorcery.core;
}