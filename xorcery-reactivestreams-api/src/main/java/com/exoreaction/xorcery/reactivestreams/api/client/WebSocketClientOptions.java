package com.exoreaction.xorcery.reactivestreams.api.client;

import java.net.HttpCookie;
import java.util.*;

public record WebSocketClientOptions(Map<String, List<String>> headers, List<String> extensions,
                                     List<HttpCookie> cookies) {

    private static final WebSocketClientOptions INSTANCE = new Builder().build();

    public static Builder builder() {
        return new Builder();
    }

    public static WebSocketClientOptions instance() {
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

        public Builder header(String name, String value) {
            headers.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
            return this;
        }

        public Builder cookie(HttpCookie cookie) {
            cookies.add(cookie);
            return this;
        }

        public WebSocketClientOptions build() {
            return new WebSocketClientOptions(headers, extensions, cookies);
        }
    }
}
