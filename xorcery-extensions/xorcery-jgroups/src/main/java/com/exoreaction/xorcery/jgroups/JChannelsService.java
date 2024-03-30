package com.exoreaction.xorcery.jgroups;

import com.exoreaction.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import org.jgroups.JChannel;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

@Service(name = "jgroups")
public class JChannelsService {

    private final JGroupsConfiguration jGroupsConfiguration;

    @Inject
    public JChannelsService(Configuration configuration) {
        jGroupsConfiguration = JGroupsConfiguration.get(configuration);
    }

    public JChannel newChannel(String name) {
        return jGroupsConfiguration.getChannels().get(name).getXMLConfig().map(url ->
        {
            try (InputStream config = url.openStream()) {
                return new JChannel(config);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).orElseThrow(() -> new IllegalArgumentException(String.format("No such channel '%s'", name)));
    }
}
