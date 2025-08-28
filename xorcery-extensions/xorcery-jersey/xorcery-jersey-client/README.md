# Xorcery Jersey Client Extension

## Overview

The `xorcery-jersey-client` module provides a comprehensive Jersey client integration for the Xorcery framework. It offers advanced features including DNS SRV record support for service discovery, SSL/TLS configuration, and seamless integration with Xorcery's HTTP client infrastructure.

### Key Features

- **Multi-Client Support**: Configure multiple named Jersey clients with different settings
- **DNS SRV Integration**: Automatic service discovery using DNS SRV records with failover support
- **SSL/TLS Support**: Full SSL configuration with keystore and truststore management
- **HTTP/2 Support**: Integration with Jetty's HTTP/2 client capabilities
- **Configurable Timeouts**: Separate connect and read timeout configurations
- **HK2 Integration**: Native dependency injection support
- **Comprehensive Logging**: Built-in request/response logging with configurable levels

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                 Jersey Client Module                        │
├─────────────────────────────────────────────────────────────┤
│  ClientBuilderFactory                                       │
│  ├── JerseyClientConfiguration                              │
│  ├── SSL/TLS Configuration                                  │
│  ├── Timeout Configuration                                  │
│  └── HTTP Client Integration                                │
├─────────────────────────────────────────────────────────────┤
│  SRV Connector (DNS Service Discovery)                     │
│  ├── SRVConnectorProvider                                   │
│  ├── SRVConnector                                          │
│  └── Automatic Failover                                     │
├─────────────────────────────────────────────────────────────┤
│  Integration Layer                                          │
│  ├── Jetty HTTP Client                                     │
│  ├── DNS Lookup Service                                    │
│  ├── KeyStores Service                                     │
│  └── Secrets Management                                    │
└─────────────────────────────────────────────────────────────┘
```

## Configuration Reference

### Basic Configuration

```yaml
jersey:
  clients:
    - name: "default"
      enabled: true
      connectTimeout: "5s"
      readTimeout: "30s"
      httpClient: "default"
      properties: {}
```

### Advanced Configuration

```yaml
jersey:
  clients:
    - name: "api-client"
      enabled: true
      keystore: "client-keystore"      # SSL client certificate
      truststore: "ca-truststore"      # Trusted CA certificates
      connectTimeout: "10s"
      readTimeout: "60s"
      httpClient: "high-performance"   # Named HTTP client configuration
      properties:
        jersey.config.client.connectTimeout: "10000"
        jersey.config.client.readTimeout: "60000"
        
    - name: "internal-service"
      enabled: true
      connectTimeout: "3s"
      readTimeout: "15s"
      httpClient: "internal"
```

### Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `name` | String | "default" | Unique identifier for the client configuration |
| `enabled` | Boolean | true | Whether this client configuration is active |
| `keystore` | String | null | Name of keystore containing client certificates |
| `truststore` | String | null | Name of truststore containing trusted CA certificates |
| `connectTimeout` | Duration | "5s" | Maximum time to establish a connection |
| `readTimeout` | Duration | "30s" | Maximum time to wait for response data |
| `httpClient` | String | "default" | Name of the underlying HTTP client configuration |
| `properties` | Object | {} | Additional Jersey client properties |

## API Reference

### ClientBuilderFactory

The main factory class for creating configured Jersey `ClientBuilder` instances.

```java
@Service
public class ClientBuilderFactory implements Factory<ClientBuilder> {
    @Override
    @PerLookup
    public ClientBuilder provide() {
        // Creates a fully configured ClientBuilder
    }
}
```

**Key Features:**
- Automatic SSL/TLS configuration based on keystore/truststore settings
- DNS SRV connector integration for service discovery
- Timeout configuration
- Logging feature registration
- HTTP client integration

### JerseyClientConfiguration

Configuration record for individual Jersey clients.

```java
public record JerseyClientConfiguration(Configuration configuration) {
    public String getName();
    public Optional<JsonNode> getProperties();
    public Duration getConnectTimeout();
    public Duration getReadTimeout();
    public Optional<String> getKeyStoreName();
    public Optional<String> getTrustStoreName();
    public String getHttpClient();
}
```

### SRVConnector

Enables DNS SRV record-based service discovery with automatic failover.

```java
public class SRVConnector implements Connector {
    // Resolves srv:// URLs to actual HTTP/HTTPS endpoints
    // Provides automatic failover between resolved servers
}
```

## Usage Examples

### Basic Client Usage

```java
@Inject
private ClientBuilder clientBuilder;

public void makeRequest() {
    Client client = clientBuilder.build();
    try {
        String response = client
            .target("https://api.example.com/data")
            .request(MediaType.APPLICATION_JSON)
            .get(String.class);
        
        // Process response
    } finally {
        client.close();
    }
}
```

### Named Client Configuration

```java
@Named("api-client")
@Inject
private ClientBuilder apiClientBuilder;

