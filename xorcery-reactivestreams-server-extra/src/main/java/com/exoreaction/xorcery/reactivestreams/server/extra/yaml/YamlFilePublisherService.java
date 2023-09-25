package com.exoreaction.xorcery.reactivestreams.server.extra.yaml;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

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

            URI fileUri = yamlFilePublisherConfiguration.getFile();

            // Test it
            URL fileUriURL = fileUri.toURL();
            fileUriURL.openStream().close();

            // Publish it
            YamlFilePublisher yamlFilePublisher = new YamlFilePublisher(fileUriURL);
            server.publisher(yamlFilePublisherConfiguration.getStream(), c -> yamlFilePublisher, YamlFilePublisher.class);

            logger.info("Published stream '{}' backed by '{}'", yamlFilePublisherConfiguration.getStream(), fileUri.toASCIIString());
        }
    }
}
