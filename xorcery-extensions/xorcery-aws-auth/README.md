# Xorcery AWS Authentication Extension

## Overview

The `xorcery-aws-auth` module provides AWS authentication capabilities for Xorcery applications. It integrates with the AWS SDK for Java v2 to offer multiple authentication mechanisms, making it easy to authenticate with AWS services from within a Xorcery application.

### Key Features

- Multiple AWS authentication strategies (access keys, session tokens, container credentials, instance profiles)
- Integration with Xorcery's configuration and secrets management
- HK2 dependency injection support
- Environment variable and template expression support

## Architecture

The module follows Xorcery's modular architecture pattern and integrates with:

- **AWS SDK v2**: Uses the official AWS authentication providers
- **Xorcery Configuration**: Leverages the configuration system for flexible setup
- **Xorcery Secrets**: Securely handles sensitive credentials
- **HK2 Dependency Injection**: Provides `AwsCredentialsProvider` as an injectable service

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Application   │ -> │ AWS Auth Module  │ -> │   AWS SDK v2    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
│                          │
v                          v
┌──────────────────┐    ┌─────────────────┐
│ Xorcery Config   │    │ AWS Services    │
│ & Secrets        │    │                 │
└──────────────────┘    └─────────────────┘
```

## Configuration

### Configuration Schema

The module is configured under the `aws.auth` section in your `xorcery.yaml`:

```yaml
aws:
  enabled: true
  region: "us-east-1"
  auth:
    enabled: true
    type: "accessKey"  # or "sessionToken", "container", "instance"
    accessKeyId: "env:AWS_ACCESS_KEY_ID"
    secretAccessKey: "env:AWS_SECRET_ACCESS_KEY"
    sessionToken: "env:AWS_SESSION_TOKEN"  # optional, for sessionToken type
```

### Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `aws.enabled` | boolean/string | `true` | Enable/disable AWS functionality |
| `aws.region` | string | `${ENV:aws.region}` | AWS region |
| `aws.auth.enabled` | boolean/string | `${aws.enabled}` | Enable/disable authentication |
| `aws.auth.type` | string | `accessKey` | Authentication type |
| `aws.auth.accessKeyId` | string | `env:aws.accessKeyId` | AWS access key ID |
| `aws.auth.secretAccessKey` | string | `env:aws.secretAccessKey` | AWS secret access key |
| `aws.auth.sessionToken` | string | `env:aws.sessionToken` | AWS session token (optional) |

### Environment Variables

The following environment variables are commonly used:

- `AWS_ACCESS_KEY_ID`: Your AWS access key
- `AWS_SECRET_ACCESS_KEY`: Your AWS secret key
- `AWS_SESSION_TOKEN`: Session token for temporary credentials
- `AWS_REGION`: Default AWS region

## Authentication Types

### 1. Access Key Authentication (`accessKey`)

Uses static AWS access keys for authentication. Best for development and testing.

```yaml
aws:
  auth:
    type: "accessKey"
    accessKeyId: "env:AWS_ACCESS_KEY_ID"
    secretAccessKey: "env:AWS_SECRET_ACCESS_KEY"
```

**Use Cases:**
- Development environments
- CI/CD pipelines with stored secrets
- Applications running outside AWS

### 2. Session Token Authentication (`sessionToken`)

Uses temporary credentials with a session token. Ideal for assumed roles or temporary access.

```yaml
aws:
  auth:
    type: "sessionToken"
    accessKeyId: "env:AWS_ACCESS_KEY_ID"
    secretAccessKey: "env:AWS_SECRET_ACCESS_KEY"
    sessionToken: "env:AWS_SESSION_TOKEN"
```

**Use Cases:**
- Multi-account access via role assumption
- Temporary credentials from AWS STS
- Enhanced security scenarios

### 3. Container Credentials (`container`)

Uses credentials provided by the container environment (ECS, Fargate).

```yaml
aws:
  auth:
    type: "container"
```

**Use Cases:**
- Amazon ECS tasks
- AWS Fargate containers
- Containers with task roles

### 4. Instance Profile Authentication (`instance`)

Uses EC2 instance profile credentials automatically assigned to the instance.

```yaml
aws:
  auth:
    type: "instance"
```

**Use Cases:**
- EC2 instances with attached IAM roles
- Auto Scaling groups
- Applications running directly on EC2

## Module Dependencies

### Maven Dependency

```xml
<dependency>
    <groupId>dev.xorcery</groupId>
    <artifactId>xorcery-aws-auth</artifactId>
    <version>${xorcery.version}</version>
</dependency>
```

### Module Requirements

The module requires the following in your `module-info.java`:

```java
module your.application {
    requires xorcery.aws.auth;
    requires software.amazon.awssdk.auth;
    // ... other requirements
}
```

## Usage Examples

### Basic Usage with Dependency Injection

```java
@Service
public class MyAwsService {
    private final AwsCredentialsProvider credentialsProvider;
    
    @Inject
    public MyAwsService(AwsCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }
    
    public void useAwsService() {
        S3Client s3Client = S3Client.builder()
            .credentialsProvider(credentialsProvider)
            .region(Region.US_EAST_1)
            .build();
            
        // Use S3 client...
    }
}
```

### Configuration-Based Setup

```java
@Service
public class AwsServiceFactory {
    private final Configuration configuration;
    private final AwsCredentialsProvider credentialsProvider;
    
