package com.lllbllllb.plugins.publisher.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

/**
 * Deploys publications to remote repositories, all or nothing.
 *
 * <p>{@link #plan} checks every publication first. A release is compared with what its repository
 * already holds under the same coordinates: nothing there means deploy, identical bytes mean skip,
 * different bytes mean a failure asking for a new {@code info.version}. A snapshot is always
 * deployed. {@link #deploy} is then called only for a plan without a single failure, so a
 * conflict in one schema never leaves the others half-published.
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteDeployer {

    private final RepositorySystem repositorySystem;

    private final PublishedArtifactFetcher fetcher;

    public RemoteDeployer(RepositorySystem repositorySystem) {
        this(repositorySystem, new PublishedArtifactFetcher(repositorySystem));
    }

    /**
     * @param checkDirectory where a throw-away local repository is created for downloading the
     *                       published files; it is removed again before this method returns
     */
    public DeploymentPlan plan(
            RepositorySystemSession session,
            List<Publication> publications,
            DeploymentRepositories repositories,
            Path checkDirectory) throws IOException, ArtifactResolutionException {

        Files.createDirectories(checkDirectory);

        var downloads = Files.createTempDirectory(checkDirectory, "published-");

        try {
            return plan(fetcher.isolatedSession(session, downloads), publications, repositories);
        } finally {
            deleteRecursively(downloads);
        }
    }

    private DeploymentPlan plan(
            RepositorySystemSession isolatedSession,
            List<Publication> publications,
            DeploymentRepositories repositories) throws IOException, ArtifactResolutionException {

        var repositoriesByKey = new LinkedHashMap<String, RemoteRepository>();
        var publicationsByKey = new LinkedHashMap<String, List<Publication>>();
        var skipped = new ArrayList<Publication>();
        var failures = new ArrayList<String>();

        for (var publication : publications) {
            var target = repositories.forVersion(publication.isSnapshot());

            if (target.isEmpty()) {
                failures.add("no %s repository to deploy %s to: declare it in distributionManagement or set altDeploymentRepository"
                        .formatted(publication.isSnapshot() ? "snapshot" : "release", publication.coordinates()));

                continue;
            }

            var repository = target.get();

            if (!publication.isSnapshot()) {
                var published = fetcher.fetch(isolatedSession, publication.schema(), repository);

                if (published.isPresent() && isIdentical(published.get(), publication.candidate().file())) {
                    log.info("Skipping {}: already published to {} with identical content", publication.coordinates(), describe(repository));
                    skipped.add(publication);

                    continue;
                }

                if (published.isPresent()) {
                    failures.add("%s is already published to %s with different content: bump info.version in %s"
                            .formatted(publication.coordinates(), describe(repository), publication.candidate().file()));

                    continue;
                }
            }

            var key = repository.getId() + " " + repository.getUrl();

            repositoriesByKey.putIfAbsent(key, repository);
            publicationsByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(publication);
        }

        var deployments = publicationsByKey.entrySet().stream()
                .map(entry -> new Deployment(repositoriesByKey.get(entry.getKey()), entry.getValue()))
                .toList();

        return new DeploymentPlan(deployments, skipped, failures);
    }

    public void deploy(RepositorySystemSession session, List<Deployment> deployments) throws DeploymentException {
        for (var deployment : deployments) {
            var request = new DeployRequest().setRepository(deployment.repository());

            deployment.publications().forEach(publication -> publication.artifacts().forEach(request::addArtifact));

            repositorySystem.deploy(session, request);

            deployment.publications().forEach(publication -> log.info("Deployed {} to {}",
                    publication.coordinates(), describe(deployment.repository())));
        }
    }

    private static boolean isIdentical(Path published, Path local) throws IOException {
        return Files.mismatch(published, local) == -1L;
    }

    private static String describe(RemoteRepository repository) {
        return "%s (%s)".formatted(repository.getId(), repository.getUrl());
    }

    private static void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            for (var path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
