package dev.xorcery.configuration.validation;

import com.networknt.schema.ValidationMessage;
import dev.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.Set;

@Service(name="configuration.validation")
@RunLevel(0)
public class StartupConfigurationValidation {

    @Inject
    public StartupConfigurationValidation(Configuration configuration, Logger logger) {
        Set< ValidationMessage> errors = new ConfigurationValidator().validate(configuration);
        if (!errors.isEmpty())
        {
            for (ValidationMessage error : errors) {
                logger.error(error.toString());
            }
            throw new IllegalStateException("Configuration validation failed");
        }
    }
}
