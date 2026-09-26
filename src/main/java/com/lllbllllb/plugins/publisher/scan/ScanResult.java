package com.lllbllllb.plugins.publisher.scan;

import org.apache.maven.plugin.MojoFailureException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Everything one scan found: the candidates to publish and every violation, collected rather than
 * thrown on the first one, so a single build failure can name all of them.
 *
 * @param resourcesDirectory the directory that was scanned
 * @param candidates         schema files that passed every per-file check, in scan order
 * @param unsupportedTypes   folder names under the resources directory that have no handler
 * @param problems           every other violation, one rendered message each, in scan order
 * @param ignoredFiles       files lying directly in the resources directory, outside any type
 */
public record ScanResult(
        Path resourcesDirectory,
        List<SchemaCandidate> candidates,
        SortedSet<String> unsupportedTypes,
        List<String> problems,
        List<Path> ignoredFiles) {

    public static final String UNSUPPORTED_TYPES_FOUND = "unsupported types found: ";

    public ScanResult {
        candidates = List.copyOf(candidates);
        unsupportedTypes = Collections.unmodifiableSortedSet(new TreeSet<>(unsupportedTypes));
        problems = List.copyOf(problems);
        ignoredFiles = List.copyOf(ignoredFiles);
    }

    public boolean isValid() {
        return unsupportedTypes.isEmpty() && problems.isEmpty();
    }

    /**
     * Every violation as one message: the unsupported types first, as
     * {@code unsupported types found: [a, b]}, then every other problem.
     */
    public List<String> violations() {
        var violations = new ArrayList<String>();

        if (!unsupportedTypes.isEmpty()) {
            violations.add(UNSUPPORTED_TYPES_FOUND + unsupportedTypes);
        }
        violations.addAll(problems);

        return violations;
    }

    public String failureMessage() {
        var violations = violations();
        var message = new StringBuilder("api-publisher found %d problem(s) in %s:".formatted(violations.size(), resourcesDirectory));

        violations.forEach(violation -> message.append(System.lineSeparator()).append("  - ").append(violation));

        return message.toString();
    }

    /**
     * Fails the build with every violation in one message, or returns the candidates when there is
     * none.
     */
    public List<SchemaCandidate> requireValid() throws MojoFailureException {
        if (!isValid()) {
            throw new MojoFailureException(failureMessage());
        }

        return candidates;
    }
}
