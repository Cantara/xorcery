package com.exoreaction.reactiveservices.service.mapdatabase;

import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.service.helpers.ServiceResourceObjectBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class MapDatabaseService
{
    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return "mapdatabase";
        }

        @Override
        protected void configure() {
            bind(new MapDatabaseService());
        }
    }

    private final Map<String, String> database = new ConcurrentHashMap<>();
    @Inject
    public MapDatabaseService() {
    }

    public void put(String name, String value)
    {
        database.put(name, value);
    }

    public String get(String name)
    {
        return database.get(name);
    }

    public Map<String, String> getDatabase() {
        return database;
    }
}
