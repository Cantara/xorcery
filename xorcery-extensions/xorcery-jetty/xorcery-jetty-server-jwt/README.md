# Xorcery Jetty Server JWT Authentication Extension

## Overview

The `xorcery-jetty-server-jwt` module provides JWT (JSON Web Token) authentication capabilities for Jetty servers within the Xorcery framework. This module enables secure authentication using industry-standard JWT tokens, supporting both Bearer token authentication and cookie-based authentication.

## Module Information

- **Artifact ID**: `xorcery-jetty-server-jwt`
- **Group ID**: `dev.xorcery`
- **Package**: `dev.xorcery.jetty.server.jwt`

## Dependencies

```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-jetty-server-jwt</artifactId>
    <version>${xorcery.version}</version>
</dependency>
```

## Architecture

### Core Components

#### 1. Auth0JwtAuthenticator
The primary authenticator implementing Jetty's `LoginAuthenticator` interface.

**Key Responsibilities:**
- JWT token extraction from HTTP requests (Authorization header or cookies)
- Token validation and signature verification
- User identity creation and role assignment
- Session management integration

**Supported Token Sources:**
- **Bearer Token**: `Authorization: Bearer <token>`
- **Cookie**: Configurable cookie name (default: "token")

#### 2. JwtCredential
Extends Jetty's `Credential` class to wrap decoded JWT tokens.

```java
public class JwtCredential extends Credential {
    public DecodedJWT getJwt();
    public boolean check(Object credentials);
}
```

#### 3. JwtUserPrincipal
Extends Jetty's `UserPrincipal` to provide access to JWT claims.

```java
public class JwtUserPrincipal extends UserPrincipal {
    public DecodedJWT getJwt();
}
```

#### 4. Configuration Classes

**JwtConfiguration**: Main configuration wrapper
- Token cookie name configuration
- Login/error page settings
- Issuer configurations

**IssuerConfiguration**: JWT issuer settings
- Issuer name and JWKS URL
- Multiple key support with key IDs

## Configuration

### Basic Configuration

```yaml
jetty:
  server:
    security:
      jwt:
        enabled: true
        cookie: "token"  # Cookie name for JWT token
        loginPage: "/login"  # Optional login page
        errorPage: "/error"  # Optional error page
        issuers: []  # List of JWT issuers
```

### Issuer Configuration

```yaml
jetty:
  server:
    security:
      jwt:
        issuers:
          - name: "my-issuer"
            jwks: "https://issuer.example.com/.well-known/jwks.json"  # Optional JWKS URL
            keys:
              - kid: "key-1"  # Key ID (optional)
                alg: "RS256"  # Algorithm: RS256, RS384, RS512, ES256, ES384, ES512
                publicKey: "secret:jwt-public-key"  # Reference to secret containing public key
              - kid: "key-2"
                alg: "ES256"
                publicKey: "secret:jwt-es256-key"
```

### Complete Example

```yaml
jetty:
  server:
    security:
      enabled: true
      type: "jwt"
      jwt:
        enabled: true
        cookie: "auth_token"
        loginPage: "/auth/login"
        errorPage: "/auth/error"
        issuers:
          - name: "authentication.example.com"
            keys:
              - kid: "primary-key"
                alg: "RS256"
                publicKey: "secret:jwt-rsa-public-key"
              - kid: "backup-key"
                alg: "ES256"
                publicKey: "secret:jwt-ec-public-key"
```

## Supported Algorithms

### RSA Algorithms
- **RS256**: RSA with SHA-256
- **RS384**: RSA with SHA-384
- **RS512**: RSA with SHA-512

### ECDSA Algorithms
- **ES256**: ECDSA with SHA-256
- **ES384**: ECDSA with SHA-384
- **ES512**: ECDSA with SHA-512

## JWT Token Requirements

### Required Claims
- **sub** (subject): User identifier
- **exp** (expiration): Token expiration time
- **iss** (issuer): Token issuer (optional, used for key selection)

### Optional Claims
- **kid** (key ID): Used for key selection during verification
- **roles**: Array of role strings for authorization

### Example JWT Payload

```json
{
  "sub": "user123",
  "iss": "authentication.example.com",
  "exp": 1640995200,
  "kid": "primary-key",
  "roles": ["user", "admin"]
}
```

## Security Features

### Token Validation
- **Signature Verification**: Uses configured public keys
- **Expiration Check**: Validates token hasn't expired (with 800-hour tolerance)
- **Issuer Verification**: Matches token issuer with configured issuers
- **Key ID Matching**: Attempts to match JWT kid with configured keys

### Key Management
- Public keys stored securely using Xorcery Secrets API
- Support for key rotation with multiple keys per issuer
- Base64-encoded X.509 public key format

### Session Integration
- Automatic session creation for authenticated users
- Session cleanup on logout
- Cookie removal on logout

## Usage Patterns

### 1. Bearer Token Authentication

```http
GET /api/protected HTTP/1.1
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 2. Cookie-Based Authentication

```http
GET /api/protected HTTP/1.1
Cookie: token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 3. Accessing JWT Claims in Application Code

```java
@Context
SecurityContext securityContext;

public Response getProtectedResource() {
    if (securityContext.getUserPrincipal() instanceof JwtUserPrincipal jwtPrincipal) {
        DecodedJWT jwt = jwtPrincipal.getJwt();
        String userId = jwt.getSubject();
        List<String> roles = jwt.getClaim("roles").asList(String.class);
        // Use JWT claims...
    }
    return Response.ok().build();
}
```

## Error Handling

The authenticator handles various error scenarios:

- **Expired Tokens**: Returns authentication failure
- **Invalid Signatures**: Logs warning and fails authentication
- **Missing Keys**: Attempts all available keys before failing
- **Malformed Tokens**: Catches and logs JWT parsing errors

## Integration with Xorcery

### Module System
Exports the following packages:
- `dev.xorcery.jetty.server.jwt`
- `dev.xorcery.jetty.server.jwt.providers`

### Required Modules
- `com.auth0.jwt`
- `org.bouncycastle.provider`
- `xorcery.secrets.api`
- `xorcery.configuration.api`
- `org.eclipse.jetty.security`

### Service Registration
The authenticator is automatically registered as a Jetty `Authenticator` service with name `"jetty.server.security.jwt"`.

## Troubleshooting

### Common Issues

1. **Token Not Found**: Ensure tokens are sent in Authorization header or configured cookie
2. **Signature Verification Failed**: Check public key configuration and algorithm match
3. **Key ID Mismatch**: Verify JWT kid claim matches configured key IDs
4. **Expired Tokens**: Check token expiration times and system clocks

### Logging

The module uses Log4j2 for logging. Enable DEBUG level for `dev.xorcery.jetty.server.jwt` to see detailed authentication flow.

```yaml
Loggers:
  dev.xorcery.jetty.server.jwt:
    level: DEBUG
```
