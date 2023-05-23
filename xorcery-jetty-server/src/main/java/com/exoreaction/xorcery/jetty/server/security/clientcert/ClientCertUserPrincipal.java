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
package com.exoreaction.xorcery.jetty.server.security.clientcert;

import org.eclipse.jetty.security.UserPrincipal;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;

public class ClientCertUserPrincipal
        extends UserPrincipal {
    private final X500Principal x500Principal;

    public ClientCertUserPrincipal(X500Principal x500Principal, CertificateCredential credential) {
        super(x500Principal.getName(), credential);
        this.x500Principal = x500Principal;
    }

    public X500Principal getX500Principal() {
        return x500Principal;
    }

    public X509Certificate getCertificate() {
        return ((CertificateCredential) _credential).getCertificate();
    }
}
