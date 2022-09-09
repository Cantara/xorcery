package com.exoreaction.xorcery.service.certmanager;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.jaxrs.readers.JsonApiMessageBodyReader;
import com.exoreaction.xorcery.jsonapi.client.JsonApiClient;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.jsonapi.model.ResourceObjects;
import com.exoreaction.xorcery.server.Xorcery;
import com.exoreaction.xorcery.server.model.ServiceIdentifier;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.AbstractConductorListener;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;
import org.glassfish.jersey.jetty.connector.JettyHttpClientContract;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@Singleton
public class CertificateManagerService
        implements ContainerLifecycleListener {
    public static final String SERVICE_TYPE = "certificatemanager";

    private final Logger logger = LogManager.getLogger(getClass());

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            if (configuration().getBoolean("certificatemanager.is_master").orElse(false)) {
                builder.api("certificatemanager", "api/certmanager");
            }
        }

        @Override
        protected void configure() {
            context.register(CertificateManagerService.class, ContainerLifecycleListener.class);
        }
    }

    private ServiceResourceObject sro;
    private Conductor conductor;
    private SslContextFactory.Server serverSslContextFactory;
    private SslContextFactory.Client clientSslContextFactory;
    private Configuration configuration;
    private Xorcery xorcery;
    private JsonApiClient client;

    @Inject
    public CertificateManagerService(@Named(SERVICE_TYPE) ServiceResourceObject sro,
                                     Conductor conductor,
                                     SslContextFactory.Server serverSslContextFactory,
                                     SslContextFactory.Client clientSslContextFactory,
                                     JettyHttpClientContract instance,
                                     Configuration configuration) throws URISyntaxException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        this.sro = sro;
        this.conductor = conductor;
        this.serverSslContextFactory = serverSslContextFactory;
        this.clientSslContextFactory = clientSslContextFactory;
        this.configuration = configuration.getConfiguration("certificatemanager");

/*
        KeyStore keyStore = KeyStore.getInstance(new File(new File(".").toURI().relativize(getClass().getResource("/keystore.p12").toURI()).getPath()), "password".toCharArray());
        logger.info("Certs");
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            logger.info(alias + "=" + keyStore.getCertificate(alias));
        }
*/

        Client client = ClientBuilder.newBuilder()
                .withConfig(new ClientConfig()
                        .register(new JsonApiMessageBodyReader(new ObjectMapper()))
                        .register(new LoggingFeature.LoggingFeatureBuilder().withLogger(java.util.logging.Logger.getLogger("client.certmanager")).build())
                        .register(instance)
                        .connectorProvider(new JettyConnectorProvider()))
                .build();
        this.client = new JsonApiClient(client);
    }

    @Override
    public void onStartup(Container container) {

        if (configuration.getBoolean("renew_on_startup").orElse(false)) {
            conductor.addConductorListener(new CheckCertConductorListener(sro.serviceIdentifier(), "certificatemanager"));
        }
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
    }

    private class CheckCertConductorListener extends AbstractConductorListener {

        public CheckCertConductorListener(ServiceIdentifier serviceIdentifier, String rel) {
            super(serviceIdentifier, rel);
        }

        public void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration) {
            client.get(link)
                    .thenAccept(this::checkCert)
                    .whenComplete((rd, throwable) ->
                    {
                        if (throwable != null) {
                            logger.error("Could not check cert", throwable);
                        }
                    });
        }

        private void checkCert(ResourceDocument cert) {
            ResourceObjects ro = cert.getResources().orElseThrow(NotFoundException::new);

            for (ResourceObject resourceObject : ro) {
                try {
                    String base64Cert = resourceObject.getAttributes().getString("base64").orElseThrow();
                    byte[] certBytes = Base64.getDecoder().decode(base64Cert);

                    File keyStoreFile = new File(new File(".").toURI().relativize(getClass().getResource("/keystore.p12").toURI()).getPath());

                    Files.write(keyStoreFile.toPath(), certBytes, StandardOpenOption.WRITE);

                    serverSslContextFactory.reload(scf ->
                    {
                        LogManager.getLogger(getClass()).info("Reloaded server cert");
                    });
                    clientSslContextFactory.reload(scf ->
                    {
                        LogManager.getLogger(getClass()).info("Reloaded client cert");
                    });
                } catch (Exception e) {
                    LogManager.getLogger(getClass()).info("Error updated certificate", e);
                }
            }

            LogManager.getLogger(getClass()).info("Cert check:" + ro);
        }
    }
}
