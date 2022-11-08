package com.exoreaction.xorcery.service.certmanager;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.TopicSubscribers;
import com.exoreaction.xorcery.jsonapi.client.JsonApiClient;
import com.exoreaction.xorcery.jsonapi.jaxrs.providers.JsonElementMessageBodyReader;
import com.exoreaction.xorcery.jsonapi.jaxrs.providers.JsonElementMessageBodyWriter;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.jsonapi.model.ResourceObjects;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceIdentifier;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.helpers.AbstractGroupListener;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.messaging.Topic;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.client.ClientConfig;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Base64;

import static com.exoreaction.xorcery.service.certmanager.CertificateManagerService.SERVICE_TYPE;

@Service
@Named(SERVICE_TYPE)
public class CertificateManagerService {
    public static final String SERVICE_TYPE = "certificatemanager";

    private final Logger logger = LogManager.getLogger(getClass());

    private ServiceResourceObject sro;
    private SslContextFactory.Server serverSslContextFactory;
    private SslContextFactory.Client clientSslContextFactory;
    private Configuration configuration;
    private JsonApiClient client;

    @Inject
    public CertificateManagerService(ServiceResourceObjects serviceResourceObjects,
                                     ServiceLocator serviceLocator,
                                     SslContextFactory.Server serverSslContextFactory,
                                     SslContextFactory.Client clientSslContextFactory,
                                     ClientConfig clientConfig,
                                     Configuration configuration) throws URISyntaxException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        this.sro = new ServiceResourceObject.Builder(() -> configuration, SERVICE_TYPE)
                .with(b ->
                {
                    if (configuration.getBoolean("certificatemanager.is_master").orElse(false)) {
                        b.api("certificatemanager", "api/certmanager");
                    }
                })
                .build();
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
        Client client = ClientBuilder.newClient(clientConfig
                .register(JsonElementMessageBodyReader.class)
                .register(JsonElementMessageBodyWriter.class));
        this.client = new JsonApiClient(client);

        if (configuration.getBoolean("certificatemanager.renew_on_startup").orElse(false)) {
            TopicSubscribers.addSubscriber(serviceLocator,new CheckCertGroupListener(sro.getServiceIdentifier(), "certificatemanager"));
        }

        serviceResourceObjects.publish(sro);
    }

    private class CheckCertGroupListener extends AbstractGroupListener {

        public CheckCertGroupListener(ServiceIdentifier serviceIdentifier, String rel) {
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

                    File keyStoreFile = new File(new File(".").toURI().relativize(ClassLoader.getSystemResource("/keystore.p12").toURI()).getPath());

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
