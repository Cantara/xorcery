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
package com.exoreaction.xorcery.thymeleaf.providers;

import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.uri.UriTemplate;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Provider
@Produces(MediaType.TEXT_HTML)
public class ThymeleafHtmlMessageBodyWriter
        implements MessageBodyWriter<WebContext> {

    private final ITemplateEngine templateEngine;

    jakarta.inject.Provider<ContainerRequestContext> requestContextProvider;

    @Inject
    public ThymeleafHtmlMessageBodyWriter(ITemplateEngine templateEngine,
                                          jakarta.inject.Provider<ContainerRequestContext> requestContextProvider) {
        this.templateEngine = templateEngine;
        this.requestContextProvider = requestContextProvider;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == WebContext.class;
    }

    @Override
    public void writeTo(WebContext context, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        String path = calculateTemplate(requestContextProvider.get().getUriInfo());

        OutputStreamWriter writer = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8);
        templateEngine.process(path, context, writer);
        writer.close();
    }

    protected String calculateTemplate(UriInfo uriInfo) {
        if (uriInfo instanceof ExtendedUriInfo) {
            ExtendedUriInfo extendedUriInfo = (ExtendedUriInfo) uriInfo;
            return calculateTemplateFromExtendedUriInfo(extendedUriInfo);
        } else {
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
            htmlTemplate += uriTemplate.replaceAll("\\{|\\}", "");
        }

        if (htmlTemplate.charAt(0) != '/') {
            htmlTemplate = "/".concat(htmlTemplate);
        }
        return htmlTemplate;
    }

}
