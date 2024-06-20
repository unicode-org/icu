© 2024 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html

The format of the JSON files in this directory follows the same format as `test-core.json`
in the spec, described in:

https://github.com/unicode-org/message-format-wg/blob/main/test/README.md

The `parts` field is not used.

# JSON extensions

An additional `comment` field may be present, which is only for human readers.

A "srcs" field, whose value is an array of strings, may be present instead
of "src". The strings are concatenated to get the message.

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

# ICU4C vs. ICU4J tests

The following tests are run in both ICU4C and ICU4J:

* alias-selector-annotations.json
* duplicate-declarations.json
* icu-parser-tests.json
  - Two tests removed while single-sourcing tests, because a `{{}}` message body
  had to be added to get it to parse in ICU4C, and this broke the test in ICU4J.
  These tests are in icu-parser-tests-old.json
* icu-test-functions.json
  - Some tests marked as ignored
* icu-test-previous-release.json
  - Some tests marked as ignored
* icu-test-selectors.json
* markup.json
* matches-whitespace.json
  - Some tests marked as ignored
* more-data-model-errors.json
* more-syntax-errors.json
* reserved-syntax.json
  - All tests marked as ignored in Java (resolution errors are suppressed)
* resolution-errors.json
  - All tests marked as ignored in Java (resolution errors are suppressed)
* runtime-errors.json
  - All tests marked as ignored in Java (message function errors are suppressed)
* syntax-errors-diagnostics.json
* tricky-declarations.json
* valid-tests.json
  - Some tests marked as ignored
* spec/*
  - Some tests in test-core.json and test-functions.json marked as ignored

The following tests are only run in ICU4C, either because ICU4J doesn't check
for invalid options, or because ICU4J doesn't report line/column numbers for
parse errors:
* invalid-number-literals-diagnostics.json
* invalid-options.json
* syntax-errors-diagnostics-multiline.json
* syntax-errors-end-of-input.json
