package com.lllbllllb.plugins.publisher.repository;

import org.eclipse.aether.repository.RemoteRepository;

import java.util.Optional;

/**
 * Where releases and where snapshots are deployed, with authentication and proxy already applied.
 * Either may be absent, when the project declares no repository for it.
 *
 * @param release  the repository for release versions
 * @param snapshot the repository for {@code -SNAPSHOT} versions
 */
public record DeploymentRepositories(Optional<RemoteRepository> release, Optional<RemoteRepository> snapshot) {

    public Optional<RemoteRepository> forVersion(boolean isSnapshot) {
        return isSnapshot ? snapshot : release;
    }
}
