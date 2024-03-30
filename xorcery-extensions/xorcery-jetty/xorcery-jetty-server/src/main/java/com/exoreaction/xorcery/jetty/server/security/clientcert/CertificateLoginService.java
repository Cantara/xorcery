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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Session;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.security.auth.Subject;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Service(name="jetty.server.security.certificate")
@ContractsProvided(LoginService.class)
public class CertificateLoginService
        extends AbstractLoginService {
    private final Logger logger = LogManager.getLogger(getClass());

    @Override
    public String getName() {
        return "clientcert";
    }

    @Override
    public UserIdentity login(String username, Object credentials, Request request, Function<Boolean, Session> getOrCreateSession) {
        if (username == null)
            return null;

        // These are already validated
        X509Certificate[] certs = (X509Certificate[])request.getAttribute("jakarta.servlet.request.X509Certificate");
        if (certs == null)
            return null;
        X509Certificate userCertificate = certs[0];
        ClientCertUserPrincipal userPrincipal = new ClientCertUserPrincipal(userCertificate.getSubjectX500Principal(), new CertificateCredential(userCertificate));
        Subject subject = new Subject();
        userPrincipal.configureSubject(subject);
        subject.setReadOnly();

        logger.debug(username + " logged in using a client certificate");

        return _identityService.newUserIdentity(subject, userPrincipal, new String[0]);
    }

    @Override
    protected List<RolePrincipal> loadRoleInfo(UserPrincipal user) {
        return Collections.emptyList();
    }

    @Override
    protected UserPrincipal loadUserInfo(String username) {
        return null;
    }
}
