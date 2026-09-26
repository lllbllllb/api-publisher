package com.lllbllllb.plugins.publisher.repository;

import com.lllbllllb.plugins.publisher.scan.SchemaCandidate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the minimal POM that is published next to each schema, so the schema is a well-formed
 * Maven artifact that repository managers index and that {@code maven-metadata.xml} can list.
 *
 * <p>The POM carries the coordinates and, as its packaging, the schema's extension. It declares no
 * dependencies and no parent: the schema file itself is the whole artifact.
 */
public class PomWriter {

    /**
     * Joined from lines rather than written as a text block: the plugin descriptor generator parses
     * every source file with QDox, which cannot read a text block that contains quotes.
     */
    private static final String TEMPLATE = String.join("\n",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"",
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">",
            "    <modelVersion>4.0.0</modelVersion>",
            "    <groupId>%s</groupId>",
            "    <artifactId>%s</artifactId>",
            "    <version>%s</version>",
            "    <packaging>%s</packaging>",
            "</project>",
            "");

    /**
     * Writes the POM of one candidate under {@code directory}, one subfolder per groupId, and
     * returns its path. An existing file is overwritten.
     */
    public Path write(SchemaCandidate candidate, Path directory) throws IOException {
        var pom = directory
                .resolve(candidate.groupId())
                .resolve("%s-%s.pom".formatted(candidate.artifactId(), candidate.version()));

        Files.createDirectories(pom.getParent());
        Files.writeString(pom, render(candidate), StandardCharsets.UTF_8);

        return pom;
    }

    String render(SchemaCandidate candidate) {
        return TEMPLATE.formatted(
                escape(candidate.groupId()),
                escape(candidate.artifactId()),
                escape(candidate.version()),
                escape(candidate.extension()));
    }

    private static String escape(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
