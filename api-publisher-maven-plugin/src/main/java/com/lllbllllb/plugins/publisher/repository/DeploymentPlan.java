package com.lllbllllb.plugins.publisher.repository;

import org.apache.maven.plugin.MojoFailureException;

import java.util.List;

/**
 * The outcome of checking every publication against its target repository, before anything is
 * deployed: what to deploy, what is already published unchanged, and every reason not to deploy
 * at all.
 *
 * @param deployments what to deploy, one entry per target repository
 * @param skipped     releases already published with identical bytes
 * @param failures    every problem found, one rendered message each; any failure blocks the whole
 *                    deployment
 */
public record DeploymentPlan(List<Deployment> deployments, List<Publication> skipped, List<String> failures) {

    public DeploymentPlan {
        deployments = List.copyOf(deployments);
        skipped = List.copyOf(skipped);
        failures = List.copyOf(failures);
    }

    public boolean isValid() {
        return failures.isEmpty();
    }

    public boolean hasNothingToDeploy() {
        return deployments.stream().allMatch(deployment -> deployment.publications().isEmpty());
    }

    public String failureMessage() {
        var message = new StringBuilder("api-publisher cannot deploy, %d problem(s) found, nothing was deployed:".formatted(failures.size()));

        failures.forEach(failure -> message.append(System.lineSeparator()).append("  - ").append(failure));

        return message.toString();
    }

    /**
     * Fails the build with every collected problem in one message, or returns the deployments.
     */
    public List<Deployment> requireValid() throws MojoFailureException {
        if (!isValid()) {
            throw new MojoFailureException(failureMessage());
        }

        return deployments;
    }
}
