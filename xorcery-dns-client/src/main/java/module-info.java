open module xorcery.dns.client {
    exports com.exoreaction.xorcery.service.dns.client;
    exports com.exoreaction.xorcery.service.dns.client.api;

    requires xorcery.configuration.api;

    requires org.dnsjava;
    requires org.glassfish.hk2.api;
    requires org.apache.logging.log4j;
    requires jakarta.inject;
    requires com.fasterxml.jackson.databind;
}