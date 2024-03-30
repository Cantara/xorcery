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

REM Copy created stores to the correct locations
copy rootcakeystore.p12
copy intermediatecakeystore.p12 src/main/resources/META-INF/intermediatecakeystore.p12
copy ssl.p12 ../xorcery-certificates-provisioning/src/main/resources/META-INF/ssl.p12
copy truststore.p12 ../xorcery-certificates-provisioning/src/main/resources/META-INF/truststore.p12
copy test-ssl.p12 test-ssl.p12
