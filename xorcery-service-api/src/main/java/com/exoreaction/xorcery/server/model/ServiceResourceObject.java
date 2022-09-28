package com.exoreaction.xorcery.server.model;

import com.exoreaction.xorcery.configuration.StandardConfiguration;
import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.Links;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;

import java.net.URI;
import java.util.Optional;

public record ServiceResourceObject(ResourceObject resourceObject) {

    public record Builder(StandardConfiguration configuration, ResourceObject.Builder builder, Links.Builder links,
                          Attributes.Builder attributes) {

        public Builder(StandardConfiguration configuration, String serviceType) {
            this(configuration, new ResourceObject.Builder(serviceType, configuration.getId()), new Links.Builder(), new Attributes.Builder());
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
            links.link(rel, getServerUriBuilder(configuration.getServerUri()).path(path));

            return this;
        }

        public Builder websocket(String rel, String path) {
            links.link(rel, getServerUriBuilder(configuration.getServerUri())
                    .scheme(configuration.getServerUri().getScheme().equals("https") ? "wss" : "ws")
                    .path(path));
            return this;
        }

        public ServiceResourceObject build() {
            return new ServiceResourceObject(builder
                    .attributes(attributes.build())
                    .links(links.build()).build());
        }
    }

    // TODO Use common method instead, but avoid the UriBuilder class in the basic config library
    private static jakarta.ws.rs.core.UriBuilder getServerUriBuilder(URI uri) {
        return jakarta.ws.rs.core.UriBuilder.fromUri(uri);
    }

    public ServiceIdentifier serviceIdentifier() {
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
