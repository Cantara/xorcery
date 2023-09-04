# Xorcery

![GitHub tag (latest SemVer)](https://img.shields.io/github/v/tag/Cantara/xorcery) 
![Build Status](https://jenkins.quadim.ai/buildStatus/icon?Cantara%20%20xorcery) ![GitHub commit activity](https://img.shields.io/github/commit-activity/y/Cantara/xorcery) [![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](http://www.repostatus.org/badges/latest/active.svg)](http://www.repostatus.org/#active) [![Known Vulnerabilities](https://snyk.io/test/github/Cantara/xorcery/badge.svg)](https://snyk.io/test/github/Cantara/xorcery)

Xorcery, a boostrap like library designed to help you grow your solution and helping the developers with common service design features, such as REST API clients and servers, as well as reactive streaming of data in a way that doesn't suck.

## Installation

Run mvn install to compile everything.

## JVM version requirement
Xorcery has been designed with Java 17 features, so use that or newer in your own projects.

## Adding Xorcery to your project

To add Xorcery to your own project, you can use the following Maven BOM artifact. After that you can refer to the individual modules without specifying versions explicitly.
Note that we use our own artifact repository, so make sure to include that as well.

```bash
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.exoreaction.xorcery</groupId>
            <artifactId>xorcery-bom</artifactId>
            <version>${xorcery.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        ...
    </dependencies>
</dependencyManagement>
            
<dependencies>
    ...
    <dependency>
        <groupId>com.exoreaction.xorcery</groupId>
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

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
[ASL2](https://choosealicense.com/licenses/apache-2.0/)
