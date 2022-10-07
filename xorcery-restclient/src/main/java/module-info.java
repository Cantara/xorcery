open module xorcery.restclient {
    exports com.exoreaction.xorcery.jetty.client;
    exports com.exoreaction.xorcery.rest;

    requires transitive xorcery.jsonapi;

    requires transitive jakarta.ws.rs;
    requires org.apache.logging.log4j;
    requires org.eclipse.jetty.client;
    requires org.eclipse.jetty.websocket.jetty.api;
    requires org.eclipse.jetty.websocket.jetty.client;
    requires com.fasterxml.jackson.databind;
    requires xorcery.util;
}