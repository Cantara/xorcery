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
package com.exoreaction.xorcery.reactivestreams.server.extra.yaml;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

@Service(name="yamlfilepublisher")
@RunLevel(6)
public class YamlFilePublisherService {

    @Inject
    public YamlFilePublisherService(
            Configuration configuration,
            ReactiveStreamsServer server,
            Logger logger) throws IOException {
        YamlFilePublishersConfiguration cfg = new YamlFilePublishersConfiguration(configuration.getConfiguration("yamlfilepublisher"));

        for (YamlFilePublishersConfiguration.YamlFilePublisherConfiguration yamlFilePublisherConfiguration : cfg.getYamlFilePublishers()) {

            File file = new File(yamlFilePublisherConfiguration.getFile());

            // Test it
            if (!file.exists())
                throw new FileNotFoundException(file.getAbsolutePath());

            // Publish it
            YamlFilePublisher yamlFilePublisher = new YamlFilePublisher(file.toURI().toURL());
            server.publisher(yamlFilePublisherConfiguration.getStream(), c -> yamlFilePublisher, YamlFilePublisher.class);

            logger.info("Published stream '{}' backed by '{}'", yamlFilePublisherConfiguration.getStream(), file.toString());
        }
    }
}
