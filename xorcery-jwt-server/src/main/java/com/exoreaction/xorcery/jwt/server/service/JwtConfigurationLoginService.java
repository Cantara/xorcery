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
package com.exoreaction.xorcery.jwt.server.service;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.service.jetty.server.security.jwt.ClaimsCredential;
import com.exoreaction.xorcery.service.jetty.server.security.jwt.JwtUserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.inject.Inject;
import jakarta.servlet.ServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.server.UserIdentity;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.List;

@Service(name = "jwt.server.configuration")
@ContractsProvided({LoginService.class,JwtConfigurationLoginService.class})
public class JwtConfigurationLoginService
        extends AbstractLoginService {
    private final Logger logger = LogManager.getLogger(getClass());

    private final Configuration users;

    @Inject
    public JwtConfigurationLoginService(Configuration configuration) {
        this.users = configuration.getConfiguration("jwt.server.configuration.users");
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
        if (!user.getString("password").orElse("").equals(credentials.toString()))
            return null;

        Claims claims = Jwts.claims();
        claims.putAll(JsonElement.toMap(user.getConfiguration("claims").json(), JsonNode::textValue));

        JwtUserPrincipal jwtUserPrincipal = new JwtUserPrincipal(username, new ClaimsCredential(claims));
        Subject subject = new Subject();
        jwtUserPrincipal.configureSubject(subject);
        subject.setReadOnly();

        UserIdentity userIdentity = new DefaultUserIdentity(subject, jwtUserPrincipal, new String[0]);

        logger.debug(username + " created a JWT token");

        return userIdentity;
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