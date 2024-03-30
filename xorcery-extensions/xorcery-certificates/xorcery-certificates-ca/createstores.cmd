@REM
@REM Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM     http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM

REM Remove existing files first
del rootcakeystore.p12
del intermediatecakeystore.p12
del ssl.p12
del truststore.p12
del test-ssl.p12

REM Create Root CA and add to all truststores
keytool -keystore rootcakeystore.p12 -alias root -genkeypair -dname "cn=Xorcery Root CA, ou=Xorcery, o=eXOReaction, c=NO"  -ext KeyUsage=keyCertSign,cRLSign -ext BasicConstraints=ca:true,PathLen:10 -keyalg EC -validity 365000 -storepass password
keytool -keystore rootcakeystore.p12 -export -alias root -storepass password | keytool -importcert -alias root -keystore intermediatecakeystore.p12 -noprompt -storepass password
keytool -keystore rootcakeystore.p12 -export -alias root -storepass password | keytool -importcert -alias root -keystore ssl.p12 -noprompt -storepass password
keytool -keystore rootcakeystore.p12 -export -alias root -storepass password | keytool -importcert -alias root -keystore truststore.p12 -noprompt -storepass password
keytool -keystore rootcakeystore.p12 -export -alias root -storepass password | keytool -importcert -alias root -keystore test-ssl.p12 -noprompt -storepass password
keytool -keystore rootcakeystore.p12 -export -alias root -storepass password | keytool -importcert -alias root -keystore test-truststore.p12 -noprompt -storepass password

REM Create Intermediate CA
keytool -keystore intermediatecakeystore.p12 -alias intermediate -genkeypair -dname "cn=Xorcery Intermediate CA, ou=Xorcery, o=eXOReaction, c=NO" -keyalg EC -validity 365000 -storepass password
keytool -keystore intermediatecakeystore.p12 -alias intermediate -certreq -storepass password | keytool -keystore rootcakeystore.p12 -alias root -gencert -ext KeyUsage=keyCertSign,cRLSign -ext BasicConstraints=ca:true,PathLen:0 -validity 365000 -storepass password | keytool -keystore intermediatecakeystore.p12 -alias intermediate -importcert -noprompt -storepass password

REM Create Provisioning certificate
keytool -keystore ssl.p12 -alias provisioning -genkeypair -dname "cn=Provisioning, ou=Xorcery, o=eXOReaction, c=NO" -keyalg EC -validity 365000 -storepass password
keytool -keystore ssl.p12 -alias provisioning -certreq -storepass password | keytool -keystore rootcakeystore.p12 -alias root -gencert -ext KeyUsage=digitalSignature -ext BasicConstraints=ca:true,PathLen:0 -validity 365000 -storepass password | keytool -keystore ssl.p12 -alias provisioning -importcert -noprompt -storepass password

REM Create test service keypair and certificate
keytool -keystore test-ssl.p12 -alias self -genkeypair -dname "cn=Test Service" -keyalg EC -validity 365000 -storepass password
keytool -keystore test-ssl.p12 -alias self -certreq -storepass password | keytool -keystore intermediatecakeystore.p12 -alias intermediate -gencert -ext KeyUsage=digitalSignature -ext ExtendedKeyUsage=serverAuth,clientAuth -ext SubjectAlternativeName:critical=DNS:localhost,DNS:server.xorcery.test,IP:127.0.0.1 -validity 365000 -storepass password | keytool -keystore test-ssl.p12 -alias self -importcert -storepass password -noprompt -trustcacerts
