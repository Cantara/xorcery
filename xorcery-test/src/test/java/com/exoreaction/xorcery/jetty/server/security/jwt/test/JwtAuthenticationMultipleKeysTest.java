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
package com.exoreaction.xorcery.jetty.server.security.jwt.test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.jsonapi.MediaTypes;
import com.exoreaction.xorcery.net.Sockets;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.shiro.codec.Base64;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.ECKey;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

public class JwtAuthenticationMultipleKeysTest {
    String config = """
            jetty.server.http.enabled: true
            jetty.server.http.port: "{{ SYSTEM.port }}"
            jetty.server.ssl.enabled: false
            jetty.server.security.enabled: true
            jetty.server.security.method: "jwt"
            jetty.server.security.jwt:
                enabled: true
                issuers:
                  authentication.catalystone.com:
                    keys:
                      - publicKey: "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAj6uESe/1pQZKpFEfsjLXXNEm6yGWH1NpJp1SBUYJFKzbPOcbrdxSeYq0ZzQGwb68v41aUiMVvprdUJJv2dkTSVd92SokgxyEXecESgyKxSNzqdbGYcJ9Q66RtKBQx9uzVKdQaDk+nkhGKMadWStkQ8hybsczWtYzSF1yJgcf0pXsYFk/sIetIb0LNlhHuSasqJikiKivW2kTE60+KwC/E3QcKStg1qbCDxGyWouOC4r1eQX5RhWGNE570y+zPePfMc4RNS6rXEH9OVELuXPGwlKV30cZPT3GYa49GACIF6LDXosPW+ct3Qj1SiV6VkrCIs5uTQ3+Trvs9xauCA8pBwIDAQAB"                        
                        alg: "RS256"
                      - publicKey: "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAj3jFbQaQzXpmuFrNeYphJd4y2EmEX4lHihIAmeUeNMQUd4BAzVDLbw0Ubzpm6jhkRRo/psXvrwZp1SLNqhoxwGooQ+/hPxSy4CPLFiXdQQzbllGaJwMGMrolGg2nYb2YZ1Wzs/GvzfqG1NzIr05iJOTYs5/96dcPN20wWEKPoEVlRZr918flPyAmZ7zN4eVLa2Ck6ZN3NNhDMlRfQTL4RamfoR63Nt88odWOnh9kQsVoYL9A2EYcJfSEyVB7PHP8TFy0IvbSxXWrcr4tWrgkN3di5jf0d5PYIzcEpN/lmaGRFxdOcSkzcYklsGoUrs3rYW7Z4w7WEK1dAGunzk/UOQIDAQAB"
                        alg: "RS256"                        
                      - publicKey: "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAk/d+A9CEAHvPP8g12J/1atIzYIwAdheVfuJukZD8KIs2dp3KoX7hayPtLh2yp4d/yzECcYqJMVNWSrxDRV5eTzv02T5753KG84FFrN87EHpJ0arhVT3YluboDqlMF0lv/V61k4DptZM1rHY24PIncjFWT8R0kkGHsZsZJWXngghP4DZjAH40U7owZ2UL1PVZQperhC/PWtIU0NvtcZn9j6gmoj6bFmqaeDeaCTyZDMnW08pG5aPVP8hqgVlV+bZdMWwCG3dVTulvXUQJXj8HjqdIAjs0gd7k5E5v7d/1NULhK7Pi4bhgt0W+A2uWFZcIum9Q6fNnclAL7cOJVgC9VwIDAQAB"
                        alg: "RS256"
            """;

    @Inject
    ClientBuilder clientBuilder;

    @Test
    public void testValidJWTAuthentication() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(256);
        KeyPair keyPair = g.generateKeyPair();

