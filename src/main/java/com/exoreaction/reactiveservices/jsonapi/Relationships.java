package com.exoreaction.reactiveservices.jsonapi;

import com.exoreaction.reactiveservices.json.AbstractJsonElement;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;

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

public class Relationships
    extends AbstractJsonElement
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

    public Relationships( JsonStructure json )
    {
        super( json );
    }

    public List<Relationship> getRelationshipList()
    {
        return object().values().stream().map( value -> new Relationship( (JsonStructure) value ) ).collect( toList() );
    }

    public Map<String,Relationship> getRelationships()
    {
        return object()
            .entrySet()
            .stream()
            .collect( toMap( Map.Entry::getKey, entry -> new Relationship( (JsonStructure) entry.getValue() ) ) );
    }

    public Optional<Relationship> getRelationship( String name )
    {
        return Optional.ofNullable( object().get( name ) ).map( v -> new Relationship( (JsonStructure) v ) );
    }

    public Optional<Relationship> getRelationship( Enum<?> name )
    {
        return Optional.ofNullable( object().get( name.name() ) ).map( v -> new Relationship( (JsonStructure) v ) );
    }
}
