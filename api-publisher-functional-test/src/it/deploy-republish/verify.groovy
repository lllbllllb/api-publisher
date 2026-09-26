// Three deploys into the same file:// repository, all logged into one build.log:
//   1. both releases are deployed, each with its POM;
//   2. the same files again: each release is skipped as identical, and nothing is deployed;
//   3. auth-admin-api changed under the same version: the build fails asking for a version bump,
//      and the never-published auth-room-api of that run is not deployed either.

def invocations = new File(basedir, 'build.log').text.split(/(?m)^\[INFO\] Scanning for projects\.\.\.$/)
        .findAll { !it.isBlank() }
assert invocations.size() == 3: "expected 3 invocations in build.log, found ${invocations.size()}"
def (first, second, third) = invocations

def remote = new File(basedir, 'remote-repo').toPath()
def resources = new File(basedir, 'src/main/resources').toPath()

// 1. first publish
assert first.contains('BUILD SUCCESS')
assert first.contains('Deployed it.deploy.openapi:auth-admin-api:yaml:0.0.1 to it-remote')
assert first.contains('Deployed it.deploy.asyncapi:auth-api:yaml:1.0.0 to it-remote')

// 2. unchanged republish
assert second.contains('BUILD SUCCESS')
assert second.contains('Skipping it.deploy.openapi:auth-admin-api:yaml:0.0.1: already published to it-remote')
assert second.contains('Skipping it.deploy.asyncapi:auth-api:yaml:1.0.0: already published to it-remote')
assert second.contains('Every schema is already published with identical content, nothing to deploy')
assert !second.contains('Deployed ')
assert !second.contains('Uploading to it-remote')

// 3. changed file, same version
assert third.contains('BUILD FAILURE')
assert third.contains('it.deploy.openapi:auth-admin-api:yaml:0.0.1 is already published to it-remote')
assert third.contains('with different content: bump info.version in ')
assert third.contains('nothing was deployed')
assert !third.contains('Uploading to it-remote')

// The repository holds exactly what the first run deployed, with the original bytes.
[
        'it/deploy/openapi/auth-admin-api/0.0.1/auth-admin-api-0.0.1': 'openapi/auth/auth-admin-api.yaml',
        'it/deploy/asyncapi/auth-api/1.0.0/auth-api-1.0.0'           : 'asyncapi/auth/auth-api.yaml',
].each { published, source ->
    def schema = remote.resolve("${published}.yaml")
    assert schema.toFile().isFile(): "not deployed: ${schema}"
    assert schema.toFile().bytes == resources.resolve(source).toFile().bytes: "published schema was overwritten: ${schema}"
    assert remote.resolve("${published}.pom").toFile().isFile(): "POM not deployed for ${published}"
}
assert !remote.resolve('it/deploy/asyncapi/auth-room-api').toFile().exists(): 'auth-room-api was deployed although the run failed'

return true
