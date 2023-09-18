package com.exoreaction.xorcery.jmxconnector;

import com.exoreaction.xorcery.secrets.Secrets;

import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXPrincipal;
import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public record ConfigJmxAuthenticator(List<JmxConnectorConfiguration.User> users,
                                     Secrets secrets) implements JMXAuthenticator {
    @Override
    public Subject authenticate(Object credentials) {

        if (credentials == null) throw new SecurityException("Incorrect username or password");
        String[] usernamePassword = (String[]) credentials;
        if (usernamePassword.length != 2) throw new SecurityException("Incorrect username or password");
        String username = usernamePassword[0];
        String password = usernamePassword[1];

        for (JmxConnectorConfiguration.User user : users) {

            if (username.equals(user.getUsername()) && password.equals(secrets.getSecretString(user.getPassword()))) {
                Set<? extends Principal> principals = Set.of(new JMXPrincipal(usernamePassword[0]));
                return new Subject(true, principals, Collections.emptySet(), Set.of(usernamePassword[1]));
            }
        }

        throw new SecurityException("Incorrect username or password");
    }
}
