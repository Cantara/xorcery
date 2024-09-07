package com.exoreaction.xorcery.configuration.jsonschema.maven;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.configuration.jsonschema.ConfigurationSchemaBuilder;
import com.exoreaction.xorcery.json.JsonMerger;
import com.exoreaction.xorcery.jsonschema.JsonSchema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
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

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/xorcery-schema.yaml", readonly = true)
    private String schemaExtensionsFile;

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/xorcery-schema.json", required = true, readonly = true)
    private String generatedSchemaFile;

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/xorcery-override-schema.json", readonly = true)
    private String generatedOverrideSchemaFile;

    public void execute() throws MojoExecutionException, MojoFailureException {

        if (!project.getPackaging().equals("jar"))
        {
            getLog().info("Packaging is not a jar, skipping JSON schema generation");
            return;
        }

        try {
            List<File> dependencyJarFiles = getDependencyJarFiles();
            generateModuleSchema(dependencyJarFiles);
            generateModuleOverrideSchema(dependencyJarFiles);

        } catch (Throwable e) {
            throw new MojoFailureException("Could not create JSON Schema for xorcery.yaml configuration", e);
        }
    }

    private void generateModuleSchema(List<File> dependencyJarFiles) throws IOException {
        File xorceryConfigFile = new File(configurationFile);
        File xorceryConfigJsonSchemaExtensionFile = new File(schemaExtensionsFile);

        if (!xorceryConfigFile.exists() && !xorceryConfigJsonSchemaExtensionFile.exists()) {
            return; // Nothing here
        }

        File xorceryConfigJsonSchemaFile = new File(generatedSchemaFile);
        getLog().debug("META-INF/xorcery.yaml: " + xorceryConfigFile + "(" + xorceryConfigFile.exists() + ")");
        getLog().debug("META-INF/xorcery-schema.yaml: " + xorceryConfigJsonSchemaExtensionFile + "(exists:" + xorceryConfigJsonSchemaExtensionFile.exists() + ")");
        getLog().debug("META-INF/xorcery-schema.json: " + xorceryConfigJsonSchemaFile + "(exists:" + xorceryConfigJsonSchemaFile.exists() + ")");

        ConfigurationBuilder moduleBuilder = new ConfigurationBuilder();
        ConfigurationBuilder moduleWithDependenciesBuilder = new ConfigurationBuilder();

        if (!xorceryConfigFile.getName().equals("xorcery-defaults.yaml")) {
            List<URL> dependencyJarURLs = new ArrayList<>(dependencyJarFiles.size());
            for (File dependencyJarFile : dependencyJarFiles) {
                dependencyJarURLs.add(dependencyJarFile.toURI().toURL());
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

        moduleWithDependenciesBuilder.addYaml("""
                    instance.host: "service"
                    instance.ip: "192.168.0.2"
                    """);

        if (xorceryConfigFile.exists()) {
            moduleBuilder.addFile(xorceryConfigFile);
            moduleWithDependenciesBuilder.addFile(xorceryConfigFile);
        }

        JsonSchema schema = new ConfigurationSchemaBuilder()
                .id("http://xorcery.exoreaction.com/modules/" + project.getGroupId() + "/" + project.getArtifactId() + "/schema")
                .title(project.getArtifactId() + " configuration JSON Schema")
                .generateJsonSchema(moduleBuilder.builder().builder(), moduleWithDependenciesBuilder.build().json());

        ObjectNode schemaJson = schema.json();

        // Add extensions
        if (xorceryConfigJsonSchemaExtensionFile.exists()) {
            YAMLMapper yamlMapper = new YAMLMapper();
            JsonNode extensions = yamlMapper.readTree(xorceryConfigJsonSchemaExtensionFile);
            schemaJson = new JsonMerger().merge(schemaJson, (ObjectNode) extensions);
        }

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

    private void generateModuleOverrideSchema(List<File> artifactJars) throws IOException {
        JsonSchema schema = new JsonSchema.Builder()
                .id("http://xorcery.exoreaction.com/applications/" + project.getGroupId() + "/" + project.getArtifactId() + "/override-schema")
                .title(project.getArtifactId() + " configuration override JSON Schema")
                .build();

        ObjectNode uberSchema = schema.json();
        JsonMapper jsonMapper = new JsonMapper();
        JsonMerger jsonMerger = new JsonMerger();
        for (File artifactJar : artifactJars) {
            try (JarFile jarFile = new JarFile(artifactJar)) {
                ZipEntry zipEntry = jarFile.getEntry("META-INF/xorcery-schema.json");
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
        getLog().debug("xorcery-override-schema.json: " + xorceryConfigOverrideJsonSchemaFile + "(" + xorceryConfigOverrideJsonSchemaFile.exists() + ")");

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
