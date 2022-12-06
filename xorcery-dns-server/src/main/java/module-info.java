open module xorcery.dns.server {
    exports com.exoreaction.xorcery.service.dns.server;
    exports com.exoreaction.xorcery.service.dns.server.discovery;

    requires org.dnsjava;
    requires javax.jmdns;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires org.apache.logging.log4j;
    requires jakarta.inject;
    requires com.fasterxml.jackson.databind;
    requires xorcery.configuration.api;
    requires xorcery.jsonapi;
    requires xorcery.service.api;
}