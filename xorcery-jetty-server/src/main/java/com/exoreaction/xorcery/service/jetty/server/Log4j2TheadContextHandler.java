package com.exoreaction.xorcery.service.jetty.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.ThreadContext;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Log4j2TheadContextHandler
    extends HandlerWrapper
{
    private final Map<String, String> context;

    public Log4j2TheadContextHandler(Configuration configuration) {
        context = new HashMap<>();
        InstanceConfiguration instanceConfiguration = new InstanceConfiguration(configuration.getConfiguration("instance"));
        context.put("id", instanceConfiguration.getId());
        context.put("name", instanceConfiguration.getName());
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        ThreadContext.putAll(context);

        try {
            super.handle(target, baseRequest, request, response);
        } finally {
            ThreadContext.clearAll();
        }
    }
}
