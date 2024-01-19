/**
 * @author rickardoberg
 * @since 19/01/2024
 */

module xorcery.opentelemetry.log4j {
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.inject;
    requires xorcery.opentelemetry.api;
    requires org.apache.logging.log4j;
    requires io.opentelemetry.instrumentation.log4j_appender_2_17;
    requires org.apache.logging.log4j.core;
}