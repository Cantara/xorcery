module xorcery.configuration.validation {
    exports com.exoreaction.xorcery.configuration.validation;

    requires xorcery.configuration.api;

    requires com.networknt.schema;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
}