/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.thymeleaf.jsonapi.providers;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.hyperschema.HyperSchema;
import com.exoreaction.xorcery.hyperschema.client.HyperSchemaClient;
import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.jsonapi.JsonApiRels;
import com.exoreaction.xorcery.jsonapi.Link;
import com.exoreaction.xorcery.jsonapi.providers.JsonNodeMessageBodyReader;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
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
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.uri.UriTemplate;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolution;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Singleton
@Provider
@Produces(MediaType.TEXT_HTML)
public class ThymeleafJsonApiMessageBodyWriter
        implements MessageBodyWriter<JsonElement> {

    private final jakarta.inject.Provider<HttpServletRequest> servletRequestProvider;
/*
    private final jakarta.inject.Provider<HttpServletResponse> servletResponseProvider;
    private final jakarta.inject.Provider<ServletContext> servletContextProvider;
*/
    private final Logger logger;

    private final Client client;
    private final ITemplateEngine templateEngine;
    private final jakarta.inject.Provider<ContainerRequestContext> requestContext;
    private final String title;

    @Inject
    public ThymeleafJsonApiMessageBodyWriter(ITemplateEngine templateEngine,
                                             jakarta.inject.Provider<ContainerRequestContext> requestContext,
                                             jakarta.inject.Provider<HttpServletRequest> servletRequestProvider,
/*
                                             jakarta.inject.Provider<HttpServletResponse> servletResponseProvider,
                                             jakarta.inject.Provider<ServletContext> servletContextProvider,
*/
                                             ClientBuilder clientBuilder,
                                             Configuration configuration,
                                             Logger logger) {
        this.templateEngine = templateEngine;
        this.servletRequestProvider = servletRequestProvider;
/*
        this.servletResponseProvider = servletResponseProvider;
        this.servletContextProvider = servletContextProvider;
*/
        this.logger = logger;
        this.requestContext = requestContext;
        InstanceConfiguration instanceConfiguration = InstanceConfiguration.get(configuration);
        this.title = String.format("%s (%s,%s)", instanceConfiguration.getId(), instanceConfiguration.getFQDN(), instanceConfiguration.getIp().toString());
        client = clientBuilder
                .register(JsonNodeMessageBodyReader.class)
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

        HttpServletRequest httpServletRequest = servletRequestProvider.get();
/* Wait for https://github.com/eclipse-ee4j/jersey/issues/5527 to be resolved
        HttpServletResponse httpServletResponse = servletResponseProvider.get();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContextProvider.get());
        IWebExchange webExchange = webApplication.buildExchange(httpServletRequest, httpServletResponse);
*/
        Context context = new Context(httpServletRequest.getLocale());

        // Check if we have a JSON Schema
        getSchema(jsonElement, httpHeaders).ifPresent(link ->
        {
            HyperSchema hyperSchema = new HyperSchemaClient(client).get(link).toCompletableFuture().join();
            context.setVariable("schema", hyperSchema);
        });

        context.setVariable("title", title);
        context.setVariable("json", jsonElement);
        context.setVariable("jsonapi", new JsonApiHelper());

        OutputStreamWriter writer = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8);
        String path = "jsonapi" + calculateTemplate(requestContext.get().getUriInfo());

        for (ITemplateResolver templateResolver : templateEngine.getConfiguration().getTemplateResolvers()) {
            TemplateResolution templateResolution = templateResolver.resolveTemplate(templateEngine.getConfiguration(), null, path, null);
            if (templateResolution != null && templateResolution.getTemplateResource().exists())
            {
                templateEngine.process(path, context, writer);
                writer.flush();
                return;
            }
        }

        // Fallback to generic rendering
        String templateName = jsonElement.getClass().getSimpleName().toLowerCase();
        templateEngine.process("jsonapi/" + templateName, context, writer);
        writer.flush();
    }

    private Optional<Link> getSchema(JsonElement jsonElement, MultivaluedMap<String, Object> httpHeaders) {
        JsonNode schema = jsonElement.json().path("links").path(JsonApiRels.describedby);
        if (!schema.isMissingNode()) {
            URI schemaUri = requestContext.get().getUriInfo().getAbsolutePath().resolve(schema.textValue());
            return Optional.of(new Link(JsonApiRels.describedby, schemaUri));
        }

        return Optional.ofNullable(httpHeaders.get("Link"))
                .flatMap(list -> list.stream()
                        .map(jakarta.ws.rs.core.Link.class::cast)
                        .filter(link -> link.getRel().equals(JsonApiRels.describedby))
                        .findFirst()
                        .map(link -> {
                            URI schemaUri = requestContext.get().getUriInfo().getAbsolutePath().resolve(link.getUri());
                            return new Link(link.getRel(), schemaUri);
                        }));
    }

    protected String calculateTemplate(UriInfo uriInfo) {
        if (uriInfo instanceof ExtendedUriInfo) {
            ExtendedUriInfo extendedUriInfo = (ExtendedUriInfo) uriInfo;
            return calculateTemplateFromExtendedUriInfo(extendedUriInfo);
        } else {
            logger.error("Expected ExtendedUriInfo, got {}", uriInfo.getClass().getCanonicalName());
            throw new InternalServerErrorException("Can't calculate template");
        }
    }

    private String calculateTemplateFromExtendedUriInfo(ExtendedUriInfo extendedUriInfo) {
        String htmlTemplate = "";
        List<UriTemplate> matchedTemplates = extendedUriInfo.getMatchedTemplates();

        if (matchedTemplates == null || matchedTemplates.size() == 0) {
            return htmlTemplate;
        }

        for (int i = matchedTemplates.size() - 1; i >= 0; i--) {
            String uriTemplate = matchedTemplates.get(i).getTemplate();
            if (uriTemplate.equals("/"))
                continue;
            htmlTemplate += uriTemplate.replaceAll("\\{|\\}", "");
        }

        if (htmlTemplate.charAt(0) != '/') {
            htmlTemplate = "/".concat(htmlTemplate);
        }
        return htmlTemplate;
    }
}
