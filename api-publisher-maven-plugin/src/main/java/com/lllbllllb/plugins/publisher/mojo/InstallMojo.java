package com.lllbllllb.plugins.publisher.mojo;

import com.lllbllllb.plugins.publisher.repository.LocalInstaller;
import com.lllbllllb.plugins.publisher.repository.Publication;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.aether.installation.InstallationException;

import java.util.List;

/**
 * Installs every schema under {@code src/main/resources}, each with a generated POM, into the local
 * repository. Anything already installed under the same coordinates is overwritten.
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true)
public class InstallMojo extends AbstractPublisherMojo {

    @Override
    protected String goal() {
        return "install";
    }

    @Override
    protected void publish(List<Publication> publications) throws MojoExecutionException {
        try {
            new LocalInstaller(repositorySystem).install(session.getRepositorySession(), publications);
        } catch (InstallationException e) {
            throw new MojoExecutionException("Cannot install the schemas: " + e.getMessage(), e);
        }
    }
}