    @Inject
    public AwsServiceFactory(Configuration configuration, 
                           AwsCredentialsProvider credentialsProvider) {
        this.configuration = configuration;
        this.credentialsProvider = credentialsProvider;
    }
    
    public DynamoDbClient createDynamoDbClient() {
        String region = configuration.getString("aws.region").orElse("us-east-1");
        
        return DynamoDbClient.builder()
            .credentialsProvider(credentialsProvider)
            .region(Region.of(region))
            .build();
    }
}
```

### Multiple AWS Clients

```java
@Service
public class MultiServiceAws {
    private final AwsCredentialsProvider credentialsProvider;
    private final Configuration configuration;
    
    @Inject
    public MultiServiceAws(AwsCredentialsProvider credentialsProvider,
                          Configuration configuration) {
        this.credentialsProvider = credentialsProvider;
        this.configuration = configuration;
    }
    
    @PostConstruct
    public void initialize() {
        String region = configuration.getString("aws.region").orElse("us-east-1");
        Region awsRegion = Region.of(region);
        
        S3Client s3 = S3Client.builder()
            .credentialsProvider(credentialsProvider)
            .region(awsRegion)
            .build();
            
        SqsClient sqs = SqsClient.builder()
            .credentialsProvider(credentialsProvider)
            .region(awsRegion)
            .build();
            
        // Store clients as instance variables
    }
}
```

## API Reference

### AwsAuthConfiguration

The main configuration class that handles AWS authentication settings.

```java
public record AwsAuthConfiguration(Configuration configuration, Secrets secrets) {
    public static AwsAuthConfiguration get(Configuration configuration, Secrets secrets);
    public AuthType getAuthType();
    public Optional<String> getAccessKeyId();
    public Optional<String> getSecretAccessKey();
    public Optional<String> getSessionToken();
}
```

**Methods:**
- `get(Configuration, Secrets)`: Factory method to create configuration instance
- `getAuthType()`: Returns the configured authentication type
- `getAccessKeyId()`: Returns the access key ID (resolved from secrets)
- `getSecretAccessKey()`: Returns the secret access key (resolved from secrets)
- `getSessionToken()`: Returns the session token if configured

### AuthType Enum

```java
public enum AuthType {
    accessKey,
    sessionToken, 
    container,
    instance
}
```

### CredentialsProviderFactory

HK2 factory that creates and provides `AwsCredentialsProvider` instances.

```java
@Service(name = "aws.auth")
public class CredentialsProviderFactory implements Factory<AwsCredentialsProvider> {
    @Override
    @Singleton
    public AwsCredentialsProvider provide();
    
    @Override
    public void dispose(AwsCredentialsProvider instance);
}
```

## Security Considerations

### Best Practices

1. **Use Environment Variables**: Store sensitive credentials in environment variables, not in configuration files
2. **Prefer Instance/Container Roles**: When running on AWS, use IAM roles instead of static keys
3. **Rotate Credentials**: Regularly rotate access keys and use temporary credentials when possible
4. **Least Privilege**: Grant only the minimum required permissions to your IAM roles/users
5. **Secrets Management**: Leverage Xorcery's secrets management for credential storage

### Secret Resolution

The module integrates with Xorcery's secrets system, supporting various secret sources:

```yaml
aws:
  auth:
    accessKeyId: "env:AWS_ACCESS_KEY_ID"      # Environment variable
    secretAccessKey: "secret:aws-secret-key" # Xorcery secret store
    sessionToken: "system:aws.session.token" # System property
```

### Production Recommendations

For production deployments:

1. **Use IAM Roles**: Prefer `container` or `instance` authentication types
2. **Enable CloudTrail**: Monitor AWS API calls for security auditing
3. **Use VPC Endpoints**: When possible, use VPC endpoints to avoid internet traffic
4. **Implement Credential Rotation**: For static keys, implement automatic rotation

## Troubleshooting

### Common Issues

1. **"Unable to load AWS credentials"**
    - Verify environment variables are set correctly
    - Check IAM role permissions for container/instance authentication
    - Ensure credentials haven't expired

2. **"Region not specified"**
    - Set the `aws.region` configuration property
    - Set the `AWS_REGION` environment variable
    - Use explicit region in AWS client builders

3. **"Access Denied" errors**
    - Verify IAM permissions for the specific AWS service
    - Check resource-based policies (S3 bucket policies, etc.)
    - Ensure cross-account trust relationships are configured

4. **"Configuration validation failed"**
    - Check JSON schema compliance in configuration
    - Verify all required properties are present
    - Check for typos in configuration keys

### Debug Configuration

Enable debug logging to troubleshoot authentication issues:

```yaml
log4j2:
  Configuration:
    Loggers:
      logger:
        - name: "dev.xorcery.aws.auth"
          level: "debug"
        - name: "software.amazon.awssdk.auth"
          level: "debug"
```

### Testing Authentication

Create a simple test to verify authentication is working:

```java
@Test
public void testAwsAuthentication() {
    AwsCredentials credentials = credentialsProvider.resolveCredentials();
    assertNotNull(credentials.accessKeyId());
    assertNotNull(credentials.secretAccessKey());
}
```

## Integration Examples

### With Other Xorcery Modules

The AWS auth module works seamlessly with other Xorcery components:

```yaml
# Integration with secrets management
secrets:
  aws:
    enabled: true
    region: "${aws.region}"

aws:
  auth:
    accessKeyId: "secret:aws/access-key"
    secretAccessKey: "secret:aws/secret-key"
```
