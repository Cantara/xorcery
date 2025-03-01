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
package dev.xorcery.dns.client.api;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Lookup API to convert a URI with names (hostnames, SRV names, etc.) into a list of URIs with IPs, schemes and other information.
 */
public interface DnsLookup {
    CompletableFuture<List<URI>> resolve(URI uri);
}
