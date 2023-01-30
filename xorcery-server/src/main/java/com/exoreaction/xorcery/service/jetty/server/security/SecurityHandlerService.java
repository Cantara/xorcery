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

@Service(name = "server.security")
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
        configuration.getString("server.security.method").ifPresent(securityHandler::setAuthMethod);
    }

    @Override
    @Singleton
    @Named("server.security")
    public SecurityHandler provide() {
        return securityHandler;
    }

    @Override
    public void dispose(SecurityHandler instance) {

    }
}
