package com.exoreaction.xorcery.service.certmanager.resources.api;

import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.jsonapi.model.ResourceObjects;
import com.exoreaction.xorcery.service.registry.jsonapi.resources.JsonApiResource;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("api/certmanager")
public class CertificateManagerResource
        extends JsonApiResource {

    private static final Logger logger = LogManager.getLogger(CertificateManagerResource.class);

    @GET
    public ResourceDocument get() throws CertificateException, IOException, ExecutionException, InterruptedException, KeyStoreException, NoSuchAlgorithmException {

        ResourceObjects.Builder builder = new ResourceObjects.Builder();

        X509Certificate[] property = (X509Certificate[]) getContainerRequestContext().getProperty("jakarta.servlet.request.X509Certificate");
        if (property != null) {
            List<X509Certificate> certs = List.of(property);

            for (X509Certificate cert : certs) {

                logger.info("Current expiry date:"+cert.getNotAfter());

                Date tomorrow = Date.from((Instant) Duration.ofDays(1).addTo(Instant.now()));
                if (tomorrow.after(cert.getNotAfter()))
                {
                    // Create new cert
                    String caName = getCaName(cert);


                    String subjectName = getSubjectName(cert);
                    String pathName = subjectName.replace('*', '_');

                    File caDir = new File(caName).getAbsoluteFile();
                    File certDir = new File(caDir, pathName).getAbsoluteFile();

                    Files.walkFileTree(certDir.toPath(), new DeleteDirectoryFiles());

                    Process createCert = Runtime.getRuntime().exec(new String[]{"minica", "-domains", subjectName}, new String[0], caDir).onExit().get();

                    int exitValue = createCert.exitValue();

                    String errors = new String(createCert.getErrorStream().readAllBytes());
                    if (errors.length() > 0)
                        logger.error(errors);

                    Process exportCert = Runtime.getRuntime().exec(new String[]{"openssl", "pkcs12", "-export", "-in", "cert.pem", "-inkey", "key.pem", "-out", "keystore.p12", "-name", "server", "-passout", "pass:password"}, new String[0], certDir).onExit().get();

                    java.nio.file.Path resolve = certDir.toPath().resolve("keystore.p12");
                    try (FileInputStream certIn = new FileInputStream(resolve.toFile())) {
                        byte[] exportedCert = certIn.readAllBytes();
                        String exportedCertString = Base64.getEncoder().encodeToString(exportedCert);

                        KeyStore pkcsKeyStore = KeyStore.getInstance("PKCS12");
                        pkcsKeyStore.load(new ByteArrayInputStream(exportedCert), "password".toCharArray());

                        X509Certificate newCert = (X509Certificate) pkcsKeyStore.getCertificate("server");

                        builder.resource(new ResourceObject.Builder("pkcs12", subjectName)
                                .attributes(new Attributes.Builder()
                                        .attribute("base64", exportedCertString)
                                        .attribute("not_after", newCert.getNotAfter().toString())
                                        .attribute("cert", newCert.toString()))
                                .build());
                    }
                }
            }
        }

        return new ResourceDocument.Builder()
                .data(builder)
                .build();
    }

    private String getCaName(X509Certificate cert) {
        Matcher matcher = Pattern.compile("CN=([a-z]*).*").matcher(cert.getIssuerX500Principal().getName());
        if (matcher.matches()) {
            return matcher.group(1);
        }

        throw new IllegalArgumentException("No CA name found");
    }

    private String getSubjectName(X509Certificate cert) throws CertificateParsingException {
        for (List<?> subjectAlternativeName : cert.getSubjectAlternativeNames()) {
            String name = subjectAlternativeName.stream().filter(String.class::isInstance).map(String.class::cast).findFirst().orElse(null);
            if (name != null)
                return name;
        }

        throw new IllegalArgumentException("No subject name found");
    }

    private static class DeleteDirectoryFiles implements FileVisitor<java.nio.file.Path> {
        @Override
        public FileVisitResult preVisitDirectory(java.nio.file.Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(java.nio.file.Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(java.nio.file.Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }
    }
}
