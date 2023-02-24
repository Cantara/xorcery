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
import java.util.Objects;
import java.util.Optional;

public final class ServiceResourceObject {
    private final ResourceObject resourceObject;

    public ServiceResourceObject(ResourceObject resourceObject) {
        this.resourceObject = resourceObject;
    }

    public static final class Builder
            implements With<Builder> {
        private final ResourceObject.Builder builder;
        private final Links.Builder links;
        private final Attributes.Builder attributes;
        private final URI baseServerUri;

        public Builder(ResourceObject.Builder builder, Links.Builder links,
                       Attributes.Builder attributes, URI baseServerUri) {
            this.builder = builder;
            this.links = links;
            this.attributes = attributes;
            this.baseServerUri = baseServerUri;
        }

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
                        "/streams/publishers/" + streamName,
                        baseServerUri.getQuery(),
                        baseServerUri.getFragment()
                );
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            links.link(streamName + " publisher", resolvedUri);
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
                        "/streams/subscribers/" + streamName,
                        baseServerUri.getQuery(),
                        baseServerUri.getFragment()
                );
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            links.link(streamName + " subscriber", resolvedUri);
            return this;
        }

        public ServiceResourceObject build() {
            return new ServiceResourceObject(builder
                    .attributes(attributes.build())
                    .links(links.build()).build());
        }

        public ResourceObject.Builder builder() {
            return builder;
        }

        public Links.Builder links() {
            return links;
        }

        public Attributes.Builder attributes() {
            return attributes;
        }

        public URI baseServerUri() {
            return baseServerUri;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Builder) obj;
            return Objects.equals(this.builder, that.builder) &&
                   Objects.equals(this.links, that.links) &&
                   Objects.equals(this.attributes, that.attributes) &&
                   Objects.equals(this.baseServerUri, that.baseServerUri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(builder, links, attributes, baseServerUri);
        }

        @Override
        public String toString() {
            return "Builder[" +
                   "builder=" + builder + ", " +
                   "links=" + links + ", " +
                   "attributes=" + attributes + ", " +
                   "baseServerUri=" + baseServerUri + ']';
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

    public ResourceObject resourceObject() {
        return resourceObject;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ServiceResourceObject) obj;
        return Objects.equals(this.resourceObject, that.resourceObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceObject);
    }

    @Override
    public String toString() {
        return "ServiceResourceObject[" +
               "resourceObject=" + resourceObject + ']';
    }

}
