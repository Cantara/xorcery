# Xorcery Configuration Modules

## Overview

The Xorcery Configuration system is a powerful, extensible configuration management framework built on top of Jackson JSON processing. It provides a hierarchical, type-safe configuration system with support for multiple sources, formats, validation, and internationalization.

The configuration system is composed of three main modules:
```
xorcery-configuration-api     # Core interfaces and data structures
xorcery-configuration         # Implementation and providers  
xorcery-configuration-validation # JSON Schema validation
```

## Module Structure

### xorcery-configuration-api

**Purpose**: Defines core configuration interfaces and data structures.

**Key Components**:
- `Configuration` - Core configuration record and builder
- `InstanceConfiguration` - Instance-specific configuration wrapper
- `ApplicationConfiguration` - Application metadata configuration
- `ServiceConfiguration` - Service enablement interface
- `ResourceBundles` - Internationalization support

### xorcery-configuration

**Purpose**: Provides configuration loading, building, and provider implementations.

**Key Components**:
- `ConfigurationBuilder` - Fluent configuration building with multiple sources
- `ConfigurationProvider` SPI - Extensible provider system
- Built-in providers for system properties, environment variables, calculated values
- Resource bundle integration and translation support

**Dependencies**: Extends configuration-api with additional Jackson modules and logging.

### xorcery-configuration-validation

**Purpose**: JSON Schema validation for configurations.

**Key Components**:
- `ConfigurationValidator` - Schema validation using NetworkNT library
- `StartupConfigurationValidation` - Automatic validation at application startup

## Core Components

### Configuration Class

The `Configuration` record is the heart of the system:

```java
public record Configuration(ObjectNode json) implements JsonElement {
    // Immutable configuration wrapper
    // Fluent builder pattern
    // YAML serialization support
    // Resource URL resolution
}
```

**Key Features**:
- **Immutable**: Thread-safe configuration objects
- **Type-safe**: Strongly typed accessors with Optional returns
- **Hierarchical**: Nested configuration support via dot notation
- **Format-agnostic**: Works with JSON, YAML, Properties

### Configuration Builder

The builder supports hierarchical configuration loading:

```java
Configuration config = new ConfigurationBuilder()
    .addDefaults()              // Load default configuration hierarchy
    .addTestDefaults()          // Load test-specific defaults
    .addYaml(yamlString)        // Add YAML configuration
    .addFile(configFile)        // Add file-based configuration
    .build();
```

**Loading Hierarchy** (in precedence order):
1. Module configurations (`META-INF/xorcery.yaml`)
2. Module overrides (`META-INF/xorcery-override.yaml`)
3. Application configuration (`xorcery.yaml`)
4. System-specific overrides (`/etc/<application-name>/<application-name>.yaml`)
5. User home overrides (`~/.xorcery/xorcery.yaml`)
6. Working directory overrides (`./xorcery.yaml`)
7. Environment variable overrides (`XORCERY_*`)
8. Configuration providers (dynamic values)

### Configuration Providers

The provider system allows dynamic configuration values:

```java
public interface ConfigurationProvider {
    String getNamespace();
    JsonNode getJson(String name);
}
```

**Built-in Providers**:

- **SystemPropertiesConfigurationProvider** (`SYSTEM`): Java system properties
- **EnvironmentVariablesConfigurationProvider** (`ENV`): Environment variables
- **CalculatedConfigurationProvider** (`CALCULATED`): Computed values (hostname, IP, ports)
- **ResourceConfigurationProvider** (`RESOURCE`): Classpath resources

**Usage Example**:
```yaml
# Reference calculated values
instance:
  host: "{{ CALCULATED.hostName }}"
  ip: "{{ CALCULATED.ip }}"
  
# Reference environment variables  
database:
  url: "{{ ENV.DATABASE_URL }}"
  
# Reference system properties
app:
  version: "{{ SYSTEM.app_version }}"
```

### Specialized Configuration Wrappers

#### InstanceConfiguration
```java
public record InstanceConfiguration(Configuration configuration) {
    public String getId();
    public String getHost();
    public InetAddress getIp();
    public String getDomain();
    public String getEnvironment();
    public List<String> getTags();
    public URI getURI();
    public URI getAPI();
}
```

#### ApplicationConfiguration
```java
public record ApplicationConfiguration(Configuration configuration) {
    public String getName();
    public String getVersion();
    public List<String> getVersionPackages();
}
```

#### ServiceConfiguration
```java
public interface ServiceConfiguration extends WithContext<Configuration> {
    default boolean isEnabled() {
        return context().getBoolean("enabled").orElse(false);
    }
}
```

### Resource Bundle Integration

Supports internationalization with configuration-based resource bundles:

```java
ResourceBundles bundle = ResourceBundles.getBundle("mymodule", locale);
String localizedMessage = bundle.getString("welcome.message");
```

**Features**:
- Hierarchical locale resolution (language-country → language → default)
- Async translation provider support
- Configuration file-based bundles

## Configuration Loading Process

### 1. Module Configuration Loading
```
META-INF/xorcery.yaml          # Module defaults
META-INF/xorcery-override.yaml # Module overrides  
META-INF/xorcery-test.yaml     # Test-specific module configs
```

### 2. Application Configuration Loading
```
xorcery.yaml                   # Main application config
xorcery-test.yaml             # Test-specific application config
```

### 3. Environment Overrides
```
/etc/appname/appname.yaml     # System-wide config
~/.xorcery/xorcery.yaml       # User-specific config
./xorcery.yaml                # Working directory config
```

