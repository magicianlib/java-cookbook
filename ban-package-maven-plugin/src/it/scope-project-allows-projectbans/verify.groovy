File log = new File(basedir, "build.log")
assert log.text.contains("ban-package check passed") : "expected the check to run and pass, but build.log:\n" + log.text
