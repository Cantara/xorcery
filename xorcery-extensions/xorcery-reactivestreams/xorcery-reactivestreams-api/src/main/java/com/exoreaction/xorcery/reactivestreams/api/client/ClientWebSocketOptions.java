package com.exoreaction.xorcery.reactivestreams.api.client;

import java.net.HttpCookie;
import java.util.*;

public record ClientWebSocketOptions(Map<String, List<String>> headers, List<String> extensions,
                                     List<HttpCookie> cookies) {

    private static final ClientWebSocketOptions INSTANCE = new Builder().build();

    public static Builder builder() {
        return new Builder();
    }

    public static ClientWebSocketOptions instance() {
        return INSTANCE;
    }

    public static class Builder {
        protected final Map<String, List<String>> headers = new HashMap<>();
        protected final List<String> extensions = new ArrayList<>();
        protected final List<HttpCookie> cookies = new ArrayList<>();

        private Builder() {
        }

        public Builder extension(String parameterizedName) {
            extensions.add(parameterizedName);
            return this;
        }

        public Builder extensionPerMessageDeflate()
        {
            return extension("permessage-deflate");
        }

        public Builder header(String name, String value) {
            headers.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
            return this;
        }

        public Builder cookie(HttpCookie cookie) {
            cookies.add(cookie);
            return this;
        }

        public ClientWebSocketOptions build() {
            return new ClientWebSocketOptions(headers, extensions, cookies);
        }
    }
}
