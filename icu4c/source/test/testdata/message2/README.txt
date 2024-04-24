© 2024 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html

The format of the JSON files in this directory follows the same format as `test-core.json`
in the spec, described in:

https://github.com/unicode-org/message-format-wg/blob/main/test/README.md

The `parts` and `cleanSrc` fields are not used.

Some extensions:

Additional "char" and "line" fields may be present with integer values,
used for tests expected to trigger a syntax error.
If present, "char" reflects the expected character offset and "line"
reflects the expected line number in the parse error.
The files with "diagnostics" in the name have these fields filled in.

An additional `comment` field may be present, which is only for human readers.

A "srcs" field, whose value is an array of strings, may be present instead
of "src". The strings are concatenated to get the message.

In the "params" field, a date parameter can be expressed as:
{ "date": n }
where n is a number representing a Unix timestamp.

An optional field, "ignoreTest", can be used for tests that are
currently expected to fail. The field may have any value; if it's
present, the test is ignored. (The value can be a comment explaining
why it's expected to fail.)

Tests in the `icu4j/` subdirectory are taken from:
 icu4j/main/core/src/test/resources/com/ibm/icu/dev/test/message2
 and need to be manually synced with those files. The format is a bit
 different in some cases.

Tests in the `spec/` subdirectory are taken from https://github.com/unicode-org/message-format-wg/blob/main/test
and need to be manually updated if the contents change upstream.

