package com.lllbllllb.plugins.publisher.repository;

import lombok.RequiredArgsConstructor;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.DefaultSessionData;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Downloads what a remote repository already holds under an artifact's coordinates.
 *
 * <p>The download goes through a session of its own, over an empty local repository and with no
 * workspace reader, cache or session data shared with the build. Otherwise the Resolver would
 * answer from the user's local repository — which the {@code install} goal has just filled with
 * the very files being deployed — and every comparison would trivially find identical bytes.
 */
@RequiredArgsConstructor
public class PublishedArtifactFetcher {

    private static final String SIMPLE_LAYOUT = "simple";

    private final RepositorySystem repositorySystem;

    /**
     * A copy of {@code session} that keeps its authentication, proxies, transport and checksum
     * settings, but resolves into {@code localRepository} and nowhere else.
     */
    public RepositorySystemSession isolatedSession(RepositorySystemSession session, Path localRepository) {
        var isolated = new DefaultRepositorySystemSession(session);

        isolated.setWorkspaceReader(null);
        isolated.setCache(new DefaultRepositoryCache());
        isolated.setData(new DefaultSessionData());
        isolated.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(
                isolated, new LocalRepository(localRepository.toFile(), SIMPLE_LAYOUT)));

        return isolated;
    }

    /**
     * The published file, or empty when {@code repository} has nothing under these coordinates.
     *
     * @throws ArtifactResolutionException when the repository could not answer — unreachable,
     *                                     unauthorized, a checksum failure — so absence cannot be
     *                                     told apart from an error and nothing may be deployed
     */
    public Optional<Path> fetch(RepositorySystemSession isolatedSession, Artifact artifact, RemoteRepository repository)
            throws ArtifactResolutionException {

        var coordinates = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getClassifier(), artifact.getExtension(), artifact.getVersion());
        var request = new ArtifactRequest(coordinates, List.of(repository), null);

        try {
            return Optional.of(repositorySystem.resolveArtifact(isolatedSession, request).getArtifact().getFile().toPath());
        } catch (ArtifactResolutionException e) {
            if (e.getResult() != null && e.getResult().isMissing()) {
                return Optional.empty();
            }

            throw e;
        }
    }
}
