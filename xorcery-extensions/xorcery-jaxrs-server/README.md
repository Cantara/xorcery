# Xorcery JAX-RS Server Module Documentation

## Overview

The `xorcery-jaxrs-server` module provides foundational infrastructure for building JAX-RS REST endpoints within the Xorcery framework. It offers a clean abstraction layer that simplifies access to HTTP context, dependency injection, security information, and URI manipulation for JAX-RS resource implementations.

**Key Features:**
- Simplified access to JAX-RS and Servlet API context objects
- HK2 service location and dependency injection integration
- Built-in security context and authentication utilities
- URI building and path manipulation helpers
- Clean separation between interface contracts and implementation

## Architecture

### Core Components

```
ContextResource (Interface)
↑
BaseResource (Implementation)
↑
YourResource (User Implementation)
```

#### ContextResource Interface
A comprehensive helper interface that provides access to:
- **Service Location**: HK2 ServiceLocator integration
- **HTTP Context**: Servlet request/response objects
- **JAX-RS Context**: Container request context, URI info, security context
- **Authentication**: User identity and subject extraction
- **URI Utilities**: Path building and manipulation

#### BaseResource Class
A concrete base class that:
- Implements the `ContextResource` interface
- Handles dependency injection of all context objects
- Provides a foundation for JAX-RS resource implementations
- Uses method-based injection to allow constructor customization

## API Reference

### ContextResource Interface

#### Service Location Methods

```java
ServiceLocator getServiceLocator()
```
Returns the HK2 ServiceLocator for service lookup.

```java
<T> T service(Class<T> serviceType, Annotation... annotations)
```
Looks up a service by type with optional qualifiers. Throws `IllegalArgumentException` if service not found.

#### HTTP Context Methods

```java
HttpServletRequest getHttpServletRequest()
HttpServletResponse getHttpServletResponse()
ContainerRequestContext getContainerRequestContext()
```
Provide access to HTTP servlet and JAX-RS container contexts.

#### JAX-RS Helper Methods

```java
UriInfo getUriInfo()
String getFirstPathParameter(String parameterName)
String getFirstQueryParameter(String parameterName)
SecurityContext getSecurityContext()
```

#### URI Building Methods

```java
UriBuilder getAbsolutePathBuilder()
URI getBaseUri()
UriBuilder getBaseUriBuilder()
UriBuilder getRequestUriBuilder()
URI getAbsolutePath()
URI getParentPath()
UriBuilder getUriBuilderFor(Class<?> resourceClass)
```

#### Authentication Methods

```java
Optional<UserIdentity> getUserIdentity()
Optional<Subject> getSubject()
```
Extract user identity and security subject from Jetty's authentication system.

### BaseResource Class

```java
public class BaseResource implements ContextResource
```

**Injection Method:**
```java
@Inject
private void init(
    ServiceLocator serviceLocator,
    ContainerRequestContext containerRequestContext,
    HttpServletRequest httpServletRequest,
    HttpServletResponse httpServletResponse)
```

The `init` method is called by HK2 to inject all required dependencies after construction.

## Usage Guide

### Basic Usage

1. **Extend BaseResource:**
```java
@Path("/api/users")
@Component
public class UserResource extends BaseResource {
    
    @GET
    @Path("/{id}")
    public Response getUser(@PathParam("id") String userId) {
        // Access path parameter
        String id = getFirstPathParameter("id");
        
        // Get a service
        UserService userService = service(UserService.class);
        
        // Build response URI
        URI location = getUriBuilderFor(UserResource.class)
            .path("/{id}")
            .build(userId);
            
        return Response.ok(userService.getUser(id))
            .location(location)
            .build();
    }
}
```

2. **Implement ContextResource directly:**
```java
@Path("/api/orders")
@Component
public class OrderResource implements ContextResource {
    
    private ServiceLocator serviceLocator;
    private ContainerRequestContext containerRequestContext;
    private HttpServletRequest httpServletRequest;
    private HttpServletResponse httpServletResponse;
    
    @Inject
    private void init(/* same parameters as BaseResource */) {
        // Initialize fields
    }
    
    // Implement ContextResource methods...
}
```

### Common Patterns

#### Service Lookup
```java
// Get required service (throws exception if not found)
EmailService emailService = service(EmailService.class);

// Get service with qualifier
@Named("primary")
DatabaseService dbService = service(DatabaseService.class, 
    AnnotationLiteral.of(Named.class, "primary"));
```

#### URI Building
```java
// Build absolute path to current resource
URI selfLink = getAbsolutePathBuilder().build();

// Build link to parent collection
URI parentLink = getParentPath();

// Build link to related resource
URI relatedLink = getUriBuilderFor(RelatedResource.class)
    .path("/related/{id}")
    .build(relatedId);
```

