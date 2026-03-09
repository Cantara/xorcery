# Xorcery

## Purpose
A comprehensive set of Java libraries for building reactive, event-sourced microservices. Xorcery provides dependency injection (HK2), composable configuration (YAML + JSON Schema), logging integration (Log4j2), and a rich ecosystem of extensions for building production-grade distributed systems.

## Tech Stack
- Language: Java 21+
- Framework: HK2 (dependency injection), Jetty 12 (HTTP/WebSocket), Jersey (JAX-RS)
- Build: Maven (multi-module)
- Key dependencies: HK2, Jetty, Jersey, Jackson, Log4j2, Kurrent (EventStore)

## Architecture
Multi-module Maven project organized into core modules and extensions:
- **Core:** DI (HK2), Configuration (YAML+JSON-Schema), JSON utilities, JUnit helpers, Log4j2 integration, Runner (CLI startup)
- **Extensions:** AWS auth, certificates, DNS, EventStore/Kurrent client, JAX-RS server, Jetty server/client, JSON:API, JSON-Schema, JWT, keystore management, OpenSearch, Maven plugins, Reactivestreams, and more
- **Patterns:** Event-sourced, reactive streams, CQRS, domain-driven design

Configuration is composable and overridable using YAML with JSON-Schema validation. Services register via HK2 dependency injection and communicate through reactive streams.

## Key Entry Points
- `xorcery-core/` - Core DI and runtime
- `xorcery-configuration/` - YAML configuration system
- `xorcery-runner/` - Standard Main class with CLI
- `xorcery-extensions/` - All extension modules

## Development
```bash
# Build
mvn clean install

# Test
mvn test

# Run an application
java -jar target/your-xorcery-app.jar
```

## Domain Context
Reactive microservices framework. The primary application framework maintained by Cantara/eXOReaction for building event-sourced, reactive distributed systems. Successor to the reactiveservices prototype, now production-ready and actively maintained.
