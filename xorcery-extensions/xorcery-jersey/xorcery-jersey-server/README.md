# Xorcery Jersey Server Extension

## Overview

The `xorcery-jersey-server` module provides Jersey JAX-RS server integration for the Xorcery framework. It bridges Jersey's powerful JAX-RS implementation with Xorcery's configuration system, dependency injection, and Jetty servlet container, enabling developers to build robust RESTful web services with minimal boilerplate code.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Dependencies](#dependencies)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [API Reference](#api-reference)
- [Examples](#examples)
- [Integration](#integration)
- [Troubleshooting](#troubleshooting)

## Features

- **Seamless Jersey Integration**: Automatic setup and configuration of Jersey JAX-RS server
- **Configuration-Driven**: Full configuration through Xorcery's YAML-based configuration system
- **Dependency Injection**: HK2-based dependency injection integrated with Xorcery's service locator
- **Media Type Support**: Configurable media type mappings for content negotiation
- **Provider Registration**: Dynamic registration of JAX-RS providers via configuration
- **Lifecycle Management**: Proper startup and shutdown handling integrated with Xorcery's run levels
- **Schema Validation**: JSON Schema validation for configuration
- **WADL Disabled**: WADL generation disabled by default for security

## Architecture

### Component Structure

```
┌─────────────────────────────────────┐
│           Application Layer          │
│        (JAX-RS Resources)           │
└─────────────────┬───────────────────┘
│
┌─────────────────▼───────────────────┐
│        Jersey Framework             │
│      (JAX-RS Implementation)        │
└─────────────────┬───────────────────┘
│
┌─────────────────▼───────────────────┐
│      JerseyServletContainer         │
│    (Custom Servlet Container)       │
└─────────────────┬───────────────────┘
│
┌─────────────────▼───────────────────┐
│        Jetty Servlet Context        │
│       (Web Server Layer)            │
└─────────────────┬───────────────────┘
│
┌─────────────────▼───────────────────┐
│         Xorcery Framework           │
│   (DI, Configuration, Lifecycle)    │
└─────────────────────────────────────┘
```

### Key Classes

1. **JerseyServerService** (`@Service`, `@RunLevel(6)`)
   - Main service responsible for Jersey initialization
   - Configures ResourceConfig with properties and providers
   - Registers servlet with Jetty servlet context
   - Handles proper shutdown sequence

2. **JerseyConfiguration**
   - Configuration wrapper for Jersey-specific settings
   - Extracts media types, properties, and provider registrations
   - Converts Xorcery configuration to Jersey format

3. **JerseyServletContainer**
   - Extends Jersey's ServletContainer
   - Provides custom shutdown handling
   - Prevents Jetty lifecycle conflicts during shutdown

## Dependencies

### Required Modules

- `xorcery-jetty-server`: Provides servlet container
- `xorcery-configuration-api`: Configuration system integration
- `xorcery-util`: Utility classes

### External Libraries

- **Jersey**: JAX-RS implementation
  - `jersey-container-servlet`: Servlet container integration
  - `jersey-hk2`: HK2 dependency injection
  - `jersey-server`: Core server functionality
- **HK2**: Dependency injection framework
- **Jakarta Inject**: Dependency injection annotations
- **Jackson**: JSON processing
- **Log4j**: Logging framework

## Installation

### Maven Dependency

```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-jersey-server</artifactId>
</dependency>
```

### Module Declaration

Add to your `module-info.java`:

```java
module your.application {
    requires xorcery.jersey.server;
    // other requires...
}
```

## Configuration

### Basic Configuration

The module is configured through the `jersey.server` section in your `xorcery.yaml`:

```yaml
jersey:
  server:
    enabled: true  # Inherits from jetty.server.enabled by default
    register: []   # List of JAX-RS provider class names
    mediaTypes: {} # Media type mappings
    properties:    # Jersey-specific properties
      jersey:
        config:
          server:
            unwrap:
              completion:
                stage:
                  writer:
                    enable: true
            wadl:
              disableWadl: true
```

### Configuration Schema

The module provides JSON Schema validation for configuration:

#### Core Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | boolean/string | `{{ jetty.server.enabled }}` | Enable/disable Jersey server |
| `register` | array[string] | `[]` | List of JAX-RS provider class names to register |
| `mediaTypes` | object | `{}` | Media type mappings (suffix -> media type) |
| `properties` | object | See below | Jersey configuration properties |

#### Media Type Mappings

```yaml
jersey:
  server:
    mediaTypes:
      html: "text/html"
      txt: "text/plain"
      json: "application/json"
      yaml: "application/yaml"
      schema: "application/schema+json"
```

#### Provider Registration

```yaml
jersey:
  server:
    register:
      - "com.example.providers.AuthenticationFilter"
      - "com.example.providers.ValidationExceptionMapper"
      - "com.example.providers.JsonProvider"
```

#### Jersey Properties

```yaml
jersey:
  server:
    properties:
      jersey:
        config:
          server:
            unwrap:
              completion:
                stage:
                  writer:
                    enable: true
            wadl:
              disableWadl: true
            # Add other Jersey ServerProperties here
```

### Default Configuration

The module comes with sensible defaults:

```yaml
jersey:
  server:
    enabled: "{{ jetty.server.enabled }}"
    register: []
    mediaTypes:
      html: "text/html"
      txt: "text/plain"
      json: "application/json"
      yaml: "application/yaml"
      schema: "application/schema+json"
    properties:
      jersey:
        config:
          server:
            unwrap:
              completion:
                stage:
                  writer:
                    enable: true
            wadl:
              disableWadl: true
```

## Usage

### Basic Setup

1. **Add Dependency**: Include the module in your project dependencies
2. **Enable Configuration**: Ensure Jersey server is enabled in configuration
3. **Create Resources**: Implement JAX-RS resource classes
4. **Register Providers**: Add any custom providers to configuration

### Creating JAX-RS Resources

```java
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.inject.Singleton;

@Path("/api/users")
@Singleton
public class UserResource {
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsers() {
        // Implementation
        return Response.ok().build();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUser(User user) {
        // Implementation
        return Response.status(201).build();
    }
    
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("id") String id) {
        // Implementation
        return Response.ok().build();
    }
}
```

### Custom Providers

```java
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.Response;

@Provider
public class ValidationExceptionMapper 
    implements ExceptionMapper<ValidationException> {
    
    @Override
    public Response toResponse(ValidationException exception) {
        return Response.status(400)
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
    }
}
```

### Application Startup

```java
import dev.xorcery.core.Xorcery;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;

public class Application {
    public static void main(String[] args) throws Exception {
        Configuration configuration = new ConfigurationBuilder()
            .addDefaults()
            .addYaml("""
                jersey:
                  server:
                    enabled: true
                    register:
                      - "com.example.providers.ValidationExceptionMapper"
                    mediaTypes:
                      json: "application/json"
                jetty:
                  server:
                    enabled: true
                    http:
                      port: 8080
                """)
            .build();
            
        try (Xorcery xorcery = new Xorcery(configuration)) {
            // Server is now running and ready to handle requests
            Thread.currentThread().join();
        }
    }
}
```

## API Reference

### JerseyServerService

Main service class responsible for Jersey initialization and lifecycle management.

**Annotations:**
- `@Service(name = "jersey.server")`
- `@RunLevel(6)`

**Constructor:**
```java
@Inject
public JerseyServerService(Configuration configuration,
                          ServletContextHandler servletContextHandler)
```

**Key Responsibilities:**
- Configures Jersey ResourceConfig
- Registers JAX-RS providers
- Sets up media type mappings
- Integrates with Jetty servlet context
- Handles proper shutdown

### JerseyConfiguration

Configuration wrapper for Jersey-specific settings.

**Static Factory Method:**
```java
public static JerseyConfiguration get(Configuration configuration)
```

**Key Methods:**
```java
public Map<String, Object> getProperties()
public Optional<Map<String, String>> getMediaTypes()
```

### JerseyServletContainer

Custom servlet container extending Jersey's ServletContainer.

**Constructor:**
```java
public JerseyServletContainer(ResourceConfig resourceConfig)
```

**Key Methods:**
```java
public void stop()  // Proper shutdown handling
public void destroy()  // Overridden to prevent double shutdown
```

## Examples

### Complete REST API Example

```java
// User model
public class User {
    private String id;
    private String name;
    private String email;
    
    // Constructors, getters, setters
}

// User service
@Service
public class UserService {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    public Optional<User> getUser(String id) {
        return Optional.ofNullable(users.get(id));
    }
    
    public User createUser(User user) {
        user.setId(UUID.randomUUID().toString());
        users.put(user.getId(), user);
        return user;
    }
}

// REST resource
@Path("/api/users")
@Singleton
public class UserResource {
    
    @Inject
    private UserService userService;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsers() {
        List<User> users = userService.getAllUsers();
        return Response.ok(users).build();
    }
    
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("id") String id) {
        return userService.getUser(id)
            .map(user -> Response.ok(user).build())
            .orElse(Response.status(404).build());
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUser(User user) {
        User created = userService.createUser(user);
        return Response.status(201).entity(created).build();
    }
}
```

### Configuration Example

```yaml
# xorcery.yaml
application:
  name: "user-service"
  version: "1.0.0"

instance:
  host: "api-server"
  domain: "example.com"

jetty:
  server:
    enabled: true
    http:
      enabled: true
      port: 8080
    ssl:
      enabled: false

jersey:
  server:
    enabled: true
    register:
      - "com.example.providers.CorsFilter"
      - "com.example.providers.JsonProvider"
      - "com.example.providers.ValidationExceptionMapper"
    mediaTypes:
      json: "application/json"
      xml: "application/xml"
      html: "text/html"
    properties:
      jersey:
        config:
          server:
            wadl:
              disableWadl: true
```

## Integration

### With Other Xorcery Modules

#### OpenTelemetry Integration

```yaml
# Enable OpenTelemetry for Jersey
dependencies:
  - xorcery-opentelemetry-jersey-server

# Automatic tracing of JAX-RS endpoints
```

#### JSON API Integration

```yaml
# Enable JSON:API support
dependencies:
  - xorcery-jsonapi-jaxrs

jersey:
  server:
    register:
      - "dev.xorcery.jsonapi.jaxrs.JsonApiProvider"
```

#### Security Integration

```yaml
# Enable JWT security
dependencies:
  - xorcery-jetty-server-jwt

jetty:
  server:
    security:
      enabled: true
      type: "jwt"
```

### Custom Initialization

```java
@Service(name = "custom.jersey.config")
@RunLevel(5)  // Before Jersey server starts
public class CustomJerseyConfiguration {
    
    @Inject
    public CustomJerseyConfiguration() {
        // Custom Jersey setup before server starts
    }
}
```

## Troubleshooting

### Common Issues

#### 1. Jersey Server Not Starting

**Symptoms:** No JAX-RS endpoints available, 404 for all requests

**Solutions:**
- Check that `jersey.server.enabled` is `true`
- Verify that `jetty.server.enabled` is `true`
- Ensure proper run level ordering

```yaml
jersey:
  server:
    enabled: true
jetty:
  server:
    enabled: true
```

#### 2. Provider Registration Fails

**Symptoms:** ClassNotFoundException for registered providers

**Solutions:**
- Verify class names are correct and fully qualified
- Ensure classes are on the classpath
- Check module dependencies

```yaml
jersey:
  server:
    register:
      - "com.example.providers.MyProvider"  # Full class name required
```

#### 3. Media Type Issues

**Symptoms:** Incorrect Content-Type headers, content negotiation failures

**Solutions:**
- Check media type mappings
- Verify Jersey properties configuration

```yaml
jersey:
  server:
    mediaTypes:
      json: "application/json"
      xml: "application/xml"
```

#### 4. Shutdown Issues

**Symptoms:** Application hangs on shutdown

**Solutions:**
- The module handles this automatically through JerseyServletContainer
- Ensure proper service dependencies

### Debug Configuration

Enable debug logging for Jersey:

```yaml
log4j2:
  Configuration:
    Loggers:
      logger:
        - name: "org.glassfish.jersey"
          level: "debug"
        - name: "dev.xorcery.jersey"
          level: "debug"
```

### Performance Tuning

```yaml
jetty:
  server:
    minThreads: 10
    maxThreads: 200
    outputBufferSize: 32768

jersey:
  server:
    properties:
      jersey:
        config:
          server:
            # Add Jersey performance properties
```

## Best Practices

1. **Resource Lifecycle**: Use `@Singleton` for stateless resources
2. **Exception Handling**: Implement ExceptionMapper providers for consistent error responses
3. **Content Negotiation**: Configure appropriate media types
4. **Security**: Always disable WADL in production
5. **Monitoring**: Integrate with OpenTelemetry for observability
6. **Configuration**: Use Xorcery's configuration templating for environment-specific settings

## Version Information

- **Module Version**: 0.164.6-SNAPSHOT
- **Jersey Version**: (Check pom.xml for exact version)
- **Java Version**: 21+
- **Xorcery Framework**: Compatible with current Xorcery version
