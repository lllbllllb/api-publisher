// Both schemas are installed under <prefix>.<type>:<file name>:<info.version>, each as the raw
// schema file plus a generated POM. The service subfolder plays no part in the coordinates, and an
// unquoted `version: 1.10` keeps its literal text.

def repository = localRepositoryPath.toPath()
def resources = basedir.toPath().resolve('src/main/resources')

def expected = [
        [dir: 'it/happy/openapi/auth-admin-api/1.10', name: 'auth-admin-api-1.10',
         source: 'openapi/auth/auth-admin-api.yaml', groupId: 'it.happy.openapi', artifactId: 'auth-admin-api', version: '1.10'],
        [dir: 'it/happy/asyncapi/auth-api/1.0.0', name: 'auth-api-1.0.0',
         source: 'asyncapi/auth/auth-api.yaml', groupId: 'it.happy.asyncapi', artifactId: 'auth-api', version: '1.0.0'],
]

expected.each { artifact ->
    def directory = repository.resolve(artifact.dir)
    def schema = directory.resolve("${artifact.name}.yaml")
    def pom = directory.resolve("${artifact.name}.pom")

    assert schema.toFile().isFile(): "schema not installed: ${schema}"
    assert schema.toFile().bytes == resources.resolve(artifact.source).toFile().bytes: "installed schema differs from its source: ${schema}"

    assert pom.toFile().isFile(): "POM not installed: ${pom}"
    def project = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom.toFile()).documentElement
    def text = { String tag -> project.getElementsByTagName(tag).item(0)?.textContent }
    assert text('groupId') == artifact.groupId
    assert text('artifactId') == artifact.artifactId
    assert text('version') == artifact.version
    assert text('packaging') == 'yaml'
}

// Nothing but the two artifacts under the prefix: no jar, no stray coordinates.
def published = []
repository.resolve('it/happy').toFile().eachFileRecurse(groovy.io.FileType.FILES) { file ->
    if (file.name.endsWith('.yaml') || file.name.endsWith('.pom')) {
        published << repository.relativize(file.toPath()).toString().replace('\\', '/')
    }
}
assert published.sort() == [
        'it/happy/asyncapi/auth-api/1.0.0/auth-api-1.0.0.pom',
        'it/happy/asyncapi/auth-api/1.0.0/auth-api-1.0.0.yaml',
        'it/happy/openapi/auth-admin-api/1.10/auth-admin-api-1.10.pom',
        'it/happy/openapi/auth-admin-api/1.10/auth-admin-api-1.10.yaml',
]

return true
