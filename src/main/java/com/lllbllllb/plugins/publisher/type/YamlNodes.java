package com.lllbllllb.plugins.publisher.type;

import lombok.experimental.UtilityClass;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.util.Optional;

/**
 * Navigation over a composed (not constructed) YAML node tree. Working on nodes rather than on
 * loaded Java objects is what keeps a scalar's literal text: an unquoted {@code 1.10} is the
 * string {@code "1.10"} here, never the double {@code 1.1}.
 */
@UtilityClass
public class YamlNodes {

    public Optional<Node> child(MappingNode mapping, String key) {
        return mapping.getValue().stream()
                .filter(tuple -> tuple.getKeyNode() instanceof ScalarNode scalarKey && key.equals(scalarKey.getValue()))
                .map(NodeTuple::getValueNode)
                .findFirst();
    }

    public boolean hasKey(MappingNode mapping, String key) {
        return child(mapping, key).isPresent();
    }

    public Optional<MappingNode> childMapping(MappingNode mapping, String key) {
        return child(mapping, key)
                .filter(MappingNode.class::isInstance)
                .map(MappingNode.class::cast);
    }

    public Optional<String> childScalarText(MappingNode mapping, String key) {
        return child(mapping, key)
                .filter(ScalarNode.class::isInstance)
                .map(node -> ((ScalarNode) node).getValue());
    }
}
