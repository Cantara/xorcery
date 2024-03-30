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
package com.exoreaction.xorcery.certificates;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.ServiceConfiguration;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.exoreaction.xorcery.configuration.Configuration.missing;

public record CertificatesConfiguration(Configuration context)
        implements ServiceConfiguration {

    public static CertificatesConfiguration get(Configuration configuration)
    {
        return new CertificatesConfiguration(configuration.getConfiguration("certificates"));
    }

    Optional<String> getURI() {
        return context().getString("uri");
    }

    String getCertificateStoreName() {
        return context().getString("keystore").orElse("ssl");
    }

    String getSubject() {
        return context().getString("subject").orElseThrow(missing("certificates.subject"));
    }

    boolean isRenewOnStartup() {
        return context().getBoolean("renewOnStartup").orElse(false);
    }

    String getAlias() {
        return context().getString("alias").orElseThrow(missing("certificates.alias"));
    }

    List<String> getIpAddresses() {
        return context().getListAs("ipAddresses", JsonNode::textValue).orElse(Collections.emptyList());
    }

    List<String> getDnsNames() {
        return context().getListAs("dnsNames", JsonNode::textValue).orElse(Collections.emptyList());
    }
}
