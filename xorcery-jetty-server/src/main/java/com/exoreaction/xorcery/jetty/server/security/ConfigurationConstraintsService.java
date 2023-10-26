package com.exoreaction.xorcery.jetty.server.security;

import com.exoreaction.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.HashMap;
import java.util.Map;

@Service(name = "jetty.server.security")
@RunLevel(16)
public class ConfigurationConstraintsService {

    @Inject
    public ConfigurationConstraintsService(Configuration configuration,
                                           ConstraintSecurityHandler constraintSecurityHandler,
                                           Logger logger) {
        JettySecurityConfiguration jettySecurityConfiguration = JettySecurityConfiguration.get(configuration);

        Map<String, Constraint> constraints = new HashMap<>();
        jettySecurityConfiguration.getConstraints().forEach(c -> constraints.put(c.getName(),
                ConstraintSecurityHandler.createConstraint(c.getName(), true, c.getRoles().toArray(new String[0]), Constraint.DC_NONE)));

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
                constraintSecurityHandler.addConstraintMapping(mapping);
                logger.debug("Path '{}' mapped to security constraint '{}'", m.getPath(), m.getConstraint());
            }, () ->
            {
                logger.debug("Skipped constraint mapping for path '{}', constraint name was not set", m.getPath());
            });
        });
    }
}
