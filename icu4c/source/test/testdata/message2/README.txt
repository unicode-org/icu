© 2024 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html

The format of the JSON files in this directory and subdirectories
follow the test schema defined in the Conformance repository:

https://github.com/unicode-org/conformance/blob/main/schema/message_fmt2/testgen_schema.json

(as of https://github.com/unicode-org/conformance/pull/255 or later).

# JSON notes

In the "params" field, a date parameter can be expressed as:
{ "date": n }
where n is a number representing a Unix timestamp.

In the "params" field, a decimal string parameter can be expressed as:
{ "decimal": s }
where s is a string.

Optional fields, "ignoreJava" and "ignoreCpp" can be used
for tests currently expected to fail in the respective language.
The field may have any value; if it's
present, the test is ignored. (The value can be a comment explaining
why it's expected to fail.)

Tests in the `spec/` subdirectory are taken from https://github.com/unicode-org/message-format-wg/blob/main/test .
If the contents change upstream, then the corresponding tests in CLDR
need to be updated (also see https://unicode-org.atlassian.net/browse/ICU-22812 ).

## ICU4J only

The `cleanSrc` fields is used to represent normalized input (ICU4C has its
own function for normalizing input).

## ICU4C only

Additional "char" and "line" fields may be present with integer values,
used for tests expected to trigger a syntax error.
If present, "char" reflects the expected character offset and "line"
reflects the expected line number in the parse error.
The files with "diagnostics" in the name have these fields filled in.

