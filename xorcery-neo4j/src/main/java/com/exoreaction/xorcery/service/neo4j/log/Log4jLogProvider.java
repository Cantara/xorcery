package com.exoreaction.xorcery.service.neo4j.log;

import org.apache.logging.log4j.LogManager;
import org.neo4j.logging.Log;
import org.neo4j.logging.LogProvider;

public class Log4jLogProvider
        implements LogProvider {
    @Override
    public Log getLog(Class<?> aClass) {
        return new Log4jLog(LogManager.getLogger(aClass));
    }

    @Override
    public Log getLog(String s) {
        return new Log4jLog(LogManager.getLogger(s));
    }
}
