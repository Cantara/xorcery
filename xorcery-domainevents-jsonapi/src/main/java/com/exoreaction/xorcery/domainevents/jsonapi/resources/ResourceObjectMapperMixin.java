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
package com.exoreaction.xorcery.domainevents.jsonapi.resources;

import com.exoreaction.xorcery.domainevents.helpers.context.DomainContext;
import com.exoreaction.xorcery.domainevents.helpers.entity.Command;
import com.exoreaction.xorcery.domainevents.helpers.model.EntityModel;
import com.exoreaction.xorcery.domainevents.helpers.model.Model;
import com.exoreaction.xorcery.jsonapi.*;
import com.exoreaction.xorcery.jsonapi.server.resources.IncludesMixin;
import com.exoreaction.xorcery.jsonapi.server.resources.RelationshipsMixin;
import jakarta.ws.rs.core.UriBuilder;

import java.net.URI;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.exoreaction.xorcery.jsonapi.JsonApiRels.related;
import static com.exoreaction.xorcery.jsonapi.JsonApiRels.self;

public interface ResourceObjectMapperMixin
    extends IncludesMixin, RelationshipsMixin
{
    default <T extends Model> ResourceObjectMapperBuilder<T> resourceObjectMapper(Function<T, ResourceObject.Builder> mapper, Included.Builder included)
    {
        return new ResourceObjectMapperBuilder<>(mapper, included);
    }

    // Attributes
    default <T extends Model> BiConsumer<T, ResourceObjectMapper> modelAttributes(Predicate<String> fieldSelection)
    {
        return (model, mapper) -> model.object().fields().forEachRemaining(entry ->
        {
            if (fieldSelection.test(entry.getKey()))
                mapper.attributes().attribute(entry.getKey(), entry.getValue());
        });
    }

    // Relationships
    default <T extends Model> BiConsumer<T, ResourceObjectMapper> modelRelationship(Enum<?> relationshipName,
                                                                                    Predicate<String> fieldSelection,
                                                                                    String includePrefix,
                                                                                    Function<T, URI> relationshipRelatedMapper,
                                                                                    IncludedResourceObjects<T> includedResourceObjects)
    {
        String modelRelationshipPath = includePrefix+relationshipName;
        if (shouldInclude(modelRelationshipPath))
        {
            return (model, mapper) ->
            {
                ResourceObjects resources = includedResourceObjects.resourceObjects(model, mapper, includePrefix);

                Links.Builder links = new Links.Builder();
                links.link(related, relationshipRelatedMapper.apply(model));

                Relationship relationship = new Relationship.Builder()
                        .resourceIdentifiers(resources)
                        .links(links.build())
                        .build();

                mapper.relationships().relationship(relationshipName, relationship);
                mapper.included().include(resources);
            };
        } else if (fieldSelection.test(relationshipName.name()))
        {
            return (model, mapper) ->
            {
                Links.Builder links = new Links.Builder();
                links.link(related, relationshipRelatedMapper.apply(model));

                Relationship relationship = new Relationship.Builder()
                        .links(links.build())
                        .build();

                mapper.relationships().relationship(relationshipName, relationship);
            };
        } else
        {
            return (model, mapper) -> {};
        }
    }

    interface IncludedResourceObjects<T extends Model>
    {
        ResourceObjects resourceObjects(T model, ResourceObjectMapper mapper, String relationshipIncludePrefix);
    }

    // Links
    default <T extends Model> BiConsumer<T, ResourceObjectMapper> selfLink(Function<T, URI> linkMapper)
    {
        return (model, mapper) -> mapper.links().link(self, linkMapper.apply(model));
    }

    default <T extends EntityModel> BiConsumer<T, ResourceObjectMapper> selfLink(Class<?> resourceClass)
    {
        UriBuilder uriBuilder = getUriBuilderFor(resourceClass);
        return selfLink(model -> uriBuilder.build(model.getId()));
    }

    default <T extends Model> BiConsumer<T, ResourceObjectMapper> commandLinks(Function<T, UriBuilder> baseUriMapper, Function<T, DomainContext> contextMapper) {
        return (model, mapper) ->
        {
            DomainContext context = contextMapper.apply(model);
            UriBuilder uriBuilder = baseUriMapper.apply(model);
            Links.Builder links = mapper.links();
            for (Command command : context.commands()) {
                if (!Command.isDelete(command.getClass())) {
                    String commandName = Command.getName(command);
                    links.link(commandName, uriBuilder.replaceQueryParam("rel", commandName).toTemplate());
                }
            }
        };
    }

    default <T extends EntityModel> BiConsumer<T, ResourceObjectMapper> commandLinks(Class<?> resourceClass, Function<T, DomainContext> contextMapper) {
        UriBuilder uriBuilder = getUriBuilderFor(resourceClass);
        return commandLinks(model -> uriBuilder.clone().resolveTemplate("id", model.getId()), contextMapper);
    }

    // Builders and helpers
    class ResourceObjectMapperBuilder<T extends Model>
    {
        Function<T, ResourceObject.Builder> resourceObjectBuilderMapper;
        private Included.Builder included;
        BiConsumer<T, ResourceObjectMapper> consumers;

        public ResourceObjectMapperBuilder(Function<T, ResourceObject.Builder> mapper, Included.Builder included) {
            this.resourceObjectBuilderMapper = mapper;
            this.included = included;
        }

        @SafeVarargs
        public final ResourceObjectMapperBuilder<T> with(BiConsumer<T, ResourceObjectMapper>... consumers)
        {
            for (BiConsumer<T, ResourceObjectMapper> consumer : consumers) {
                if (this.consumers == null)
                {
                    this.consumers = consumer;
                } else
                {
                    this.consumers = this.consumers.andThen(consumer);
                }
            }
            return this;
        }

        public Function<T, ResourceObject> build()
        {
            return model ->
            {
                ResourceObjectMapper resourceObjectMapper = new ResourceObjectMapper(resourceObjectBuilderMapper.apply(model), included);
                consumers.accept(model, resourceObjectMapper);
                return resourceObjectMapper.build();
            };
        }
    }

    record ResourceObjectMapper(ResourceObject.Builder resource, Attributes.Builder attributes, Links.Builder links, Relationships.Builder relationships,
                                Meta.Builder meta, Included.Builder included)
    {
        public ResourceObjectMapper(ResourceObject.Builder resource, Included.Builder included) {
            this(resource, new Attributes.Builder(), new Links.Builder(), new Relationships.Builder(), new Meta.Builder(), included);
        }

        public ResourceObject build()
        {
            if (!attributes.builder().isEmpty())
            {
                resource.attributes(attributes.build());
            }

            if (!links.builder().isEmpty())
            {
                resource.links(links.build());
            }

            if (!relationships.builder().isEmpty())
            {
                resource.relationships(relationships.build());
            }

            if (!meta.builder().isEmpty())
            {
                resource.meta(meta.build());
            }

            return resource.build();
        }
    }

}
