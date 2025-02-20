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
package dev.xorcery.certificates.client;

import dev.xorcery.certificates.spi.CertificatesProvider;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.jsonapi.Attributes;
import dev.xorcery.jsonapi.Link;
import dev.xorcery.jsonapi.ResourceObject;
import dev.xorcery.jsonapi.client.JsonApiClient;
import dev.xorcery.jsonapi.providers.JsonElementMessageBodyReader;
import dev.xorcery.jsonapi.providers.JsonElementMessageBodyWriter;
import dev.xorcery.jsonapi.service.ServerResourceDocument;
import dev.xorcery.jsonapi.service.ServiceIdentifier;
import dev.xorcery.jsonapi.service.ServiceResourceObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

public class ClientCertificatesProvider
        implements CertificatesProvider, Closeable {

    private final JsonApiClient jsonApiClient;
    private final CertificatesClientConfiguration certificatesClientConfiguration;
    private final Client client;

    public ClientCertificatesProvider(ClientBuilder clientBuilder,
                                      Configuration configuration) {
        this.certificatesClientConfiguration = () -> configuration.getConfiguration("certificates.client");

        client = clientBuilder
                .register(JsonElementMessageBodyReader.class)
                .register(JsonElementMessageBodyWriter.class)
                .build();

        this.jsonApiClient = new JsonApiClient(client);
    }

    @Override
    public CompletionStage<List<X509Certificate>> requestCertificates(PKCS10CertificationRequest csr) {

        return certificatesClientConfiguration.getURI()
                .map(uri -> jsonApiClient.get(new Link("self", uri))
                        .thenApply(ServerResourceDocument::new)
                        .thenApply(this::getCertificatesRequest)
                        .thenCompose(sendCertificateRequest(csr))
                        .thenCompose(toCertificateChain()))
                .orElseGet(() -> CompletableFuture.failedStage(new IllegalArgumentException("Missing certificates server URI")));
    }

    private Optional<Link> getCertificatesRequest(ServerResourceDocument certificatesHostApi) {
        return certificatesHostApi.getServiceByType("certificates")
                .or(() -> certificatesHostApi.getServiceByIdentifier(ServiceIdentifier.serviceId("certificates")))
                .flatMap(sro -> sro.getLinkByRel("request"));
    }

    private Function<Optional<Link>, CompletionStage<ServiceResourceObject>> sendCertificateRequest(PKCS10CertificationRequest csr) {
        return requestLink -> requestLink.map(link ->
        {
            try {
                // Convert to PEM format
                StringWriter stringWriter = new StringWriter();
                PemWriter pWrt = new PemWriter(stringWriter);
                pWrt.writeObject(new PemObject(PEMParser.TYPE_CERTIFICATE_REQUEST, csr.getEncoded()));
                pWrt.close();
                String csrPem = stringWriter.toString();

                return jsonApiClient.submit(link, new ResourceObject.Builder("certificaterequest")
                                .attributes(new Attributes.Builder().attribute("csr", csrPem).build())
                                .build())
                        .thenApply(ServiceResourceObject::new);
            } catch (Throwable ex) {
                return CompletableFuture.<ServiceResourceObject>failedStage(ex);
            }
        }).orElseGet(() -> CompletableFuture.failedStage(new IllegalArgumentException("No request link in certificates API")));
    }

    private Function<ServiceResourceObject, CompletionStage<List<X509Certificate>>> toCertificateChain() {
        return sro ->
                sro.getAttributes().attributes().getString("pem").map(certificatePem ->
                {
                    try {
                        CertificateFactory cf = CertificateFactory.getInstance("X.509");
                        List<X509Certificate> chain = cf.generateCertificates(new ByteArrayInputStream(certificatePem.getBytes(StandardCharsets.UTF_8))).stream()
                                .map(c -> (X509Certificate) c)
                                .collect(toList());

                        return CompletableFuture.completedStage(chain);
                    } catch (Throwable e) {
                        return CompletableFuture.<List<X509Certificate>>failedStage(new CompletionException("Could not parse certificate PEM", e));
                    }
                }).orElseGet(() -> CompletableFuture.failedStage(new IllegalArgumentException("Missing PEM in response")));
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public String toString() {
        return "Client Certificate provider";
    }
}
