package com.exoreaction.xorcery.service.jetty.server.security;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletContext;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.DefaultAuthenticatorFactory;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Server;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service
@ContractsProvided(Authenticator.Factory.class)
public class ProviderAuthenticatorFactory
    extends DefaultAuthenticatorFactory
{
    private IterableProvider<Authenticator> authenticators;

    @Inject
    public ProviderAuthenticatorFactory(IterableProvider<Authenticator> authenticators) {
        this.authenticators = authenticators;
    }

    @Override
    public Authenticator getAuthenticator(Server server, ServletContext context, Authenticator.AuthConfiguration configuration, IdentityService identityService, LoginService loginService) {

        String authMethod = configuration.getAuthMethod();

        Authenticator authenticator = authenticators.named(authMethod).get();
        if (authenticator != null)
            return authenticator;

        return super.getAuthenticator(server, context, configuration, identityService, loginService);
    }
}
