# Reactive Services Architecture

Proof of concept for Reactive Services Architecture.

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

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
[ASL2](https://choosealicense.com/licenses/apache-2.0/)
