package com.lllbllllb.plugins.publisher.type;

import org.yaml.snakeyaml.nodes.MappingNode;

import java.util.Optional;
import java.util.Set;

/**
 * Base for the specification formats that keep their version in {@code info.version} and are
 * written as YAML or JSON; a subclass names only its folder and root key.
 */
public abstract class InfoVersionTypeHandler implements SchemaTypeHandler {

    private static final Set<String> YAML_OR_JSON = Set.of("yaml", "yml", "json");

    @Override
    public Set<String> acceptedExtensions() {
        return YAML_OR_JSON;
    }

    @Override
    public Optional<String> readVersion(MappingNode document) {
        return InfoVersionReader.read(document);
    }
}
