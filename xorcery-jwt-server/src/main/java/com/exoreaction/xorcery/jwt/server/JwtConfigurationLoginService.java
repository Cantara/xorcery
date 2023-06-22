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
package com.exoreaction.xorcery.jwt.server;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.secrets.Secrets;
import jakarta.inject.Inject;
import jakarta.servlet.ServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Password;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.List;

@Service(name = "jwt.server.configuration")
@ContractsProvided({LoginService.class,JwtConfigurationLoginService.class})
public class JwtConfigurationLoginService
        extends AbstractLoginService {
    private final Configuration users;
    private final Secrets secrets;

    @Inject
    public JwtConfigurationLoginService(Configuration configuration, Secrets secrets) {
        this.users = configuration.getConfiguration("jwt.users");
        this.secrets = secrets;
    }

    @Override
    public String getName() {
        return "jwt";
    }

    @Override
    public UserIdentity login(String username, Object credentials, ServletRequest request) {
        if (username == null || !users.has(username))
            return null;

        Configuration user = users.getConfiguration(username);

        // Check password
        if (!user.getString("password").map(secrets::getSecretString).orElse("").equals(credentials.toString()))
            return null;

        UserPrincipal userPrincipal = new UserPrincipal(username, new Password(credentials.toString()));
        Subject subject = new Subject();
        userPrincipal.configureSubject(subject);
        subject.setReadOnly();

        return new DefaultUserIdentity(subject, userPrincipal, new String[0]);
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