# api-publisher

A Maven plugin (`com.lllbllllb.plugins:api-publisher-maven-plugin`) that turns a project's schema
files into versioned, directly fetchable artifacts. Attach it to any Maven project and it scans
that project's `src/main/resources`, treats every immediate subdirectory as a schema *type*, and
publishes each schema file it finds as an artifact of its own — not a jar, the raw schema file
itself, so any client technology (JS, Unity, Godot, Defold, …) can pull it by a plain repository
URL.

Supported types today: `openapi`, `asyncapi`. Any other subdirectory under `src/main/resources`
(for example `flatbuffers`) fails the build with `unsupported types found: [flatbuffers]`.

## Coordinates rule

For a schema file found at `src/main/resources/<type>/.../<name>.<ext>`:

| Coordinate | Value |
|---|---|
| `groupId` | `<prefix>.<type>` — e.g. prefix `com.lllbllllb.api` + type `openapi` → `com.lllbllllb.api.openapi` |
| `artifactId` | `<name>` — the file name without its extension, e.g. `auth-admin-api` |
| `version` | the schema document's `info.version`, taken as literal text (`1.10` stays `1.10`, never `1.1`) |
| artifact file | the schema file itself; its extension matches the source file's (`yaml`, `yml`, or `json`), and the generated POM's `packaging` uses that same extension |

A type folder is walked recursively — `openapi/auth/auth-admin-api.yaml` publishes exactly like
`openapi/auth-admin-api.yaml` would, with artifactId `auth-admin-api`; the subfolder plays no part
in the coordinates. Files placed directly in `src/main/resources`, outside any type folder, are
ignored with a log line. A missing or empty `src/main/resources` logs a warning and publishes
nothing.

Every problem in the tree — an unsupported type folder, a file extension `openapi`/`asyncapi` does
not accept, a file missing its type's root key, a missing or blank `info.version`, or two files
that resolve to the same `groupId:artifactId` — is collected before anything is installed or
deployed, and reported together in one build failure. A half-published set is never left behind.

## Goals

| Goal | Default phase | What it does |
|---|---|---|
| `install` | `install` | Publishes every schema into the local repository. Always overwrites. |
| `deploy` | `deploy` | Publishes every schema to the project's `distributionManagement` repository (release or snapshot, chosen by version), or to an optional alternative repository. Republishing an unchanged release is skipped; republishing a release whose bytes changed fails, asking the author to bump `info.version`. A `SNAPSHOT` version always deploys. |

Both goals run the same scan and validation first and fail before publishing anything if the tree
has a problem. The `deploy` goal adds a second validation pass of its own: every schema's status
against the target repository (skip / deploy / fail) is decided before the first one is actually
deployed, so a single conflict fails the build and deploys nothing.

### Parameters

| Parameter | Property | Required | Description |
|---|---|---|---|
| `groupIdPrefix` | `api-publisher.groupIdPrefix` | yes | The prefix prepended to each type folder to form that type's `groupId`. |
| `skip` | `api-publisher.skip` | no | Skips the goal when set. |
| `resourcesDirectory` | *(none — configuration only)* | no | The directory whose immediate subdirectories are the schema types. Defaults to `${project.basedir}/src/main/resources`. |
| `altDeploymentRepository` | `api-publisher.altDeploymentRepository` | no | `deploy` goal only. A repository that receives both releases and snapshots instead of the ones in `distributionManagement`, as `id::url`. Credentials come from the `settings.xml` server with that id. |

`groupIdPrefix` has no default so the plugin can also be run ad hoc, from the command line,
against a project whose `pom.xml` does not declare it:

```bash
mvn com.lllbllllb.plugins:api-publisher-maven-plugin:1.0.0-SNAPSHOT:install \
    -Dapi-publisher.groupIdPrefix=com.lllbllllb.api
```

### Attaching it to a project

```xml
<plugin>
    <groupId>com.lllbllllb.plugins</groupId>
    <artifactId>api-publisher-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <groupIdPrefix>com.lllbllllb.api</groupIdPrefix>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>install</goal>
                <goal>deploy</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Adding a type

A type is a pluggable handler keyed by its folder name — nothing else in the plugin needs to
change to add one. A handler declares:

- the folder name it answers to (e.g. `openapi`);
- the file extensions it accepts (`.yaml`, `.yml`, `.json`);
- the root key that must be present for a file to belong to it (`openapi:`, `asyncapi:`);
- how to read that document's version (both current handlers share a reader for `info.version`
  that keeps the scalar's literal text).

The unsupported-type check is simply "this folder name has no registered handler" — so adding
`flatbuffers` later is exactly one new handler, registered alongside the existing two, and nothing
else in the scan, validation or publishing code changes.

## Layout

| Module | What it holds |
|---|---|
| `api-publisher` (root) | The parent POM: versions, plugin management, `distributionManagement`. |
| `api-publisher-maven-plugin` | The plugin itself and its unit tests. |
| `api-publisher-functional-test` | `maven-invoker-plugin` fixture projects under `src/it`, run against the plugin built in the same reactor. |

## Building

```bash
mvn clean install -Dasyncapi.generator.agent=node
```

`api-publisher-functional-test/src/it` holds the fixture projects exercised during `verify`, each
against its own local repository under that module's `target/`, isolated from `~/.m2`. Each fixture
names the plugin by its literal artifactId; `invoker:install` copies it, with this parent POM, into
that repository first.
