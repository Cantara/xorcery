package dev.xorcery.configuration.jsonschema.maven;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.jsonschema.JsonSchema;
import dev.xorcery.jsonschema.generator.SchemaByExampleGenerator;
import dev.xorcery.jsonschema.generator.SchemaMerger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

@Mojo(name = "module-configuration-jsonschema", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class ModuleConfigurationJsonSchemaMojo extends JsonSchemaCommonMojo {

    @Parameter(defaultValue = "xorcery")
    public String baseName;

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/")
    private String mainMetaInf;

    @Parameter(defaultValue = "${project.basedir}/src/test/resources/META-INF/")
    private String testMetaInf;

    public void execute() throws MojoExecutionException, MojoFailureException {

        if (!project.getPackaging().equals("jar"))
        {
            getLog().info("Packaging is not a jar, skipping JSON schema generation");
            return;
        }

        try {
            List<Artifact> dependencies = getDependencies();
            generateModuleSchema(dependencies);
            generateModuleOverrideSchema(dependencies);

        } catch (Throwable e) {
            throw new MojoFailureException("Could not create JSON Schema for "+baseName+".yaml configuration", e);
        }
    }

    private void generateModuleSchema(List<Artifact> dependencies) throws IOException {
        File configFile = new File(mainMetaInf+baseName+".yaml");

        if (!configFile.exists()) {
            return; // Nothing here
        }

        File configJsonSchemaFile = new File(mainMetaInf+baseName+"-schema.json");
        getLog().debug("META-INF/"+baseName+".yaml: " + configFile + "(" + configFile.exists() + ")");
        getLog().debug("Module schema: " + configJsonSchemaFile + "(exists:" + configJsonSchemaFile.exists() + ")");

        ConfigurationBuilder moduleBuilder = new ConfigurationBuilder();
        ConfigurationBuilder moduleWithDependenciesBuilder = new ConfigurationBuilder();

        List<URL> dependencyJarURLs = new ArrayList<>(dependencies.size());
        for (Artifact dependency : dependencies) {
            dependencyJarURLs.add(dependency.getFile().toURI().toURL());
        }
        try (URLClassLoader dependenciesClassLoader = new URLClassLoader(dependencyJarURLs.toArray(new URL[0]), getClass().getClassLoader())) {
            Thread.currentThread().setContextClassLoader(dependenciesClassLoader);
            moduleWithDependenciesBuilder
                    .addModules()
                    .addModuleOverrides()
                    .addApplication()
                    .addConfigurationProviders();
            Thread.currentThread().setContextClassLoader(null);
        }

        if (configFile.exists()) {
            moduleBuilder.addFile(configFile);
            moduleWithDependenciesBuilder.addFile(configFile);
        }

        if (baseName.equals("xorcery")){
            moduleWithDependenciesBuilder.addYaml("""
                    instance.host: "service"
                    instance.ip: "192.168.0.2"
                    """);
        }

        JsonSchema schema = new SchemaByExampleGenerator()
                .title(baseName + ".yaml JSON Schema")
                .generateJsonSchema(moduleBuilder.builder().builder(), moduleWithDependenciesBuilder.build().json());

        JsonMapper jsonMapper = new JsonMapper();

        // Read existing schema and see if it has changed
        if (configJsonSchemaFile.exists()) {
            if (jsonMapper.readTree(configJsonSchemaFile) instanceof ObjectNode existingSchema) {
                if (existingSchema.equals(schema.json()))
                    return;

                schema = new SchemaMerger().mergeGenerated(new JsonSchema(existingSchema), schema);

                // Check post-merge as well
                if (schema.json().equals(existingSchema))
                    return;
            }
        }

        configJsonSchemaFile.getParentFile().mkdirs();
        jsonMapper.writerWithDefaultPrettyPrinter().writeValue(configJsonSchemaFile, schema.json());
        getLog().info("Updated "+baseName+" JSON Schema definition: " + configJsonSchemaFile);
    }

    private void generateModuleOverrideSchema(List<Artifact> dependencies) throws IOException {
        JsonSchema uberSchema = new JsonSchema.Builder()
                .title(baseName + ".yaml override JSON Schema")
                .build();
        JsonMapper jsonMapper = new JsonMapper();
        SchemaMerger schemaMerger = new SchemaMerger();
        for (Artifact dependency : dependencies) {
            if (!dependency.getFile().getName().endsWith(".jar"))
                continue;
            if (dependency.getScope().equals("test"))
                continue;
            try (JarFile jarFile = new JarFile(dependency.getFile())) {
                ZipEntry zipEntry = jarFile.getEntry("META-INF/"+baseName+"-schema.json");
                if (zipEntry != null) {
                    try (InputStream configStream = jarFile.getInputStream(zipEntry))
                    {
                        if (jsonMapper.readTree(configStream) instanceof ObjectNode moduleSchema)
                        {
                            moduleSchema.remove("title");
                            moduleSchema.remove("$id");
                            if (moduleSchema.path("properties") instanceof ObjectNode properties)
                            {
                                properties.remove("$schema");
                            }
                            uberSchema = schemaMerger.combine(uberSchema, new JsonSchema(moduleSchema));
                        }
                    }
                }
            } catch (Throwable e) {
                getLog().error("Could not read schema from "+dependency.getFile().toString(), e);
            }
        }

        File configOverrideJsonSchemaFile = new File(mainMetaInf+baseName+"-override-schema.json");
        File testConfigOverrideJsonSchemaFile = new File(testMetaInf+baseName+"-test-override-schema.json");
        getLog().debug("Module override JSON schema: " + configOverrideJsonSchemaFile + "(" + configOverrideJsonSchemaFile.exists() + ")");
        getLog().debug("Module test override JSON schema: " + testConfigOverrideJsonSchemaFile + "(" + testConfigOverrideJsonSchemaFile.exists() + ")");

        // Merge in own module schema
        File configJsonSchemaFile = new File(mainMetaInf+baseName+"-schema.json");
        if (configJsonSchemaFile.exists())
        {
            try (InputStream configStream = new FileInputStream(configJsonSchemaFile))
            {
                if (jsonMapper.readTree(configStream) instanceof ObjectNode moduleSchema)
                {
                    moduleSchema.remove("title");
                    moduleSchema.remove("$id");
                    uberSchema = schemaMerger.combine(uberSchema, new JsonSchema(moduleSchema));
                }
            }
        }

        // Read existing schema and see if it has changed
        if (configOverrideJsonSchemaFile.exists())
        {
            JsonNode existingSchema = jsonMapper.readTree(configOverrideJsonSchemaFile);
            if (existingSchema.equals(uberSchema.json()))
                return;
        }

        // Check if empty
        if (uberSchema.json().path("properties").isEmpty())
        {
            // Don't write an empty schema
            getLog().info("No configuration found in dependencies, skipping module configuration override JSON Schema");
            return;
        }

        configOverrideJsonSchemaFile.getParentFile().mkdirs();
        jsonMapper.writerWithDefaultPrettyPrinter().writeValue(configOverrideJsonSchemaFile, uberSchema.json());
        testConfigOverrideJsonSchemaFile.getParentFile().mkdirs();
        jsonMapper.writerWithDefaultPrettyPrinter().writeValue(testConfigOverrideJsonSchemaFile, uberSchema.json());
        getLog().info("Updated module configuration override JSON Schema definition: "+configOverrideJsonSchemaFile);
    }
}
