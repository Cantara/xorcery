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
package com.exoreaction.xorcery.status.spi;

import com.exoreaction.xorcery.jsonapi.Attributes;
import com.exoreaction.xorcery.jsonapi.Links;
import com.exoreaction.xorcery.jsonapi.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.ResourceObject;
import org.jvnet.hk2.annotations.Contract;

import static com.exoreaction.xorcery.jsonapi.JsonApiRels.describedby;

@Contract
public interface StatusProvider {
    String getId();

    default ResourceDocument getResourceDocument(String include) {
        return new ResourceDocument.Builder()
                .links(new Links.Builder()
                        .link(describedby, ".schema"))
                .data(new ResourceObject.Builder("status", getId())
                        .links(new Links.Builder()
                                .link("self", "/api/status/"+getId()+"?include={include}")
                                .build())
                        .attributes(new Attributes.Builder()
                                .with(attrs -> addAttributes(attrs, include)))
                        .build()
                )
                .build();
    }

    default void addAttributes(Attributes.Builder attrs, String include)
    {
    }
}
