package com.exoreaction.reactiveservices.jsonapi;

import com.exoreaction.reactiveservices.json.AbstractJsonElement;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.ws.rs.core.UriBuilder;

import java.net.URI;
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

public class Links
    extends AbstractJsonElement
{
    public static class Builder
    {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        public Builder link( Consumer<Builder> consumer )
        {
            consumer.accept( this );
            return this;
        }

        public Builder link( String rel, String href )
        {
            builder.add( rel, href );
            return this;
        }

        public Builder link( String rel, URI href )
        {
            return link( rel, href.toASCIIString() );
        }

        public Builder link( Enum<?> rel, URI href )
        {
            return link( rel.name(), href );
        }

        public Builder link( String rel, UriBuilder href )
        {
            return link( rel, href.build() );
        }

        public Builder link( String rel, String href, Meta meta )
        {
            builder.add( rel, Json.createObjectBuilder()
                                  .add( "href", href )
                                  .add( "meta", meta.json() )
                                  .build() );
            return this;
        }

        public Builder link( String rel, URI href, Meta meta )
        {
            return link( rel, href.toASCIIString(), meta );
        }

        public Builder link( String rel, UriBuilder href, Meta meta )
        {
            return link( rel, href.build().toASCIIString(), meta );
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

        public Links build()
        {
            return new Links( builder.build() );
        }
    }

    public Links( JsonStructure json )
    {
        super( json );
    }

    public Optional<Link> getSelf()
    {
        return getRel( "self" );
    }

    public Optional<Link> getNext()
    {
        return getRel( "next" );
    }

    public Optional<Link> getRelated()
    {
        return getRel( "related" );
    }

    public Optional<Link> getDescribedBy()
    {
        return getRel( "describedBy" );
    }

    public Optional<Link> getRel( String name )
    {
        return Optional.ofNullable( object().get( name ) ).map( v -> new Link( name, v ) );
    }

    public List<Link> getLinks()
    {
        return object()
            .entrySet()
            .stream()
            .map( entry -> new Link( entry.getKey(), entry.getValue() ) )
            .collect( toList() );
    }

    public Map<String,Link> getLinkMapNoSelf()
    {
        return object()
            .entrySet()
            .stream()
            .filter( ( entry ) -> !entry.getKey().equals( "self" ) )
            .collect( toMap( Map.Entry::getKey, entry -> new Link( entry.getKey(), (JsonValue) entry.getValue() ) ) );
    }
}
