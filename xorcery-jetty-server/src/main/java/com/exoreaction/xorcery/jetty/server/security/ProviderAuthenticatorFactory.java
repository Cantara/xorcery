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
package com.exoreaction.xorcery.jetty.server.security;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletContext;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.DefaultAuthenticatorFactory;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Context;
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
    public Authenticator getAuthenticator(Server server, Context context, Authenticator.Configuration configuration) {

        String authType = configuration.getAuthenticationType();
        if (authType == null)
            return null;
        Authenticator authenticator = authenticators.named(authType).get();
        if (authenticator != null)
            return authenticator;
        authenticator = authenticators.named("jetty.server.security."+authType).get();
        if (authenticator != null)
            return authenticator;

        return super.getAuthenticator(server, context, configuration);
    }
}
