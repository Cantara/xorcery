# Xorcery Core Module Technical Documentation

## Overview

The `xorcery-core` module is the foundational component of the Xorcery framework, providing the bootstrap mechanism, dependency injection infrastructure, and service lifecycle management. It serves as the entry point for all Xorcery applications and orchestrates the initialization and shutdown of the entire system.

## Architecture

### Core Design Principles

- **Dependency Injection**: Built using HK2 for robust service management
- **Configuration-Driven**: Services are enabled/disabled and configured through external configuration
- **Run-Level Management**: Ordered service startup/shutdown using HK2's run-level system
- **Modular Design**: Clean separation between core functionality, HK2 integration, and logging

### Key Architectural Decisions

1. **HK2 as DI Container**: Chosen for its mature run-level support and Jakarta compatibility
2. **Configuration-First Approach**: All services can be controlled via configuration without code changes
3. **Fail-Fast Initialization**: Startup errors are immediately visible with detailed diagnostics
4. **Graceful Shutdown**: Proper cleanup in reverse run-level order

## Key Components

### Xorcery Class

The main entry point and orchestrator of the framework.

**Responsibilities:**
- Bootstrap the HK2 service locator
- Process configuration and apply system properties
- Manage service lifecycle through run levels (0-20)
- Provide graceful shutdown capabilities
- Handle startup errors with detailed diagnostics

**Run Level Definitions:**
- **0**: Configuration refresh
- **2**: Certificate request/refresh
- **4**: Servers and Clients
- **6**: Server publishers/subscribers
- **8**: Client publishers/subscribers
- **18**: Server start/stop
- **20**: DNS registration/deregistration

**Key Features:**
```java
// Basic usage
Configuration config = new ConfigurationBuilder().addDefaults().build();
try (Xorcery xorcery = new Xorcery(config)) {
    ServiceLocator serviceLocator = xorcery.getServiceLocator();
    // Application logic
}
```
If you are implementing applications running as servers this is typically done in a Main class with the Xorcery.close() call
being executed by a shutdown-hook.
### HK2 Integration Components

#### ConfigurationPostPopulatorProcessor
Filters HK2 service descriptors based on configuration settings.

**Features:**
- Enables/disables services via `servicename.enabled` configuration
- Adds configuration metadata to service descriptors
- Supports default enablement policies
- Provides detailed logging of service activation decisions

#### Hk2Configuration
Configuration wrapper providing type-safe access to HK2 settings.

**Configurable Options:**
- **Threading Policy**: Controls how run-level transitions are threaded
- **Thread Count**: Maximum threads for run-level operations
- **Run Level**: Target run level to reach
- **Immediate Scope**: Controls HK2's immediate scope behavior
- **Thread Scope**: Enables per-thread scoping

#### RunLevelLogger
Provides detailed logging of run-level transitions and errors.

**Logged Events:**
- Run level progress notifications
- Run level cancellations
- Startup/shutdown errors with service context

#### UniqueDescriptorFileFinder
Ensures HK2 descriptor files are not processed multiple times.

**Purpose:**
- Wraps the classpath descriptor finder
- Deduplicates descriptor files that appear multiple times
- Prevents duplicate service registrations

#### XorceryDefaultTopicDistributionErrorService
Handles errors in HK2's topic distribution system.

**Features:**
- Logs message delivery failures
- Integrates with the application's logging system
- Provides context about failed topics and messages

### Log4j Integration

#### LoggerFactory
HK2-aware factory for Log4j loggers.

**Features:**
- Automatically injects appropriate loggers based on injection context
- Uses the injecting class name for logger naming
- Integrates with custom LoggerContext if provided
- Fallback to default "xorcery" logger

## Service Lifecycle Management

### Configuration-Based Service Control

Services are controlled through configuration:

