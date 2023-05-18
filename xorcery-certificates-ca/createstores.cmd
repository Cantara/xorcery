REM Remove existing files first
del rootcakeystore.p12
del src\main\resources\META-INF\intermediatecakeystore.p12
del src\main\resources\META-INF\intermediatecatruststore.p12
del ..\xorcery-certificates-client\src\main\resources\META-INF\keystore.p12
del ..\xorcery-certificates-client\src\main\resources\META-INF\truststore.p12
del ..\xorcery-core\src\test\resources\META-INF\test-keystore.p12
del ..\xorcery-core\src\test\resources\META-INF\test-truststore.p12

REM Create Root CA and add to all truststores
keytool -keystore rootcakeystore.p12 -alias root -genkeypair -dname "cn=Xorcery Root CA, ou=Xorcery, o=eXOReaction, c=NO"  -ext KeyUsage=keyCertSign,cRLSign -ext BasicConstraints=ca:true,PathLen:10 -keyalg EC -validity 365000 -storepass password
keytool -keystore rootcakeystore.p12 -export -alias root -storepass password | keytool -importcert -alias root -keystore src/main/resources/META-INF/intermediatecakeystore.p12 -noprompt -storepass password
keytool -keystore rootcakeystore.p12 -export -alias root -storepass password | keytool -importcert -alias root -keystore src/main/resources/META-INF/intermediatecatruststore.p12 -noprompt -storepass password
keytool -keystore rootcakeystore.p12 -export -alias root -storepass password | keytool -importcert -alias root -keystore ../xorcery-certificates-client/src/main/resources/META-INF/keystore.p12 -noprompt -storepass password
keytool -keystore rootcakeystore.p12 -export -alias root -storepass password | keytool -importcert -alias root -keystore ../xorcery-certificates-client/src/main/resources/META-INF/truststore.p12 -noprompt -storepass password
keytool -keystore rootcakeystore.p12 -export -alias root -storepass password | keytool -importcert -alias root -keystore ../xorcery-core/src/test/resources/META-INF/test-keystore.p12 -noprompt -storepass password
keytool -keystore rootcakeystore.p12 -export -alias root -storepass password | keytool -importcert -alias root -keystore ../xorcery-core/src/test/resources/META-INF/test-truststore.p12 -noprompt -storepass password

REM Create Intermediate CA
keytool -keystore src/main/resources/META-INF/intermediatecakeystore.p12 -alias intermediate -genkeypair -dname "cn=Xorcery Intermediate CA, ou=Xorcery, o=eXOReaction, c=NO" -keyalg EC -validity 365000 -storepass password
keytool -keystore src/main/resources/META-INF/intermediatecakeystore.p12 -alias intermediate -certreq -storepass password | keytool -keystore rootcakeystore.p12 -alias root -gencert -ext KeyUsage=keyCertSign,cRLSign -ext BasicConstraints=ca:true,PathLen:0 -validity 365000 -storepass password | keytool -keystore src/main/resources/META-INF/intermediatecakeystore.p12 -alias intermediate -importcert -noprompt -storepass password

REM Create Provisioning CA
keytool -keystore ../xorcery-certificates-client/src/main/resources/META-INF/keystore.p12 -alias provisioning -genkeypair -dname "cn=Provisioning CA, ou=Xorcery, o=eXOReaction, c=NO" -keyalg EC -validity 365000 -storepass password
keytool -keystore ../xorcery-certificates-client/src/main/resources/META-INF/keystore.p12 -alias provisioning -certreq -storepass password | keytool -keystore rootcakeystore.p12 -alias root -gencert -ext KeyUsage=digitalSignature -ext BasicConstraints=ca:true,PathLen:0 -validity 365000 -storepass password | keytool -keystore ../xorcery-certificates-client/src/main/resources/META-INF/keystore.p12 -alias provisioning -importcert -noprompt -storepass password
keytool -keystore ../xorcery-certificates-client/src/main/resources/META-INF/keystore.p12 -export -alias provisioning -storepass password | keytool -importcert -alias provisioning -keystore src/main/resources/META-INF/intermediatecatruststore.p12 -noprompt -storepass password

REM Create test service keypair and certificate
keytool -keystore ../xorcery-core/src/test/resources/META-INF/test-keystore.p12 -alias self -genkeypair -dname "cn=Test Service" -keyalg EC -validity 365000 -storepass password
keytool -keystore ../xorcery-core/src/test/resources/META-INF/test-keystore.p12 -alias self -certreq -storepass password | keytool -keystore src/main/resources/META-INF/intermediatecakeystore.p12 -alias intermediate -gencert -ext KeyUsage=digitalSignature -ext ExtendedKeyUsage=serverAuth,clientAuth -ext SubjectAlternativeName:critical=DNS:localhost,DNS:server.xorcery.test,IP:127.0.0.1 -validity 365000 -storepass password | keytool -keystore ../xorcery-core/src/test/resources/META-INF/test-keystore.p12 -alias self -importcert -storepass password -noprompt -trustcacerts
