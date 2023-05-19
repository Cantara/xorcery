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

REM List all keystores
cls
keytool -keystore rootcakeystore.p12 -list -storepass password
keytool -keystore src/main/resources/META-INF/intermediatecakeystore.p12 -list -storepass password
keytool -keystore src/main/resources/META-INF/intermediatecatruststore.p12 -list -storepass password
keytool -keystore ../xorcery-certificates-client/src/main/resources/META-INF/keystore.p12 -list -storepass password
keytool -keystore ../xorcery-certificates-client/src/main/resources/META-INF/truststore.p12 -list -storepass password
keytool -keystore ../xorcery-core/src/test/resources/META-INF/test-keystore.p12 -list -storepass password
keytool -keystore ../xorcery-core/src/test/resources/META-INF/test-truststore.p12 -list -storepass password
