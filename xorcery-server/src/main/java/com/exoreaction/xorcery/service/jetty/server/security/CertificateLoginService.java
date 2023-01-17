package com.exoreaction.xorcery.service.jetty.server.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.RolePrincipal;
import org.eclipse.jetty.security.UserPrincipal;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.util.Collections;
import java.util.List;

@Service(name="server.security.certificate")
@ContractsProvided(LoginService.class)
public class CertificateLoginService
        extends AbstractLoginService {
    private final Logger logger = LogManager.getLogger(getClass());

    @Override
    public String getName() {
        return "clientcert";
    }

    @Override
    protected List<RolePrincipal> loadRoleInfo(UserPrincipal user) {
        return Collections.emptyList();
    }

    @Override
    protected UserPrincipal loadUserInfo(String username) {
        logger.debug("Service " + username + " logged in using a client certificate");

        return new UserPrincipal(username, new TrueCredentials());
    }
}
