package com.exoreaction.reactiveservices.service.model;

import com.exoreaction.reactiveservices.jsonapi.model.Attributes;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.jsonapi.model.Links;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceReference;

import java.util.Optional;

public record ServiceResourceObject(ResourceObject resourceObject)
{
    public static class Builder {
        private final Server server;
        private final ResourceObject.Builder builder;
        private final Links.Builder links = new Links.Builder();
        private final Attributes.Builder attributes = new Attributes.Builder();

        public Builder(Server server, String serviceType) {
            this.server = server;
            this.builder = new ResourceObject.Builder(serviceType, server.getServerId());
        }

        public Builder version(String v) {
            attributes.attribute("version", v);
            return this;
        }

        public Builder api(String rel, String path) {
            links.link(rel, server.getBaseUriBuilder().path(path));

            return this;
        }

        public Builder websocket(String rel, String path) {
            links.link(rel, server.getBaseUriBuilder().scheme("ws").path(path));
            return this;
        }

        public Builder websocket(String rel, String path, String queryParameters) {
            links.link(rel, server.getBaseUriBuilder().scheme("ws").path(path).replaceQuery(queryParameters).toTemplate());
            return this;
        }

        public ServiceResourceObject build() {
            return new ServiceResourceObject(builder
                    .attributes(attributes.build())
                    .links(links.build()).build());
        }
    }

    public ServiceReference serviceReference()
    {
        return new ServiceReference(resourceObject.getType(), resourceObject.getId());
    }

    public String version()
    {
        return resourceObject().getAttributes().getString("version");
    }

    public Optional<Link> linkByRel(String rel)
    {
        return resourceObject().getLinks().getRel(rel);
    }
}
