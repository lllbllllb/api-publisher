package com.lllbllllb.plugins.publisher.repository;

import com.lllbllllb.plugins.publisher.scan.SchemaCandidate;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.nio.file.Path;
import java.util.List;

/**
 * One schema ready to publish: the schema file as the main artifact and its generated POM, under
 * the same coordinates.
 *
 * @param candidate the scanned schema the two artifacts come from
 * @param schema    the schema file, with the source file's extension and no classifier
 * @param pom       the generated POM
 */
public record Publication(SchemaCandidate candidate, Artifact schema, Artifact pom) {

    public static Publication of(SchemaCandidate candidate, Path pomFile) {
        var schema = new DefaultArtifact(candidate.groupId(), candidate.artifactId(), "", candidate.extension(), candidate.version())
                .setFile(candidate.file().toFile());
        var pom = new DefaultArtifact(candidate.groupId(), candidate.artifactId(), "", "pom", candidate.version())
                .setFile(pomFile.toFile());

        return new Publication(candidate, schema, pom);
    }

    public List<Artifact> artifacts() {
        return List.of(schema, pom);
    }

    public boolean isSnapshot() {
        return schema.isSnapshot();
    }

    public String coordinates() {
        return candidate.coordinates();
    }
}
