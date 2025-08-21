# Xorcery Runner Module

## Overview

The `xorcery-runner` module provides a command-line application framework for running Xorcery-based applications. It offers a standardized way to bootstrap, configure, and manage the lifecycle of Xorcery applications with built-in support for configuration management, graceful shutdown, and extensibility.

## Architecture

### Core Components

```
xorcery-runner/
├── Main.java                    # Primary CLI entry point
├── BaseMain.java               # Extensible base runner class  
├── ConfigurationVersionProvider.java  # Version provider for CLI
```

### Component Relationships

- **Main**: Extends `BaseMain` and provides PicoCLI command-line interface
- **BaseMain**: Core application lifecycle manager, designed for extension
- **ConfigurationVersionProvider**: Integrates with PicoCLI for version display
- **Configuration**: Managed through [xorcery-configuration](../xorcery-configuration/README.md) module integration

## Configuration

### Default Configuration

```yaml
runner:
  keepRunning: true
```

### Configuration Options

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `runner.keepRunning` | boolean | true | Controls whether the application stays running until explicitly shutdown |

## Usage Examples

### Basic Usage

```bash
# Run with default configuration
java -jar xorcery-app.jar

# Run with custom configuration file
java -jar xorcery-app.jar config.yaml

# Display version information
java -jar xorcery-app.jar --version
```

### Extending BaseMain

Create a custom runner by extending `BaseMain`:

```java
@CommandLine.Command(name = "myapp")
public class MyAppMain extends BaseMain {
    
    @CommandLine.Option(names = "--debug")
    private boolean debug;
    
    @Override
    public Integer call() throws Exception {
        // Add more configuration overrides here
        setConfiguration(new ConfigurationBuilder().addDefaults().build());
        
        // Call parent implementation
        return super.call();
    }
    
    public static void main(String[] args) {
        System.exit(new CommandLine(new MyAppMain()).execute(args));
    }
}
```
Specify this as the Main class in your jar file when building, and package however you like.

## Maven Dependency

```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-runner</artifactId>
</dependency>
```

## Best Practices

### 1. Custom Runner Implementation
- Always extend `BaseMain` for custom functionality
- Call `super.call()` to maintain proper lifecycle management
- Use PicoCLI annotations for additional command-line options

### 2. Configuration Management
- Leverage the configuration builder pattern
- Validate configuration before setting on runner
- Use configuration logging for debugging

### 3. Error Handling
- Handle exceptions in custom `call()` implementations
- Return appropriate exit codes (0 for success, non-zero for errors)
- Use proper logging for error reporting

### 4. Shutdown Handling
- Rely on the built-in shutdown hooks for graceful termination
- Set `runner.keepRunning: false` for short-lived applications
- Implement custom cleanup in Xorcery services, not in the runner

## Integration Points

The runner module integrates with several other Xorcery modules:

- **xorcery-configuration**: For configuration loading and management
- **xorcery-core**: For Xorcery instance lifecycle
- **xorcery-log4j**: For logging configuration and management

## Troubleshooting

### Common Issues

1. **"No configuration set" error**: Ensure `setConfiguration()` is called before `call()`
2. **Application doesn't shutdown**: Check `runner.keepRunning` configuration
3. **Version not displayed**: Verify application configuration contains version information
