package com.exoreaction.xorcery.server.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.jsonapi.model.*;
import jakarta.ws.rs.core.UriBuilder;

import java.net.URI;
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
            links.link(rel, UriBuilder.fromUri(baseServerUri).path(path));

            return this;
        }

        public Builder websocket(String rel, String path) {
            links.link(rel, UriBuilder.fromUri(baseServerUri)
                    .scheme(baseServerUri.getScheme().equals("https") ? "wss" : "ws")
                    .path(path));
            return this;
        }

        public Builder publisher(String streamName) {
            links.link(streamName+" publisher", UriBuilder.fromUri(baseServerUri)
                    .scheme(baseServerUri.getScheme().equals("https") ? "wss" : "ws")
                    .path("streams/publishers/"+streamName));
            return this;
        }

        public Builder subscriber(String streamName) {
            links.link(streamName+" subscriber", UriBuilder.fromUri(baseServerUri)
                    .scheme(baseServerUri.getScheme().equals("https") ? "wss" : "ws")
                    .path("streams/subscribers/"+streamName));
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
