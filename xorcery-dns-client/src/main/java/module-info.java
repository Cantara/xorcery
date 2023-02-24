open module xorcery.dns.client {
    exports com.exoreaction.xorcery.service.dns.client;
    exports com.exoreaction.xorcery.service.dns.client.api;
    exports com.exoreaction.xorcery.service.dns.client.discovery;

    requires xorcery.configuration.api;

    requires org.dnsjava;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.databind;
    requires javax.jmdns;
}