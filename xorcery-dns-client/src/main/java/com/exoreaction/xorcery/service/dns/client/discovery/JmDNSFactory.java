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
package com.exoreaction.xorcery.service.dns.client.discovery;

import com.exoreaction.xorcery.configuration.model.Configuration;
import org.apache.logging.log4j.LogManager;

import javax.jmdns.JmDNS;
import java.io.IOException;

public class JmDNSFactory {

    private final JmDNS jmDNS;

    public JmDNSFactory(Configuration configuration) throws IOException {
        jmDNS = JmDNS.create(null, configuration.getString("host").orElse(null));
    }

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
