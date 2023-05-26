---
layout: default
title: Number Skeletons
nav_order: 3
grand_parent: Formatting
parent: Formatting Numbers
---
<!--
© 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Number Skeletons
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

Number skeletons are a locale-agnostic way to configure a `NumberFormatter` in
ICU. Number skeletons work in `MessageFormat`.

Number skeletons consist of case-sensitive tokens that correspond to settings
in ICU `NumberFormatter`. For example, to format a currency in compact notation
with the sign always shown, you could use this skeleton:

    sign-always compact-short currency/GBP

***Since ICU 67***, you can also use more concise syntax:

    +! K currency/GBP

To use a skeleton in `MessageFormat`, use the "number" type and prefix the
skeleton with `::`

    {0, number, :: +! K currency/GBP}

The ICU `toSkeleton()` API outputs the long-form skeletons, but all parts of
ICU that read user-specified number skeletons accept both long-form and
concise skeletons.

## Syntax

A token consists of a *stem* and zero or more *options*.  The stem is what
occurs before the first `"/"` character in a token, and the options are each of
the subsequent `"/"`-delimited strings.  For example, `"compact-short"` and
"currency" are stems, and `"GBP"` is an option.

Tokens are space-separated, with exceptions for concise skeletons listed at
the end of this document.

Stems might also be dynamic strings (not a fixed list); these are called
*blueprint stems*.  For example, to format a number with 2-3 significant
digits, you could use the following stem:

    @@#

