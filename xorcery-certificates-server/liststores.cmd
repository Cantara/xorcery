REM List all keystores
cls
keytool -keystore rootcakeystore.p12 -list -storepass password
keytool -keystore src/main/resources/META-INF/intermediatecakeystore.p12 -list -storepass password
keytool -keystore src/main/resources/META-INF/intermediatecatruststore.p12 -list -storepass password
keytool -keystore ../xorcery-certificates-client/src/main/resources/META-INF/keystore.p12 -list -storepass password
keytool -keystore ../xorcery-certificates-client/src/main/resources/META-INF/truststore.p12 -list -storepass password
keytool -keystore ../xorcery-core/src/test/resources/META-INF/test-keystore.p12 -list -storepass password
keytool -keystore ../xorcery-core/src/test/resources/META-INF/test-truststore.p12 -list -storepass password
