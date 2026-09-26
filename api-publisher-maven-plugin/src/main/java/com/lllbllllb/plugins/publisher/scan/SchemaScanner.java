package com.lllbllllb.plugins.publisher.scan;

import com.lllbllllb.plugins.publisher.type.SchemaTypeHandler;
import com.lllbllllb.plugins.publisher.type.SchemaTypeRegistry;
import com.lllbllllb.plugins.publisher.type.YamlNodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Walks a resources directory and turns every schema file into a {@link SchemaCandidate}.
 *
 * <p>Each immediate subdirectory is a type, looked up in the {@link SchemaTypeRegistry}; a known
 * type's folder is walked recursively, and its subfolders play no part in the coordinates. Files
 * directly in the resources directory are ignored. Nothing is thrown for a bad file: every
 * violation in the tree is collected into the {@link ScanResult}.
 */
@Slf4j
@RequiredArgsConstructor
public class SchemaScanner {

    private final SchemaTypeRegistry registry;

    private final SchemaDocumentReader documentReader;

    public SchemaScanner(SchemaTypeRegistry registry) {
        this(registry, new SchemaDocumentReader());
    }

    public ScanResult scan(Path resourcesDirectory, String groupIdPrefix) throws IOException {
        if (groupIdPrefix == null || groupIdPrefix.isBlank()) {
            throw new IllegalArgumentException("groupIdPrefix must not be blank");
        }

        var scan = new Scan(resourcesDirectory.toAbsolutePath().normalize(), groupIdPrefix.trim());

        if (!Files.isDirectory(scan.root)) {
            log.warn("No resources directory at {}, nothing to publish", scan.root);

            return scan.result();
        }

        for (var entry : sortedList(scan.root)) {
            var name = entry.getFileName().toString();

            if (Files.isDirectory(entry)) {
                registry.find(name).ifPresentOrElse(
                        handler -> scanType(scan, entry, handler),
                        () -> scan.unsupportedTypes.add(name));
            } else {
                log.info("Ignoring {}: it lies outside any type folder", scan.relative(entry));
                scan.ignoredFiles.add(entry);
            }
        }

        scan.collectDuplicates();

        var result = scan.result();

        if (result.isValid() && result.candidates().isEmpty()) {
            log.warn("No schema files found under {}, nothing to publish", scan.root);
        }

        return result;
    }

    private void scanType(Scan scan, Path typeDirectory, SchemaTypeHandler handler) {
        var groupId = scan.groupIdPrefix + "." + handler.folderName();

        try (Stream<Path> files = Files.walk(typeDirectory)) {
            files.filter(Files::isRegularFile)
                    .sorted()
                    .forEach(file -> scanFile(scan, file, groupId, handler));
        } catch (IOException e) {
            scan.problems.add("cannot walk %s: %s".formatted(scan.relative(typeDirectory), e.getMessage()));
        }
    }

    private void scanFile(Scan scan, Path file, String groupId, SchemaTypeHandler handler) {
        var relative = scan.relative(file);
        var fileName = file.getFileName().toString();
        var dot = fileName.lastIndexOf('.');
        var extension = dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);

        if (dot <= 0 || !handler.acceptedExtensions().contains(extension)) {
            scan.problems.add("unsupported file %s: type '%s' accepts only %s"
                    .formatted(relative, handler.folderName(), new TreeSet<>(handler.acceptedExtensions())));

            return;
        }

        var artifactId = fileName.substring(0, dot);

        scan.pathsByCoordinates.computeIfAbsent(groupId + ":" + artifactId, key -> new ArrayList<>()).add(relative);

        MappingNode document;
        try {
            document = documentReader.read(file)
                    .filter(MappingNode.class::isInstance)
                    .map(MappingNode.class::cast)
                    .filter(root -> YamlNodes.hasKey(root, handler.rootKey()))
                    .orElse(null);
        } catch (IOException | YAMLException e) {
            scan.problems.add("unreadable file %s: %s".formatted(relative, singleLine(e.getMessage())));

            return;
        }

        if (document == null) {
            scan.problems.add("misplaced file %s: it has no root key '%s', so it is not a %s document"
                    .formatted(relative, handler.rootKey(), handler.folderName()));

            return;
        }

        handler.readVersion(document).ifPresentOrElse(
                version -> scan.candidates.add(new SchemaCandidate(groupId, artifactId, version, file, extension)),
                () -> scan.problems.add("missing info.version in %s".formatted(relative)));
    }

    private static List<Path> sortedList(Path directory) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.sorted().toList();
        }
    }

    private static String singleLine(String message) {
        return message == null ? "" : message.strip().replaceAll("\\s*\\R\\s*", " ");
    }

    /**
     * The mutable state of one scan call, so the scanner itself stays stateless and reusable.
     */
    @RequiredArgsConstructor
    private static final class Scan {

        private final Path root;

        private final String groupIdPrefix;

        private final List<SchemaCandidate> candidates = new ArrayList<>();

        private final TreeSet<String> unsupportedTypes = new TreeSet<>();

        private final List<String> problems = new ArrayList<>();

        private final List<Path> ignoredFiles = new ArrayList<>();

        private final Map<String, List<String>> pathsByCoordinates = new LinkedHashMap<>();

        private String relative(Path path) {
            return root.relativize(path).toString().replace('\\', '/');
        }

        private void collectDuplicates() {
            pathsByCoordinates.forEach((coordinates, paths) -> {
                if (paths.size() > 1) {
                    problems.add("duplicate artifact %s: %s".formatted(coordinates, paths));
                }
            });
        }

        private ScanResult result() {
            return new ScanResult(root, candidates, unsupportedTypes, problems, ignoredFiles);
        }
    }
}
