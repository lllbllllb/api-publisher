package com.lllbllllb.plugins.publisher.type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaTypeRegistryTest {

    private final SchemaTypeRegistry registry = SchemaTypeRegistry.defaults();

    @Test
    void supportsOpenApiAndAsyncApi() {
        assertThat(registry.find("openapi")).get()
                .satisfies(handler -> {
                    assertThat(handler.rootKey()).isEqualTo("openapi");
                    assertThat(handler.acceptedExtensions()).containsExactlyInAnyOrder("yaml", "yml", "json");
                });
        assertThat(registry.find("asyncapi")).get()
                .satisfies(handler -> {
                    assertThat(handler.rootKey()).isEqualTo("asyncapi");
                    assertThat(handler.acceptedExtensions()).containsExactlyInAnyOrder("yaml", "yml", "json");
                });
    }

    @Test
    void hasNoHandlerForAnyOtherFolder() {
        assertThat(registry.find("flatbuffers")).isEmpty();
        assertThat(registry.find("OpenAPI")).isEmpty();
    }
}