@Named("internal-service")
@Inject  
private ClientBuilder internalClientBuilder;
```

### DNS SRV Service Discovery

```java
// Use srv:// scheme for automatic service discovery
Client client = clientBuilder.build();
String response = client
    .target("srv://api-service.company.com/endpoint")
    .request()
    .get(String.class);
```

The SRV connector will:
1. Resolve `_api-service._tcp.company.com` SRV record
2. Extract available servers with priorities and weights
3. Attempt connection to servers in priority order
4. Automatically failover on connection failures

### SSL Client Authentication

```yaml
# Configuration
jersey:
  clients:
    - name: "secure-client"
      keystore: "client-certs"
      truststore: "ca-bundle"
      
keystores:
  stores:
    - name: "client-certs"
      path: "client.p12"
      password: "secret:client-cert-password"
    - name: "ca-bundle"  
      path: "truststore.jks"
      password: "secret:truststore-password"
```

```java
@Named("secure-client")
@Inject
private ClientBuilder secureClientBuilder;

public void makeSecureRequest() {
    Client client = secureClientBuilder.build();
    // Client automatically uses configured certificates
    String response = client
        .target("https://secure-api.example.com/data")
        .request()
        .get(String.class);
}
```

## Advanced Features

### Custom Properties

You can pass additional Jersey client properties through configuration:

```yaml
jersey:
  clients:
    - name: "custom-client"
      properties:
        jersey.config.client.connectTimeout: "15000"
        jersey.config.client.readTimeout: "45000"
        jersey.config.client.followRedirects: "true"
```

### HTTP Client Integration

The module integrates with Xorcery's HTTP client infrastructure:

```yaml
# HTTP client configuration
jetty:
  clients:
    - name: "high-performance"
      idleTimeout: "60s"
      connectTimeout: "5s"
      http2:
        enabled: true

# Jersey client using the HTTP client
jersey:
  clients:
    - name: "api-client"
      httpClient: "high-performance"
```

### Logging Configuration

Jersey client requests/responses are automatically logged. Configure logging levels:

```yaml
log4j2:
  Configuration:
    Loggers:
      logger:
        - name: "jersey.client.api-client"
          level: "DEBUG"
```

## Integration with Other Modules

### Dependencies

The module integrates with several other Xorcery modules:

- **xorcery-configuration-api**: Configuration management
- **xorcery-jetty-client**: HTTP client infrastructure
- **xorcery-keystores**: SSL certificate management
- **xorcery-dns-client**: DNS resolution and SRV record support
- **xorcery-secrets-api**: Secure credential management

### Module Definition

```java
module xorcery.jersey.client {
    exports dev.xorcery.jersey.client;
    exports dev.xorcery.jersey.client.providers;
    
    requires xorcery.configuration.api;
    requires xorcery.keystores;
    requires xorcery.dns.client;
    requires xorcery.jetty.client;
    // ... other requirements
}
```

## Error Handling

The module provides robust error handling:

1. **Connection Failures**: Automatic retry with SRV failover
2. **SSL Errors**: Clear error messages for certificate issues
3. **Timeout Handling**: Separate connect and read timeout controls
4. **Configuration Errors**: Validation with helpful error messages

### Common Error Scenarios

```java
// Handle connection failures with SRV failover
try {
    String response = client
        .target("srv://unreliable-service.com/api")
        .request()
        .get(String.class);
} catch (ProcessingException e) {
    // All SRV servers failed
    logger.error("Service unavailable: {}", e.getMessage());
}

// Handle SSL certificate issues
try {
    client.target("https://secure-api.com").request().get();
} catch (SSLException e) {
    logger.error("SSL handshake failed: {}", e.getMessage());
}
```

## Best Practices

1. **Resource Management**: Always close clients when done
2. **Named Configurations**: Use named clients for different services
3. **Timeout Configuration**: Set appropriate timeouts for your use case
4. **SSL Configuration**: Use proper certificate validation in production
5. **Logging**: Configure appropriate log levels for debugging vs. production
6. **SRV Records**: Use DNS SRV for dynamic service discovery
7. **Error Handling**: Implement proper retry logic and circuit breakers

## Troubleshooting

### Common Issues

1. **"No config found for Jersey client"**: Ensure client name matches configuration
2. **SSL handshake failures**: Check keystore/truststore configuration and certificates
3. **DNS resolution failures**: Verify DNS server configuration and SRV records
4. **Connection timeouts**: Adjust timeout settings or check network connectivity

### Debug Configuration

```yaml
log4j2:
  Configuration:
    Loggers:
      logger:
        - name: "dev.xorcery.jersey.client"
          level: "DEBUG"
        - name: "jersey.client"
          level: "DEBUG"
        - name: "org.eclipse.jetty.client"
          level: "DEBUG"
```
```

The markdown documentation is now ready for export. You can copy this content and save it as a `.md` file for your project documentation.