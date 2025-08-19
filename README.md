# Xorcery

![GitHub tag (latest SemVer)](https://img.shields.io/github/v/tag/Cantara/xorcery)
![Build Status](https://jenkins.quadim.ai/buildStatus/icon?Cantara%20%20xorcery) ![GitHub commit activity](https://img.shields.io/github/commit-activity/y/Cantara/xorcery) [![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](http://www.repostatus.org/badges/latest/active.svg)](http://www.repostatus.org/#active) [![Known Vulnerabilities](https://snyk.io/test/github/Cantara/xorcery/badge.svg)](https://snyk.io/test/github/Cantara/xorcery)
[![Java Version](https://img.shields.io/badge/Java-21+-blue.svg)](https://openjdk.java.net/)

A set of Java libraries dedicated to helping you build better applications.

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Building from Source](#building-from-source)
- [Running Tests](#running-tests)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

## Features
Xorcery contains several core modules, which are used by most applications and services:
- Core: dependency injection and runtime level support through HK2
- [Configuration](xorcery-configuration/README.md): composable, overridable, uses YAML+JSON-SCHEMA for editing and validation
- JSON: JSON merging and reference resolving, primarily for configuration handling
- JUnit: helpers to run Xorcery in tests
- Log4j: integration of configuration and DI with Log4j2
- Util: various helper classes

The majority of the useful modules used to build are in extensions, which you can add to your own project as needed. 
- Certificate management
- DNS client and registration
- EventStore client
- Jersey server, for JAX-RS support
- Jetty client
- Jetty server
- JSON:API
- JSON-Schema
- JWT server
- Keystore management
- Kurrent client
- Maven plugins, with JSON-Schema generation for module configurations etc.
- OpenSearch client
- OpenTelemetry (SDK integration, exporters, module and JDK integrations)
- Reactive streams over websockets, integrated with Project Reactor/Flux
- Secrets access
- Status API
- Thymeleaf integration
- Build and runtime translation

Most modules are integrated with the configuration and OpenTelemetry observability support.

## Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **Java 21** or higher
- **Maven 3.6+** or **Gradle 6.0+**
- **Git** (for cloning the repository)

You can verify your Java installation:
```bash
java -version
javac -version
```

### Installation

#### Option 1: Maven (Recommended)

Add the Maven BOM dependency to your `pom.xml`:
```xml
<dependencyManagement>
  <dependency>
      <groupId>dev.xorcery</groupId>
      <artifactId>xorcery-bom</artifactId>
      <version>${xorcery.version}</version>
      <type>pom</type>
      <scope>import</scope>
  </dependency>
</dependencyManagement>
```
and then add any modules you want to use which are included in the BOM, including a reference to our repository server:
```xml
<dependencies>
    ...
    <dependency>
        <groupId>dev.xorcery</groupId>
        <artifactId>xorcery-dns-client</artifactId>
    </dependency>
    ...
</dependencies>

<repositories>
    ...
    <repository>
        <id>cantara-releases</id>
        <name>Cantara Release Repository</name>
        <url>https://mvnrepo.cantara.no/content/repositories/releases/</url>
    </repository>
    <repository>
        <id>cantara-snapshots</id>
        <name>Cantara Snapshot Repository</name>
        <url>https://mvnrepo.cantara.no/content/repositories/snapshots/</url>
    </repository>
</repositories>
```

#### Option 2: Clone and Build

```bash
git clone https://github.com/Cantara/xorcery.git
cd xorcery
mvn clean install
```

## Usage

### Basic Example

```java
import dev.xorcery.core.Xorcery;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;

public class Example {
    public static void main(String[] args) throws Throwable {
        // Add all module configuration and optional overrides, and then merge them into a single configuration
        Configuration configuration = new ConfigurationBuilder().addDefaults().build();
        try (Xorcery xorcery = new Xorcery(configuration)){
            // This will use HK2 to find any enabled services, instantiate them using DI, and optionally use
            // run-level semantics to get the application into a started state
        }
        // At this point the Xorcery instance has been closed/stopped, and all services are shutdown. 
        // Use shutdown hooks in your application for long-lived services that are stopped manually
    }
}
```
You can do this both for your applications and for running tests. Each named service automatically 
gets a feature flag in the configuration (e.g. service "foo" is controlled by "foo.enabled: true/false" in config), which makes it
straightforward to run Xorcery with all or a subset of the packaged services, depending on your needs.

## API Documentation
See Javadocs

## Building from Source

### Using Maven

```bash
# Clone the repository
git clone https://github.com/Cantara/xorcery.git
cd xorcery

# Compile and install all modules
mvn clean install
```

## Running Tests

### Unit Tests

```bash
# Maven
mvn test
```

## Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Quick Start for Contributors

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Setup

```bash
git clone https://github.com/Cantara/xorcery.git
cd xorcery
mvn clean install
```

### Code Style

This project follows [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). Please ensure your code adheres to these standards.

## FAQ

**Q: What Java versions are supported?**
A: Java 21 and higher are supported.

**Q: How do I report a bug?**
A: Please create an issue on GitHub with a detailed description and steps to reproduce.

**Q: Is this project production-ready?**
A: Yes, this project is actively used in production environments.

## License

This project is licensed under the ASLv2 License - see the [LICENSE](LICENSE) file for details.

## Contact

**Project Maintainer:** Rickard Oberg

- Email: rickard@exoreaction.com
- GitHub: [@rickardoberg](https://github.com/rickardoberg)
- LinkedIn: [Your Profile](https://www.linkedin.com/in/rickardoberg/)

## Acknowledgments

- Thanks to all contributors who have helped shape this project
- Built with ❤️ using Java

---

**⭐ If you find this project helpful, please give it a star on GitHub!**
