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
package dev.xorcery.jsonapi.server.providers;

import dev.xorcery.jsonapi.Error;
import dev.xorcery.jsonapi.Errors;
import dev.xorcery.jsonapi.ResourceDocument;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletionException;

import static dev.xorcery.jsonapi.MediaTypes.APPLICATION_JSON_API;

@Provider
@Produces(APPLICATION_JSON_API)
public class JsonApiExceptionMapper
    implements ExceptionMapper<Throwable>
{
    Logger logger = LogManager.getLogger(getClass());
    @Override
    public Response toResponse(Throwable exception)
    {
        if (exception instanceof CompletionException)
        {
            exception = exception.getCause();
        }

        try {
            throw exception;
        } catch (WebApplicationException e) {
            return e.getResponse();
        } catch (Throwable e) {
            logger.error("Response exception", exception);

            return Response.serverError().entity(new ResourceDocument.Builder()
                    .errors(new Errors.Builder()
                            .error(new Error.Builder()
                                    .status(500)
                                    .title(e.getMessage())
                                    .detail(e.toString())
                                    .build())
                            .build()).build())
                    .type(APPLICATION_JSON_API)
                    .build();
        }
    }
}
