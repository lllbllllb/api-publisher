// Two files mapping to the same groupId:artifactId fail the build and list both paths.

def log = new File(basedir, 'build.log').text
def lines = log.readLines()

def duplicate = lines.find { it.contains('- duplicate artifact it.duplicate.openapi:admin-api: ') }
assert duplicate: 'build log lacks the duplicate artifact failure'
assert duplicate.contains('openapi/auth/admin-api.yaml')
assert duplicate.contains('openapi/room-manager/admin-api.yaml')
assert log.contains('api-publisher found 1 problem(s)')

// Validation runs before anything is published: the valid schema next to the broken one was not
// installed either.
def installed = new File(localRepositoryPath, 'it/duplicate')
assert !installed.exists(): "something was installed under ${installed} although the build failed"

return true