        String publicKey = Base64.encodeToString(keyPair.getPublic().getEncoded());
        String keyId = UUID.randomUUID().toString();
        System.setProperty("key", publicKey);
        System.setProperty("port", Integer.toString(Sockets.nextFreePort()));
        Configuration serverConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(config).build();
        System.out.println(serverConfiguration);
        try (Xorcery server = new Xorcery(serverConfiguration)) {

            server.getServiceLocator().inject(this);

            InstanceConfiguration cfg = new InstanceConfiguration(serverConfiguration.getConfiguration("instance"));
            //String jwt = createJwt(keyId, keyPair.getPrivate());
            String jwt = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJhdXRoZW50aWNhdGlvbi5jYXRhbHlzdG9uZS5jb20iLCJleHAiOjE2ODczMzQ1ODgsImlhdCI6MTY4NzMzMDk4OCwianRpIjoiNWYxNDU4MWEtZWFjZS00MDljLTkwMGYtYjc4YTFlYmUwNzRmIiwic3ViIjoiYjI1YjQ2YTQtODQ0Yi00Mjg1LWJmYTgtOWM1NWRmYmY3MGNiIiwiaHR0cHM6Ly93d3cuY2F0YWx5c3RvbmUuY29tL2NsYWltcy9wcmVmZXJyZWRfdXNlcm5hbWUiOiJocmMiLCJ0ZW5hbnRfbmFtZSI6ImFuYWx5dGljc3JvdXRpbmVkZXYxIiwiaHR0cHM6Ly93d3cuY2F0YWx5c3RvbmUuY29tL2NsYWltcy9hdXRoX3RpbWUiOjE2ODczMzA5ODgyMTYsImh0dHBzOi8vd3d3LmNhdGFseXN0b25lLmNvbS9jbGFpbXMvdXNlcl9lbWFpbF9pZCI6Im5vcmVwbHlAY2F0YWx5c3RvbmUuY29tIiwiaHR0cHM6Ly93d3cuY2F0YWx5c3RvbmUuY29tL2NsYWltcy9wcm9maWxlIjoiMDMzMmFlMjctMGJjNS00MTBjLTk1YTEtNTllOGFhYTc4YjcwIiwicHJvZmlsZSI6IjAzMzJhZTI3LTBiYzUtNDEwYy05NWExLTU5ZThhYWE3OGI3MCIsImh0dHBzOi8vd3d3LmNhdGFseXN0b25lLmNvbS9jbGFpbXMvbG9jYWxlIjoiZW4iLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJocmMiLCJsb2NhbGUiOiJlbiIsInVzZXJfZW1haWxfaWQiOiJub3JlcGx5QGNhdGFseXN0b25lLmNvbSIsImFhdCI6MTY4NzMzMDk4ODIxNiwiaHR0cHM6Ly93d3cuY2F0YWx5c3RvbmUuY29tL2NsYWltcy90ZW5hbnRfbmFtZSI6ImFuYWx5dGljc3JvdXRpbmVkZXYxIiwiaHR0cHM6Ly93d3cuY2F0YWx5c3RvbmUuY29tL2NsYWltcy90ZW5hbnQiOiIyZTg5ZTk5NS1lNjA1LTViM2YtYWJiNy1mYzUyZGFiYmQ0YTIiLCJ0ZW5hbnQiOiIyZTg5ZTk5NS1lNjA1LTViM2YtYWJiNy1mYzUyZGFiYmQ0YTIiLCJodHRwczovL3d3dy5jYXRhbHlzdG9uZS5jb20vY2xhaW1zL3N1Yl90eXBlIjoidXNlciJ9.CNQ8foaLmW-EpgRh90AzspAWNtlgsDeB8o9NCC2Y-McAxSE8r7Nz1iFV_W_Lm-WVjYser2aoCg6pvbIPw4dq8-fAHNGU-8wFbUpGlFUkGtWvDFO_gQLLuU94FJr_FUAdcLZsruqKHJW6FkirrBEiy9m-_u_KbzR-rntqGUnyUOZShyMTq8m6HDNxFs6LvsC8QGu3vqecrBInKRAavs6C6ZBHEeHeaQzBlhBCTjvvRYI6tHbc8dFQR5BZbL261a_uyJlBpuVSFb8uP2-lFVnLhcOB-sx2zDEJwdHZ2HUOUD7FOrZ5Ndd5pgTXPGN_98mOnf4j9u9KXgMGwyXNvw19MA";
            try (Client client = clientBuilder.build()) {
                String response = client.target(cfg.getAPI().resolve("subject")).request().accept(MediaTypes.APPLICATION_JSON_API).header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt).get().readEntity(String.class);

                System.out.println(response);
            }
        }
    }

    private String createJwt(String keyId, PrivateKey key) {
        Date now = new Date();
        Date tomorrow = Date.from(now.toInstant().plus(1, ChronoUnit.DAYS));

        Algorithm algorithm = Algorithm.ECDSA256((ECKey) key);
        String token = JWT.create()
                .withIssuer("authentication.xorcery.test")
                .withKeyId(keyId)
                .withSubject("gandalf")
                .withClaim("name", "Gandalf")
                .withClaim("scope", "users")
                .withClaim("tenant", "SomeTenant")
                .withIssuedAt(now)
                .withExpiresAt(tomorrow)
                .sign(algorithm);
        return token;
    }

}
