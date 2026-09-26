package com.lllbllllb.plugins.publisher.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;

import java.util.List;

/**
 * Installs publications into the session's local repository through the Resolver, which also
 * writes the local {@code maven-metadata} for every artifact. An artifact already there is
 * overwritten.
 */
@Slf4j
@RequiredArgsConstructor
public class LocalInstaller {

    private final RepositorySystem repositorySystem;

    public void install(RepositorySystemSession session, List<Publication> publications) throws InstallationException {
        var request = new InstallRequest();

        publications.forEach(publication -> publication.artifacts().forEach(request::addArtifact));

        repositorySystem.install(session, request);

        publications.forEach(publication -> log.info("Installed {} into {}",
                publication.coordinates(), session.getLocalRepository().getBasedir()));
    }
}
