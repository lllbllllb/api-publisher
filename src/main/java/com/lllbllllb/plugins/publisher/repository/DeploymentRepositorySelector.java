package com.lllbllllb.plugins.publisher.repository;

import lombok.RequiredArgsConstructor;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import java.util.Optional;

/**
 * Chooses the deployment repositories the same way {@code maven-deploy-plugin} does.
 *
 * <p>An alternative repository, given as {@code id::url} (or the legacy {@code id::default::url}),
 * receives both releases and snapshots. Otherwise the project's {@code distributionManagement}
 * decides: releases go to its {@code repository}, snapshots to its {@code snapshotRepository},
 * falling back to {@code repository} when no snapshot repository is declared.
 *
 * <p>Every repository is passed through {@link RepositorySystem#newDeploymentRepository}, which
 * applies the credentials of the {@code settings.xml} server with the same id, and the session's
 * proxy for its URL. Mirrors are deliberately not applied: a deployment goes to the repository
 * named, never to a mirror of it.
 */
@RequiredArgsConstructor
public class DeploymentRepositorySelector {

    private static final String SEPARATOR = "::";

    private static final String DEFAULT_LAYOUT = "default";

    private final RepositorySystem repositorySystem;

    public DeploymentRepositories select(
            RepositorySystemSession session,
            DistributionManagement distributionManagement,
            String altDeploymentRepository) throws MojoFailureException {

        if (altDeploymentRepository != null && !altDeploymentRepository.isBlank()) {
            var alternative = Optional.of(forDeployment(session, parseAlternative(altDeploymentRepository.trim())));

            return new DeploymentRepositories(alternative, alternative);
        }

        if (distributionManagement == null) {
            return new DeploymentRepositories(Optional.empty(), Optional.empty());
        }

        var release = declared(distributionManagement.getRepository())
                .map(repository -> forDeployment(session, repository));
        var snapshot = declared(distributionManagement.getSnapshotRepository())
                .map(repository -> forDeployment(session, repository))
                .or(() -> release);

        return new DeploymentRepositories(release, snapshot);
    }

    private RemoteRepository forDeployment(RepositorySystemSession session, RemoteRepository repository) {
        return repositorySystem.newDeploymentRepository(session, repository);
    }

    private static Optional<RemoteRepository> declared(DeploymentRepository repository) {
        if (repository == null || repository.getUrl() == null || repository.getUrl().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new RemoteRepository.Builder(repository.getId(), DEFAULT_LAYOUT, repository.getUrl().trim()).build());
    }

    static RemoteRepository parseAlternative(String specification) throws MojoFailureException {
        var parts = specification.split(SEPARATOR, 3);

        String id;
        String url;

        if (parts.length == 2) {
            id = parts[0];
            url = parts[1];
        } else if (parts.length == 3 && DEFAULT_LAYOUT.equals(parts[1])) {
            id = parts[0];
            url = parts[2];
        } else {
            throw new MojoFailureException(
                    "Invalid altDeploymentRepository '%s': expected id::url".formatted(specification));
        }

        if (id.isBlank() || url.isBlank()) {
            throw new MojoFailureException(
                    "Invalid altDeploymentRepository '%s': expected id::url".formatted(specification));
        }

        return new RemoteRepository.Builder(id.trim(), DEFAULT_LAYOUT, url.trim()).build();
    }
}
