package com.exoreaction.xorcery.configuration.jsonschema.maven;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.jsonschema.ConfigurationSchemaBuilder;
import com.exoreaction.xorcery.json.JsonMerger;
import com.exoreaction.xorcery.jsonschema.JsonSchema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

@Mojo(name = "application-configuration-jsonschema", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class ApplicationConfigurationJsonSchemaMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/xorcery-schema.json")
    private String applicationSchemaFile;

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/xorcery-schema.json")
    private String generatedSchemaFile;

    /**
     * Gives access to the Maven project information.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}")
    private List<RemoteRepository> projectRepos;

    @Component
    private MavenSession session;

    @Component
    protected RepositorySystem repositorySystem;

    @Component(hint = "default")
    private DependencyCollectorBuilder dependencyCollectorBuilder;

    /**
     * The dependency graph builder to use.
     */
    @Component(hint = "default")
    private DependencyGraphBuilder dependencyGraphBuilder;

    public void execute() throws MojoExecutionException, MojoFailureException {

        try {

            List<File> artifactJars = getDependencyJarFiles();

            JsonSchema schema = new JsonSchema.Builder()
                    .id("http://xorcery.exoreaction.com/applications/" + project.getGroupId() + "/" + project.getArtifactId() + "/schema")
                    .title(project.getArtifactId() + " configuration JSON Schema")
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
                                uberSchema = jsonMerger.merge(uberSchema, moduleSchema);
                            }
                        }
                    }
                } catch (Throwable e) {
                    getLog().error(e);
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

    private List<File> getDependencyJarFiles() throws DependencyGraphBuilderException {
        ProjectBuildingRequest buildingRequest =
                new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());

        buildingRequest.setProject(project);

        DependencyNode rootNode = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, null);

        final List<File> artifactJars = new ArrayList<>();
        rootNode.accept(new DependencyNodeVisitor() {
            @Override
            public boolean visit(DependencyNode dependencyNode) {
                ArtifactResolutionRequest request = new ArtifactResolutionRequest();
                request.setArtifact(dependencyNode.getArtifact());
                ArtifactResolutionResult result = repositorySystem.resolve(request);
                for (Artifact artifact : result.getArtifacts()) {
                    getLog().debug("Using JSON Schema from:"+artifact.getArtifactId() + ":" + artifact.getFile());
                    if (artifact.getFile() != null) {
                        artifactJars.add(artifact.getFile());
                    }
                }
                return true;
            }

            @Override
            public boolean endVisit(DependencyNode dependencyNode) {
                return true;
            }
        });
        return artifactJars;
    }
}
