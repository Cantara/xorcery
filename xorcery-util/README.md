# Xorcery Util Module Technical Documentation

## Overview

The `xorcery-util` module is a core utility library within the Xorcery framework, providing a comprehensive collection of helper classes, interfaces, and utilities across multiple domains including concurrency, functional programming, I/O operations, networking, and language extensions.

## Maven Dependency

```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-util</artifactId>
</dependency>
```

## Package Structure

The module is organized into the following packages:

### 1. `dev.xorcery.builders`
Builder pattern utilities for fluent APIs.

#### Classes:
- **`With<T>`** - Interface for implementing visitor pattern in builders
- **`WithContext<T>`** - Interface for builders that provide context access

### 2. `dev.xorcery.collections`
Collection utilities and element access patterns.

#### Classes:
- **`Element`** - Interface for type-safe map value access with conversion utilities
- **`MapElement`** - Implementation of Element for Map-based data

**Key Features:**
- Type-safe value extraction from maps
- Automatic type conversion (String, Integer, Boolean, URL, URI, etc.)
- Null-safe operations with Optional return types
- Support for enum values and custom mappings

### 3. `dev.xorcery.concurrent`
Concurrency utilities and thread management.

#### Classes:
- **`CloseableSemaphore`** - Semaphore that can interrupt waiting threads on close
- **`CompletableFutures`** - Utilities for CompletableFuture operations
- **`NamedThreadFactory`** - ThreadFactory that creates named threads
- **`SmartBatcher<T>`** - Intelligent batching system that adapts batch size based on processing time

**SmartBatcher Features:**
- Adaptive batch sizing based on processing time
- High-load → larger batches, low-load → smaller batches
- Thread-safe queue management
- Automatic retry and error handling
- Graceful shutdown support

### 4. `dev.xorcery.function`
Functional programming utilities and lazy evaluation patterns.

#### Classes:
- **`FallbackFunction<T,R>`** - Function with fallback behavior when primary returns null
- **`LazyReference<T>`** - Lazy evaluation wrapper with optional AutoCloseable support
- **`LazySupplier<T>`** - Thread-safe lazy supplier implementation

**Usage Examples:**

```java
// Lazy Reference
LazyReference<String> lazyValue = LazyReference.lazyReference();
String result = lazyValue.apply(() -> expensiveComputation());

// Lazy Supplier
Supplier<Database> lazyDb = LazySupplier.lazy(() -> createDatabase());
Database db = lazyDb.get(); // Only created on first access
```

### 5. `dev.xorcery.hk2`
HK2 dependency injection utilities.

#### Classes:
- **`@Instance`** - Annotation for named instance injection
- **`Instances`** - Helper methods for extracting instance names from injection points
- **`Services`** - Utilities for service discovery and type-based lookups

### 6. `dev.xorcery.io`
I/O utilities and stream handling.

#### Classes:
- **`ByteBufferBackedInputStream`** - InputStream implementation backed by ByteBuffer
- **`ZipFiles`** - Utilities for ZIP file extraction with security protections

**Security Features:**
- Path traversal protection (Zip Slip vulnerability prevention)
- Safe file extraction with proper directory creation

### 7. `dev.xorcery.lang`
Language extensions and reflection utilities.

#### Classes:
- **`AutoCloseables`** - Collector for managing multiple AutoCloseable resources
- **`Classes`** - Reflection and type resolution utilities
- **`Enums`** - Enum manipulation utilities
- **`Exceptions`** - Exception unwrapping and cause analysis
- **`Strings`** - String manipulation utilities

**Key Features:**
- Generic type resolution for complex inheritance hierarchies
- Exception cause chain analysis
- Safe exception unwrapping
- Resource management with suppressed exception handling

### 8. `dev.xorcery.net`
Networking utilities.

#### Classes:
- **`Sockets`** - Socket and port management utilities
- **`URIs`** - URI manipulation utilities

**Features:**
- Dynamic free port allocation (range: 49152-65535)
- Socket address parsing
- URI scheme modification

### 9. `dev.xorcery.process`
Process management and long-running task utilities.

#### Classes:
- **`Process<T>`** - Interface for long-running processes with retry logic
- **`ActiveProcesses`** - Manager for tracking and shutting down active processes

**Process Features:**
- Automatic retry with exponential backoff
- Graceful cancellation support
- Result handling with CompletableFuture
- Configurable retry policies

### 10. `dev.xorcery.util`
General utilities and resource management.

#### Classes:
- **`Resources`** - Resource loading utilities with multiple fallback strategies
- **`ResourceURLStreamHandlerProvider`** - Custom URL handler for "resource:" protocol
- **`Streams`** - Stream concatenation utilities with automatic cleanup
- **`UUIDs`** - UUID utilities for generating dash-free identifiers

**Stream Concatenation:**
```java
Stream<String> combined = Streams.concatenate(
    Stream.of(() -> stream1, () -> stream2, () -> stream3)
);
// Automatically closes each stream as iteration completes
```

## Module Configuration

The module provides a custom URL stream handler for the `resource:` protocol, allowing URLs like:
```
resource://com/example/config.yml
```

This handler delegates to the `Resources.getResource()` method for flexible resource loading.

## Best Practices

1. **Use LazySupplier for expensive computations** that may not always be needed
2. **Leverage Element interface** for safe configuration and data access
3. **Use SmartBatcher** for high-throughput scenarios requiring batching
4. **Implement Process interface** for long-running tasks requiring retry logic
5. **Use AutoCloseables** to ensure proper resource cleanup in complex scenarios

## Thread Safety

Most utilities in this module are designed to be thread-safe:
- `LazySupplier` uses atomic operations
- `SmartBatcher` handles concurrent submissions
- `CloseableSemaphore` safely interrupts waiting threads
