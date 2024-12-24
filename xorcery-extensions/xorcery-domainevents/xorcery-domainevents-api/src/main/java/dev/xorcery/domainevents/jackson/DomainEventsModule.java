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
package dev.xorcery.domainevents.jackson;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import dev.xorcery.domainevents.api.JsonDomainEvent;
import dev.xorcery.domainevents.api.JsonSystemEvent;

import java.io.IOException;

public class DomainEventsModule
        extends SimpleModule {
    public DomainEventsModule() {
        super("xorcery-domainevents-api");
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        if (context.getOwner() instanceof ObjectMapper objectMapper) {
            objectMapper.addHandler(new DeserializationProblemHandler() {
                @Override
                public JavaType handleUnknownTypeId(DeserializationContext ctxt, JavaType baseType, String subTypeId, TypeIdResolver idResolver, String failureMsg) throws IOException {
                    return switch (subTypeId) {
                        case "com.exoreaction.xorcery.domainevents.api.JsonDomainEvent", "JsonDomainEvent" ->
                                TypeFactory.defaultInstance().constructType(JsonDomainEvent.class);
                        case "JsonSystemEvent" -> TypeFactory.defaultInstance().constructType(JsonSystemEvent.class);
                        default -> null;
                    };
                }
            });
        }
    }
}
