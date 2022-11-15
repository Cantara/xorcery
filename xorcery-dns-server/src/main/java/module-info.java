open module xorcery.registry {
    requires org.dnsjava;
    requires org.glassfish.hk2.api;
    requires org.apache.logging.log4j;
    requires jakarta.inject;
    requires com.fasterxml.jackson.databind;
    requires xorcery.configuration.api;
    exports com.exoreaction.xorcery.service.dns;

}