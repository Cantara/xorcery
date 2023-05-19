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
package com.exoreaction.xorcery.service.jetty.server;

import org.eclipse.jetty.util.security.CertificateValidator;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.security.KeyStore;
import java.security.cert.CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;

public class CustomSslContextFactoryServer
    extends SslContextFactory.Server
{
    private Collection<? extends CRL> crls;

    public CustomSslContextFactoryServer(Collection<? extends CRL> crls) {
        this.crls = crls;
    }

    @Override
    public void validateCerts(X509Certificate[] certs) throws Exception {
        KeyStore trustStore = this.getTrustStore();
        CertificateValidator validator = new CertificateValidator(trustStore, crls);
        validator.validate(certs);
    }
}
