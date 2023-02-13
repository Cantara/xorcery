package com.exoreaction.xorcery.service.jetty.server.security.jwt;

import com.exoreaction.xorcery.configuration.model.Configuration;
import io.jsonwebtoken.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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
