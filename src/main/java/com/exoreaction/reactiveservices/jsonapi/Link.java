package com.exoreaction.reactiveservices.jsonapi;

import com.exoreaction.reactiveservices.json.JsonElement;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.glassfish.jersey.uri.UriTemplate;

import java.net.URI;
import java.util.Optional;

/**
 * @author rickardoberg
 * @since 28/11/2018
 */

public class Link
    implements JsonElement
{
    private final String rel;
    private final JsonValue value;

    public Link( String rel, JsonValue value )
    {
        this.rel = rel;
        this.value = value;
    }

    @Override
    public JsonValue json()
    {
        return value;
    }

    public String getRel()
    {
        return rel;
    }

    public String getHref()
    {
        if ( value.getValueType().equals( JsonValue.ValueType.STRING ) )
        {
            return ((JsonString) value).getString();
        }
        else
        {
            return ((JsonObject) value).getString( "href" );
        }
    }

    public URI getHrefAsUri()
    {
        return URI.create(getHref());
    }

    public URI getHrefAsUriBuilder()
    {
        return URI.create(getHref());
    }

    public UriTemplate getHrefAsUriTemplate()
    {
        return new UriTemplate(getHref());
    }

    public boolean isTemplate()
    {
        return !new UriTemplate(getHref()).getTemplateVariables().isEmpty();
    }

    public Optional<Meta> getMeta()
    {
        if ( value.getValueType().equals( JsonValue.ValueType.STRING ) )
        {
            return Optional.empty();
        }
        else
        {
            return Optional.ofNullable( ((JsonObject) value).getJsonObject( "meta" ) ).map( Meta::new );
        }
    }
}
