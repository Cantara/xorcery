module xorcery.dns.registration.azure {
  exports com.exoreaction.xorcery.service.dns.registration.azure;
  exports com.exoreaction.xorcery.service.dns.registration.azure.model;

  requires xorcery.configuration.api;
  requires xorcery.jetty.client;

  requires org.eclipse.jetty.client;
  requires jakarta.inject;
  requires org.glassfish.hk2.api;
  requires org.glassfish.hk2.runlevel;
  requires com.fasterxml.jackson.core;

  requires org.apache.logging.log4j;
}
