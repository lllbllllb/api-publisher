package com.lllbllllb.plugins.publisher.mojo;

import com.lllbllllb.plugins.publisher.repository.DeploymentRepositorySelector;
import com.lllbllllb.plugins.publisher.repository.Publication;
import com.lllbllllb.plugins.publisher.repository.RemoteDeployer;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.resolution.ArtifactResolutionException;

import java.io.IOException;
import java.util.List;

/**
 * Deploys every schema under {@code src/main/resources}, each with a generated POM, to the
 * project's {@code distributionManagement} repository — the release or the snapshot one, chosen by
 * each schema's version — or to {@link #altDeploymentRepository}.
 *
 * <p>A release already published with identical bytes is skipped; one published with different
 * bytes fails the build. Every schema is checked before the first one is deployed, and a single
 * failure deploys nothing.
 */
@Slf4j
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class DeployMojo extends AbstractPublisherMojo {

    /**
     * A repository that receives both releases and snapshots instead of the ones in
     * {@code distributionManagement}, as {@code id::url}. Credentials come from the
     * {@code settings.xml} server with that id.
     */
    @Parameter(property = "api-publisher.altDeploymentRepository")
    protected String altDeploymentRepository;

    @Override
    protected String goal() {
        return "deploy";
    }

    @Override
    protected void publish(List<Publication> publications) throws MojoExecutionException, MojoFailureException {
        var repositorySession = session.getRepositorySession();
        var repositories = new DeploymentRepositorySelector(repositorySystem)
                .select(repositorySession, project.getDistributionManagement(), altDeploymentRepository);
        var deployer = new RemoteDeployer(repositorySystem);

        try {
            var plan = deployer.plan(repositorySession, publications, repositories, workDirectory.toPath().resolve("remote-check"));
            var deployments = plan.requireValid();

            if (plan.hasNothingToDeploy()) {
                log.info("Every schema is already published with identical content, nothing to deploy");

                return;
            }

            deployer.deploy(repositorySession, deployments);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("Cannot check what is already published, nothing was deployed: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot compare with what is already published, nothing was deployed: " + e.getMessage(), e);
        } catch (DeploymentException e) {
            throw new MojoExecutionException("Cannot deploy the schemas: " + e.getMessage(), e);
        }
    }
}
