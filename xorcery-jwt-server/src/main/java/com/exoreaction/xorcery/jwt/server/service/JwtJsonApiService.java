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
