File log = new File(basedir, "build.log")
assert log.text.contains("禁用包违规") : "build.log did not contain the ban-violation marker:\n" + log.text