A few examples of number skeletons are shown below. The list of available
stems and options can be found below in [Skeleton Stems and
Options](#skeleton-stems-and-options).

## Examples

| Long Skeleton | Concise Skeleton | Input | en-US Output | Comments |
|---|---|---|---|---|
| `percent` | `%` | 25 | 25% |
| `.00` | `.00` | 25 | 25.00 | Equivalent to `Precision::fixedFraction(2)` |
| `percent .00` | `% .00` | 25 | 25.00% |
| `scale/100` | `scale/100` | 0.3 | 30 | Multiply by 100 before formatting |
| `percent scale/100` | `%x100` | 0.3 | 30% |
| `measure-unit/length-meter` | `unit/meter` | 5 | 5 m | `UnitWidth` defaults to `Short` |
| `measure-unit/length-meter` <br/> `unit-width-full-name` | `unit/meter` <br/> `unit-width-full-name` | 5 | 5 meters |
| `currency/CAD` | `currency/CAD` | 10 | CA$10.00 |
| `currency/CAD` <br/> `unit-width-narrow` | `currency/CAD` <br/> `unit-width-narrow` | 10 | $10.00 | Use the narrow symbol variant |
| `compact-short` | `K` | 5000 | 5K |
| `compact-long` | `KK` | 5000 | 5 thousand |
| `compact-short` <br/> `currency/CAD` | `K currency/CAD` | 5000 | CA$5K |
| - | - | 5000 | 5,000 |
| `group-min2` | `,?` | 5000 | 5000 | Require 2 digits in group for separator |
| `group-min2` | `,?` | 15000 | 15,000 |
| `sign-always` | `+!` | 60 | +60 | Show sign on all numbers |
| `sign-always` | `+!` | 0 | +0 |
| `sign-except-zero` | `+?` | 60 | +60 | Show sign on all numbers except 0 |
| `sign-except-zero` | `+?` | 0 | 0 |
| `sign-accounting` <br/> `currency/CAD` | `() currency/CAD` | -40 | (CA$40.00) |

## Skeleton Stems and Options

The full set of features supported by number skeletons is listed by category below.

### Notation

Use one of the following stems to select compact or simple notation:

- `compact-short` or `K` (concise)
- `compact-long` or `KK` (concise)
- `notation-simple` (or omit since this is default)

There are two ways to select scientific or engineering notation: using long-form syntax or concise syntax.

#### Scientific and Engineering Notation: Long Form

Start with the stem `scientific` or `engineering`.  Those stems take the following optional options:

- `/sign-xxx` sets the sign display option for the exponent; see [Sign](#sign).
- `/*ee` sets exponent digits to "at least 2"; use `/*eee` for at least 3 digits, etc.
    - ***Prior to ICU 67***, use `/+ee` instead of `/*ee`.

For example, all the following skeletons are valid:

- `scientific`
- `scientific/sign-always`
- `scientific/*ee`
- `scientific/*ee/sign-always`

#### Scientific and Engineering Notation: Concise Form

The following are examples of concise form:

| Concise Skeleton | Equivalent Long-Form Skeleton |
|---|---|
| `E0` | `scientific` |
| `E00` | `scientific/*ee` |
| `EE+!0` | `engineering/sign-always` |
| `E+?00` | `scientific/sign-except-zero/+ee` |

More precisely:

1. Start with `E` for scientific or `EE` for engineering.
2. Allow either `+!` or `+?` as a concise sign display option.
3. Expect one or more `0`s.  If more than one, set minimum integer digits.

### Unit

The supported types of units are percent, currency, and measurement units.
The following skeleton tokens are accepted:

- `percent` or `%` (concise)
- Special: `%x100` to scale the number by 100 and then format with percent
- `permille`
- `base-unit`
- `currency/XXX`
- `measure-unit/aaaa-bbbb` or `unit/bbb` (concise)

The `percent`, `permille`, and `base-unit` stems do not take any options.

The `currency` stem takes one required option: the three-letter ISO code of
the currency to be formatted.

The `measure-unit` stem takes one required option: the unit identifier of the
unit to be formatted.  The full unit identifier is required: both the type and
the subtype (for example, `length-meter`).

The `unit` stem is an alternative to `measure-unit` that accepts a core unit
identifier with the subtype but not the type (for example, `meter` instead of
`length-meter`).  It also supports variations allowed by UTS 35, including the per unit with the `-per-` infix (for example, `unit/furlong-per-second`).

### Per Unit

To specify a unit to put in the denominator, use the following skeleton token.
As with the `measure-unit` stem, pass the unit identifier as the option:

- `per-measure-unit/aaaa-bbbb`

Note that if the `unit` stem is used, the denominator can be placed in the same
token as the numerator.

### Unit Width

The unit width can be specified by the following stems:

- `unit-width-narrow`
- `unit-width-short`
- `unit-width-full-name`
- `unit-width-iso-code`
- `unit-width-hidden`

For more details, see
[`UNumberUnitWidth`](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/unumberformatter_8h.html).

### Precision

The precision category has more blueprint stems than most other categories;
they are documented in detail below. The following non-blueprint stems are
accepted:

- `precision-integer` (round to the nearest integer) --- accepts fraction-precision options
- `precision-unlimited` (do not perform rounding; display all digits)
- `precision-increment/dddd` (round to *`dddd`*, a decimal number) --- see below
- `precision-currency-standard`
- `precision-currency-cash`

To round to the nearest nickel, for example, use the skeleton
`precision-increment/0.05`.  For more information on the decimal number
syntax, see [Scale](#scale).

`precision-increment/dddd` implies minimum and maximum fraction digits equal to the
precision of the decimal. There is not currently a way in skeleton syntax to specify
an arbitrary min/max fraction digits when a rounding increment is used.

All rounding occurs after the number is scaled, such as in scientific notation or
the `scale` skeleton.

#### Fraction Precision

The following are examples of fraction-precision stems:

| Stem | Explanation | Equivalent C++ Code |
|---|---|---|
| `.00` | Exactly 2 fraction digits | `Precision::fixedFraction(2) ` |
| `.00*` | At least 2 fraction digits | `Precision::minFraction(2)` |
| `.##` | At most 2 fraction digits | `Precision::maxFraction(2) ` |
| `.0#` | Between 1 and 2 fraction digits | `Precision::minMaxFraction(1, 2)` |

More precisely, the fraction precision stem starts with `.`, then contains
zero or more `0` symbols, which implies the minimum fraction digits.  Then it
contains either a `*`, for unlimited maximum fraction digits, or zero or more
`#` symbols, which implies the minimum fraction digits when added to the `0`
symbols.

Note that the stem `.` is considered valid and is equivalent to `precision-integer`.

Fraction-precision stems accept a single optional option: a number of significant digits.
The options here correspond to the API functions on `FractionPrecision`. Some options
require specifying `r` or `s` for relaxed mode or strict mode. For more information, see
the API docs for UNumberRoundingPriority.

| Skeleton | Explanation | Equivalent C++ Code |
|---|---|---|
| `.##/@@@*` | At most 2 fraction digits, but guarantee <br/> at least 3 significant digits | `Precision::maxFraction(2)` <br/> `.withMinDigits(3)` |
| `.##/@##r` | Same as above | `Precision::maxFraction(2)` <br/> `.withSignificantDigits(1, 3, RELAXED)` |
| `.##/@@@r` | Same as above, but pad trailing zeros <br/> to at least 3 significant digits | `Precision::maxFraction(2)` <br/> `.withSignificantDigits(3, 3, RELAXED)` |
| `.00/@##` | Exactly 2 fraction digits, but do not <br/> display more than 3 significant digits | `Precision::fixedFraction(2)` <br/> `.withMaxDigits(3)` |
| `.00/@##s` | Same as above | `Precision::fixedFraction(2)` <br/> `.withSignificantDigits(1, 3, STRICT)` |
| `.00/@@@s` | Same as above, but pad trailing zeros <br/> to at least 3 significant digits | `Precision::fixedFraction(2)` <br/> `.withSignificantDigits(3, 3, STRICT)` |

Precisely, the option follows the syntax of the significant digits stem (see below),
but one of the following must be true:

- Option has one or more `@`s followed by the wildcard character (`withMinDigits`)
- Option has exactly one `@` followed by zero or more `#`s (`withMaxDigits`)
- Option has one or more `@`s followed by zero or more `#`s and ends in `s` or `r` (`withSignificantDigits`)

#### Significant Digits Precision

The following are examples of stems for significant figures:

| Stem | Explanation | Equivalent C++ Code|
|---|---|---|
| `@@@` | Exactly 3 significant digits | `Precision::fixedSignificantDigits(3)` |
| `@@@*` | At least 3 significant digits | `Precision::minSignificantDigits(3)` |
| `@##` | At most 3 significant digits | `Precision::maxSignificantDigits(3)` |
| `@@#` | Between 2 and 3 significant digits | `...::minMaxSignificantDigits(2, 3)` |

The precise syntax is very similar to fraction precision.  The blueprint stem
starts with one or more `@` symbols, which implies the minimum significant
digits.  Then it contains either a `*`, for unlimited maximum significant
digits, or zero or more `#` symbols, which implies the minimum significant
digits when added to the `@` symbols.

#### Trailing Zero Display

***Starting with ICU 69***, a new option called `trailingZeroDisplay` was added.
To enable this in an ICU number skeleton, append `/w` to any precision token:

| Skeleton | Explanation | Equivalent C++ Code |
|---|---|---|
| `.00/w` | Exactly 2 fraction digits, but hide <br/> them if they are all 0 | `Precision::fixedFraction(2)` <br/> `.trailingZeroDisplay(` <br/> `UNUM_TRAILING_ZERO_HIDE_IF_WHOLE)` |
| `precision-curren` <br/> `cy-standard/w` | Currency rounding, but hide <br/> fraction digits if they are all 0 | `Precision::currency(UCURR_USAGE_STANDARD)` <br/> `.trailingZeroDisplay(` <br/> `UNUM_TRAILING_ZERO_HIDE_IF_WHOLE)` |

#### Wildcard Character

***Prior to ICU 67***, the symbol `+` was used for unlimited precision, instead
of `*` (for example, `.00+`). For backwards compatibility, either `+` or `*` is
accepted. This applies for both fraction digits and significant digits.

### Rounding Mode

The rounding mode can be specified by the following stems:

- `rounding-mode-ceiling`
- `rounding-mode-floor`
- `rounding-mode-down`
- `rounding-mode-up`
- `rounding-mode-half-even`
- `rounding-mode-half-down`
- `rounding-mode-half-up`
- `rounding-mode-unnecessary`

For more details, see [Rounding Modes](rounding-modes.md).

### Integer Width

The following examples show how to specify integer width (minimum or maximum
integer digits):

| Long Form | Concise Form | Explanation | Equivalent C++ Code |
|---|---|---|---|
| `integer-width/*000` | `000` | At least 3 <br/> integer digits | `IntegerWidth::zeroFillTo(3)` |
| `integer-width/##0` | - | Between 1 and 3 <br/> integer digits | `IntegerWidth::zeroFillTo(1)` <br/> `.truncateAt(3)`
| `integer-width/00` | - | Exactly 2 <br/> integer digits | `IntegerWidth::zeroFillTo(2)` <br/> `.truncateAt(2)` |
| `integer-width/*` | - | Zero or more <br/> integer digits | `IntegerWidth::zeroFillTo(0) `
| `integer-width-trunc` | - | Zero integer digits | `IntegerWidth::zeroFillTo(0)` <br/> `.truncateAt(0)`

The long-form option starts with either a single `*` symbol, signaling no limit
on the number of integer digits (no *`truncateAt`*), or zero or more `#` symbols.
It should then be followed by zero or more `0` symbols, indicating the minimum
integer digits (the argument to *`zeroFillTo`*).  If there is no `*` symbol, the
maximum integer digits (the argument to *`truncateAt`*) is the number of `#`
symbols plus the number of `0` symbols.

The concise skeleton is simply one or more `0` characters. This supports
minimum integer digits but not maximum integer digits.

The special stem `integer-width-trunc` covers the case when both *`truncateAt`* and *`zeroFillTo`* are zero.

***Prior to ICU 67***, use the symbol `+` instead of `*`.

### Scale

To specify the scale, use the following stem and option:

- `scale/dddd`

where *`dddd`* is a decimal number. For example, the following are valid skeletons:

- `scale/100` (multiply by 100)
- `scale/1E2` (same as above)
- `scale/0.5` (multiply by 0.5)

The decimal number should conform to a standard decimal number syntax. In
C++, it is parsed using the decimal number library described in
[LocalizedNumberFormatter::formatDecimal](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classicu_1_1number_1_1LocalizedNumberFormatter.html).
In Java, it is parsed using
[BigDecimal](https://docs.oracle.com/javase/7/docs/api/java/math/BigDecimal.html#BigDecimal%28java.lang.String%29).
For maximum compatibility, it is highly recommended that your decimal number
is able to be parsed by both engines.

### Grouping

The grouping strategy can be specified by the following stems:

- `group-off` or `,_` (concise)
- `group-min2` or `,?` (concise)
- `group-auto` (or omit since this is the default)
- `group-on-aligned` or `,!` (concise)
- `group-thousands` (no concise equivalent)

For more details, see
[`UNumberGroupingStrategy`](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/unumberformatter_8h.html).

### Symbols

The following stems are allowed for specifying the number symbols:

- `latin` (use Latin-script digits)
- `numbering-system/nnnn` (use the `nnnn` numbering system)

A custom `NDecimalFormatSymbols` instance is not supported at this time.

### Sign Display

The following stems specify sign display:

- `sign-auto` (or omit since this is the default)
- `sign-always` or `+!` (concise)
- `sign-never` or `+_` (concise)
- `sign-accounting` or `()` (concise)
- `sign-accounting-always` or `()!` (concise)
- `sign-except-zero` or `+?` (concise)
- `sign-accounting-except-zero` or `()?` (concise)
- `sign-negative` or `+-` (concise)
- `sign-accounting-negative` or `()-` (concise)

For more details, see
[`UNumberSignDisplay`](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/unumberformatter_8h.html).

### Decimal Separator Display

The following stems specify decimal separator display:

- `decimal-auto`
- `decimal-always`

For more details, see
[`UNumberDecimalSeparatorDisplay`](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/unumberformatter_8h.html).
