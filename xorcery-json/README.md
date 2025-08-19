# Xorcery JSON Module Documentation

## Overview

The `xorcery-json` module provides advanced JSON processing capabilities built on top of Jackson, offering sophisticated merging, variable resolution, and element manipulation functionality. This module is designed to handle complex JSON operations commonly needed in configuration management and data transformation scenarios.

## Module Structure

```
xorcery.json/
├── dev.xorcery.json.JsonElement      # JSON element interface and utilities
├── dev.xorcery.json.JsonMerger       # Advanced JSON merging
└── dev.xorcery.json.JsonResolver     # Variable resolution system
```

## Core Components

### 1. JsonElement Interface

`JsonElement` is a marker interface that provides utility methods for working with Jackson `JsonNode` objects in a type-safe manner.

#### Key Features
- Type-safe accessors for JSON values
- Conversion utilities between JSON and Java objects
- Support for hierarchical property access with dot notation
- Collection mapping and transformation utilities

#### Usage Example

```java
import dev.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

public record MyConfig(ObjectNode json) implements JsonElement {
    // Access nested properties with dot notation
    public Optional<String> getDatabaseUrl() {
        return getString("database.url");
    }
    
    // Get arrays as typed lists
    public List<String> getServerNames() {
        return getListAs("servers", JsonNode::asText).orElse(List.of());
    }
}
```
Implementing wrappers using records instead of classes is recommended.

#### Key Methods

```java
// Basic property access
Optional<T> get(String name)
Optional<String> getString(String name)
Optional<Integer> getInt(String name)
Optional<Boolean> getBoolean(String name)

// Complex property access
Optional<JsonNode> getJson(String name)
Optional<Iterable<JsonNode>> getList(String name)
<T> Optional<List<T>> getListAs(String name, Function<JsonNode, T> mapper)

// Conversion utilities
Map<String, Object> asMap()
String toJsonString()
```

### 2. JsonMerger Class

`JsonMerger` provides sophisticated JSON tree merging capabilities with support for complex scenarios including array merging, object identification, and dotted notation keys.

#### Key Features
- **Deep Object Merging**: Recursively merges nested objects
- **Intelligent Array Merging**: Merges arrays based on object identity
- **Dot Notation Support**: Handles keys like `"object.property.subproperty"`
- **Configurable Merge Behavior**: Customizable merge rules via predicates
- **Reference Preservation**: Special handling for template references (e.g., `"{{ variable }}"`)

#### Usage Example

```java
import dev.xorcery.json.JsonMerger;
import com.fasterxml.jackson.databind.node.ObjectNode;

// Basic merging
JsonMerger merger = new JsonMerger();
ObjectNode result = merger.apply(baseConfig, overrideConfig);

// Custom merge behavior
JsonMerger customMerger = new JsonMerger(path -> {
    // Skip merging arrays containing JSON Schema keywords
    return !Set.of("anyOf", "allOf", "oneOf").contains(path.peek());
});
ObjectNode result = customMerger.apply(schema1, schema2);
```

#### Merge Behavior

1. **Primitive Values**: Override completely
2. **Objects**: Merge recursively
3. **Arrays**:
    - Simple values: Append unique items
    - Objects: Merge by identity (first property as key)
    - References: Replace completely
4. **Dotted Keys**: Navigate and merge at target location

### 3. JsonResolver Class

`JsonResolver` provides a powerful variable resolution system for JSON configurations with support for hierarchical lookups, conditionals, and default values.

#### Key Features
- **Variable Interpolation**: Replace `{{ variable }}` expressions
- **Hierarchical Lookup**: Access nested properties with dot notation
- **Default Values**: Fallback values using pipe syntax `{{ var | default }}`
- **Conditional Expressions**: Ternary-like conditionals `{{ flag ? value1 | value2 }}`
- **Recursive Resolution**: Variables can reference other variables
- **Array References**: Support for expanding array variables

#### Syntax

```yaml
# Basic variable reference
message: "Hello {{ user.name }}"

# Default values
port: "{{ server.port | 8080 }}"

# Conditional expressions
environment: "{{ production ? prod | dev }}"

# Complex conditional with defaults
database: "{{ production ? prod.db | dev.enabled ? dev.db | localhost }}"

# Array references
services: "{{ microservices }}"  # Expands entire array

# Hierarchical access
config: "{{ application.database.connection.url }}"
```

#### Usage Example

```java
import dev.xorcery.json.JsonResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

ObjectMapper mapper = new ObjectMapper();
ObjectNode config = (ObjectNode) mapper.readTree("""
    {
        "user": {"name": "John"},
        "greeting": "Hello {{ user.name }}!",
        "port": "{{ server.port | 8080 }}",
        "debug": "{{ environment.debug | false }}"
    }
    """);

JsonResolver resolver = new JsonResolver();
ObjectNode resolved = resolver.apply(config, config);
// Result: {"user": {"name": "John"}, "greeting": "Hello John!", "port": 8080, "debug": false}
```

## Advanced Usage Examples

### Complex Configuration Merging

```java
// Base configuration
ObjectNode baseConfig = mapper.readTree("""
    {
        "server": {
            "port": 8080,
            "ssl": false
        },
        "features": ["auth", "logging"]
    }
    """);

// Environment override
ObjectNode prodOverride = mapper.readTree("""
    {
        "server.port": 443,
        "server.ssl": true,
        "features": ["monitoring"]
    }
    """);

JsonMerger merger = new JsonMerger();
ObjectNode result = merger.apply(baseConfig, prodOverride);
// Result merges port and ssl settings, and appends monitoring to features array
```

### Configuration with Variable Resolution

```yaml
# configuration.yaml
defaults:
  database:
    host: "localhost"
    port: 5432
    
environments:
  production:
    database:
      host: "prod-db.example.com"
      
instance:
  environment: "{{ ENV.ENVIRONMENT | development }}"

environment: "environments.{{ instance.environment }}"
  
database:
  host: "{{ environment.database.host | defaults.database.host }}"
  port: "{{ environment.database.port | defaults.database.port }}"
  url: "jdbc:postgresql://{{ database.host }}:{{ database.port }}/myapp"
```

## Testing

The module includes comprehensive tests demonstrating various scenarios:

### JsonMerger Tests
- Basic object and array merging
- Dotted notation key handling
- Object array merging with identity matching
- JSON Schema merging with custom predicates

### JsonResolver Tests
- Variable interpolation and recursive resolution
- Conditional expressions with defaults
- Array reference expansion
- Named object array lookups

## Best Practices

1. **JsonElement Interface**: Implement this interface for configuration classes to get type-safe JSON access
2. **JsonMerger**: Use custom predicates to control merge behavior for specialized data structures
3. **JsonResolver**: Structure your configuration to minimize resolution cycles and use defaults appropriately
4. **Error Handling**: Both JsonMerger and JsonResolver provide detailed error messages for debugging
5. **Performance**: Cache resolved configurations when possible, as resolution can be expensive for deep hierarchies

## Common Use Cases

1. **Configuration Management**: Merging environment-specific settings with base configurations
2. **Template Processing**: Resolving variables in configuration templates
3. **Schema Composition**: Combining JSON Schema definitions
4. **Data Transformation**: Converting between different JSON structures
5. **Environment Adaptation**: Adapting configurations based on runtime conditions

This module provides powerful tools for sophisticated JSON processing needs while maintaining simplicity for common use cases.

## API Reference

See Javadocs
