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
package com.exoreaction.xorcery.service.jetty.server.security;

import com.exoreaction.xorcery.configuration.model.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.eclipse.jetty.security.*;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

import java.util.Optional;

@Service(name = "jetty.server.security")
public class SecurityHandlerService
        implements Factory<SecurityHandler> {

    private final ConstraintSecurityHandler securityHandler;

    @Inject
    public SecurityHandlerService(Configuration configuration,
                                  Provider<Authenticator.Factory> authenticatorFactoryProvider,
                                  Provider<LoginService> loginServiceProvider,
                                  Provider<IdentityService> identityServiceProvider
                                      ) {

        securityHandler = new ConstraintSecurityHandler();
        LoginService loginService = Optional.ofNullable(loginServiceProvider.get()).orElseGet(EmptyLoginService::new);
        IdentityService identityService = Optional.ofNullable(identityServiceProvider.get()).orElseGet(DefaultIdentityService::new);
        loginService.setIdentityService(identityService);
        securityHandler.setLoginService(loginService);
        securityHandler.setIdentityService(identityService);
        securityHandler.setAuthenticatorFactory(Optional.ofNullable(authenticatorFactoryProvider.get()).orElseGet(DefaultAuthenticatorFactory::new));
        configuration.getString("jetty.server.security.method").ifPresent(securityHandler::setAuthMethod);
    }

    @Override
    @Singleton
    @Named("jetty.server.security")
    public SecurityHandler provide() {
        return securityHandler;
    }

    @Override
    public void dispose(SecurityHandler instance) {

    }
}
