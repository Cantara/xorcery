package com.exoreaction.reactiveservices.jaxrs.writers;

import com.exoreaction.reactiveservices.jaxrs.MediaTypes;
import com.exoreaction.reactiveservices.json.JsonElement;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.exoreaction.reactiveservices.jsonapi.model.JsonApiRels.describedby;

@Singleton
@Provider
@Produces({MediaType.WILDCARD})
public class JsonApiMessageBodyWriter
        implements MessageBodyWriter<JsonElement> {
    private final ObjectMapper objectMapper;
    private final ObjectMapper yamlObjectMapper;

    @Inject
    public JsonApiMessageBodyWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.yamlObjectMapper = new ObjectMapper(new YAMLFactory());
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonElement.class.isAssignableFrom(type) &&
                (mediaType.isCompatible(MediaTypes.APPLICATION_JSON_API_TYPE) ||
                        mediaType.isCompatible(MediaTypes.APPLICATION_JSON_SCHEMA_TYPE) ||
                        mediaType.isCompatible(MediaTypes.APPLICATION_YAML_TYPE));
    }

    @Override
    public void writeTo(JsonElement jsonElement, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {

        if (jsonElement instanceof ResourceDocument rd) {
            rd.getLinks().getByRel(describedby).ifPresent(link ->
            {
                httpHeaders.add("Link", String.format("<%s>; rel=\"%s\"; title=\"%s\"", link.getHref(), link.rel(), "JSON Schema"));
            });
        }

        if (mediaType.isCompatible(MediaTypes.APPLICATION_YAML_TYPE)) {
            yamlObjectMapper.writer().writeValue(entityStream, jsonElement.json());
        } else {
            objectMapper.writer().withDefaultPrettyPrinter().writeValue(entityStream, jsonElement.json());
        }
    }
}
