package dev.xorcery.configuration.jsonschema.maven;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.jsonschema.JsonSchema;
import dev.xorcery.jsonschema.generator.SchemaMerger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

@Mojo(name = "application-configuration-jsonschema", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class ApplicationConfigurationJsonSchemaMojo extends JsonSchemaCommonMojo {

    @Parameter(defaultValue = "xorcery")
    public String baseName;

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/")
    private String mainMetaInf;

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/")
    private String mainResources;

    public void execute() throws MojoExecutionException, MojoFailureException {

        if (!project.getPackaging().equals("jar")) {
            getLog().info("Packaging is not a jar, skipping JSON schema generation");
            return;
        }

        try {

            List<Artifact> dependencies = getDependencies();

            JsonSchema uberSchema = new JsonSchema.Builder().build();
            JsonMapper jsonMapper = new JsonMapper();
            SchemaMerger schemaMerger = new SchemaMerger();
            for (Artifact dependency : dependencies) {
                if (!dependency.getFile().getName().endsWith(".jar"))
                    continue;
                try (JarFile jarFile = new JarFile(dependency.getFile())) {
                    ZipEntry zipEntry = jarFile.getEntry("META-INF/" + baseName + "-schema.json");
                    if (zipEntry != null) {
                        try (InputStream configStream = jarFile.getInputStream(zipEntry)) {
                            if (jsonMapper.readTree(configStream) instanceof ObjectNode moduleSchema) {
                                moduleSchema.remove("title");
                                moduleSchema.remove("$id");
                                uberSchema = schemaMerger.combine(uberSchema, new JsonSchema(moduleSchema));
                            }
                        }
                    }
                } catch (Throwable e) {
                    getLog().error("Could not read schema from " + dependency.getFile().toString(), e);
                }
            }

            uberSchema = new JsonSchema.Builder(uberSchema.json())
                    .title(baseName + " application configuration JSON Schema")
                    .build();


            // Merge in own schema
            File customApplicationSchemaFile = new File(mainMetaInf+baseName+"-schema.json");
            if (customApplicationSchemaFile.exists()) {
                try (InputStream configStream = new FileInputStream(customApplicationSchemaFile)) {
                    if (jsonMapper.readTree(configStream) instanceof ObjectNode moduleSchema) {
                        moduleSchema.remove("title");
                        moduleSchema.remove("$id");
                        uberSchema = schemaMerger.combine(uberSchema, new JsonSchema(moduleSchema));
                    }
                }
            }

            // Check if empty
            if (uberSchema.json().size() == 2) {
                return;
            }

            File applicationConfigJsonSchemaFile = new File(mainResources+baseName+"-schema.json");
            getLog().debug(baseName+"-schema.json: " + applicationConfigJsonSchemaFile + "(" + applicationConfigJsonSchemaFile.exists() + ")");

            // Read existing schema and see if it has changed
            if (applicationConfigJsonSchemaFile.exists()) {
                JsonNode existingSchema = jsonMapper.readTree(applicationConfigJsonSchemaFile);
                if (existingSchema.equals(uberSchema.json()))
                    return;
            }

            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(applicationConfigJsonSchemaFile, uberSchema.json());
            getLog().info("Updated application configuration JSON Schema definition: " + applicationConfigJsonSchemaFile);
        } catch (Throwable e) {
            throw new MojoFailureException("Could not create JSON Schema for "+baseName+".yaml configuration", e);
        }
    }
}
