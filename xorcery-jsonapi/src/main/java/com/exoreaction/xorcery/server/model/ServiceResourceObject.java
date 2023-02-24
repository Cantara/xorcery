package com.exoreaction.xorcery.server.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.Links;
import com.exoreaction.xorcery.jsonapi.model.Meta;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public record ServiceResourceObject(ResourceObject resourceObject) {

    public record Builder(ResourceObject.Builder builder, Links.Builder links,
                          Attributes.Builder attributes, URI baseServerUri)
            implements With<Builder> {

        public Builder(StandardConfiguration configuration, String serviceType) {
            this(new ResourceObject.Builder(serviceType, configuration.getId()), new Links.Builder(), new Attributes.Builder(), configuration.getServerUri());
            attributes.attribute("environment", configuration.getEnvironment());
            attributes.attribute("tag", configuration.getTag());
            builder.meta(new Meta.Builder()
                    .meta("timestamp", System.currentTimeMillis())
                    .build());
        }

        public Builder version(String v) {
            attributes.attribute("version", v);
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
