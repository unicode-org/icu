# Unicode MessageFormat Test Suite

These test files are intended to be useful for testing multiple different _message_ processors in different ways:

- `syntax.json` — Test cases that do not depend on any registry definitions.

- `syntax-errors.json` — Strings that should produce a Syntax Error when parsed.

> [!NOTE]
> Tests for the disallowed uses of unpaired surrogate code points are not included
> because JSON does not permit unpaired surrogate code points.
> If your implementation uses UTF-16 based strings (such as JavaScript `String` or Java `java.lang.String`)
> or otherwise allows unpaired surrogates in text or literals, you will need to implement tests equivalent
> to the following for syntax errors:
>
> ```json
> {
>   "locale": "en-US",
>   "src": "{\ud800}",
>   "expErrors": [{ "type": "syntax-error" }]
> }
> ```

- `data-model-errors.json` - Strings that should produce a Data Model Error when processed.
  Error names are defined in ["MessageFormat 2.0 Errors"](../spec/errors.md) in the spec.

- `u-options.json` — Test cases for the `u:` options, using built-in functions.

- `functions/` — Test cases that correspond to built-in functions.
  The behaviour of the built-in formatters is implementation-specific so the `exp` field is often
  omitted and assertions are made on error cases.

Some examples of test harnesses using these tests, from the source repository:

