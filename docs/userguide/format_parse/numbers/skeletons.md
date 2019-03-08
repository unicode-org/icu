<!--
© 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

Number Skeletons
================

Number skeletons are a locale-agnostic way to configure a NumberFormatter in
ICU.  Number skeletons work in MessageFormat.

Number skeletons consist of *space-separated tokens* that correspond to
settings in ICU NumberFormatter.  For example, to format a currency in compact
notation, you could use this skeleton:

    compact-short currency/GBP

To use a skeleton in MessageFormat, use the "number" type and prefix the
skeleton with `::`

    {0, number, ::compact-short currency/GBP}

## Syntax

A token consists of a *stem* and zero or more *options*.  The stem is what
occurs before the first "/" character in a token, and the options are each of
the subsequent "/"-delimited strings.  For example, "compact-short" and
"currency" are stems, and "GBP" is an option.

Stems might also be dynamic strings (not a fixed list); these are called
*blueprint stems*.  For example, to format a number with 2-3 significant
digits, you could use the following stem:

    @@#

A few examples of number skeletons are shown below.  The list of available
stems and options can be found below in [Skeleton Stems and
Options](#skeleton-stems-and-options).

## Examples

| Skeleton | Input | en-US Output | Comments |
|---|---|---|---|
| `percent` | 25 | 25% |
| `.00` | 25 | 25.00 | Equivalent to Precision::fixedFraction(2) |
| `percent .00` | 25 | 25.00% |
| `scale/100` | 0.3 | 30 | Multiply by 100 before formatting |
| `percent scale/100` | 0.3 | 30% |
| `measure-unit/length-meter` | 5 | 5 m | UnitWidth defaults to Short |
| `measure-unit/length-meter` <br/> `unit-width-full-name` | 5 | 5 meters |
| `currency/CAD` | 10 | CA$10.00 |
| `currency/CAD` <br/> `unit-width-narrow` | 10 | $10.00 | Use the narrow symbol variant |
| `compact-short` | 5000 | 5K |
| `compact-long` | 5000 | 5 thousand |
| `compact-short` <br/> `currency/CAD` | 5000 | CA$5K |
| - | 5000 | 5,000 |
| `group-min2` | 5000 | 5000 | Require 2 digits in group for separator |
| `group-min2` | 15000 | 15,000 |
| `sign-always` | 60 | +60 | Show sign on all numbers |
| `sign-always` | 0 | +0 |
| `sign-except-zero` | 60 | +60 | Show sign on all numbers except 0 |
| `sign-except-zero` | 0 | 0 |
| `sign-accounting` <br/> `currency/CAD` | -40 | (CA$40.00) |

## Skeleton Stems and Options

The full set of features supported by number skeletons is listed by category
below.

### Notation

Use one of the following stems to select your notation style:

- `compact-short`
- `compact-long`
- `scientific`
- `engineering`
- `notation-simple`

The skeletons `scientific` and `engineering` take the following optional
options:

- `/sign-xxx` sets the sign display option for the exponent; see [Sign](#sign).
- `/+ee` sets exponent digits to "at least 2"; use `/+eee` for at least 3 digits, etc.

For example, all of the following skeletons are valid:

- `scientific`
- `scientific/sign-always`
- `scientific/+ee`
- `scientific/+ee/sign-always`

### Unit

The supported types of units are percent, currency, and measurement units.
The following skeleton tokens are accepted:

- `percent`
- `permille`
- `base-unit`
- `currency/XXX`
- `measure-unit/aaaa-bbbb`

The `percent`, `permille`, and `base-unit` stems do not take any options.

The `currency` stem takes one required option: the three-letter ISO code of
the currency to be formatted.

The `measure-unit` stem takes one required option: the unit identifier of the
unit to be formatted.  The full unit identifier is required: both the type and
the subtype (for example, `length-meter`).

### Per Unit

To specify a unit to put in the denominator, use the following skeleton token:

- `per-measure-unit/aaaa-bbbb`

As with the `measure-unit` stem, pass the unit identifier as the option.

### Unit Width

The unit width can be specified by the following stems:

- `unit-width-narrow`
- `unit-width-short`
- `unit-width-full-name`
- `unit-width-iso-code`
- `unit-width-hidden`

For more details, see
[UNumberUnitWidth](http://icu-project.org/apiref/icu4c/unumberformatter_8h.html).

### Precision

The precision category has more blueprint stems than most other categories;
they are documented in detail below.  The following non-blueprint stems are
accepted:

- `precision-integer` (round to the nearest integer) --- accepts fraction-precision options
- `precision-unlimited` (do not perform rounding; display all digits)
- `precision-increment/dddd` (round to *dddd*, a decimal number) --- see below
- `precision-currency-standard`
- `precision-currency-cash`

To round to the nearest nickel, for example, use the skeleton
`precision-increment/0.05`.  For more information on the decimal number
syntax, see [Scale](#scale).

#### Fraction Precision

The following are examples of fraction-precision stems:

| Stem | Explanation | Equivalent C++ Code |
|---|---|---|
| `.00` | Exactly 2 fraction digits | `Precision::fixedFraction(2) ` |
| `.00+` | At least 2 fraction digits | `Precision::minFraction(2)` |
| `.##` | At most 2 fraction digits | `Precision::maxFraction(2) ` |
| `.0#` | Between 1 and 2 fraction digits | `Precision::minMaxFraction(1, 2)` |

More precisely, the fraction precision stem starts with `.`, then contains
zero or more `0` symbols, which implies the minimum fraction digits.  Then it
contains either a `+`, for unlimited maximum fraction digits, or zero or more
`#` symbols, which implies the minimum fraction digits when added to the `0`
symbols.

Note that the stem `.` is considered valid and is equivalent to `precision-integer`.

Fraction-precision stems accept a single optional option: the minimum or
maximum number of significant digits.  This allows you to combine fraction
precision with certain significant digits capabilities.  The following are
examples:

| Skeleton | Explanation | Equivalent C++ Code |
|---|---|---|
| `.##/@@@+` | At most 2 fraction digits, but guarantee <br/> at least 3 significant digits | `Precision::maxFraction(2)` <br/> `.withMinDigits(3)` |
| `.00/@##` | Exactly 2 fraction digits, but do not <br/> display more than 3 significant digits | `Precision::fixedFraction(2)` <br/> `.withMaxDigits(3)` |

Precisely, the option starts with one or more `@` symbols.  Then it contains
either a `+`, for `::withMinDigits`, or one or more `#` symbols, for
`::withMaxDigits`.  If a `#` symbol is present, there must be only one `@`
symbol.

#### Significant Digits Precision

The following are examples of stems for significant figures:

| Stem | Explanation | Equivalent C++ Code|
|---|---|---|
| `@@@` | Exactly 3 significant digits | `Precision::fixedSignificantDigits(3)` |
| `@@@+` | At least 3 significant digits | `Precision::minSignificantDigits(3)` |
| `@##` | At most 3 significant digits | `Precision::maxSignificantDigits(3)` |
| `@@#` | Between 2 and 3 significant digits | `...::minMaxSignificantDigits(2, 3)` |

The precise syntax is very similar to fraction precision.  The blueprint stem
starts with one or more `@` symbols, which implies the minimum significant
digits.  Then it contains either a `+`, for unlimited maximum significant
digits, or zero or more `#` symbols, which implies the minimum significant
digits when added to the `@` symbols.

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

For more details, see [Rounding
Modes](http://userguide.icu-project.org/formatparse/numbers/rounding-modes).

### Integer Width

The following examples show how to specify integer width (minimum or maximum
integer digits):

| Token | Explanation | Equivalent C++ Code |
|---|---|---|
| `integer-width/+000` | At least 3 <br/> integer digits | `IntegerWidth::zeroFillTo(3)` |
| `integer-width/##0` | Between 1 and 3 <br/> integer digits | `IntegerWidth::zeroFillTo(1)` <br/> `.truncateAt(3)`
| `integer-width/00` | Exactly 2 <br/> integer digits | `IntegerWidth::zeroFillTo(2)` <br/> `.truncateAt(2)` |
| `integer-width/+` | Zero or more <br/> integer digits | `IntegerWidth::zeroFillTo(0) `

The option start with either a single `+` symbols, signaling no limit on the
number of integer digits (no *truncateAt*), or zero or more `#` symbols.  It
should then be followed by zero or more `0` symbols, indicating the minimum
integer digits (the argument to *zeroFillTo*).  If there is no `+` symbol, the
maximum integer digits (the argument to *truncateAt*) is the number of `#`
symbols plus the number of `0` symbols.

### Scale

To specify the scale, use the following stem and option:

- `scale/dddd`

where *dddd* is a decimal number.  For example, the following are valid
skeletons:

- `scale/100` (multiply by 100)
- `scale/1E2` (same as above)
- `scale/0.5` (multiply by 0.5)

The decimal number should conform to a standard decimal number syntax.  In
C++, it is parsed using the decimal number library described in
[LocalizedNumberFormatter::formatDecimal](http://icu-project.org/apiref/icu4c/classicu_1_1number_1_1LocalizedNumberFormatter.html).
In Java, it is parsed using
[BigDecimal](https://docs.oracle.com/javase/7/docs/api/java/math/BigDecimal.html#BigDecimal%28java.lang.String%29).
For maximum compatibility, it is highly recommended that your decimal number
is able to be parsed by both engines.

### Grouping

The grouping strategy can be specified by the following stems:

- `group-off`
- `group-min2`
- `group-auto`
- `group-on-aligned`
- `group-thousands`

For more details, see
[UNumberGroupingStrategy](http://icu-project.org/apiref/icu4c/unumberformatter_8h.html).

### Symbols

The following stems are allowed for specifying the number symbols:

- `latin` (use Latin-script digits)
- `numbering-system/nnnn` (use the `nnnn` numbering system)

A custom NDecimalFormatSymbols instance is not supported at this time.

### Sign Display

The following stems specify sign display:

- `sign-auto`
- `sign-always`
- `sign-never`
- `sign-accounting`
- `sign-accounting-always`
- `sign-except-zero`
- `sign-accounting-except-zero`

For more details, see
[UNumberSignDisplay](http://icu-project.org/apiref/icu4c/unumberformatter_8h.html).

### Decimal Separator Display

The following stems specify decimal separator display:

- `decimal-auto`
- `decimal-always`

For more details, see
[UNumberDecimalSeparatorDisplay](http://icu-project.org/apiref/icu4c/unumberformatter_8h.html).
