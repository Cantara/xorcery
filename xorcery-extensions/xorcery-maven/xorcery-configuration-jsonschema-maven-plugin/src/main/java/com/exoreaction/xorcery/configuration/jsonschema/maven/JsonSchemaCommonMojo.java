package com.exoreaction.xorcery.configuration.jsonschema.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class JsonSchemaCommonMojo
        extends AbstractMojo {
    /**
     * Gives access to the Maven project information.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "${repositorySystemSession}")
    RepositorySystemSession repoSession;

    @Component
    MavenSession session;

    @Component
    RepositorySystem repositorySystem;

    @Component(hint = "default")
    DependencyCollectorBuilder dependencyCollectorBuilder;

    /**
     * The dependency graph builder to use.
     */
    @Component(hint = "default")
    DependencyGraphBuilder dependencyGraphBuilder;

    List<File> getDependencyJarFiles() throws DependencyGraphBuilderException {
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
                    getLog().debug("Using JSON Schema from:" + artifact.getArtifactId() + ":" + artifact.getFile());
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
        artifactJars.sort(Comparator.comparing(File::getName));
        return artifactJars;
    }
}
