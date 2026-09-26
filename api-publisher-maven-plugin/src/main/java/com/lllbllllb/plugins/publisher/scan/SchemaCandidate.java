package com.lllbllllb.plugins.publisher.scan;

import java.nio.file.Path;

/**
 * One schema file that passed validation, with the coordinates it will be published under.
 *
 * @param groupId    the configured prefix, a dot, and the type folder name
 * @param artifactId the file name without its extension
 * @param version    the document's version, literal text as written in the source
 * @param file       the absolute path of the schema file, which is the artifact itself
 * @param extension  the source file's extension, lower case and without the dot
 */
public record SchemaCandidate(String groupId, String artifactId, String version, Path file, String extension) {

    public String coordinates() {
        return "%s:%s:%s:%s".formatted(groupId, artifactId, extension, version);
    }
}
