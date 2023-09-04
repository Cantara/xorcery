# Xorcery

![GitHub tag (latest SemVer)](https://img.shields.io/github/v/tag/Cantara/xorcery) 
![Build Status](https://jenkins.quadim.ai/buildStatus/icon?Cantara%20%20xorcery) ![GitHub commit activity](https://img.shields.io/github/commit-activity/y/Cantara/xorcery) [![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](http://www.repostatus.org/badges/latest/active.svg)](http://www.repostatus.org/#active) [![Known Vulnerabilities](https://snyk.io/test/github/Cantara/xorcery/badge.svg)](https://snyk.io/test/github/Cantara/xorcery)

Xorcery, a boostrap like library designed to help you grow your solution and helping the developers with common service design features, such as REST API clients and servers, as well as reactive streaming of data in a way that doesn't suck.

## Installation


Run mvn install to compile everything

## Usage
The Neo4j service is not yet updated for JDK18 so a few JVM options need to be added.

```bash
java --add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED -Dfile.encoding=UTF-8 -classpath "<classpath>" com.exoreaction.xorcery.server.Main -id=server1
```

Add these to the IntelliJ JVM options tab if you are starting Main from within that environment.
On IntelliJ >2021.3.2: "Open 'Edit/run configurations' dialog" -> "Edit Configurations" -> "Choose run configuration for Application" -> under "Build and run" -> "Modify options" -> "Add VM options" -> Enter all options starting with "--add-...." 

You can also use mvn exec:exec to run it with the correct settings.
Alterative in intelliJ, which can run on IntelliJ-downloaded/confed JVM: "Maven" tab on the right hand side -> "manager" -> Plugins -> "exec" -> "exec:exec" 

## Adding xorcery to your project

You may want to xorcery to your porject, you may use maven like this

```bash
    <properties>
        <maven.compiler.release>17</maven.compiler.release>
        <typelib.version>0.30.1</typelib.version>
    </properties>

  <dependencies>
        ...
        <dependency>
            <groupId>com.exoreaction.xorcery</groupId>
            <artifactId>xorcery-dns-client</artifactId>
            <version>${xorcery.version}</version>
        </dependency>
        <dependency>
            <groupId>com.exoreaction.xorcery</groupId>
            <artifactId>xorcery-jetty-client</artifactId>
            <version>${xorcery.version}</version>
        </dependency>
        <dependency>
            <groupId>com.exoreaction.xorcery</groupId>
            <artifactId>xorcery-keystores</artifactId>
            <version>${xorcery.version}</version>
        </dependency>
        <dependency>
            <groupId>com.exoreaction.xorcery</groupId>
            <artifactId>xorcery-certificates</artifactId>
            <version>${xorcery.version}</version>
        </dependency>
        <dependency>
            <groupId>com.exoreaction.xorcery</groupId>
            <artifactId>xorcery-reactivestreams-api</artifactId>
            <version>${xorcery.version}</version>
        </dependency>
        <dependency>
            <groupId>com.exoreaction.xorcery</groupId>
            <artifactId>xorcery-reactivestreams-client</artifactId>
            <version>${xorcery.version}</version>
        </dependency>
        <dependency>
            <groupId>com.exoreaction.xorcery</groupId>
            <artifactId>xorcery-domainevents</artifactId>
            <version>${xorcery.version}</version>
        </dependency>
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


## Here are the commands to re-create the self-signed test keys and certificates
```
keytool -genkeypair -alias server -validity 9000 -keyalg RSA -keysize 2048 -keystore test-keystore.p12 -storetype pkcs12 -dname "CN=xorcery.cantara.no, OU=Unit, O=Cantara, L=Oslo, S=, C=Norway" -v
keytool -list -keystore test-keystore.p12
keytool -genkeypair -alias certkey -validity 9000 -keyalg RSA -keysize 2048 -keystore tmp.p12 -storetype pkcs12 -dname "CN=xorcery.cantara.no, OU=Unit, O=Cantara, L=Oslo, S=, C=Norway" -v
keytool --export -alias certkey -file tmp.cer -keystore tmp.p12
keytool -import -alias minica -file tmp.cer -keystore test-truststore.jks
keytool -list -keystore test-truststore.jks
```

## License
[ASL2](https://choosealicense.com/licenses/apache-2.0/)
