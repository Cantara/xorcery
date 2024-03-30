/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.neo4j.browser;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.keystores.KeyStoreConfiguration;
import com.exoreaction.xorcery.keystores.KeyStores;
import com.exoreaction.xorcery.keystores.KeyStoresConfiguration;
import com.exoreaction.xorcery.secrets.Secrets;
import jakarta.inject.Inject;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

@Service(name = "neo4jbrowser.ssl")
@RunLevel(3)
public class Neo4jBoltSSL {

    @Inject
    public Neo4jBoltSSL(Configuration configuration,
                        KeyStores keyStores,
                        Secrets secrets) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, IOException, CertificateEncodingException {

        // Save SSL cert files for Bolt to use
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
    }
}
