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
package dev.xorcery.certificates.letsencrypt;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.keystores.KeyStores;
import dev.xorcery.secrets.Secrets;
import dev.xorcery.secrets.providers.EnvSecretsProvider;
import dev.xorcery.secrets.providers.SecretSecretsProvider;
import dev.xorcery.secrets.spi.SecretsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import picocli.CommandLine;

import java.io.File;
import java.security.KeyPair;
import java.security.Provider;
import java.security.Security;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "letsencrypt", version = "1.0")
public class Main
        implements Callable<Integer> {

    @CommandLine.Option(names = "-domain", description = "Domain", required = true)
    private String domain;

    @CommandLine.Option(names = "-keystore", description = "Keystore file", defaultValue = "letsencrypt.p12", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private File keyStoreFile;

    @CommandLine.Option(names = "-password", description = "Keystore password", defaultValue = "password")
    private String keyStorePassword;

    @CommandLine.Option(names = "-url", description = "Let's Encrypt URL", defaultValue = "acme://letsencrypt.org", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private String url;

    private Logger logger;

    @Override
    public Integer call() throws Exception {

        Configuration config = new Configuration.Builder()
                .add("keystores.letsencrypt.path", keyStoreFile.getAbsolutePath())
                .add("keystores.letsencrypt.password", keyStorePassword)
                .build();

        Map<String, SecretsProvider> providers = Map.of("env", new EnvSecretsProvider(), "secret", new SecretSecretsProvider());
        KeyStores keyStores = new KeyStores(config, new Secrets(providers::get, "secret"), LogManager.getLogger(KeyStores.class));

        System.setProperty("log4j2.configurationFile", "META-INF/letsencryptlog4j2.yaml");
        logger = LogManager.getLogger("letsencrypt");
        logger.info("Let's Encrypt DNS authorization");

        Provider p = new org.bouncycastle.jce.provider.BouncyCastleProvider();
        if (null == Security.getProvider(p.getName())) {
            Security.addProvider(p);
        }

        KeyPair userKeyPair = keyStores.getOrCreateKeyPair("user", "letsencrypt");

        // This just needs to be created and put into the keystore for later requests/renewals of certificates
        KeyPair domainKeyPair = keyStores.getOrCreateKeyPair("domains", "letsencrypt");

        Session session = new Session(url);

        Account account = new AccountBuilder()
                .agreeToTermsOfService()
                .useKeyPair(userKeyPair)
                .create(session);

        logger.info("Account location:");
        logger.info(account.getLocation());

        Order order = account.newOrder()
                .domains(domain)
                .create();

        logger.info("Order location:" + order.getLocation());

        // Perform all required authorizations
        for (Authorization auth : order.getAuthorizations()) {
            authorize(auth);
        }

        // Generate a CSR for all of the domains,
        // and sign it with the domain key pair.
        CSRBuilder csrb = new CSRBuilder();
        csrb.addDomains(domain);
        csrb.sign(domainKeyPair);

        // Order the certificate
        order.execute(csrb.getEncoded());

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

        logger.info("Success! The certificate for domains {} has been generated!", domain);
        logger.info("Certificate URL: {}", certificate.getLocation());

        return 0;
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
        Dns01Challenge challenge = dnsChallenge(auth);

        if (challenge == null) {
            throw new AcmeException("No challenge found");
        }

        // If the challenge is already verified,
        // there's no need to execute it again.
        if (challenge.getStatus() == Status.VALID) {
            logger.info("Already authorized");
            return;
        }

        // Wait for user to complete DNS record
        logger.info("Press enter when DNS record has been registered");
        Scanner scanner = new Scanner(System.in);
        String line = scanner.nextLine();

        // Now trigger the challenge.
        challenge.trigger();

        // Poll for the challenge to complete.
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
        }

        // All reattempts are used up and there is
        // still no valid authorization?
        if (challenge.getStatus() != Status.VALID) {
            throw new AcmeException("Failed to pass the challenge for domain "
                    + auth.getIdentifier().getDomain() + ", ... Giving up.");
        }

        logger.info("Authorization complete");
    }

    public Dns01Challenge dnsChallenge(Authorization auth) throws AcmeException {
        // Find a single dns-01 challenge
        Dns01Challenge challenge = auth.findChallenge(Dns01Challenge.TYPE);
        if (challenge == null) {
            throw new AcmeException("Found no " + Dns01Challenge.TYPE + " challenge, don't know what to do...");
        }

        // Output the challenge, wait for acknowledge...
        logger.info("Please create a TXT record:");
        logger.info("{} IN TXT {}",
                Dns01Challenge.toRRName(auth.getIdentifier()), challenge.getDigest());

        return challenge;
    }

    public static void main(String[] args) throws Exception {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
