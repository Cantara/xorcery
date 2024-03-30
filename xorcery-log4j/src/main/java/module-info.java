module xorcery.log4j {
    exports com.exoreaction.xorcery.log4j;

    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.api;
    requires xorcery.configuration.api;
    requires org.apache.logging.log4j.core;
    requires jakarta.inject;
    requires xorcery.core;
}