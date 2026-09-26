// A folder with no type handler fails the build with the exact unsupported-types message.

def log = new File(basedir, 'build.log').text
def lines = log.readLines()

assert lines.any { it.endsWith('- unsupported types found: [flatbuffers]') }: 'build log lacks "unsupported types found: [flatbuffers]"'
assert log.contains('api-publisher found 1 problem(s)')

// Validation runs before anything is published: the valid schema next to the broken one was not
// installed either.
def installed = new File(localRepositoryPath, 'it/unsupported')
assert !installed.exists(): "something was installed under ${installed} although the build failed"

return true