### 4. Environment Variables
Environment variables with `XORCERY_` prefix override configuration:
```bash
XORCERY_SERVER_PORT=8080      # Overrides server.port
XORCERY_DATABASE_URL=...      # Overrides database.url
```

### 5. Configuration Providers
Dynamic values resolved at runtime through provider system.

## Validation System

### JSON Schema Validation

Configure schema validation:
```yaml
$schema: "xorcery-schema.json"
```

The validator will:
1. Resolve schema from classpath
2. Validate configuration against schema
3. Report detailed validation errors
4. Fail application startup on validation errors

### Startup Validation
```java
@Service(name="configuration.validation")
@RunLevel(0)
public class StartupConfigurationValidation {
    // Automatic validation at application startup
}
```

## Usage Examples

### Basic Configuration Usage
```java
// Load default configuration
Configuration config = new ConfigurationBuilder()
    .addDefaults()
    .build();

// Access values with type safety
String serverHost = config.getString("server.host").orElse("localhost");
int serverPort = config.getInt("server.port").orElse(8080);
boolean enabled = config.getBoolean("features.enabled").orElse(false);

// Access nested configurations
Configuration dbConfig = config.getConfiguration("database");
String dbUrl = dbConfig.getString("url").orElseThrow();
```

### Custom Configuration Provider
```java
public class FooConfigurationProvider implements ConfigurationProvider {
    
    @Override
    public String getNamespace() {
        return "FOO";
    }
    
    @Override
    public JsonNode getJson(String name) {
        // Fetch or compute configuration from Foo
        return consulClient.getValue(name);
    }
}
```
Register the provider using the "provides" keyword in the module-info.java file.
### Service Configuration
```java
public record MyServiceConfiguration(Configuration configuration) 
    implements ServiceConfiguration {
    
    public static MyServiceConfiguration get(Configuration config) {
        return new MyServiceConfiguration(config.getConfiguration("myservice"));
    }
    
    public Duration getTimeout() {
        return configuration.getDuration("timeout").orElse(Duration.ofSeconds(30));
    }
    
    public int getMaxConnections() {
        return configuration.getInt("maxConnections").orElse(100);
    }
}

// Usage
@Service
public class MyService {
    private final MyServiceConfiguration config;
    
    @Inject
    public MyService(Configuration configuration) {
        this.config = MyServiceConfiguration.get(configuration);
        if (!config.isEnabled()) {
            throw new IllegalStateException("Service not enabled");
        }
    }
}
```
If you name the service, e.g. @Service(name="foo"), then Xorcery will automatically use the configuration setting foo.enabled to determine
whether to enable and start the service or not. This provides an easy way to implement feature-flags for the services in the application.

### Test Configuration
```java
@Test
void testWithConfiguration() {
    Configuration config = new ConfigurationBuilder()
        .addTestDefaults()
        .addYaml("""
            server:
              port: 0  # Use random port for testing
            database:
              url: "jdbc:h2:mem:test"
            """)
        .build();
        
    // Use config in test...
}
```

## Integration Guide

### Maven Dependencies

For basic configuration support:
```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-configuration</artifactId>
    <version>${xorcery.version}</version>
</dependency>
```

For validation support:
```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-configuration-validation</artifactId>
    <version>${xorcery.version}</version>
</dependency>
```

### Module Declaration
```java
module my.application {
    requires xorcery.configuration.api;
    requires xorcery.configuration;
    
    // Optional validation
    requires xorcery.configuration.validation;
    
    // If providing custom configuration providers
    provides dev.xorcery.configuration.spi.ConfigurationProvider 
        with my.application.MyConfigurationProvider;
}
```

### HK2 Integration
```java
public class ApplicationBinder extends AbstractBinder {
    @Override
    protected void configure() {
        Configuration configuration = new ConfigurationBuilder()
            .addDefaults()
            .build();
            
        bind(configuration).to(Configuration.class);
    }
}
```

## Best Practices

### 1. Configuration Structure
- Use hierarchical structure with meaningful namespaces
- Group related configurations together
- Use consistent naming conventions (camelCase recommended)

```yaml
# Good structure
server:
  http:
    port: 8080
    host: "localhost"
  ssl:
    enabled: false
    keystore: "path/to/keystore"

database:
  primary:
    url: "jdbc:postgresql://localhost/app"
    maxConnections: 10
  cache:
    enabled: true
    ttl: "PT5M"
```

### 2. Environment-Specific Configuration
- Keep environment-specific values in separate files or inject using environment variables
- Use configuration providers to reference secrets
- Validate required configuration at startup

### 3. Schema Validation
- Define JSON schemas for your configuration using the Maven plugin
- Use validation in development and testing
- Provide clear error messages for invalid configuration
- IDEs like IntelliJ support auto-complete for easy YAML-file configuration editing

### 4. Type Safety
- Create strongly-typed configuration wrappers
- Use Optional for optional values with sensible defaults
- Validate configuration values in constructors

### 5. Performance Considerations
- Configuration objects are immutable - cache them if needed
- Use lazy evaluation for expensive computed values
- Consider using singleton pattern for application-wide configuration

### 6. Testing
- Use `addTestDefaults()` for test-specific configuration
- Override specific values needed for tests
- Use random ports and temporary directories in tests

### Configuration Templating
The system supports basic templating and dynamic value resolving through configuration providers:
```yaml
database:
  url: "jdbc:postgresql://{{ ENV.DB_HOST }}/{{ ENV.DB_NAME }}"
  username: "{{ ENV.DB_USER }}"
  password: "{{ ENV.DB_PASSWORD }}"
```

This comprehensive configuration system provides a robust foundation for building configurable, maintainable applications with Xorcery.
