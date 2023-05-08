package com.exoreaction.xorcery.jwt.server.service;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import jakarta.inject.Inject;
import org.eclipse.jetty.security.LoginService;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service(name="jwt.server")
public class JwtJsonApiService {

    private final Configuration configuration;
    private final JwtConfigurationLoginService loginService;
    private final Key privateKey;

    @Inject
    public JwtJsonApiService(Configuration configuration,
                             ServiceResourceObjects serviceResourceObjects,
                             JwtConfigurationLoginService loginService) throws NoSuchAlgorithmException, InvalidKeySpecException {
        this.configuration = configuration;
        this.loginService = loginService;

        String encodedKey = configuration.getString("jwt.server.key").orElseThrow(()->new IllegalStateException("Missing JWT signing key"));

        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");
        privateKey = kf.generatePrivate(keySpec);

        serviceResourceObjects.add(new ServiceResourceObject.Builder(new InstanceConfiguration(configuration.getConfiguration("instance")), "login")
                .with(b ->
                {
                    b.api("login", "api/login");
                })
                .build());

    }

    public LoginService getLoginService() {
        return loginService;
    }

    public Key getSigningKey()
    {
        return privateKey;
    }
}
