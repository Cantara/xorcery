package com.exoreaction.xorcery.configuration.jsonschema.maven;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.json.JsonMerger;
import com.exoreaction.xorcery.jsonschema.JsonSchema;
import com.exoreaction.xorcery.jsonschema.generator.SchemaByExampleGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

@Mojo(name = "module-configuration-jsonschema", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class ModuleConfigurationJsonSchemaMojo extends JsonSchemaCommonMojo {

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/xorcery.yaml")
    private String configurationFile;

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/${project.artifactId}-schema.json", required = true, readonly = true)
    private String generatedSchemaFile;

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/${project.artifactId}-override-schema.json", readonly = true)
    private String generatedOverrideSchemaFile;

    @Parameter(defaultValue = "${project.basedir}/src/test/resources/META-INF/${project.artifactId}-override-schema.json", readonly = true)
    private String generatedTestOverrideSchemaFile;

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
            throw new MojoFailureException("Could not create JSON Schema for xorcery.yaml configuration", e);
        }
    }

    private void generateModuleSchema(List<Artifact> dependencies) throws IOException {
        File xorceryConfigFile = new File(configurationFile);

        if (!xorceryConfigFile.exists()) {
            return; // Nothing here
        }

        File xorceryConfigJsonSchemaFile = new File(generatedSchemaFile);
        getLog().debug("META-INF/xorcery.yaml: " + xorceryConfigFile + "(" + xorceryConfigFile.exists() + ")");
        getLog().debug("Module schema: " + xorceryConfigJsonSchemaFile + "(exists:" + xorceryConfigJsonSchemaFile.exists() + ")");

        ConfigurationBuilder moduleBuilder = new ConfigurationBuilder();
        ConfigurationBuilder moduleWithDependenciesBuilder = new ConfigurationBuilder();

        if (!xorceryConfigFile.getName().equals("xorcery-defaults.yaml")) {
            List<URL> dependencyJarURLs = new ArrayList<>(dependencies.size());
            for (Artifact dependency : dependencies) {
                dependencyJarURLs.add(dependency.getFile().toURI().toURL());
            }
            try (URLClassLoader dependenciesClassLoader = new URLClassLoader(dependencyJarURLs.toArray(new URL[0]), getClass().getClassLoader())) {
                Thread.currentThread().setContextClassLoader(dependenciesClassLoader);
                moduleWithDependenciesBuilder
                        .addDefault()
                        .addModules()
                        .addModuleOverrides()
                        .addApplication()
                        .addConfigurationProviders();
                Thread.currentThread().setContextClassLoader(null);
            }
        }

        if (xorceryConfigFile.exists()) {
            moduleBuilder.addFile(xorceryConfigFile);
            moduleWithDependenciesBuilder.addFile(xorceryConfigFile);
        }

        moduleWithDependenciesBuilder.addYaml("""
                    instance.host: "service"
                    instance.ip: "192.168.0.2"
                    """);

        JsonSchema schema = new SchemaByExampleGenerator()
                .id("http://xorcery.exoreaction.com/modules/" + project.getGroupId() + "/" + project.getArtifactId() + "/schema")
                .title(project.getArtifactId() + " configuration JSON Schema")
                .generateJsonSchema(moduleBuilder.builder().builder(), moduleWithDependenciesBuilder.build().json());

        ObjectNode schemaJson = schema.json();

        JsonMapper jsonMapper = new JsonMapper();

        // Read existing schema and see if it has changed
        if (xorceryConfigJsonSchemaFile.exists()) {
            if (jsonMapper.readTree(xorceryConfigJsonSchemaFile) instanceof ObjectNode existingSchema) {
                if (existingSchema.equals(schemaJson))
                    return;

                // Remove properties in existing schema that are not in the generated schema
                pruneExistingSchema(existingSchema, schemaJson);

                // Merge existing schema into generated schema
                schemaJson = new JsonMerger().merge(schemaJson, existingSchema);
            }
        }

        xorceryConfigJsonSchemaFile.getParentFile().mkdirs();
        jsonMapper.writerWithDefaultPrettyPrinter().writeValue(xorceryConfigJsonSchemaFile, schemaJson);
        getLog().info("Updated module configuration JSON Schema definition: " + xorceryConfigJsonSchemaFile);
    }

    private void generateModuleOverrideSchema(List<Artifact> dependencies) throws IOException {
        JsonSchema schema = new JsonSchema.Builder()
                .id("http://xorcery.exoreaction.com/applications/" + project.getGroupId() + "/" + project.getArtifactId() + "/override-schema")
                .title(project.getArtifactId() + " configuration override JSON Schema")
                .build();

        ObjectNode uberSchema = schema.json();
        JsonMapper jsonMapper = new JsonMapper();
        JsonMerger jsonMerger = new JsonMerger();
        for (Artifact dependency : dependencies) {
            try (JarFile jarFile = new JarFile(dependency.getFile())) {
                ZipEntry zipEntry = jarFile.getEntry("META-INF/"+dependency.getArtifactId()+"-schema.json");
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
                            uberSchema = jsonMerger.merge(uberSchema, moduleSchema);
                        }
                    }
                }
            } catch (Throwable e) {
                getLog().error(e);
            }
        }

        File xorceryConfigOverrideJsonSchemaFile = new File(generatedOverrideSchemaFile);
        File xorceryTestConfigOverrideJsonSchemaFile = new File(generatedTestOverrideSchemaFile);
        getLog().debug("Module override JSON schema: " + xorceryConfigOverrideJsonSchemaFile + "(" + xorceryConfigOverrideJsonSchemaFile.exists() + ")");
        getLog().debug("Module test override JSON schema: " + xorceryTestConfigOverrideJsonSchemaFile + "(" + xorceryTestConfigOverrideJsonSchemaFile.exists() + ")");

        // Merge in own module schema
        File xorceryConfigJsonSchemaFile = new File(generatedSchemaFile);
        if (xorceryConfigJsonSchemaFile.exists())
        {
            try (InputStream configStream = new FileInputStream(xorceryConfigJsonSchemaFile))
            {
                if (jsonMapper.readTree(configStream) instanceof ObjectNode moduleSchema)
                {
                    moduleSchema.remove("title");
                    moduleSchema.remove("$id");
                    uberSchema = jsonMerger.merge(uberSchema, moduleSchema);
                }
            }
        }

        // Read existing schema and see if it has changed
        if (xorceryConfigOverrideJsonSchemaFile.exists())
        {
            JsonNode existingSchema = jsonMapper.readTree(xorceryConfigOverrideJsonSchemaFile);
            if (existingSchema.equals(uberSchema))
                return;
        }

        // Check if empty
        if (uberSchema.path("properties").isEmpty())
        {
            // Don't write an empty schema
            getLog().info("No configuration found in dependencies, skipping module configuration override JSON Schema");
            return;
        }

        xorceryConfigOverrideJsonSchemaFile.getParentFile().mkdirs();
        jsonMapper.writerWithDefaultPrettyPrinter().writeValue(xorceryConfigOverrideJsonSchemaFile, uberSchema);
        xorceryTestConfigOverrideJsonSchemaFile.getParentFile().mkdirs();
        jsonMapper.writerWithDefaultPrettyPrinter().writeValue(xorceryTestConfigOverrideJsonSchemaFile, uberSchema);
        getLog().info("Updated module configuration override JSON Schema definition: "+xorceryConfigOverrideJsonSchemaFile);
    }

    private void pruneExistingSchema(ObjectNode existingSchema, ObjectNode generatedSchema) {
        JsonNode generatedSchemaRootProperties = generatedSchema.path("properties");
        for (Map.Entry<String, JsonNode> property : new HashSet<>(existingSchema.path("properties").properties())) {
            if (generatedSchemaRootProperties.get(property.getKey()) instanceof ObjectNode) {
                if (existingSchema.path("$defs").path(property.getKey()) instanceof ObjectNode existingObject) {
                    if (generatedSchema.path("$defs").path(property.getKey()) instanceof ObjectNode generatedObject) {
                        pruneExistingSchema0(existingObject, generatedObject);
                    }
                }
            } else {
                existingSchema.remove(property.getKey());
                if (existingSchema.path("$defs") instanceof ObjectNode defs) {
                    defs.remove(property.getKey());
                }
            }
        }
    }

    // Prune properties from existing schema that are missing in the generated schema
    private void pruneExistingSchema0(ObjectNode existingObject, ObjectNode generatedObject) {
        if (existingObject.path("properties") instanceof ObjectNode existingProperties) {
            for (Map.Entry<String, JsonNode> property : new HashSet<>(existingProperties.properties())) {
                if (generatedObject.path("properties").get(property.getKey()) instanceof ObjectNode propertyNode) {
                    if (propertyNode.has("properties")) {
                        if (property.getValue() instanceof ObjectNode existingChildProperty)
                        {
                            pruneExistingSchema0(existingChildProperty, propertyNode);
                        }
                    }
                } else {
                    existingProperties.remove(property.getKey());
                }
            }
        }
    }
}
