# Xorcery Jetty Client Extension

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Configuration](#configuration)
4. [Usage](#usage)
5. [Features](#features)
6. [API Reference](#api-reference)
7. [Dependencies](#dependencies)
8. [Testing](#testing)

## Overview

The `xorcery-jetty-client` module provides a comprehensive HTTP client solution built on Eclipse Jetty, integrated with the Xorcery framework. It offers enterprise-grade features including:

- Multiple named HTTP client instances
- HTTP/1.1 and HTTP/2 support
- SSL/TLS with certificate management
- Automatic OpenTelemetry tracing
- Custom DNS resolution
- HK2 dependency injection integration
- Configuration-driven setup with JSON Schema validation

**Module Coordinates:**
```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-jetty-client</artifactId>
    <version>0.164.6-SNAPSHOT</version>
</dependency>
```

## Architecture

### Core Components

```
┌─────────────────────────────────────────┐
│           HttpClientFactoryHK2          │
│         (HK2 Integration Layer)         │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│           HttpClientFactory             │
│         (Core Factory Logic)           │
└─────────────────┬───────────────────────┘
                  │
    ┌─────────────┴─────────────┐
    │                           │
┌───▼────────┐       ┌─────────▼────────┐
│ HttpClient │       │ HttpClient       │
│ (default)  │       │ (custom-name)    │
└────────────┘       └──────────────────┘
```

### Key Design Patterns

1. **Factory Pattern**: `HttpClientFactory` creates and manages client instances
2. **Configuration Object Pattern**: Type-safe configuration classes
3. **Builder Pattern**: Jetty's transport and connector builders
4. **Observer Pattern**: OpenTelemetry request listeners
5. **Provider Pattern**: HK2 integration for dependency injection

### Integration Points

- **DNS Resolution**: Custom `DnsLookupSocketAddressResolver`
- **SSL/TLS**: Integration with Xorcery keystore management
- **Observability**: Automatic OpenTelemetry tracing
- **Configuration**: YAML-based configuration with schema validation

## Configuration

### Basic Configuration

The module is configured through YAML files with the following structure:

```yaml
# META-INF/xorcery.yaml
jetty:
  clients:
    - name: "default"
      enabled: true
      connectTimeout: "5s"
      idleTimeout: "-1s"
      requestBufferSize: 4096
      reusePort: false
      
    - name: "custom"
      enabled: true
      connectTimeout: "20s"
      idleTimeout: "30s"
```

### SSL/TLS Configuration

```yaml
jetty:
  clients:
    - name: "secure-client"
      ssl:
        enabled: true
        keystore: "client-keystore"
        truststore: "trusted-cas"
        alias: "client-cert"
        endpointIdentificationAlgorithm: "HTTPS"
        trustAll: false
```

### HTTP/2 Configuration

```yaml
jetty:
  clients:
    - name: "http2-client"
      http2:
        enabled: true
        idleTimeout: "60s"
```

### Configuration Schema

The module provides JSON Schema validation located in `META-INF/xorcery-schema.json`. Key configuration properties:

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `name` | string | "default" | Client instance name |
| `enabled` | boolean | true | Whether the client is enabled |
| `connectTimeout` | string | "5s" | Connection timeout (ISO 8601 duration) |
| `idleTimeout` | string | "-1s" | Idle timeout (-1 = no timeout) |
| `requestBufferSize` | number | 4096 | Request buffer size in bytes |
| `reusePort` | boolean | false | Enable SO_REUSEPORT |

## Usage

### Dependency Injection with HK2

```java
@Service
public class MyService {
    
    @Inject
    public MyService(HttpClient httpClient) {
        // Uses the "default" client
        this.httpClient = httpClient;
    }
    
    @Inject
    public MyService(@Named("custom") HttpClient customClient) {
        // Uses a named client configuration
        this.customClient = customClient;
    }
}
```

### Direct Factory Usage

```java
// Create factory with dependencies
HttpClientFactory factory = new HttpClientFactory(
    jettyClientsConfiguration,
    applicationConfiguration,
    dnsLookupService,
    clientSslContextFactory,
    openTelemetry
);

// Get a configured client
HttpClient client = factory.newHttpClient("default");
```

### Making HTTP Requests

```java
@Service
public class ApiClient {
    
    private final HttpClient httpClient;
    
    @Inject
    public ApiClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    public CompletableFuture<String> getAsync(String url) {
        CompletableFuture<ContentResponse> future = new CompletableFuture<>();
        
        httpClient.newRequest(url)
            .method(HttpMethod.GET)
            .send(new CompletableFutureResponseListener(future));
            
        return future.thenApply(ContentResponse::getContentAsString);
    }
    
    public ContentResponse getSync(String url) throws Exception {
        return httpClient.newRequest(url)
            .method(HttpMethod.GET)
            .send();
    }
}
```

### Testing Configuration

For testing, use the provided test configuration:

```java
@RegisterExtension
static XorceryExtension xorcery = XorceryExtension.xorcery()
    .configuration(ConfigurationBuilder::addTestDefaults)
    .addYaml("""
        jetty.clients:
          - name: "test-client"
            connectTimeout: 10s
            idleTimeout: 5s
        """)
    .build();

@Test
public void testHttpClient(@Named("test-client") HttpClient client) {
    // Test with custom configuration
    assertEquals(10000, client.getConnectTimeout());
    assertEquals(5000, client.getIdleTimeout());
}
```

## Features

### 1. Multiple Client Configurations

Configure different HTTP clients for different purposes:
- Default client for general use
- Secure client with mutual TLS
- Fast client with reduced timeouts
- HTTP/2 client for modern APIs

### 2. SSL/TLS Security

- **Keystore Integration**: Automatic certificate loading from Xorcery keystores
- **Mutual TLS**: Client certificate authentication
- **Trust Management**: Configurable truststore and trust-all options
- **Certificate Reloading**: Automatic reload when certificates are updated

### 3. Observability

- **Request Tracing**: Automatic OpenTelemetry span creation for all requests
- **Metrics Collection**: Built-in HTTP client metrics
- **Context Propagation**: Trace context forwarded to downstream services
- **Error Tracking**: Automatic span status and error recording

### 4. Advanced Networking

- **DNS Integration**: Custom DNS resolution with service discovery
- **Connection Pooling**: Efficient connection reuse
- **HTTP/2 Support**: Modern protocol with multiplexing
- **Load Balancing**: Random distribution across resolved IPs

### 5. Thread Management

Custom thread pool (`JettyClientConnectorThreadPool`) with:
- OpenTelemetry context propagation
- Proper exception handling and logging
- Named threads for debugging

## API Reference

### HttpClientFactory

```java
public class HttpClientFactory implements AutoCloseable {
    public HttpClient newHttpClient(String name);
    public void close() throws Exception;
}
```

### Configuration Classes

```java
public record JettyClientConfiguration(Configuration context) {
    public String getName();
    public Duration getIdleTimeout();
    public Duration getConnectTimeout();
    public boolean getReusePort();
    public int getRequestBufferSize();
    public JettyHttp2Configuration getHTTP2Configuration();
    public JettyClientSslConfiguration getSSLConfiguration();
}
```

### SSL Configuration

```java
public record JettyClientSslConfiguration(Configuration configuration) {
    public boolean isEnabled();
    public Optional<String> getKeyStoreName();
    public Optional<String> getTrustStoreName();
    public String getAlias();
    public String getEndpointIdentificationAlgorithm();
    public boolean isTrustAll();
}
```

### Utilities

```java
public class CompletableFutureResponseListener extends BufferingResponseListener {
    public CompletableFutureResponseListener(CompletableFuture<ContentResponse> future);
    public CompletableFutureResponseListener(CompletableFuture<ContentResponse> future, int maxLength);
}
```

## Dependencies

### Required Dependencies

- **xorcery-dns-client**: Custom DNS resolution
- **xorcery-keystores**: Certificate management
- **xorcery-opentelemetry-api**: Observability integration
- **org.eclipse.jetty.client**: Core HTTP client functionality
- **org.eclipse.jetty.http2.client**: HTTP/2 support

### Optional Dependencies

- **dev.xorcery.secrets**: Secret management for passwords
- **org.glassfish.hk2**: Dependency injection (for HK2 integration)

### Module Declaration

```java
module xorcery.jetty.client {
    exports dev.xorcery.jetty.client;
    exports dev.xorcery.jetty.client.providers;

    requires xorcery.configuration.api;
    requires xorcery.secrets.api;
    requires xorcery.keystores;
    requires xorcery.dns.client;
    requires transitive org.eclipse.jetty.client;
    requires org.apache.logging.log4j;
    requires org.eclipse.jetty.http2.client;
    requires org.eclipse.jetty.http2.client.transport;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires io.opentelemetry.api;
    requires io.opentelemetry.context;
    requires io.opentelemetry.semconv;
}
```

## Testing

### Unit Testing

The module provides comprehensive test support:

```java
public class HttpClientTest {
    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
        .configuration(ConfigurationBuilder::addTestDefaults)
        .addYaml("""
            jetty.clients:
              - name: "default"
                idleTimeout: 5s
              - name: "custom"
                connectTimeout: 20s
            """)
        .build();

    @Test
    public void testDefaultHttpClient(HttpClient httpClient) {
        assertEquals(5000, httpClient.getConnectTimeout());
        assertEquals(5000, httpClient.getIdleTimeout());
    }

    @Test
    public void testCustomHttpClient(@Instance("custom") HttpClient httpClient) {
        assertEquals(20000, httpClient.getConnectTimeout());
        assertEquals(5000, httpClient.getIdleTimeout());
    }
}
```

### Test Configuration

Use `xorcery-test.yaml` for test-specific settings:

```yaml
$schema: xorcery-schema.json
jetty:
  clients:
    - name: "default"
      enabled: true
```

### Integration Testing

The module supports integration testing with real HTTP endpoints:

```java
@Test
public void testRealHttpRequest(HttpClient httpClient) throws Exception {
    ContentResponse response = httpClient
        .newRequest("https://httpbin.org/get")
        .timeout(10, TimeUnit.SECONDS)
        .send();
    
    assertEquals(200, response.getStatus());
    assertTrue(response.getContentAsString().contains("httpbin.org"));
}
```
