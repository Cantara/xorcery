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
package dev.xorcery.jetty.server.security;

import dev.xorcery.configuration.Configuration;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.servlet.annotation.ServletSecurity;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.ee10.servlet.security.ConstraintMapping;
import org.eclipse.jetty.ee10.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.*;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service(name="jetty.server.security")
@Priority(4)
public class SecurityHandlerFactory
        implements Factory<ConstraintSecurityHandler> {

    private final ConstraintSecurityHandler securityHandler;

    @Inject
    public SecurityHandlerFactory(Configuration configuration,
                                  Provider<Authenticator.Factory> authenticatorFactoryProvider,
                                  Provider<LoginService> loginServiceProvider,
                                  Provider<IdentityService> identityServiceProvider,
                                  Logger logger
                                      ) {

        securityHandler = new ConstraintSecurityHandler();
        LoginService loginService = Optional.ofNullable(loginServiceProvider.get()).orElseGet(EmptyLoginService::new);
        IdentityService identityService = Optional.ofNullable(identityServiceProvider.get()).orElseGet(DefaultIdentityService::new);
        loginService.setIdentityService(identityService);
        securityHandler.setLoginService(loginService);
        securityHandler.setIdentityService(identityService);
        securityHandler.setAuthenticatorFactory(Optional.ofNullable(authenticatorFactoryProvider.get()).orElseGet(DefaultAuthenticatorFactory::new));
        JettySecurityConfiguration.get(configuration).getAuthenticationType().ifPresent(securityHandler::setAuthenticationType);

        // Load constraints
        JettySecurityConfiguration jettySecurityConfiguration = JettySecurityConfiguration.get(configuration);
        Map<String, Constraint> constraints = new HashMap<>();
        jettySecurityConfiguration.getConstraints().forEach(c -> constraints.put(c.getName(),
                ConstraintSecurityHandler.createConstraint(c.getName(), c.getRoles().toArray(new String[0]), ServletSecurity.EmptyRoleSemantic.PERMIT, ServletSecurity.TransportGuarantee.NONE)));

        // Loa path -> constraint mappings
        jettySecurityConfiguration.getMappings().forEach(m ->
        {
            m.getConstraint().ifPresentOrElse(name ->
            {
                Constraint constraint = constraints.get(name);
                if (constraint == null) {
                    logger.error("Mapping for {} with constraint {} failed, constraint does not exist", m.getPath(), m.getConstraint());
                    return;
                }

                ConstraintMapping mapping = new ConstraintMapping();
                mapping.setConstraint(constraint);
                mapping.setPathSpec(m.getPath());
                securityHandler.addConstraintMapping(mapping);
                logger.debug("Path '{}' mapped to security constraint '{}'", m.getPath(), m.getConstraint());
            }, () ->
            {
                logger.debug("Skipped constraint mapping for path '{}', constraint name was not set", m.getPath());
            });
        });

    }

    @Override
    public ConstraintSecurityHandler provide() {
        return securityHandler;
    }

    @Override
    public void dispose(ConstraintSecurityHandler instance) {

    }
}
