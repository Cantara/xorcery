# Xorcery Jetty Server Extension

## Overview

The `xorcery-jetty-server` module provides a production-ready HTTP/HTTPS server integration for the Xorcery framework, built on Eclipse Jetty 12.x. It offers comprehensive support for modern web protocols, security features, and flexible configuration options.

### Key Features

- **Multi-Protocol Support**: HTTP/1.1, HTTP/2 (both clear-text and encrypted)
- **SSL/TLS Security**: Full SSL/TLS support with client certificate authentication
- **WebSocket Support**: Built-in WebSocket server capabilities
- **Compression**: GZIP compression with configurable rules
- **Session Management**: HTTP session handling
- **Security Framework**: Constraint-based security with custom authenticators
- **Modular Architecture**: Extensible handler system via HK2 service discovery
- **Configuration-Driven**: YAML-based configuration with JSON Schema validation

## Architecture

### Core Components

```
┌─────────────────────┐    ┌──────────────────────┐    ┌─────────────────────┐
│   Configuration     │    │    HK2 Services      │    │   Jetty Server      │
│                     │    │                      │    │                     │
│ • xorcery.yaml      │───▶│ • JettyServerFactory │───▶│ • Server Instance   │
│ • Schema validation │    │ • Handler Factories  │    │ • Connectors        │
│ • Template support  │    │ • Connection Factory │    │ • Handler Chain     │
└─────────────────────┘    └──────────────────────┘    └─────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Lifecycle Management                              │
│                                                                             │
│ JettyLifecycleService (RunLevel 18)                                        │
│ • Server startup/shutdown                                                   │
│ • Handler chain assembly                                                    │
│ • Service integration                                                       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Handler Chain

The server assembles a handler chain from registered HK2 services, ranked by priority:

1. **Security Handler** - Authentication and authorization
2. **Session Handler** - HTTP session management  
3. **WebSocket Handler** - WebSocket upgrade handling
4. **GZIP Handler** - Response compression
5. **Servlet Context Handler** - Servlet and JAX-RS processing
6. **Custom Handlers** - Application-specific handlers

## Configuration Reference

### Basic Configuration

```yaml
jetty:
  server:
    enabled: true
    minThreads: 10
    maxThreads: 150
    outputBufferSize: 32768
    requestHeaderSize: 16384
    idleTimeout: "-1s"
```

### HTTP Configuration

```yaml
jetty:
  server:
    http:
      enabled: true
      port: 8080
```

### HTTPS/SSL Configuration

```yaml
jetty:
  server:
    ssl:
      enabled: true
      port: 8443
      keystore: "server-keystore"
      truststore: "server-truststore"
      alias: "server"
      needClientAuth: false
      wantClientAuth: true
      trustAll: false
      sniRequired: false
      sniHostCheck: false
```

### Security Configuration

```yaml
jetty:
  server:
    security:
      enabled: true
      type: "certificate" # or "basic", "digest", etc.
      constraints:
        - name: "admin"
          roles: ["admin", "operator"]
        - name: "user"
          roles: ["user"]
      mappings:
        - path: "/admin/*"
          constraint: "admin"
        - path: "/api/*"
          constraint: "user"
```

### WebSocket Configuration

```yaml
jetty:
  server:
    websockets:
      enabled: true
      autoFragment: true
      idleTimeout: "300s"
      inputBufferSize: 4096
      outputBufferSize: 4096
      maxFrameSize: 65536
      maxTextMessageSize: 1048576
      maxBinaryMessageSize: 1048576
```

### GZIP Configuration

```yaml
jetty:
  server:
    gzip:
      enabled: true
      minGzipSize: 32
      syncFlush: false
      excluded:
        mediatypes: ["image/png", "image/jpeg"]
        methods: ["HEAD"]
        paths: ["/api/binary/*"]
      included:
        mediatypes: ["text/html", "application/json"]
        methods: ["GET", "POST"]
        paths: ["/*"]
```

## Usage Guide

### Basic Server Setup

To use the Jetty server module in your Xorcery application:

1. **Add Dependency** (in `pom.xml`):
```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-jetty-server</artifactId>
</dependency>
```

2. **Configure** (in `xorcery.yaml`):
```yaml
jetty:
  server:
    enabled: true
    http:
      enabled: true
      port: 8080
```

3. **Module Declaration** (in `module-info.java`):
```java
module my.application {
    requires xorcery.jetty.server;
}
```

### Custom Handler Registration

To register custom handlers, create HK2 services:

```java
@Service
@Priority(10) // Lower numbers = higher priority
public class CustomHandlerFactory implements Factory<Handler> {
    
    @Override
    public Handler provide() {
        return new MyCustomHandler();
    }
    
