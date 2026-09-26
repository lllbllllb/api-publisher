package com.lllbllllb.plugins.publisher.scan;

import com.lllbllllb.plugins.publisher.type.SchemaTypeRegistry;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class SchemaScannerTest {

    private static final String PREFIX = "com.lllbllllb.api";

    private final SchemaScanner scanner = new SchemaScanner(SchemaTypeRegistry.defaults());

    @TempDir
    private Path resources;

    @Test
    void mapsEveryFileToItsCoordinatesIgnoringSubfolders() throws IOException {
        var openapi = write("openapi/auth/auth-admin-api.yaml", openapi("0.0.1"));
        var asyncapi = write("asyncapi/auth/auth-api.yaml", asyncapi("1.0.0"));

        var result = scan();

        assertThat(result.isValid()).isTrue();
        assertThat(result.candidates())
                .extracting(SchemaCandidate::groupId, SchemaCandidate::artifactId, SchemaCandidate::version,
                        SchemaCandidate::file, SchemaCandidate::extension)
                .containsExactly(
                        tuple("com.lllbllllb.api.asyncapi", "auth-api", "1.0.0", asyncapi, "yaml"),
                        tuple("com.lllbllllb.api.openapi", "auth-admin-api", "0.0.1", openapi, "yaml"));
    }

    @Test
    void acceptsYmlAndJsonAndKeepsTheirExtension() throws IOException {
        write("openapi/a.yml", openapi("1.0.0"));
        write("openapi/nested/deep/b.JSON", "{\n\t\"openapi\": \"3.0.3\",\n\t\"info\": {\"version\": \"2.0.0\"}\n}\n");

        var result = scan();

        assertThat(result.isValid()).as(result.violations().toString()).isTrue();
        assertThat(result.candidates())
                .extracting(SchemaCandidate::artifactId, SchemaCandidate::version, SchemaCandidate::extension)
                .containsExactly(tuple("a", "1.0.0", "yml"), tuple("b", "2.0.0", "json"));
    }

    @Test
    void keepsTheLiteralTextOfAnUnquotedVersion() throws IOException {
        write("openapi/api.yaml", "openapi: 3.0.3\ninfo:\n  title: api\n  version: 1.10\n");

        assertThat(scan().candidates()).extracting(SchemaCandidate::version).containsExactly("1.10");
    }

    @Test
    void failsOnAFolderWithNoHandler() throws IOException {
        write("openapi/api.yaml", openapi("1.0.0"));
        write("flatbuffers/game.fbs", "table Game {}");

        var result = scan();

        assertThat(result.isValid()).isFalse();
        assertThat(result.violations()).containsExactly("unsupported types found: [flatbuffers]");
    }

    @Test
    void listsEveryUnsupportedTypeSorted() throws IOException {
        Files.createDirectories(resources.resolve("protobuf"));
        Files.createDirectories(resources.resolve("flatbuffers"));
        Files.createDirectories(resources.resolve("avro"));

        assertThat(scan().violations()).containsExactly("unsupported types found: [avro, flatbuffers, protobuf]");
    }

    @Test
    void ignoresFilesOutsideAnyTypeFolder() throws IOException {
        var loose = write("application.yaml", "server:\n  port: 8080\n");
        write("openapi/api.yaml", openapi("1.0.0"));

        var result = scan();

        assertThat(result.isValid()).isTrue();
        assertThat(result.ignoredFiles()).containsExactly(loose);
        assertThat(result.candidates()).extracting(SchemaCandidate::artifactId).containsExactly("api");
    }

    @Test
    void publishesNothingWithoutAResourcesDirectory() throws IOException {
        var result = scanner.scan(resources.resolve("missing"), PREFIX);

        assertThat(result.isValid()).isTrue();
        assertThat(result.candidates()).isEmpty();
    }

    @Test
    void publishesNothingFromAnEmptyResourcesDirectory() throws IOException {
        var result = scan();

        assertThat(result.isValid()).isTrue();
        assertThat(result.candidates()).isEmpty();
    }

    @Test
    void failsOnAFileTheTypeDoesNotAccept() throws IOException {
        write("openapi/auth/README.md", "# auth");
        write("asyncapi/Makefile", "all:");

        assertThat(scan().violations()).containsExactly(
                "unsupported file asyncapi/Makefile: type 'asyncapi' accepts only [json, yaml, yml]",
                "unsupported file openapi/auth/README.md: type 'openapi' accepts only [json, yaml, yml]");
    }

    @Test
    void failsOnADocumentOfAnotherTypeInTheFolder() throws IOException {
        write("openapi/auth-api.yaml", asyncapi("1.0.0"));

        assertThat(scan().violations()).containsExactly(
                "misplaced file openapi/auth-api.yaml: it has no root key 'openapi', so it is not a openapi document");
    }

    @Test
    void failsOnAnEmptyFileOrANonMappingDocument() throws IOException {
        write("openapi/empty.yaml", "");
        write("openapi/list.yaml", "- openapi\n- info\n");

        assertThat(scan().violations()).containsExactly(
                "misplaced file openapi/empty.yaml: it has no root key 'openapi', so it is not a openapi document",
                "misplaced file openapi/list.yaml: it has no root key 'openapi', so it is not a openapi document");
    }

    @Test
    void failsOnAMissingOrBlankVersion() throws IOException {
        write("openapi/no-info.yaml", "openapi: 3.0.3\n");
        write("openapi/no-version.yaml", "openapi: 3.0.3\ninfo:\n  title: x\n");
        write("asyncapi/blank-version.yaml", "asyncapi: 3.0.0\ninfo:\n  version: ' '\n");

        var result = scan();

        assertThat(result.candidates()).isEmpty();
        assertThat(result.violations()).containsExactly(
                "missing info.version in asyncapi/blank-version.yaml",
                "missing info.version in openapi/no-info.yaml",
                "missing info.version in openapi/no-version.yaml");
    }

    @Test
    void failsOnAFileThatIsNotWellFormed() throws IOException {
        write("openapi/broken.yaml", "openapi: 3.0.3\ninfo: [unclosed\n");

        assertThat(scan().violations()).singleElement().asString()
                .startsWith("unreadable file openapi/broken.yaml: ")
                .doesNotContain("\n");
    }

    @Test
    void failsOnTheSameArtifactIdInTwoSubfolders() throws IOException {
        write("openapi/auth/common-api.yaml", openapi("1.0.0"));
        write("openapi/room/common-api.yaml", openapi("2.0.0"));

        assertThat(scan().violations()).containsExactly(
                "duplicate artifact com.lllbllllb.api.openapi:common-api: [openapi/auth/common-api.yaml, openapi/room/common-api.yaml]");
    }

    @Test
    void failsOnTheSameBaseNameWithTwoExtensions() throws IOException {
        write("openapi/api.json", "{\"openapi\": \"3.0.3\", \"info\": {\"version\": \"1.0.0\"}}");
        write("openapi/api.yaml", openapi("1.0.0"));

        assertThat(scan().violations()).containsExactly(
                "duplicate artifact com.lllbllllb.api.openapi:api: [openapi/api.json, openapi/api.yaml]");
    }

    @Test
    void allowsTheSameArtifactIdUnderTwoTypes() throws IOException {
        write("openapi/auth-api.yaml", openapi("1.0.0"));
        write("asyncapi/auth-api.yaml", asyncapi("1.0.0"));

        var result = scan();

        assertThat(result.isValid()).isTrue();
        assertThat(result.candidates()).extracting(SchemaCandidate::groupId)
                .containsExactly("com.lllbllllb.api.asyncapi", "com.lllbllllb.api.openapi");
    }

    @Test
    void collectsEveryViolationIntoOneBuildFailure() throws IOException {
        write("openapi/valid.yaml", openapi("1.0.0"));
        write("openapi/notes.txt", "notes");
        write("openapi/a/dup.yaml", openapi("1.0.0"));
        write("openapi/b/dup.yaml", "openapi: 3.0.3\n");
        write("flatbuffers/game.fbs", "table Game {}");
        write("protobuf/game.proto", "message Game {}");

        var result = scan();

        assertThat(result.violations()).containsExactly(
                "unsupported types found: [flatbuffers, protobuf]",
                "missing info.version in openapi/b/dup.yaml",
                "unsupported file openapi/notes.txt: type 'openapi' accepts only [json, yaml, yml]",
                "duplicate artifact com.lllbllllb.api.openapi:dup: [openapi/a/dup.yaml, openapi/b/dup.yaml]");
        assertThatThrownBy(result::requireValid)
                .isInstanceOf(MojoFailureException.class)
                .hasMessageStartingWith("api-publisher found 4 problem(s) in " + resources.toAbsolutePath().normalize() + ":")
                .hasMessageContainingAll(result.violations().toArray(String[]::new));
    }

    @Test
    void returnsTheCandidatesWhenValid() throws IOException, MojoFailureException {
        write("openapi/api.yaml", openapi("1.0.0"));

        var result = scan();

        assertThat(result.requireValid()).isEqualTo(result.candidates()).hasSize(1);
    }

    @Test
    void rejectsABlankPrefix() {
        assertThatThrownBy(() -> scanner.scan(resources, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ScanResult scan() throws IOException {
        return scanner.scan(resources, PREFIX);
    }

    private Path write(String relativePath, String content) throws IOException {
        var file = resources.resolve(relativePath);
        Files.createDirectories(file.getParent());

        return Files.writeString(file, content);
    }

    private static String openapi(String version) {
        return "openapi: 3.0.3\ninfo:\n  title: test\n  version: %s\npaths: {}\n".formatted(version);
    }

    private static String asyncapi(String version) {
        return "asyncapi: 3.0.0\ninfo:\n  title: test\n  version: %s\n".formatted(version);
    }
}
