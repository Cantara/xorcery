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
package com.exoreaction.xorcery.server.api;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.jsonapi.Attributes;
import com.exoreaction.xorcery.jsonapi.Link;
import com.exoreaction.xorcery.jsonapi.Links;
import com.exoreaction.xorcery.jsonapi.Meta;
import com.exoreaction.xorcery.jsonapi.ResourceObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public record ServiceResourceObject(ResourceObject resourceObject) {

    public record Builder(ResourceObject.Builder builder, Links.Builder links,
                          Attributes.Builder attributes, URI baseServerUri)
            implements With<Builder> {

        public Builder(InstanceConfiguration configuration, String serviceType) {
            this(new ResourceObject.Builder("service", serviceType), new Links.Builder(), new Attributes.Builder(), configuration.getURI());
        }

        public Builder version(String v) {
            if (v != null)
            {
                attributes.attribute("version", v);
            }
            return this;
        }

        public Builder attribute(String name, Object value) {
            attributes.attribute(name, value);
            return this;
        }

        public Builder api(String rel, String path) {
            URI resolvedUri;
            try {
                resolvedUri = new URI(
                        baseServerUri.getScheme(),
                        baseServerUri.getUserInfo(),
                        baseServerUri.getHost(),
                        baseServerUri.getPort(),
                        path.startsWith("/") ? path : "/" + path,
                        baseServerUri.getQuery(),
                        baseServerUri.getFragment()
                );
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            links.link(rel, resolvedUri);

            return this;
        }

        public Builder websocket(String rel, String path) {
            URI resolvedUri;
            try {
                resolvedUri = new URI(
                        baseServerUri.getScheme().equals("https") ? "wss" : "ws",
                        baseServerUri.getUserInfo(),
                        baseServerUri.getHost(),
                        baseServerUri.getPort(),
                        path.startsWith("/") ? path : "/" + path,
                        baseServerUri.getQuery(),
                        baseServerUri.getFragment()
                );
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            links.link(rel, resolvedUri);
            return this;
        }

        public Builder publisher(String streamName) {
            URI resolvedUri;
            try {
                resolvedUri = new URI(
                        baseServerUri.getScheme().equals("https") ? "wss" : "ws",
                        baseServerUri.getUserInfo(),
                        baseServerUri.getHost(),
                        baseServerUri.getPort(),
                        "/streams/publishers/"+streamName,
                        baseServerUri.getQuery(),
                        baseServerUri.getFragment()
                );
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            links.link(streamName+" publisher", resolvedUri);
            return this;
        }

        public Builder subscriber(String streamName) {
            URI resolvedUri;
            try {
                resolvedUri = new URI(
                        baseServerUri.getScheme().equals("https") ? "wss" : "ws",
                        baseServerUri.getUserInfo(),
                        baseServerUri.getHost(),
                        baseServerUri.getPort(),
                        "/streams/subscribers/"+streamName,
                        baseServerUri.getQuery(),
                        baseServerUri.getFragment()
                );
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            links.link(streamName+" subscriber", resolvedUri);
            return this;
        }

        public ServiceResourceObject build() {
            return new ServiceResourceObject(builder
                    .attributes(attributes.build())
                    .links(links.build()).build());
        }
    }

    public ServiceIdentifier getServiceIdentifier() {
        return new ServiceIdentifier(resourceObject.getType(), resourceObject.getId());
    }

    public String getServerId() {
        return resourceObject.getId();
    }

    public String getVersion() {
        return resourceObject().getAttributes().getString("version").orElse(null);
    }

    public ServiceAttributes getAttributes() {
        return new ServiceAttributes(resourceObject.getAttributes());
    }

    public Optional<Link> getLinkByRel(String rel) {
        return resourceObject().getLinks().getByRel(rel);
    }
}