    @Override
    public void dispose(Handler instance) {
        // Cleanup if needed
    }
}
```

### SSL/TLS Setup

For HTTPS support with custom certificates:

1. **Configure keystores** (managed by xorcery-keystores):
```yaml
keystores:
  server-keystore:
    path: "/path/to/keystore.p12"
    password: "${KEYSTORE_PASSWORD}"
    type: "PKCS12"
```

2. **Enable SSL**:
```yaml
jetty:
  server:
    ssl:
      enabled: true
      port: 8443
      keystore: "server-keystore"
      alias: "server-cert"
```

## API Reference

### Key Classes

#### JettyServerFactory
- **Purpose**: Creates and configures the main Jetty Server instance
- **Key Methods**: `provide()`, `dispose(Server)`
- **Configuration**: Uses `JettyServerConfiguration` and `JettyServerSslConfiguration`

#### JettyLifecycleService
- **Purpose**: Manages server lifecycle and handler chain assembly
- **RunLevel**: 18 (starts after core services)
- **Responsibilities**: Server startup/shutdown, handler registration

#### JettyServerConfiguration
- **Purpose**: Provides type-safe access to server configuration
- **Key Methods**:
    - `getMinThreads()`, `getMaxThreads()`
    - `getHttpPort()`, `isHttpEnabled()`
    - `getIdleTimeout()`, `getMediaTypes()`

#### JettyServerSslConfiguration
- **Purpose**: SSL/TLS specific configuration access
- **Key Methods**:
    - `isEnabled()`, `getPort()`
    - `getKeyStoreName()`, `getTrustStoreName()`
    - `isNeedClientAuth()`, `isWantClientAuth()`

### Configuration Schema

The module provides JSON Schema validation for configuration:
- **Schema File**: `META-INF/xorcery-schema.json`
- **Default Config**: `META-INF/xorcery.yaml`
- **Validation**: Automatic validation during application startup

## Security Features

### Client Certificate Authentication

The module supports mutual TLS authentication:

```java
@Service
public class CertificateLoginService implements LoginService {
    
    @Override
    public UserIdentity login(String username, Object credentials, ServletRequest request) {
        if (credentials instanceof CertificateCredential cert) {
            // Validate certificate and return user identity
            return createUserIdentity(cert.getCertificate());
        }
        return null;
    }
}
```

### Constraint-Based Security

Define security constraints declaratively:

```yaml
jetty:
  server:
    security:
      constraints:
        - name: "api-access"
          roles: ["api-user", "admin"]
      mappings:
        - path: "/api/v1/*"
          constraint: "api-access"
```

### Custom Authenticators

Implement custom authentication schemes:

```java
@Service
@Named("custom-auth")
public class CustomAuthenticatorFactory implements Factory<Authenticator> {
    
    @Override
    public Authenticator provide() {
        return new MyCustomAuthenticator();
    }
}
```

## Integration Examples

### JAX-RS Integration

When combined with `xorcery-jaxrs-server`:

```yaml
jetty:
  server:
    enabled: true
    servlet:
      enabled: true

jaxrs:
  server:
    enabled: true
    path: "/api/*"
```

### WebSocket Endpoints

Register WebSocket endpoints:

```java
@ServerEndpoint("/websocket")
public class MyWebSocketEndpoint {
    
    @OnOpen
    public void onOpen(Session session) {
        // Handle WebSocket connection
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        // Handle incoming message
    }
}
```

### Health Checks

Integration with monitoring:

```yaml
jetty:
  server:
    enabled: true
    
status:
  server:
    path: "/health"
    enabled: true
```

## Troubleshooting

### Common Issues

1. **Port Conflicts**
    - Check if ports are already in use
    - Verify firewall settings
    - Consider using different ports for different environments

2. **SSL Certificate Issues**
    - Verify keystore path and password
    - Check certificate validity and chain
    - Ensure correct alias configuration

3. **Handler Chain Problems**
    - Check service priorities and rankings
    - Verify HK2 service registration
    - Review handler compatibility

### Logging

Enable debug logging:

```yaml
logging:
  loggers:
    "dev.xorcery.jetty": DEBUG
    "org.eclipse.jetty": INFO
```

### Performance Tuning

For high-throughput scenarios:

```yaml
jetty:
  server:
    minThreads: 50
    maxThreads: 500
    outputBufferSize: 65536
    requestHeaderSize: 32768
    http:
      acceptors: 2
      selectors: 4
```

## Extension Points

### Custom Connection Factories

Implement custom protocols:

```java
@Service
@Named("my-protocol")
public class CustomConnectionFactoryFactory implements Factory<ConnectionFactory> {
    
    @Override
    public ConnectionFactory provide() {
        return new MyProtocolConnectionFactory();
    }
}
```

### Handler Wrappers

Create reusable handler decorators:

```java
@Service
@Priority(5)
public class LoggingHandlerFactory implements Factory<Handler> {
    
    @Override
    public Handler provide() {
        return new RequestLogHandler();
    }
}
```
