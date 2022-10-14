package com.exoreaction.xorcery.service.neo4j.log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.neo4j.logging.AbstractLog;
import org.neo4j.logging.Log;
import org.neo4j.logging.log4j.Neo4jLogMessage;
import org.neo4j.logging.log4j.Neo4jMessageSupplier;

import java.util.function.Consumer;

public class Log4jLog extends AbstractLog {
    final Logger logger;

    Log4jLog(Logger logger) {
        this.logger = logger;
    }

    public boolean isDebugEnabled() {
        return this.logger.isDebugEnabled();
    }

    public void debug(Neo4jLogMessage message) {
        this.logger.debug(message);
    }

    public void debug(Neo4jMessageSupplier supplier) {
        this.logger.debug(supplier);
    }

    public void debug(String message) {
        this.logger.debug(message);
    }

    public void debug(String message, Throwable throwable) {
        this.logger.debug(message, throwable);
    }

    public void debug(String format, Object... arguments) {
        this.logger.printf(Level.DEBUG, format, arguments);
    }

    public void info(Neo4jLogMessage message) {
        this.logger.info(message);
    }

    public void info(Neo4jMessageSupplier supplier) {
        this.logger.info(supplier);
    }

    public void info(String message) {
        this.logger.info(message);
    }

    public void info(String message, Throwable throwable) {
        this.logger.info(message, throwable);
    }

    public void info(String format, Object... arguments) {
        this.logger.printf(Level.INFO, format, arguments);
    }

    public void warn(Neo4jLogMessage message) {
        this.logger.warn(message);
    }

    public void warn(Neo4jMessageSupplier supplier) {
        this.logger.warn(supplier);
    }

    public void warn(String message) {
        this.logger.warn(message);
    }

    public void warn(String message, Throwable throwable) {
        this.logger.warn(message, throwable);
    }

    public void warn(String format, Object... arguments) {
        this.logger.printf(Level.WARN, format, arguments);
    }

    public void error(Neo4jLogMessage message) {
        this.logger.error(message);
    }

    public void error(Neo4jMessageSupplier supplier) {
        this.logger.error(supplier);
    }

    public void error(String message) {
        this.logger.error(message);
    }

    public void error(Neo4jLogMessage message, Throwable throwable) {
        this.logger.error(message, throwable);
    }

    public void error(String message, Throwable throwable) {
        this.logger.error(message, throwable);
    }

    public void error(String format, Object... arguments) {
        this.logger.printf(Level.ERROR, format, arguments);
    }

    public void bulk(Consumer<Log> consumer) {
        consumer.accept(this);
    }
}
