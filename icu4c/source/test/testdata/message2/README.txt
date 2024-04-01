© 2024 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html

The format of the JSON files in this directory follows the same format as `test-core.json`
in the spec, described in:

https://github.com/unicode-org/message-format-wg/blob/main/test/README.md

The `parts` and `cleanSrc` fields are not used.

Additional "char" and "line" fields may be present with integer values,
used for tests expected to trigger a syntax error.
If present, "char" reflects the expected character offset and "line"
reflects the expected line number in the parse error.
The files with "diagnostics" in the name have these fields filled in.

An additional `comment` field may be present, which is only for human readers.

Tests in the `spec/` subdirectory are taken from https://github.com/unicode-org/message-format-wg/blob/main/test
and need to be manually updated if the contents change upstream.

