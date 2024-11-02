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
package dev.xorcery.configuration.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import dev.xorcery.configuration.spi.ConfigurationProvider;
import dev.xorcery.net.Sockets;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CalculatedConfigurationProvider
        implements ConfigurationProvider {

    private Map<String, NumericNode> dynamicPorts = new HashMap<>();

    @Override
    public String getNamespace() {
        return "CALCULATED";
    }

    @Override
    public JsonNode getJson(String name) {

        try {
            Method method = getClass().getMethod(name);

            return (JsonNode) method.invoke(this);
        } catch (NoSuchMethodException e) {
            return MissingNode.getInstance();
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public JsonNode hostName()
    {
        try {
            return JsonNodeFactory.instance.textNode(InetAddress.getLocalHost().getHostName().toLowerCase());
        } catch (UnknownHostException e) {
            return MissingNode.getInstance();
        }
    }

    public JsonNode ip() {

        try {
            return JsonNodeFactory.instance.textNode(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            return MissingNode.getInstance();
        }
    }

    public ArrayNode ipv4Addresses() {
        ArrayNode addresses = JsonNodeFactory.instance.arrayNode();
        try {
            for (NetworkInterface netint : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress inetAddress : Collections.list(netint.getInetAddresses())) {
                    if (inetAddress instanceof Inet4Address)
                        addresses.add(JsonNodeFactory.instance.textNode(inetAddress.getHostAddress()));
                }
            }
            return addresses;
        } catch (SocketException e) {
            return addresses;
        }
    }

    public ArrayNode ipv6Addresses() {
        ArrayNode addresses = JsonNodeFactory.instance.arrayNode();
        try {
            for (NetworkInterface netint : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress inetAddress : Collections.list(netint.getInetAddresses())) {
                    if (inetAddress instanceof Inet6Address)
                        addresses.add(JsonNodeFactory.instance.textNode(inetAddress.getHostAddress()));
                }
            }
            return addresses;
        } catch (SocketException e) {
            return addresses;
        }
    }

    public ContainerNode<?> dynamicPorts()
    {
        return new ObjectNode(JsonNodeFactory.instance)
        {
            @Override
            public JsonNode get(String name) {
                return dynamicPorts.computeIfAbsent(name, n -> JsonNodeFactory.instance.numberNode(Sockets.nextFreePort()));
            }
        };
    }

    @Override
    public String toString() {
        return "Calculated configuration";
    }
}
