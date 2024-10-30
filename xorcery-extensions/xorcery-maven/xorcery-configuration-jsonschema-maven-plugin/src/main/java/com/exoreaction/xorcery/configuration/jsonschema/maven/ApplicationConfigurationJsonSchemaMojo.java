package com.exoreaction.xorcery.configuration.jsonschema.maven;

import com.exoreaction.xorcery.json.JsonMerger;
import com.exoreaction.xorcery.jsonschema.JsonSchema;
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
import java.io.InputStream;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

@Mojo(name = "application-configuration-jsonschema", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class ApplicationConfigurationJsonSchemaMojo extends JsonSchemaCommonMojo {

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/xorcery-schema.json")
    private String applicationSchemaFile;

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/xorcery-schema.json")
    private String generatedSchemaFile;

    public void execute() throws MojoExecutionException, MojoFailureException {

        try {

            List<Artifact> dependencies = getDependencies();

            JsonSchema schema = new JsonSchema.Builder()
                    .id("http://xorcery.exoreaction.com/applications/" + project.getGroupId() + "/" + project.getArtifactId() + "/schema")
                    .title(project.getArtifactId() + " configuration JSON Schema")
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
                                uberSchema = jsonMerger.merge(uberSchema, moduleSchema);
                            }
                        }
                    }
                } catch (Throwable e) {
                    getLog().error("Could not read schema from "+dependency.getFile().toString(), e);
                }
            }

            File applicationSchemaFile = new File(this.applicationSchemaFile);
            if (applicationSchemaFile.exists())
            {
                try (InputStream configStream = new FileInputStream(applicationSchemaFile))
                {
                    if (jsonMapper.readTree(configStream) instanceof ObjectNode moduleSchema)
                    {
                        moduleSchema.remove("title");
                        moduleSchema.remove("$id");
                        uberSchema = jsonMerger.merge(uberSchema, moduleSchema);
                    }
                }
            }

            // Check if empty
            if (uberSchema.size() == 2)
            {
                return;
            }

            File xorceryConfigJsonSchemaFile = new File(generatedSchemaFile);
            getLog().debug("xorcery-schema.json: " + xorceryConfigJsonSchemaFile + "(" + xorceryConfigJsonSchemaFile.exists() + ")");

            // Read existing schema and see if it has changed
            if (xorceryConfigJsonSchemaFile.exists())
            {
                JsonNode existingSchema = jsonMapper.readTree(xorceryConfigJsonSchemaFile);
                if (existingSchema.equals(uberSchema))
                    return;
            }

            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(xorceryConfigJsonSchemaFile, uberSchema);
            getLog().info("Updated application configuration JSON Schema definition: "+xorceryConfigJsonSchemaFile);
        } catch (Throwable e) {
            throw new MojoFailureException("Could not create JSON Schema for xorcery.yaml configuration", e);
        }
    }
}
