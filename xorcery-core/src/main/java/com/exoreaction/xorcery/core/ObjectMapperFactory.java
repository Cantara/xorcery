package com.exoreaction.xorcery.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.Service;

@Service
public class ObjectMapperFactory
    implements Factory<ObjectMapper>
{
    private final ObjectMapper objectMapper;

    public ObjectMapperFactory() {
        objectMapper = new ObjectMapper();
    }

    @Override
    @Rank(-1)
    @Singleton
    public ObjectMapper provide() {
        return objectMapper;
    }

    @Override
    public void dispose(ObjectMapper instance) {
    }
}
