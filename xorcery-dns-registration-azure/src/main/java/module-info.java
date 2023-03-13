module xorcery.dns.registration.azure {
  exports com.exoreaction.xorcery.service.dns.registration.azure;
  exports com.exoreaction.xorcery.service.dns.registration.azure.model;

  requires xorcery.configuration.api;
  requires xorcery.dns.registration;
  requires xorcery.jetty.client.hk2;
  requires xorcery.jetty.client;

  requires org.eclipse.jetty.client;
  requires jakarta.inject;
  requires org.glassfish.hk2.api;
  requires org.glassfish.hk2.runlevel;
  requires com.fasterxml.jackson.core;
  requires jakarta.ws.rs;
  requires org.dnsjava;

  requires org.apache.logging.log4j;
}
