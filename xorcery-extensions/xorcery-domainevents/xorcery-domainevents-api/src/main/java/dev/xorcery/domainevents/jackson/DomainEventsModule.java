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
