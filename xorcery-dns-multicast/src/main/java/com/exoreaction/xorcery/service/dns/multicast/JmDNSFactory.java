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
package com.exoreaction.xorcery.service.dns.multicast;

import com.exoreaction.xorcery.configuration.model.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

import javax.jmdns.JmDNS;
import java.io.IOException;

@Service
public class JmDNSFactory
        implements Factory<JmDNS> {

    private final JmDNS jmDNS;

    @Inject
    public JmDNSFactory(Configuration configuration) throws IOException {
        jmDNS = JmDNS.create(null, configuration.getString("host").orElse(null));
    }

    @Override
    @Singleton
    public JmDNS provide() {
        return jmDNS;
    }

    public void dispose(JmDNS instance) {
        try {
            jmDNS.close();
        } catch (IOException e) {
            LogManager.getLogger(getClass()).error("Exception closing JmDNS", e);
        }
    }
}
