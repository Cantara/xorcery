/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.translation.resourcebundle.maven;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.secrets.Secrets;
import dev.xorcery.secrets.providers.EnvSecretsProvider;
import dev.xorcery.secrets.providers.SecretSecretsProvider;
import dev.xorcery.translation.api.Translation;
import dev.xorcery.translation.deepl.DeepLTranslationProvider;
import dev.xorcery.translation.resourcebundle.ResourceBundleTranslator;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
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
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Mojo(name = "translate-resourcebundle", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class ResourceBundleTranslatorMojo extends AbstractMojo {

    /**
     * Gives access to the Maven project information.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Component
    MavenSession session;

    @Component
    RepositorySystem repositorySystem;

    /**
     * The dependency graph builder to use.
     */
    @Component(hint = "default")
    DependencyGraphBuilder dependencyGraphBuilder;

    @Parameter(defaultValue = "${project.artifactId}", required = false, readonly = false)
    private String source;

    @Parameter(defaultValue = "en", required = true, readonly = false)
    private String sourceLocale;

    @Parameter(required = true, readonly = false)
    private List<String> targetLocales;

    @Parameter()
    private String result;

    public void execute() throws MojoExecutionException, MojoFailureException {

        try {
            ConfigurationBuilder moduleWithDependenciesBuilder = new ConfigurationBuilder();
            List<File> dependencyJarFiles = getDependencyJarFiles();
            List<URL> dependencyJarURLs = new ArrayList<>(dependencyJarFiles.size());
            for (File dependencyJarFile : dependencyJarFiles) {
                dependencyJarURLs.add(dependencyJarFile.toURI().toURL());
            }
            for (Resource resource : project.getResources()) {
                dependencyJarURLs.add(new File(resource.getDirectory()).toURI().toURL());
            }
            try (URLClassLoader dependenciesClassLoader = new URLClassLoader(dependencyJarURLs.toArray(new URL[0]), getClass().getClassLoader()))
            {
                Thread.currentThread().setContextClassLoader(dependenciesClassLoader);
                moduleWithDependenciesBuilder.addDefaults();


                Configuration configuration = moduleWithDependenciesBuilder.build();
                Secrets secrets = new Secrets(Map.of("env", new EnvSecretsProvider(), "secret", new SecretSecretsProvider())::get, configuration.getString("secrets.default").orElse("secret"));
                DeepLTranslationProvider translationProvider = new DeepLTranslationProvider(configuration, secrets);
                Translation translation = new Translation(translationProvider);
                ResourceBundleTranslator resourceBundleTranslator = new ResourceBundleTranslator(translation);

                File resultFile = result != null
                        ? new File(result)
                        : new File(project.getResources().get(0).getDirectory(), "META-INF/"+source+"-override.yaml");

                Locale sourceLocaleValue = Locale.forLanguageTag(sourceLocale);
                List<Locale> targetLocaleList = targetLocales.stream().map(Locale::forLanguageTag).toList();

                ObjectNode result = resourceBundleTranslator.translate(source, sourceLocaleValue, resultFile,
                                targetLocaleList)
                        .orTimeout(10, TimeUnit.SECONDS).join();

                if (!result.isEmpty())
                {
                    ObjectMapper yamlMapper = new YAMLMapper().findAndRegisterModules();
                    yamlMapper.writeValue(resultFile, result);
                }

                System.out.println(new Configuration(result));
            } finally
            {
                Thread.currentThread().setContextClassLoader(null);
            }
        } catch (Throwable e) {
            throw new MojoFailureException("Could not translate resource bundle "+source, e);
        }
    }

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
                    if (rootNode.getArtifact().equals(artifact))
                        continue; // Skip self
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
