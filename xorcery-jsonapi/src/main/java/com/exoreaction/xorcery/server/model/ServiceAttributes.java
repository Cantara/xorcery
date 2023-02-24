package com.exoreaction.xorcery.server.model;

import com.exoreaction.xorcery.jsonapi.model.Attributes;

import java.util.Objects;
import java.util.Optional;

public final class ServiceAttributes {
    private final Attributes attributes;

    public ServiceAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    public Optional<String> getVersion() {
        return attributes.getString("version");
    }

    public Attributes attributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ServiceAttributes) obj;
        return Objects.equals(this.attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes);
    }

    @Override
    public String toString() {
        return "ServiceAttributes[" +
               "attributes=" + attributes + ']';
    }


}