- [CST parse/stringify tests](https://github.com/messageformat/messageformat/blob/11c95dab2b25db8454e49ff4daadb817e1d5b770/packages/mf2-messageformat/src/cst/cst.test.ts)
- [Data model stringify tests](https://github.com/messageformat/messageformat/blob/11c95dab2b25db8454e49ff4daadb817e1d5b770/packages/mf2-messageformat/src/data-model/stringify.test.ts)
- [Formatting tests](https://github.com/messageformat/messageformat/blob/11c95dab2b25db8454e49ff4daadb817e1d5b770/packages/mf2-messageformat/src/messageformat.test.ts)

A [JSON schema](./schemas/) is included for the test files in this repository.

## Error Codes

The following table relates the error names used in the [JSON schema](./schemas/)
to the error names used in ["MessageFormat 2.0 Errors"](../spec/errors.md) in the spec.

| Spec                        | Schema                      |
| --------------------------- | --------------------------- |
| Bad Operand                 | bad-operand                 |
| Bad Option                  | bad-option                  |
| Bad Selector                | bad-selector                |
| Bad Variant Key             | bad-variant-key             |
| Duplicate Declaration       | duplicate-declaration       |
| Duplicate Option Name       | duplicate-option-name       |
| Duplicate Variant           | duplicate-variant           |
| Missing Fallback Variant    | missing-fallback-variant    |
| Missing Selector Annotation | missing-selector-annotation |
| Syntax Error                | syntax-error                |
| Unknown Function            | unknown-function            |
| Unresolved Variable         | unresolved-variable         |
| Variant Key Mismatch        | variant-key-mismatch        |

The "Message Function Error" error name used in the spec
is not included in the schema,
as it is intended to be an umbrella category
for implementation-specific errors.

## Test Tags

Some of the tests are for functionality that is optional or for functionality that is not yet stable.
That is, the specification uses RFC2119 keywords such as SHOULD, SHOULD NOT, MAY, RECOMMENDED, or OPTIONAL,
or the specification says that given functionality is DRAFT and not yet stable.
Tests for such features have a `tags` array attached to them
to mark the features that they rely on.
This may include one or more of the following:

| Tag         | Feature                                                                     |
| ----------- | --------------------------------------------------------------------------- |
| `:currency` | The [:currency](../spec/functions/number.md#the-currency-function) function |
| `:percent`  | The [:percent](../spec/functions/number.md#the-percent-function) function   |
| `u:dir`     | The [u:dir](../spec/u-namespace.md#udir) option                             |
| `u:id`      | The [u:id](../spec/u-namespace.md#uid) option                               |

## Test Functions

As the behaviour of some of the default registry _functions_
such as `:number` and `:datetime`
is dependent on locale-specific data and may vary between implementations,
the following _functions_ are defined for **test use only**:

### `:test:function`

This function is valid both as a _selector_ and as a _formatter_.

#### Operands

The function `:test:function` requires a [Number Operand](/spec/registry.md#number-operands) as its _operand_.

#### Options

The following _options_ are available on `:test:function`:

- `decimalPlaces`, a _digit size option_ for which only `0` and `1` are valid values.
  - `0`
  - `1`
- `fails`
  - `never` (default)
  - `select`
  - `format`
  - `always`

All other _options_ and their values are ignored.

#### Behavior

When resolving a `:test:function` expression,
its `Input`, `DecimalPlaces`, `FailsFormat`, and `FailsSelect` values are determined as follows:

1. Let `DecimalPlaces` be 0.
1. Let `FailsFormat` be `false`.
1. Let `FailsSelect` be `false`.
1. Let `arg` be the _resolved value_ of the _expression_ _operand_.
1. If `arg` is the _resolved value_ of an _expression_
   with a `:test:function`, `:test:select`, or `:test:format` _annotation_
   for which resolution has succeeded, then
   1. Let `Input` be the `Input` value of `arg`.
   1. Set `DecimalPlaces` to be `DecimalPlaces` value of `arg`.
   1. Set `FailsFormat` to be `FailsFormat` value of `arg`.
   1. Set `FailsSelect` to be `FailsSelect` value of `arg`.
1. Else if `arg` is a numerical value
   or a string matching the `number-literal` production, then
   1. Let `Input` be the numerical value of `arg`.
1. Else,
   1. Emit "bad-input" _Resolution Error_.
   1. Use a _fallback value_ as the _resolved value_ of the _expression_.
      Further steps of this algorithm are not followed.
1. If the `decimalPlaces` _option_ is set, then
   1. If its value resolves to a numerical integer value 0 or 1
      or their corresponding string representations `'0'` or `'1'`, then
      1. Set `DecimalPlaces` to be the numerical value of the _option_.
   1. Else if its value is not an unresolved value set by _option resolution_,
      1. Emit "bad-option" _Resolution Error_.
      1. Use a _fallback value_ as the _resolved value_ of the _expression_.
1. If the `fails` _option_ is set, then
   1. If its value resolves to the string `'always'`, then
      1. Set `FailsFormat` to be `true`.
      1. Set `FailsSelect` to be `true`.
   1. Else if its value resolves to the string `'format'`, then
      1. Set `FailsFormat` to be `true`.
   1. Else if its value resolves to the string `'select'`, then
      1. Set `FailsSelect` to be `true`.
   1. Else if its value does not resolve to the string `'never'`, then
      1. Emit "bad-option" _Resolution Error_.

When `:test:function` is used as a _selector_,
the behaviour of calling it as the `rv` value of Match(`rv`, `key`)
(see [Pattern Selection](/spec/formatting.md#pattern-selection) for more information)
depends on its `Input`, `DecimalPlaces` and `FailsSelect` values.

- If `FailsSelect` is `true`,
  calling the method will emit a _Bad Option_ error
  and not return any value.
- If the `Input` is 1 and `DecimalPlaces` is 1,
  the method will return true for either `'1.0'` or `'1'`,
  and false for any other key.
- If the `Input` is 1 and `DecimalPlaces` is 0,
  the method will return true for `'1'`
  and false for any other key.
- If the `Input` is any other value, the method will return false.

When `:test:function` is used as a _selector_,
the behaviour of calling it as the `rv` value of BetterThan(`rv`, `key1`, `key2`)
(see [Pattern Selection](/spec/formatting.md#pattern-selection) for more information)

- The method will return true if `key1` is `'1.0'`,
  and false otherwise.

When an _expression_ with a `:test:function` _annotation_ is assigned to a _variable_ by a _declaration_
and that _variable_ is used as an _option_ value,
its _resolved value_ is the `Input` value.

When `:test:function` is used as a _formatter_,
a _placeholder_ resolving to a value with a `:test:function` _expression_
is formatted as a concatenation of the following parts:

1. If `Input` is less than 0, the character `-` U+002D Hyphen-Minus.
1. The truncated absolute integer value of `Input`, i.e. floor(abs(`Input`)),
   formatted as a sequence of decimal digit characters (U+0030...U+0039).
1. If `DecimalPlaces` is 1, then
   1. The character `.` U+002E Full Stop.
   1. The single decimal digit character representing the value floor((abs(`Input`) - floor(abs(`Input`))) \* 10)

If the formatting target is a sequence of parts,
each of the above parts will be emitted separately
rather than being concatenated into a single string.

If `FailsFormat` is `true`,
attempting to format the _placeholder_ to any formatting target will
emit a _Bad Option_ error.

> Note that emitting _Bad Option_ here does not indicate an incorrect option.
> Actual functions in your implementation might emit
> an implementation-defined or platform-specific runtime error or exception
> when the function handler is called.
> Your implementation might thus produce a _Message Function Error_
> not provided with a label in the JSON Schema of this test suite.

### `:test:select`

This _function_ accepts the same _operands_ and _options_,
and behaves exactly the same as `:test:function`,
except that it cannot be used for formatting.

When `:test:select` is used as a _formatter_,
a "not-formattable" error is emitted and the _placeholder_ is formatted with
a _fallback value_.

### `:test:format`

This _function_ accepts the same _operands_ and _options_,
and behaves exactly the same as `:test:function`,
except that it cannot be used for selection.

When `:test:format` is used as a _selector_,
the steps under 2.iii. of [Resolve Selectors](/spec/formatting.md#resolve-selectors) are followed.

## About

The tests in the `./tests/` directory were originally copied from the [messageformat project](https://github.com/messageformat/messageformat/tree/11c95dab2b25db8454e49ff4daadb817e1d5b770/packages/mf2-messageformat/src/__fixtures)
and are here relicensed by their original author (Eemeli Aro) under the Unicode License.
