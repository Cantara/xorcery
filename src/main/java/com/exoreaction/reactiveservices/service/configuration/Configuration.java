package com.exoreaction.reactiveservices.service.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author rickardoberg
 * @since 20/04/2022
 */
public class Configuration
{
    public static class Builder
    {
        private JsonObjectBuilder builder;

        public Builder()
        {
            this(Json.createObjectBuilder());
        }

        public Builder( JsonObjectBuilder builder )
        {
            this.builder = builder;
        }

        public Builder addYaml( InputStream yaml) throws IOException
        {
            JsonObject current = builder.build();
            JsonObject yamlJson = new ObjectMapper( new YAMLFactory() ).readerForUpdating( current ).readValue( yaml, JsonObject.class );
            builder = Json.createObjectBuilder(yamlJson);
            return this;
        }

        public Configuration build()
        {
            return new Configuration( builder.build() );
        }
    }

    private final JsonObject config;

    private Configuration( JsonObject config )
    {
        this.config = config;
    }

    public String getString( String name, String defaultValue )
    {
        return config.getString( name, defaultValue );
    }

    public int getInt( String name, int defaultValue )
    {
        return config.getInt( name, defaultValue );
    }

    public boolean getBoolean( String name, boolean defaultValue )
    {
        return config.getBoolean( name, defaultValue );
    }

    public Configuration getConfiguration( String name )
    {
        return new Configuration( config.getJsonObject( name ) );
    }

    public Builder asBuilder()
    {
        return new Builder(Json.createObjectBuilder(config));
    }
}
