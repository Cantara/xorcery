open module xorcery.dns.registration {
    exports com.exoreaction.xorcery.service.dns.registration;

    requires xorcery.configuration.api;
    requires xorcery.dns.client;

    requires org.dnsjava;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires org.apache.logging.log4j;
    requires jakarta.inject;
    requires com.fasterxml.jackson.databind;
    requires javax.jmdns;
    requires xorcery.service.api;
}