package com.lllbllllb.plugins.publisher.type;

import org.yaml.snakeyaml.nodes.MappingNode;

import java.util.Optional;
import java.util.Set;

/**
 * One schema type the publisher understands, keyed by the name of its folder under
 * {@code src/main/resources}. Adding a type means adding one implementation and registering it in
 * {@link SchemaTypeRegistry}; the scan treats any folder without a handler as unsupported.
 */
public interface SchemaTypeHandler {

    /**
     * The immediate subdirectory of {@code src/main/resources} this type answers to, which is also
     * the last segment of the published groupId.
     */
    String folderName();

    /**
     * File extensions this type accepts, lower case and without the leading dot.
     */
    Set<String> acceptedExtensions();

    /**
     * The top-level key whose presence proves a document belongs to this type.
     */
    String rootKey();

    /**
     * The document's version exactly as written in the source, or empty when it is absent, blank
     * or not a scalar.
     */
    Optional<String> readVersion(MappingNode document);
}
