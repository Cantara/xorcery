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
package com.exoreaction.xorcery.server.api;

import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Collection of ServiceResourceObjects for this server. Each service should add their own ServiceResourceObject instances
 * here on startup. This will get exposed by the API for the root of the JSON API so that clients can inspect what services are available.
 */
@Service
public class ServiceResourceObjects {
    private List<ServiceResourceObject> serviceResources = new CopyOnWriteArrayList<>();

    @Inject
    public ServiceResourceObjects() {
    }

    public void add(ServiceResourceObject serviceResourceObject) {
        serviceResources.add(serviceResourceObject);
    }

    public List<ServiceResourceObject> getServiceResources() {
        return serviceResources;
    }
}
