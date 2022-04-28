package com.exoreaction.reactiveservices.service.helpers;

import com.exoreaction.reactiveservices.jsonapi.Attributes;
import com.exoreaction.reactiveservices.jsonapi.Links;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.server.Server;

public class ServiceResourceObjectBuilder {
    private final Server server;
    private final ResourceObject.Builder builder;
    private final Links.Builder links = new Links.Builder();
    private final Attributes.Builder attributes = new Attributes.Builder();

    public ServiceResourceObjectBuilder(Server server, String serviceType) {
        this.server = server;
        this.builder = new ResourceObject.Builder(serviceType, server.getServerId());
    }

    public ServiceResourceObjectBuilder version(String v) {
        attributes.attribute("version", v);
        return this;
    }

    public ServiceResourceObjectBuilder api(String rel, String path) {
        links.link(rel, server.getBaseUriBuilder().path(path));

        return this;
    }

    public ServiceResourceObjectBuilder websocket(String rel, String path) {
        links.link(rel, server.getBaseUriBuilder().scheme("ws").path(path));
        return this;
    }

    public ServiceResourceObjectBuilder websocket(String rel, String path, String queryParameters) {
        links.link(rel, server.getBaseUriBuilder().scheme("ws").path(path).replaceQuery(queryParameters).toTemplate());
        return this;
    }

    public ResourceObject build() {
        return builder
                .attributes(attributes.build())
                .links(links.build()).build();
    }
}
