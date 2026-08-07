© 2024 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html

The format of the JSON files in this directory and subdirectories
follow the test schema defined in the official repository:

https://github.com/unicode-org/message-format-wg/blob/main/test/schemas/v0/tests.schema.json

# JSON notes

In the "params" field, a date parameter can be expressed as:
{ "date": n }
where n is a number representing a Unix timestamp.

In the "params" field, a decimal string parameter can be expressed as:
{ "decimal": s }
where s is a string.
