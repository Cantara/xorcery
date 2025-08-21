# Xorcery Log4j Module Technical Documentation

## Overview

The `xorcery-log4j` module provides seamless integration between Apache Log4j2 and the Xorcery framework. It allows Xorcery applications to configure and manage Log4j2 logging through Xorcery's unified configuration system, while providing bridges to other popular logging frameworks (SLF4J, Apache Commons Logging, Java Util Logging).

## Features

- **Unified Configuration**: Configure Log4j2 through Xorcery's YAML-based configuration system
- **Multiple Logging Facades**: Supports SLF4J, Apache Commons Logging (JCL), and Java Util Logging (JUL)
- **HK2 Integration**: Fully integrated with Xorcery's dependency injection container
- **Schema Validation**: JSON Schema validation for logging configuration
- **Environment-Specific Configs**: Separate configurations for development, testing, and production

## Architecture

### Core Components

1. **LoggerContextFactory**: The main factory class responsible for:
   - Creating and managing Log4j2 LoggerContext instances
   - Converting Xorcery configuration to Log4j2 format
   - Handling initialization and reconfiguration

2. **Configuration Integration**: 
   - Reads configuration from the `log4j2` section in Xorcery configuration
   - Uses Jackson YAMLMapper to convert configuration format
   - Supports all standard Log4j2 configuration options

3. **System Property Management**: Sets up required Log4j2 system properties:
   - Thread context map inheritance
   - Context selector configuration
   - Java Util Logging manager setup

## Maven Dependency

```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-log4j</artifactId>
</dependency>
```

## Configuration

### Basic Configuration Structure

The logging configuration is defined under the `log4j2` key in your Xorcery configuration:

```yaml
log4j2:
  Configuration:
    name: MyApplication
    status: warn
    properties: {}
    thresholdFilter:
      level: info
    appenders:
      # Appender configurations
    Loggers:
      logger: []
      Root:
        level: info
        AppenderRef: []
```
Extend this using the instructions from [Log4j2 YAML configuration](https://logging.apache.org/log4j/2.3.x/manual/configuration.html) management.

## Usage

### Basic Setup

1. Add the dependency to your project
2. Configure logging in your `xorcery.yaml` application configuration file:

```yaml
log4j2:
  Configuration:
    name: MyApp
    appenders:
      Console:
        name: STDOUT
        target: SYSTEM_OUT
        PatternLayout:
          Pattern: "%d [%t] %-5level %c{1.}: %msg%n"
    Loggers:
      Root:
        level: info
        AppenderRef:
          - ref: STDOUT
```

### Using in Java Code

Once configured, use standard logging APIs, optionally using @Inject with Log4j2 types:

```java
// Using Log4j2 directly (recommended)
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;

public class MyService {
    
    @Inject
    public MyService(Logger logger, LoggerContext loggerContext){
    }
    private static final Logger logger = LogManager.getLogger(MyService.class);
}

// Or using SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyService {
    private static final Logger logger = LoggerFactory.getLogger(MyService.class);

    public void doSomething() {
        logger.info("Doing something important");
    }
}

```

## Configuration Examples

### Console and File Logging

```yaml
log4j2:
  Configuration:
    name: MyApplication
    appenders:
      Console:
        name: STDOUT
        target: SYSTEM_OUT
        PatternLayout:
          Pattern: "%d [%t] %-5level %c{1.}: %msg%n"
      
      File:
        name: FILE
        fileName: logs/application.log
        PatternLayout:
          Pattern: "%d [%t] %-5level %c: %msg%n"
    
    Loggers:
      logger:
        - name: dev.xorcery
          level: debug
          additivity: false
          AppenderRef:
            - ref: STDOUT
            - ref: FILE
      
      Root:
        level: info
        AppenderRef:
          - ref: STDOUT
```

### Test Configuration

For testing environments, the module declares the following configuration:

```yaml
log4j2:
   Configuration:
      name: Xorcery tests
      appenders:
         Console:
            name: STDOUT
            target: SYSTEM_OUT
            PatternLayout:
               Pattern: "%-5level %marker %c{1.}: %msg%n%throwable"

      Loggers:
         logger:
            - name: org.apache.logging.log4j
              level: debug
              additivity: false
              AppenderRef:
                 - ref: STDOUT

            - name: org.apache.logging.log4j.status.StatusLogger
              level: debug

            - name: dev.xorcery.core
              level: debug

         Root:
            AppenderRef:
               - ref: STDOUT
```

## Advanced Features

### Custom Properties

You can use the Xorcery configuration reference syntax in your logging configuration:

```yaml
log4j2:
  Configuration:
    properties:
      level: debug
      pattern: "%d [%t] %-5level %c: %msg%n"
    
    appenders:
      Console:
        name: STDOUT
        PatternLayout:
          Pattern: "{{ log4j2.Configuration.properties.pattern }}"
    
    Loggers:
      Root:
        level: "{{ log4j2.Configuration.properties.level }}"
```

### Integration with Other Logging Frameworks

The module automatically bridges other logging frameworks:

- **SLF4J**: All SLF4J calls are routed through Log4j2
- **Apache Commons Logging**: JCL calls are handled by Log4j2
- **Java Util Logging**: JUL calls are redirected to Log4j2

## System Properties

The module automatically sets these system properties:

- `java.util.logging.manager=org.apache.logging.log4j.jul.LogManager`
- `log4j2.isThreadContextMapInheritable=true`
- `Log4jContextSelector=org.apache.logging.log4j.core.selector.BasicContextSelector`

## Troubleshooting

### Common Issues

1. **Configuration Not Applied**
    - Ensure the `log4j2` section is present in your configuration
    - Check that the module is enabled (`log4j2` configuration exists)
    - Verify JSON Schema validation passes

2. **Multiple Logging Frameworks Conflict**
    - The module handles most conflicts automatically via bridges
    - Remove other SLF4J implementations from classpath
    - Ensure only one Log4j2 implementation is present

3. **Performance Issues**
    - Consider using async appenders for high-throughput applications
    - Adjust buffer sizes and flush policies
    - Monitor Log4j2 internal status logging

## Best Practices

1. **Configure Appropriate Levels**: Use appropriate log levels to avoid performance impact
2. **Environment-Specific Configs**: Use different configurations for different environments
3. **Monitor Log File Growth**: Implement log rotation policies for file appenders
4. **Structured Logging**: Consider using structured logging formats for better log analysis
