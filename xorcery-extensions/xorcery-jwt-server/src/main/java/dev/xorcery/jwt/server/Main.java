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
package dev.xorcery.jwt.server;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

public class Main {

    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException, CertificateException, IOException {

        String algorithmFamily = "EC";

        KeyPairGenerator g = KeyPairGenerator.getInstance(algorithmFamily);
        g.initialize(256);
        KeyPair keyPair = g.generateKeyPair();

        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        System.out.println("Algorithm: ES256");
        System.out.println("Key id: " + UUID.randomUUID());
        System.out.println("Public key: " + publicKey);
        System.out.println("Private key: " + privateKey);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey));
        KeyFactory kf = KeyFactory.getInstance("EC");

        Key pk = kf.generatePrivate(keySpec);
        String pkPem = toPEM(pk);
        System.out.println("Private key PEM:\n" + pkPem);
    }

    private static String toPEM(Key pk) throws CertificateException, IOException {
        StringWriter stringWriter = new StringWriter();
        PemWriter pWrt = new PemWriter(stringWriter);
        pWrt.writeObject(new PemObject(PEMParser.TYPE_EC_PRIVATE_KEY, pk.getEncoded()));
        pWrt.close();
        return stringWriter.toString();
    }

}
