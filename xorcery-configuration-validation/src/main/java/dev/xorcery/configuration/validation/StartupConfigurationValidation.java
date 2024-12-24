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
