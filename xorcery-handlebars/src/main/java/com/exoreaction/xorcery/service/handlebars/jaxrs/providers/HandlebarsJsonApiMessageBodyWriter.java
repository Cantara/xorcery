package com.exoreaction.xorcery.service.handlebars.jaxrs.providers;

import com.exoreaction.xorcery.hyperschema.client.HyperSchemaClient;
import com.exoreaction.xorcery.hyperschema.model.HyperSchema;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.jsonapi.jaxrs.providers.JsonNodeMessageBodyReader;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.service.handlebars.helpers.OptionalValueResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;
import org.glassfish.jersey.jetty.connector.JettyHttpClientSupplier;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.uri.UriTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static com.exoreaction.xorcery.jsonapi.model.JsonApiRels.describedby;


@Singleton
@Provider
@Produces(MediaType.TEXT_HTML)
public class HandlebarsJsonApiMessageBodyWriter
        implements MessageBodyWriter<JsonElement> {

    private static final Logger logger = LogManager.getLogger(HandlebarsJsonApiMessageBodyWriter.class);

    private final Client client;
    private final Handlebars handlebars;
    private final jakarta.inject.Provider<ContainerRequestContext> requestContext;

    @Inject
    public HandlebarsJsonApiMessageBodyWriter(Handlebars handlebars,
                                              jakarta.inject.Provider<ContainerRequestContext> requestContext,
                                              JettyHttpClientSupplier clientSupplier) {
        this.handlebars = handlebars;
        this.requestContext = requestContext;
        client = ClientBuilder.newBuilder().withConfig(new ClientConfig()
                        .register(new JsonNodeMessageBodyReader(new ObjectMapper()))
                        .register(new LoggingFeature.LoggingFeatureBuilder().withLogger(java.util.logging.Logger.getLogger("client.sandbox")).build())
                        .connectorProvider(new JettyConnectorProvider())
                        .register(clientSupplier))
                .build();
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonElement.class.isAssignableFrom(type) && mediaType.isCompatible(MediaType.TEXT_HTML_TYPE);
    }

    @Override
    public void writeTo(JsonElement jsonElement, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {

        Context.Builder builder = Context.newBuilder(jsonElement)
                .push(OptionalValueResolver.INSTANCE);

        // Check if we have a JSON Schema
        getSchema(jsonElement, httpHeaders).ifPresent(link ->
        {
            HyperSchema hyperSchema = new HyperSchemaClient(client).get(link).toCompletableFuture().join();
            builder.combine("schema", hyperSchema);
        });

        Context context = builder.build();

        OutputStreamWriter writer = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8);
        String path = "jsonapi" + calculateTemplate(requestContext.get().getUriInfo());
        try {
            handlebars.compile(path).apply(context, writer);
        } catch (IOException e) {

            String templateName = jsonElement.getClass().getSimpleName().toLowerCase();
            handlebars.compile("jsonapi/" + templateName).apply(context, writer);
        }

        writer.close();
    }

    private Optional<Link> getSchema(JsonElement jsonElement, MultivaluedMap<String, Object> httpHeaders) {
        JsonNode schema = jsonElement.json().path("links").path(describedby);
        if (!schema.isMissingNode()) {
            return Optional.of(new Link(describedby, schema));
        }

        return Optional.ofNullable(httpHeaders.get("Link"))
                .flatMap(list -> list.stream()
                        .map(jakarta.ws.rs.core.Link.class::cast)
                        .filter(link -> link.getRel().equals(describedby))
                        .findFirst()
                        .map(link -> new Link(link.getRel(), link.getUri().toASCIIString())));
    }

    protected String calculateTemplate( UriInfo uriInfo )
    {
        String htmlTemplateResult = "";

        if ( uriInfo instanceof ExtendedUriInfo)
        {
            ExtendedUriInfo extendedUriInfo = (ExtendedUriInfo) uriInfo;
            htmlTemplateResult = calculateTemplateFromExtendedUriInfo( extendedUriInfo );
        }
        else
        {
            logger.error( "Expected ExtendedUriInfo, got {}", uriInfo.getClass().getCanonicalName() );
            throw new InternalServerErrorException( "Can't calculate template" );
        }

        return htmlTemplateResult;
    }


    private String calculateTemplateFromExtendedUriInfo( ExtendedUriInfo extendedUriInfo )
    {
        String htmlTemplate = "";
        List<UriTemplate> matchedTemplates = extendedUriInfo.getMatchedTemplates();

        if ( matchedTemplates == null || matchedTemplates.size() == 0 )
        {
            return htmlTemplate;
        }

        for (int i = matchedTemplates.size()-1; i >= 0; i--) {
            String uriTemplate = matchedTemplates.get(i).getTemplate();
            htmlTemplate += uriTemplate.replaceAll( "\\{|\\}", "" );
        }

        if ( htmlTemplate.charAt( 0 ) != '/' )
        { htmlTemplate = "/".concat( htmlTemplate ); }
        return htmlTemplate;
    }
}
