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
package dev.xorcery.jgroups;

import dev.xorcery.configuration.Configuration;
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
