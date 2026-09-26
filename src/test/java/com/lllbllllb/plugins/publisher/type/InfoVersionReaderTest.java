package com.lllbllllb.plugins.publisher.type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

class InfoVersionReaderTest {

    @Test
    void keepsTheLiteralTextOfAnUnquotedNumber() {
        assertThat(InfoVersionReader.read(document("openapi: 3.0.3\ninfo:\n  version: 1.10\n"))).contains("1.10");
    }

    @ParameterizedTest
    @ValueSource(strings = {"'1.10'", "\"1.10\"", "1.10   "})
    void readsQuotedAndPaddedVersionsAsWritten(String written) {
        assertThat(InfoVersionReader.read(document("info:\n  version: " + written + "\n"))).contains("1.10");
    }

    @Test
    void keepsTextThatIsNotANumber() {
        assertThat(InfoVersionReader.read(document("info:\n  version: 2.0.0-SNAPSHOT\n"))).contains("2.0.0-SNAPSHOT");
    }

    @Test
    void readsJson() {
        assertThat(InfoVersionReader.read(document("{\"openapi\": \"3.0.3\", \"info\": {\"version\": 1.10}}"))).contains("1.10");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "openapi: 3.0.3\n",
            "info:\n  title: x\n",
            "info: 1.0.0\n",
            "info:\n  version: ''\n",
            "info:\n  version: '   '\n",
            "info:\n  version:\n",
            "info:\n  version:\n    major: 1\n",
            "info:\n  version: [1, 0]\n"
    })
    void readsNothingWhenTheVersionIsAbsentBlankOrNotAScalar(String yaml) {
        assertThat(InfoVersionReader.read(document(yaml))).isEmpty();
    }

    private static MappingNode document(String yaml) {
        return (MappingNode) new Yaml().compose(new StringReader(yaml));
    }
}
