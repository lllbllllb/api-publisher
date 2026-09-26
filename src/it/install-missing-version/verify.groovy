// A schema without info.version fails the build and names the file.

def log = new File(basedir, 'build.log').text
def lines = log.readLines()

assert lines.any { it.endsWith('- missing info.version in asyncapi/auth/auth-api.yaml') }: 'build log lacks the missing info.version failure'
assert log.contains('api-publisher found 1 problem(s)')

// Validation runs before anything is published: the valid schema next to the broken one was not
// installed either.
def installed = new File(localRepositoryPath, 'it/noversion')
assert !installed.exists(): "something was installed under ${installed} although the build failed"

return true
