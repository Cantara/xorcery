package com.exoreaction.xorcery.server.model;

import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.Links;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.server.Server;
import com.exoreaction.xorcery.service.reactivestreams.api.ServiceIdentifier;

import java.util.Optional;

public record ServiceResourceObject(ResourceObject resourceObject) {

    public record Builder(Server server, ResourceObject.Builder builder, Links.Builder links,
                          Attributes.Builder attributes) {

        public Builder(Server server, String serviceType) {
            this(server, new ResourceObject.Builder(serviceType, server.getServerId()), new Links.Builder(), new Attributes.Builder());
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
            links.link(rel, server.getBaseUriBuilder().path(path));

            return this;
        }

        public Builder websocket(String rel, String path) {
            links.link(rel, server.getBaseUriBuilder()
                    .scheme(server.getBaseUri().getScheme().equals("https") ? "wss" : "ws")
                    .path(path));
            return this;
        }

        public ServiceResourceObject build() {
            return new ServiceResourceObject(builder
                    .attributes(attributes.build())
                    .links(links.build()).build());
        }
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