#### Authentication Access
```java
Optional<UserIdentity> userIdentity = getUserIdentity();
if (userIdentity.isPresent()) {
    String username = userIdentity.get().getUserPrincipal().getName();
    Set<String> roles = userIdentity.get().getRoles();
}

Optional<Subject> subject = getSubject();
subject.ifPresent(s -> {
    // Access JAAS Subject
});
```

#### Parameter Access
```java
// Path parameters
String userId = getFirstPathParameter("userId");

// Query parameters
String filter = getFirstQueryParameter("filter");
```

## Configuration

### Dependencies

Add the module to your `pom.xml`:

```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-jaxrs-server</artifactId>
</dependency>
```

### Module Requirements

The module requires the following in your `module-info.java`:

```java
module your.module {
    requires xorcery.jaxrs.server;
    requires jakarta.ws.rs;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
}
```

### Runtime Dependencies

The module transitively includes:
- **JAX-RS API** (`jakarta.ws.rs-api`)
- **Servlet API** (via `jetty-ee10-servlet`)
- **HK2 Dependency Injection** (`hk2-api`)
- **Jakarta Inject** (`jakarta.inject-api`)
- **Jetty Security** (for authentication features)

## Examples

### Complete Resource Example

```java
package com.example.resources;

import dev.xorcery.jaxrs.server.resources.BaseResource;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.api.PerLookup;

@Path("/api/products")
@PerLookup
public class ProductResource extends BaseResource {
    
    @GET
    @Path("/{productId}")
    @Produces("application/json")
    public Response getProduct(@PathParam("productId") String productId) {
        try {
            // Get business service
            ProductService productService = service(ProductService.class);
            
            // Extract path parameter (alternative to @PathParam)
            String id = getFirstPathParameter("productId");
            
            // Get product
            Product product = productService.findById(id);
            if (product == null) {
                return Response.status(404).build();
            }
            
            // Build self link
            URI selfLink = getAbsolutePathBuilder().build();
            
            // Build related links
            URI categoryLink = getUriBuilderFor(CategoryResource.class)
                .path("/{categoryId}")
                .build(product.getCategoryId());
                
            return Response.ok(product)
                .link(selfLink, "self")
                .link(categoryLink, "category")
                .build();
                
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }
    
    @POST
    @Consumes("application/json")
    @Produces("application/json")  
    public Response createProduct(ProductRequest request) {
        // Check authentication
        Optional<UserIdentity> user = getUserIdentity();
        if (user.isEmpty()) {
            return Response.status(401).build();
        }
        
        ProductService productService = service(ProductService.class);
        Product created = productService.create(request);
        
        // Build location header
        URI location = getUriBuilderFor(ProductResource.class)
            .path("/{id}")
            .build(created.getId());
            
        return Response.created(location)
            .entity(created)
            .build();
    }
}
```

### Custom Authentication Example

```java
@GET
@Path("/profile")
public Response getUserProfile() {
    Optional<UserIdentity> userIdentity = getUserIdentity();
    
    if (userIdentity.isEmpty()) {
        return Response.status(401).build();
    }
    
    UserIdentity identity = userIdentity.get();
    String username = identity.getUserPrincipal().getName();
    
    // Check roles
    if (!identity.isUserInRole("USER")) {
        return Response.status(403).build();
    }
    
    // Get additional subject information
    Optional<Subject> subject = getSubject();
    subject.ifPresent(s -> {
        // Process JAAS Subject principals and credentials
    });
    
    UserService userService = service(UserService.class);
    UserProfile profile = userService.getProfile(username);
    
    return Response.ok(profile).build();
}
```

## Best Practices

1. **Use BaseResource** for most cases unless you need custom constructor injection
2. **Cache service lookups** in fields if used frequently within the same request
3. **Handle service lookup failures** gracefully - services may not be available
4. **Use URI builders** instead of string concatenation for robust URI construction
5. **Check authentication** before accessing protected resources
6. **Prefer Optional handling** for user identity to avoid null pointer exceptions

## Integration Notes

This module is designed to work seamlessly with:
- **xorcery-jersey-server**: Provides the JAX-RS runtime
- **xorcery-jetty-server**: Provides the HTTP server
- **xorcery-core**: Provides HK2 service management
- **xorcery-jetty-server-jwt**: Adds JWT authentication support

The module follows Xorcery's modular architecture and integrates with its dependency injection and configuration systems.

## Module Information

**Maven Coordinates:**
```xml
<groupId>dev.xorcery</groupId>
<artifactId>xorcery-jaxrs-server</artifactId>
<version>0.164.6-SNAPSHOT</version>
```

**Java Module Name:** `xorcery.jaxrs.server`

**Exported Packages:**
- `dev.xorcery.jaxrs.server.resources`

**Key Dependencies:**
- HK2 API
- Jakarta Inject API
- Jakarta JAX-RS API
- Eclipse Jetty EE10 Servlet
