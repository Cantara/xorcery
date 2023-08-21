package com.exoreaction.xorcery.neo4j.browser;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.keystores.KeyStoreConfiguration;
import com.exoreaction.xorcery.keystores.KeyStores;
import com.exoreaction.xorcery.keystores.KeyStoresConfiguration;
import com.exoreaction.xorcery.secrets.Secrets;
import com.exoreaction.xorcery.server.api.ServiceResourceObject;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.util.Resources;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

@Service(name = "neo4jdatabase.browser")
@RunLevel(4)
public class Neo4jBrowser {

    @Inject
    public Neo4jBrowser(ServiceResourceObjects serviceResourceObjects,
                        Configuration configuration,
                        Provider<ServletContextHandler> ctxProvider,
                        KeyStores keyStores,
                        Secrets secrets) throws URISyntaxException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, IOException, CertificateEncodingException {

        // Save SSL cert files
        KeyStoresConfiguration keyStoresConfiguration = new KeyStoresConfiguration(configuration.getConfiguration("keystores"));
        KeyStoreConfiguration sslKeyStoreConfiguration = keyStoresConfiguration.getKeyStoreConfiguration("ssl");

        KeyStore sslKeyStore = keyStores.getKeyStore("ssl");
        String alias = configuration.getString("jetty.server.ssl.alias").orElse("self");

        Key privateKey = sslKeyStore.getKey(alias, sslKeyStoreConfiguration.getPassword().map(secrets::getSecretString).map(String::toCharArray).orElseThrow());
        File neo4jDatabasePath = configuration.getString("neo4jdatabase.path").map(File::new).orElseThrow();
        File boltCertificates = new File(neo4jDatabasePath, "certificates/bolt");
        boltCertificates.mkdirs();

        File privateKeyFile = new File(boltCertificates, "private.key");
        try (FileWriter privateOut = new FileWriter(privateKeyFile, StandardCharsets.UTF_8)) {
            PemWriter pWrt = new PemWriter(privateOut);
            pWrt.writeObject(new PemObject(PEMParser.TYPE_PRIVATE_KEY, privateKey.getEncoded()));
            pWrt.close();
        }

        Certificate[] certificates = sslKeyStore.getCertificateChain(alias);
        File certFile = new File(boltCertificates, "public.crt");
        try (FileWriter certOut = new FileWriter(certFile, StandardCharsets.UTF_8)) {
            PemWriter pWrt = new PemWriter(certOut);
            for (Certificate certificate : certificates) {
                pWrt.writeObject(new PemObject(PEMParser.TYPE_CERTIFICATE, certificate.getEncoded()));
            }
            pWrt.close();
        }

        // Install servlet for Neo4j Browser
        URL webRootLocation = Resources.getResource("browser/index.html").orElseThrow();
        URI webRootUri = URI.create(webRootLocation.toURI().toASCIIString().replaceFirst("/index.html", "/"));
        DefaultServlet defaultServlet = new DefaultServlet();
        ServletHolder servletHolder = new ServletHolder(defaultServlet);
        ServletContextHandler servletContextHandler = ctxProvider.get();
        servletContextHandler.setInitParameter(DefaultServlet.CONTEXT_INIT + "resourceBase", webRootUri.toASCIIString());
        servletContextHandler.setInitParameter(DefaultServlet.CONTEXT_INIT + "pathInfoOnly", Boolean.TRUE.toString());
        servletContextHandler.addServlet(servletHolder, "/api/neo4j/browser/*");

        serviceResourceObjects.add(new ServiceResourceObject.Builder(new InstanceConfiguration(configuration.getConfiguration("instance")), "neo4jbrowser")
                .api("browser", "api/neo4j/browser/")
                .build());
    }
}
