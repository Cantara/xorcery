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
package com.exoreaction.xorcery.certificates.letsencrypt;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.certificates.spi.CertificatesProvider;
import com.exoreaction.xorcery.keystores.KeyStores;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

@Service(name = "letsencrypt")
@ContractsProvided(CertificatesProvider.class)
public class LetsEncryptCertificatesManagementProvider
        implements CertificatesProvider {

    private final LetsEncryptConfiguration letsEncryptConfiguration;
    Logger logger = LogManager.getLogger(getClass());

    private final KeyStores keyStores;
    private final Account account;

    private final Map<String, String> challengeTokens = new ConcurrentHashMap<>();

    @Inject
    public LetsEncryptCertificatesManagementProvider(Configuration configuration, KeyStores keyStores) throws NoSuchAlgorithmException, NoSuchProviderException, UnrecoverableKeyException, KeyStoreException, AcmeException, CertificateException, IOException, OperatorCreationException {
        this.keyStores = keyStores;

        letsEncryptConfiguration = new LetsEncryptConfiguration(configuration.getConfiguration("letsencrypt"));

        KeyPair userKeyPair = keyStores.getOrCreateKeyPair("user", "letsencrypt");

        Session session = new Session(letsEncryptConfiguration.getURL());
        account = new AccountBuilder()
                .agreeToTermsOfService()
                .useKeyPair(userKeyPair)
                .create(session);

        logger.info("Account location:");
        logger.info(account.getLocation());
    }

    @Override
    public CompletionStage<List<X509Certificate>> requestCertificates(PKCS10CertificationRequest csr) {

        Collection<String> domains = new ArrayList<>();

        GeneralNames generalNames = GeneralNames.getInstance(csr.getRequestedExtensions()
                .getExtension(Extension.subjectAlternativeName)
                .getExtnValue().getOctets());

        // Only add DNS names. IP addresses are not supported by LetsEncrypt
        domains.add(csr.getSubject().getRDNs()[0].getFirst().getValue().toString());
        for (GeneralName name : generalNames.getNames()) {
            if (name.getTagNo() == GeneralName.dNSName) {
                domains.add(name.getName().toString());
            }
        }

        try {
            Order order = account.newOrder()
                    .domains(domains)
                    .create();

            // Perform all required authorizations
            for (Authorization auth : order.getAuthorizations()) {
                authorize(auth);
            }

            // Generate a CSR for all of the domains,
            // and sign it with the domain key pair.
            CSRBuilder csrb = new CSRBuilder();
            csrb.addDomains(domains);
            csrb.sign(keyStores.getOrCreateKeyPair("domains", "letsencrypt"));
            PKCS10CertificationRequest csr2 = csrb.getCSR();

            // Order the certificate
            order.execute(csr2.getEncoded());

            // Wait for the order to complete
            try {
                int attempts = 10;
                while (order.getStatus() != Status.VALID && attempts-- > 0) {
                    // Did the order fail?
                    if (order.getStatus() == Status.INVALID) {
                        logger.error("Order has failed, reason: {}", order.getError());
                        throw new AcmeException("Order failed... Giving up.");
                    }

                    // Wait for a few seconds
                    Thread.sleep(3000L);

                    // Then update the status
                    order.update();
                }
            } catch (InterruptedException ex) {
                logger.error("interrupted", ex);
                Thread.currentThread().interrupt();
            }

            // Get the certificate
            Certificate certificate = order.getCertificate();

            return CompletableFuture.completedStage(certificate.getCertificateChain());
        } catch (Throwable e) {
            return CompletableFuture.failedStage(e);
        }
    }

    public String getHttpAuthorization(String id) {
        return challengeTokens.get(id);
    }

    private void authorize(Authorization auth)
            throws AcmeException {
        logger.info("Authorization for domain {}", auth.getIdentifier().getDomain());

        // The authorization is already valid.
        // No need to process a challenge.
        if (auth.getStatus() == Status.VALID) {
            return;
        }

        // Find the desired challenge and prepare it.
        List<Challenge> challenges = auth.getChallenges();
        for (Challenge challenge : challenges) {
            if (challenge.getStatus() == Status.VALID) {
                // We're done
                return;
            }
        }

        // DNS challenges are all done offline, so HTTP challenges are the only ones
        // that we could actually do here
        Http01Challenge challenge = httpChallenge(auth);

        if (challenge == null) {
            throw new AcmeException("No challenge found");
        }

        // Now trigger the challenge.
        challenge.trigger();

        // Poll for the challenge to complete.
        // TODO This should be replace with a Process
        try {
            int attempts = 10;
            while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
                // Did the authorization fail?
                if (challenge.getStatus() == Status.INVALID) {
                    logger.error("Challenge has failed, reason: {}", challenge.getError());
                    throw new AcmeException("Challenge failed... Giving up.");
                }

                // Wait for a few seconds
                Thread.sleep(3000L);

                // Then update the status
                challenge.update();
            }
        } catch (InterruptedException ex) {
            logger.error("interrupted", ex);
            Thread.currentThread().interrupt();
        } finally {
            challengeTokens.remove(challenge.getToken());
        }

        // All reattempts are used up and there is
        // still no valid authorization?
        if (challenge.getStatus() != Status.VALID) {
            throw new AcmeException("Failed to pass the challenge for domain "
                    + auth.getIdentifier().getDomain() + ", ... Giving up.");
        }

        logger.info("Challenge has been completed. Remember to remove the validation resource.");
    }

    public Http01Challenge httpChallenge(Authorization auth) throws AcmeException {
        // Find a single http-01 challenge
        Http01Challenge challenge = auth.findChallenge(Http01Challenge.class);
        if (challenge == null) {
            throw new AcmeException("Found no " + Http01Challenge.TYPE + " challenge, don't know what to do...");
        }

        logger.debug("Challenge token must be reachable at: http://{}/.well-known/acme-challenge/{}",
                auth.getIdentifier().getDomain(), challenge.getToken());

        challengeTokens.put(challenge.getToken(), challenge.getAuthorization());

        return challenge;
    }
}
