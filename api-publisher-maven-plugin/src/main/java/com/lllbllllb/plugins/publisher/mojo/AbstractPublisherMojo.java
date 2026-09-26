package com.lllbllllb.plugins.publisher.mojo;

import com.lllbllllb.plugins.publisher.repository.Publication;
import com.lllbllllb.plugins.publisher.repository.PublicationFactory;
import com.lllbllllb.plugins.publisher.scan.SchemaCandidate;
import com.lllbllllb.plugins.publisher.scan.SchemaScanner;
import com.lllbllllb.plugins.publisher.type.SchemaTypeRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * What both goals share: the parameters, the scan and its validation, and the POM generation.
 *
 * <p>The whole resources tree is scanned and validated before a goal publishes anything; a single
 * violation anywhere fails the build with every violation listed, and nothing is published.
 */
@Slf4j
public abstract class AbstractPublisherMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Component
    protected RepositorySystem repositorySystem;

    /**
     * Prepended, with a dot, to each type folder's name to form that type's groupId:
     * {@code com.lllbllllb.api} and {@code openapi} give {@code com.lllbllllb.api.openapi}.
     */
    @Parameter(property = "api-publisher.groupIdPrefix", required = true)
    protected String groupIdPrefix;

    /**
     * Skips the goal.
     */
    @Parameter(property = "api-publisher.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * The directory whose immediate subdirectories are the schema types.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources", required = true)
    protected File resourcesDirectory;

    /**
     * Where the generated POMs and other working files are written.
     */
    @Parameter(defaultValue = "${project.build.directory}/api-publisher", readonly = true, required = true)
    protected File workDirectory;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            log.info("api-publisher {} skipped", goal());

            return;
        }

        var candidates = scan();

        if (candidates.isEmpty()) {
            return;
        }

        publish(publications(candidates));
    }

    /**
     * The goal's name, for log lines.
     */
    protected abstract String goal();

    /**
     * Publishes a validated, non-empty set of schemas.
     */
    protected abstract void publish(List<Publication> publications) throws MojoExecutionException, MojoFailureException;

    private List<SchemaCandidate> scan() throws MojoExecutionException, MojoFailureException {
        if (groupIdPrefix == null || groupIdPrefix.isBlank()) {
            throw new MojoFailureException("groupIdPrefix must not be blank: configure it or pass -Dapi-publisher.groupIdPrefix");
        }

        try {
            return new SchemaScanner(SchemaTypeRegistry.defaults())
                    .scan(resourcesDirectory.toPath(), groupIdPrefix)
                    .requireValid();
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot scan %s: %s".formatted(resourcesDirectory, e.getMessage()), e);
        }
    }

    private List<Publication> publications(List<SchemaCandidate> candidates) throws MojoExecutionException {
        var pomsDirectory = workDirectory.toPath().resolve("poms");

        try {
            return new PublicationFactory().create(candidates, pomsDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot write the generated POMs to %s: %s".formatted(pomsDirectory, e.getMessage()), e);
        }
    }
}
