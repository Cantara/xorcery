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
package com.exoreaction.xorcery.jetty.server.security.jwt;

import com.exoreaction.xorcery.configuration.Configuration;
import io.jsonwebtoken.*;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
@ContractsProvided(SigningKeyResolver.class)
public class ConfigurationSigningKeyResolver
        extends SigningKeyResolverAdapter {

    private Configuration issuersConfiguration;

    @Inject
    public ConfigurationSigningKeyResolver(Configuration configuration) {
        this.issuersConfiguration = configuration.getConfiguration("jetty.server.security.jwt.issuers");
    }

    @Override
    public byte[] resolveSigningKeyBytes(JwsHeader header, Claims claims) {

        String issuer = claims.getIssuer();
        byte[] key = issuersConfiguration
                .getString("."+issuer)
                .orElseThrow()
                .getBytes(StandardCharsets.UTF_8);
        return key;
    }

    @Override
    public byte[] resolveSigningKeyBytes(JwsHeader header, String payload) {
        return super.resolveSigningKeyBytes(header, payload);
    }

    @Override
    public Key resolveSigningKey(JwsHeader header, Claims claims) {

        String issuer = claims.getIssuer();
        try
        {
            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.forName(header.getAlgorithm());

            if (!signatureAlgorithm.isRsa() && !signatureAlgorithm.isEllipticCurve())
            {
                throw new IllegalStateException("Invalid signature algorithm when public key is provided");
            }

            byte[] publicKeyBase64 = issuersConfiguration
                    .getString("."+issuer)
                    .orElseThrow()
                    .getBytes(StandardCharsets.UTF_8);

            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance(signatureAlgorithm.isRsa() ? "RSA" : "EC");

            return kf.generatePublic(keySpec);
        } catch (Throwable t)
        {
            throw new IllegalArgumentException("Could not get signing key for issuer "+issuer, t);
        }
    }

    @Override
    public Key resolveSigningKey(JwsHeader header, String plaintext) {
        return super.resolveSigningKey(header, plaintext);
    }
}
