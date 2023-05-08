package com.exoreaction.xorcery.jwt.server;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException, CertificateException, IOException {
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.ES256);

        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        System.out.println("Public key: "+publicKey);
        System.out.println("Private key: "+privateKey);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey));
        KeyFactory kf = KeyFactory.getInstance("EC");

        Key pk = kf.generatePrivate(keySpec);
        String pkPem = toPEM(pk);
        System.out.println("Private key PEM:\n"+pkPem);
    }

    private static String toPEM(Key pk) throws CertificateException, IOException {
        StringWriter stringWriter = new StringWriter();
        PemWriter pWrt = new PemWriter(stringWriter);
        pWrt.writeObject(new PemObject(PEMParser.TYPE_EC_PRIVATE_KEY, pk.getEncoded()));
        pWrt.close();
        return stringWriter.toString();
    }

}
