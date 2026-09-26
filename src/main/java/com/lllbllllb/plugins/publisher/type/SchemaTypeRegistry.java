package com.lllbllllb.plugins.publisher.type;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The handlers the publisher knows, by folder name. A folder that has no entry here is an
 * unsupported type.
 */
public class SchemaTypeRegistry {

    private final Map<String, SchemaTypeHandler> handlersByFolder;

    public SchemaTypeRegistry(Collection<? extends SchemaTypeHandler> handlers) {
        this.handlersByFolder = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(SchemaTypeHandler::folderName, Function.identity()));
    }

    /**
     * Every type supported today. Register a new handler here.
     */
    public static SchemaTypeRegistry defaults() {
        return new SchemaTypeRegistry(List.of(new OpenApiTypeHandler(), new AsyncApiTypeHandler()));
    }

    public Optional<SchemaTypeHandler> find(String folderName) {
        return Optional.ofNullable(handlersByFolder.get(folderName));
    }
}
