package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public record Relationships(JsonObject json)
    implements JsonElement
{
    public static class Builder
    {
        private JsonObjectBuilder builder = Json.createObjectBuilder();

        public Builder relationship( Enum name, Relationship relationship )
        {
            return relationship( name.name(), relationship );
        }

        public Builder relationship( String name, Relationship relationship )
        {
            builder.add( name, relationship.json() );
            return this;
        }
        public Builder relationship( String name, Relationship.Builder relationship )
        {
            return relationship(name, relationship.build());
        }

        @SafeVarargs
        public final Builder with( Consumer<Builder>... consumers )
        {
            for ( Consumer<Builder> consumer : consumers )
            {
                consumer.accept( this );
            }
            return this;
        }

        public Relationships build()
        {
            return new Relationships( builder.build() );
        }
    }

    public List<Relationship> getRelationshipList()
    {
        return object().values().stream().map(JsonObject.class::cast).map( Relationship::new ).collect( toList() );
    }

    public Map<String,Relationship> getRelationships()
    {
        return object()
            .entrySet()
            .stream()
            .collect( toMap( Map.Entry::getKey, entry -> new Relationship( (JsonObject) entry.getValue() ) ) );
    }

    public Optional<Relationship> getRelationship( String name )
    {
        return Optional.ofNullable( object().get( name ) ).map( v -> new Relationship( (JsonObject) v ) );
    }

    public Optional<Relationship> getRelationship( Enum<?> name )
    {
        return getRelationship(name.name());
    }
}
