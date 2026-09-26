package com.lllbllllb.plugins.publisher.type;

import lombok.experimental.UtilityClass;
import org.yaml.snakeyaml.nodes.MappingNode;

import java.util.Optional;

/**
 * Reads {@code info.version}, the version field OpenAPI and AsyncAPI share, as the scalar's literal
 * text, trimmed. Absent, blank and non-scalar values all read as empty.
 */
@UtilityClass
public class InfoVersionReader {

    public static final String INFO = "info";

    public static final String VERSION = "version";

    public Optional<String> read(MappingNode document) {
        return YamlNodes.childMapping(document, INFO)
                .flatMap(info -> YamlNodes.childScalarText(info, VERSION))
                .map(String::trim)
                .filter(version -> !version.isEmpty());
    }
}
