package com.lllbllllb.plugins.publisher.repository;

import com.lllbllllb.plugins.publisher.scan.SchemaCandidate;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns validated candidates into {@link Publication}s, writing each one's POM on the way.
 */
@RequiredArgsConstructor
public class PublicationFactory {

    private final PomWriter pomWriter;

    public PublicationFactory() {
        this(new PomWriter());
    }

    /**
     * @param candidates    the scan's validated candidates
     * @param pomsDirectory where the generated POMs are written, under the build directory
     */
    public List<Publication> create(List<SchemaCandidate> candidates, Path pomsDirectory) throws IOException {
        var publications = new ArrayList<Publication>(candidates.size());

        for (var candidate : candidates) {
            publications.add(Publication.of(candidate, pomWriter.write(candidate, pomsDirectory)));
        }

        return publications;
    }
}
