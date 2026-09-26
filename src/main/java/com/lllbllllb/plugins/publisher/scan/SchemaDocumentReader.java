package com.lllbllllb.plugins.publisher.scan;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.reader.UnicodeReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Parses a YAML or JSON schema file into a node tree without constructing Java objects from it, so
 * scalars keep their literal text and no type tag in the file is ever instantiated.
 */
public class SchemaDocumentReader {

    /**
     * SnakeYAML's default of 3 MB is below what a large specification can reach.
     */
    private static final int CODE_POINT_LIMIT = 64 * 1024 * 1024;

    /**
     * The document's root node, or empty for an empty file.
     *
     * @throws IOException                              when the file cannot be read
     * @throws org.yaml.snakeyaml.error.YAMLException when it is not a single well-formed document
     */
    public Optional<Node> read(Path file) throws IOException {
        var options = new LoaderOptions();
        options.setCodePointLimit(CODE_POINT_LIMIT);

        try (var reader = new UnicodeReader(Files.newInputStream(file))) {
            return Optional.ofNullable(new Yaml(options).compose(reader));
        }
    }
}
