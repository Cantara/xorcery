# Xorcery Domain Events Extension

## Overview

The Xorcery Domain Events module is a comprehensive solution for implementing domain-driven design (DDD) with event sourcing and CQRS patterns in Java applications. It provides a robust framework for handling domain commands, generating domain events, and managing event projections with strong consistency guarantees.

## Architecture

The module is structured into five main sub-modules:

```
xorcery-domainevents/
├── xorcery-domainevents-api/          # Core API and data models
├── xorcery-domainevents-entity/       # Entity and command handling
├── xorcery-domainevents-publisher/    # Event publishing infrastructure
├── xorcery-domainevents-jsonapi/      # JSON:API REST interface
└── xorcery-domainevents-neo4jprojection/ # Neo4j event projections
```

## Core Components

### 1. Domain Events API (xorcery-domainevents-api)

**Purpose**: Defines the core data structures and contracts for domain events.

**Key Classes**:
- `DomainEvent`: Base interface for all domain events
- `JsonDomainEvent`: JSON-based implementation supporting entity lifecycle events (create, update, delete)
- `JsonSystemEvent`: System-level events for non-domain changes
- `MetadataEvents`: Container for events with associated metadata
- `DomainEventMetadata`: Enumeration of standard metadata fields

**Features**:
- JSON serialization/deserialization support
- Event builders with fluent API
- Attribute and relationship tracking
- Metadata management

### 2. Entity Framework (xorcery-domainevents-entity)

**Purpose**: Provides the foundation for domain entities and command processing.

**Key Components**:
- `Entity`: Abstract base class for domain entities
- `Command`: Interface for domain commands
- Command annotations: `@Create`, `@Update`, `@Delete`
- Exception hierarchy for entity-related errors
- `DomainContext`: Interface for command execution contexts

**Command Lifecycle**:
1. Command validation
2. Entity state loading (for updates)
3. Command execution
4. Domain event generation
5. Event publishing

### 3. Event Publishing (xorcery-domainevents-publisher)

**Purpose**: Manages the publication and projection of domain events.

**Key Services**:
- `DomainEventPublisher`: Interface for event publishing
- `CommandHandler`: Handles command execution workflow
- `EntitySnapshotProvider`: Manages entity state snapshots
- Event publisher and projection providers (SPI)

**Publishing Flow**:
1. Command received
2. Entity snapshot loaded (if needed)
3. Command executed by entity
4. Events generated
5. Events published to event store
6. Projections updated
7. Confirmation returned

### 4. JSON:API Integration (xorcery-domainevents-jsonapi)

**Purpose**: Provides REST API endpoints following JSON:API specification.

**Features**:
- Command resource endpoints
- JSON Schema generation for commands
- Error handling and validation
- Resource object mapping utilities

### 5. Neo4j Projections (xorcery-domainevents-neo4jprojection)

**Purpose**: Implements event projections using Neo4j graph database.

**Components**:
- `JsonDomainEventNeo4jEventProjection`: Projects domain events to Neo4j
- `Neo4jEntitySnapshotLoader`: Loads entity snapshots from Neo4j
- Event publisher and projection providers for Neo4j
- Cypher query templates

## Event Model

### JsonDomainEvent Structure

```json
{
  "@class": "dev.xorcery.domainevents.api.JsonDomainEvent",
  "event": "UserCreated",
  "created": {
    "type": "User",
    "id": "user-123"
  },
  "attributes": {
    "name": "John Doe",
    "email": "john@example.com"
  },
  "addedRelationships": [
    {
      "type": "Organization",
      "id": "org-456",
      "relationship": "memberOf"
    }
  ],
  "metadata": {
    "timestamp": 1640995200000,
    "userId": "admin",
    "reason": "New user registration"
  }
}
```

### Event Types

1. **Create Events**: Represent entity creation
2. **Update Events**: Represent entity modifications
3. **Delete Events**: Represent entity removal

### Attribute Changes

- `attributes`: Current attribute values
- `addedattributes`: List attributes additions
- `removedattributes`: List attribute removals

### Relationship Changes

- `addedrelationships`: New relationships
- `updatedrelationships`: Modified relationships
- `removedrelationships`: Removed relationships

## Command Processing

### Command Definition

```java
@Create
public class CreateUserCommand implements Command {
    private String id;
    private String name;
    private String email;
    
    // getters and setters
}
```

### Entity Implementation

```java
public class User extends Entity {
    
    public void handle(CreateUserCommand command) throws Exception {
        // Validation logic
        if (command.getEmail() == null) {
            throw new IllegalArgumentException("Email is required");
        }
        
        // Generate domain event
        JsonDomainEvent event = JsonDomainEvent.event("UserCreated")
            .created("User", command.getId())
            .updatedAttribute("name", command.getName())
            .updatedAttribute("email", command.getEmail())
            .build();
            
        add(event);
    }
}
```

