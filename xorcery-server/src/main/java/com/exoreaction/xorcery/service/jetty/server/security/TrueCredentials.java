package com.exoreaction.xorcery.service.jetty.server.security;

import org.eclipse.jetty.util.security.Credential;

public class TrueCredentials
    extends Credential
{
    @Override
    public boolean check(Object credentials) {
        return true;
    }
}
