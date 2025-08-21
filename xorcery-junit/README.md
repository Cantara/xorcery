# Xorcery JUnit Module Documentation

## Overview

The `xorcery-junit` module provides a comprehensive JUnit 5 extension for testing Xorcery applications. It simplifies the setup and teardown of Xorcery instances in tests, provides dependency injection for test parameters, and includes pre-configured SSL settings for secure testing scenarios.

## Features

- **Automatic Xorcery Instance Management**: Creates and manages Xorcery instances for test execution
- **Fluent Configuration API**: Builder pattern for easy test configuration
- **Dependency Injection**: Automatic injection of services into test method parameters
- **Archive Support**: Automatic extraction of test data archives
- **Nested Test Support**: Optimized for nested test classes with shared instances
- **SSL/TLS Support**: Pre-configured keystores and truststore for secure testing
- **Configuration Override**: JSON schema-based configuration validation and override

## Maven Dependency

```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-junit</artifactId>
</dependency>
```

## Basic Usage

### Simple Test Setup

```java
@ExtendWith(XorceryExtension.class)
class MyServiceTest {
    
    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery().build();
    
    @Test
    void testService(MyService service) {
        // Service is automatically injected
        assertNotNull(service);
    }
}
```

### Builder Configuration

The `XorceryExtension` provides a fluent builder API for configuration:

```java
@RegisterExtension
static XorceryExtension xorcery = XorceryExtension.xorcery()
    .id("test-instance")
    .addYaml("""
        application:
          name: "test-app"
          version: "1.0.0"
        """)
    .with(new CustomService())
    .build();
```

### Archive-based Testing

For tests requiring pre-populated data:

```java
@RegisterExtension
static XorceryExtension xorcery = XorceryExtension.xorcery()
    .archive("test-data.zip", "test-data")
    .build();
```

## Advanced Features

### Nested Test Classes

The extension optimizes nested test execution by sharing the root Xorcery instance:

```java
@ExtendWith(XorceryExtension.class)
class IntegrationTest {
    
    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery().build();
    
    @Nested
    class UserServiceTests {
        @Test
        void testUserCreation(UserService userService) {
            // Uses the same Xorcery instance as parent
        }
    }
    
    @Nested
    class OrderServiceTests {
        @Test
        void testOrderProcessing(OrderService orderService) {
            // Also uses the same Xorcery instance
        }
    }
}
```

### Custom Service Registration

Register custom services or mocks for testing:

```java
@RegisterExtension
static XorceryExtension xorcery = XorceryExtension.xorcery()
    .with(Mockito.mock(ExternalService.class))
    .with(new TestDataService())
    .build();
```

### Configuration Override

Use the JSON schema to validate and override configuration:

```java
@RegisterExtension
static XorceryExtension xorcery = XorceryExtension.xorcery()
    .configuration(builder -> {
        builder.add("instance.environment", "test");
        builder.add("log4j2.Configuration.Loggers.Root.level", "debug");
    })
    .build();
```

## Archive Management

### Archive Behavior
- Archives are extracted to `target/{targetDir}` directory
- If archive already exists and is newer than the zip file, extraction is skipped
- Archive directories are preserved between tests for performance
- Only non-archive temporary directories are cleaned up after tests

### Archive Structure
```
src/test/resources/
├── test-data.zip
│   ├── config/
│   │   └── application.yaml
│   ├── data/
│   │   └── sample.json
│   └── ssl/
│       └── certificates.p12
```

## Error Handling

The extension includes comprehensive error handling:

- **Archive Errors**: Validates archive existence and extraction
- **Configuration Errors**: Provides detailed configuration validation messages
- **Service Injection Errors**: Clear error messages for missing dependencies
- **Test Execution Errors**: Preserves temp directories on test failures for debugging

## Integration with Other Xorcery Modules

### SSL/TLS Testing
Pre-configured with test certificates for HTTPS testing:

```java
@Test
void testHttpsEndpoint(WebClient webClient) {
    // WebClient is pre-configured with test SSL settings
    ResponseEntity<String> response = webClient.get()
        .uri("https://localhost:8443/api/health")
        .retrieve()
        .toEntity(String.class)
        .block();
}
```

### Configuration Testing
Access runtime configuration in tests:

```java
@Test
void testConfiguration(Configuration configuration) {
    String instanceId = configuration.getString("instance.id").orElse("default");
    assertEquals("test-instance", instanceId);
}
```

### Service Locator Access
Direct access to the HK2 ServiceLocator:

```java
@Test
void testServiceLookup(ServiceLocator serviceLocator) {
    MyService service = serviceLocator.getService(MyService.class);
    assertNotNull(service);
}
```

## Best Practices

1. **Use Static Extension Registration**: Register extensions as static fields for proper lifecycle management
2. **Minimize Instance Creation**: Use nested tests to share expensive Xorcery instances
3. **Clean Configuration**: Use YAML for readable test configuration
4. **Archive Reuse**: Structure test archives for reuse across multiple test classes
5. **Service Mocking**: Register mocks for external dependencies to ensure test isolation

## Troubleshooting

### Common Issues

**Service Not Found**
```java
// Problem: Service not available for injection
@Test
void testService(MyService service) { /* service is null */ }

// Solution: Ensure service is properly registered
@RegisterExtension
static XorceryExtension xorcery = XorceryExtension.xorcery()
    .with(new MyService()) // Register explicitly
    .build();
```

**Configuration Override Not Applied**
```java
// Problem: Configuration not taking effect
// Solution: Use proper configuration builder syntax
.configuration(builder -> {
    builder.add("my.config.key", "value"); // Correct
    // Not: builder.put() or builder.set()
})
```

**Archive Not Found**
```java
// Problem: Archive file not in classpath
.archive("missing-file.zip")

// Solution: Ensure file exists in src/test/resources
// and use correct path relative to classpath root
.archive("test-data/my-archive.zip")
```