### Command Execution Flow

1. **Command Validation**: Structural and business rule validation
2. **Concurrency Check**: Version/timestamp verification for updates
3. **Entity Loading**: Current state retrieval for existing entities
4. **Business Logic**: Domain-specific processing
5. **Event Generation**: Creation of domain events
6. **Event Publishing**: Persistence and projection updates

## Configuration

### Basic Configuration

```yaml
domainevents:
  enabled: true
  eventstore: "http://localhost:8080/api/eventstore"
  projections: "http://localhost:8080/api/projections"

neo4jprojections:
  enabled: true
  eventBatchSize: 1024
  maxTransactionSize: "1G"
```

### Neo4j Projection Configuration

```yaml
jsondomaineventprojection:
  enabled: true
  
neo4jeventpublisherprovider:
  projectionId: "default"
```

## Usage Examples

### 1. Creating Domain Events

```java
// Create a new user event
JsonDomainEvent userCreated = JsonDomainEvent.event("UserCreated")
    .created("User", "user-123")
    .updatedAttribute("name", "John Doe")
    .updatedAttribute("email", "john@example.com")
    .addedRelationship("memberOf", "Organization", "org-456")
    .build();
```

### 2. Publishing Events

```java
@Inject
private DomainEventPublisher publisher;

public CompletableFuture<Metadata> publishUserCreated(User user) {
    List<DomainEvent> events = List.of(userCreated);
    Metadata metadata = new Metadata.Builder()
        .add("userId", "admin")
        .add("timestamp", System.currentTimeMillis())
        .build();
    
    MetadataEvents metadataEvents = new MetadataEvents(metadata, events);
    return publisher.publish(metadataEvents);
}
```

### 3. Implementing Command Handlers

```java
@Service
public class UserService {
    
    @Inject
    private CommandHandler commandHandler;
    
    public CompletableFuture<CommandResult> createUser(CreateUserCommand command) {
        CommandMetadata metadata = new CommandMetadata.Builder()
            .aggregateId(command.getId())
            .aggregateType("User")
            .commandName("CreateUser")
            .build();
            
        User userEntity = new User();
        return commandHandler.handle(userEntity, metadata, command);
    }
}
```

## Error Handling

The module provides a comprehensive exception hierarchy:

- `EntityException`: Base exception class
- `EntityNotFoundException`: Entity does not exist
- `EntityAlreadyExistsException`: Entity already exists
- `EntityWrongVersionException`: Concurrency conflict

## Integration Points

### With Event Stores
- WebSocket-based event publishing
- Configurable event store endpoints
- Retry mechanisms with backoff

### With Projections
- Real-time projection updates
- Batch processing capabilities
- Projection completion confirmation

### With Neo4j
- Graph-based entity relationships
- Cypher query execution
- Snapshot loading and storage

## Best Practices

1. **Command Design**
    - Keep commands focused on single business operations
    - Include all necessary data for processing
    - Use appropriate annotations (@Create, @Update, @Delete)

2. **Event Design**
    - Include sufficient context in events
    - Use meaningful event names
    - Separate business logic from technical concerns

3. **Entity Implementation**
    - Keep business logic in entity methods
    - Generate events for all state changes
    - Handle validation and errors appropriately

4. **Error Handling**
    - Use specific exception types
    - Include meaningful error messages
    - Handle concurrency conflicts gracefully

## Dependencies

### Core Dependencies
- Jackson (JSON processing)
- HK2 (Dependency injection)
- Jakarta WS-RS (REST APIs)
- Reactor (Reactive programming)

### Optional Dependencies
- Neo4j (Graph database projections)
- Jetty (WebSocket communication)
- OpenTelemetry (Observability)

## Maven Dependencies

### API Module
```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-domainevents-api</artifactId>
    <version>0.164.6-SNAPSHOT</version>
</dependency>
```

### Entity Module
```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-domainevents-entity</artifactId>
    <version>0.164.6-SNAPSHOT</version>
</dependency>
```

### Publisher Module
```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-domainevents-publisher</artifactId>
    <version>0.164.6-SNAPSHOT</version>
</dependency>
```

### JSON:API Module
```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-domainevents-jsonapi</artifactId>
    <version>0.164.6-SNAPSHOT</version>
</dependency>
```

### Neo4j Projection Module
```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-domainevents-neo4jprojection</artifactId>
    <version>0.164.6-SNAPSHOT</version>
</dependency>
```
