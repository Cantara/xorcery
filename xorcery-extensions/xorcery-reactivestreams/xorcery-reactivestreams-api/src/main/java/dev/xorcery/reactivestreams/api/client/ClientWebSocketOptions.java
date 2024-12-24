/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.reactivestreams.api.client;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
