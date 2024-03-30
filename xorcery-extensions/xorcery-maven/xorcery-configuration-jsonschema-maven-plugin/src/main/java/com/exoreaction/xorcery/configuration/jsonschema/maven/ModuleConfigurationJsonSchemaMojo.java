package com.exoreaction.xorcery.configuration.jsonschema.maven;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.jsonschema.ConfigurationSchemaBuilder;
import com.exoreaction.xorcery.json.JsonMerger;
import com.exoreaction.xorcery.jsonschema.JsonSchema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;

@Mojo(name = "module-configuration-jsonschema", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class ModuleConfigurationJsonSchemaMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/xorcery.yaml", required = false, readonly = true)
    private String configurationFile;

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/xorcery-schema.yaml", required = false, readonly = true)
    private String schemaExtensionsFile;

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/xorcery-schema.json", required = true, readonly = true)
    private String generatedSchemaFile;

    /**
     * Gives access to the Maven project information.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {

        try {
            File xorceryConfigFile = new File(configurationFile);
            File xorceryConfigJsonSchemaExtensionFile = new File(schemaExtensionsFile);

            if (!xorceryConfigFile.exists() && !xorceryConfigJsonSchemaExtensionFile.exists())
            {
                return; // Nothing here
            }

            File xorceryConfigJsonSchemaFile = new File(generatedSchemaFile);
            getLog().debug("META-INF/xorcery.yaml: " + xorceryConfigFile + "(" + xorceryConfigFile.exists() + ")");
            getLog().debug("META-INF/xorcery-schema.yaml: " + xorceryConfigJsonSchemaExtensionFile + "(exists:" + xorceryConfigJsonSchemaExtensionFile.exists() + ")");
            getLog().debug("META-INF/xorcery-schema.json: " + xorceryConfigJsonSchemaFile + "(exists:" + xorceryConfigJsonSchemaFile.exists() + ")");

            Configuration.Builder builder = new Configuration.Builder();
            StandardConfigurationBuilder standardConfigurationBuilder = new StandardConfigurationBuilder();

            if (xorceryConfigFile.exists())
                standardConfigurationBuilder.addFile(xorceryConfigFile).accept(builder);

            JsonSchema schema = new ConfigurationSchemaBuilder()
                    .id("http://xorcery.exoreaction.com/modules/"+project.getGroupId()+"/"+project.getArtifactId()+"/schema")
                    .title(project.getArtifactId()+" configuration JSON Schema")
                    .generateJsonSchema(builder.builder(), builder.build().json());

            ObjectNode schemaJson = schema.json();

            // Add extensions
            if (xorceryConfigJsonSchemaExtensionFile.exists())
            {
                YAMLMapper yamlMapper = new YAMLMapper();
                JsonNode extensions = yamlMapper.readTree(xorceryConfigJsonSchemaExtensionFile);
                schemaJson = new JsonMerger().merge(schemaJson, (ObjectNode) extensions);
            }

            JsonMapper jsonMapper = new JsonMapper();

            // Read existing schema and see if it has changed
            if (xorceryConfigJsonSchemaFile.exists())
            {
                JsonNode existingSchema = jsonMapper.readTree(xorceryConfigJsonSchemaFile);
                if (existingSchema.equals(schemaJson))
                    return;
            }

            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(xorceryConfigJsonSchemaFile, schemaJson);
            getLog().info("Updated module configuration JSON Schema definition: "+xorceryConfigJsonSchemaFile);
        } catch (IOException e) {
            throw new MojoFailureException("Could not create JSON Schema for xorcery.yaml configuration", e);
        }
    }
}