```yaml
# Enable/disable specific services
myservice.enabled: true
anotherservice.enabled: false

# Global default for all services
defaults.enabled: true

# Service-specific metadata
myservice:
  enabled: "{{ defaults.enabled }}" # Delegate to default setting
  metadata:
    priority: "high"
    category: "core"
```

### Service Metadata Injection

The framework supports injecting additional metadata into service descriptors:

```yaml
myservice:
  metadata:
    customProperty: "value"
    tags: ["tag1", "tag2"]
```

## Configuration Schema

The module defines a comprehensive JSON schema for configuration validation, which can be overriden by your
own module or application configuration.

### Core Configuration Sections

- **instance**: Instance-specific settings (id, host, domain, environment)
- **application**: Application metadata (name, version)
- **system**: System properties to set
- **defaults**: Default values for all services
- **hk2**: HK2-specific configuration

### Example Configuration

```yaml
instance:
  id: "myapp.production"
  environment: "production"
  domain: "example.com"

application:
  name: "MyApplication" 
  version: "1.0.0"

hk2:
  threadPolicy: "FULLY_THREADED"
  threadCount: 5
  runLevel: 20

defaults:
  enabled: true
```

## Usage Examples

### Basic Application Bootstrap

```java
public class MyApplication {
    public static void main(String[] args) throws Exception {
        Configuration config = new ConfigurationBuilder()
            .addDefaults()
            .addYaml("application.yaml")
            .build();
            
        try (Xorcery xorcery = new Xorcery(config)) {
            // Application runs until shutdown
            xorcery.getClosed().get();
        }
    }
}
```

### Service Implementation

```java
@Service
@RunLevel(10)  // Start at run level 10
public class MyService implements PreDestroy {
    
    private Logger logger;
    private MyServiceConfiguration configuration;
    
    @Inject
    public Myservice(Configuration configuration, Logger logger) {
        this.logger = logger;
        this.configuration = MyServiceConfiguration.get(configuration);
        logger.info("Starting MyService");
    }
    
    public void preDestroy() {
        logger.info("Stopping MyService");
    }
}
```

### Configuration-Based Service Control

```java
// Service will only be created if myservice.enabled=true
@Service(name = "myservice")
public class ConditionalService {
    // Service implementation
}
```
This can be very useful during development, as you can turn off the services you are not working on. 
In tests you can use the real services instead of mocks when it makes sense, but only enable the ones needed for a particular test.
This can drastically speed up the cycle time of making changes and running tests or restarting the application in large codebases.

## Extension Points

### Custom Run Level Listeners

Monitor run level transitions:

```java
@Service
public class CustomRunLevelListener implements RunLevelListener {
    @Override
    public void onProgress(ChangeableRunLevelFuture currentJob, int levelAchieved) {
        // Handle run level progress
    }
}
```

## Error Handling and Diagnostics

### Startup Error Diagnostics
- Detailed service listing on startup failures
- Configuration validation errors
- Run-level transition error reporting

### Logging Integration
- Automatic logger injection based on service class
- Run-level progress logging
- Service enablement/disablement logging

### Graceful Shutdown
- Reverse run-level shutdown sequence
- Resource cleanup handling
- Shutdown completion signaling via CompletableFuture

## Best Practices

1. **Use Run Levels Appropriately**: Place services at appropriate run levels based on dependencies
2. **Configure Don't Code**: Use configuration to control service behavior rather than hard-coding
3. **Implement Proper Cleanup**: Use `PreDestroy` for resource cleanup
4. **Monitor Run Levels**: Use logging to track service startup/shutdown
5. **Handle Errors Gracefully**: Implement proper error handling in service lifecycle methods

## Integration with Other Xorcery Modules

The `xorcery-core` module provides the foundation for all other Xorcery modules:

- **Extensions**: Use the HK2 service locator for dependency injection
- **Servers**: Start at appropriate run levels (typically 18)
- **Clients**: Initialize at run level 4
- **Configuration**: Integrates seamlessly with the configuration system
- **Logging**: Provides consistent logging across all modules
