---
layout: default
title: Legacy NumberFormat
nav_order: 1
grand_parent: Formatting
parent: Formatting Numbers
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Legacy `NumberFormat`
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Note

> :warning: Since ICU 60, the recommended way to format numbers is NumberFormatter; see [index.md](index.md).
> 
> This page is here for reference for the older NumberFormat hierarchy in ICU4C and ICU4J.

## `NumberFormat`

[`NumberFormat`](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classNumberFormat.html) is
the abstract base class for all number formats. It provides an interface for
formatting and parsing numbers. It also provides methods to determine which
locales have number formats, and what their names are. `NumberFormat` helps format
and parse numbers for any locale. Your program can be written to be completely
independent of the locale conventions for decimal points or
thousands-separators. It can also be written to be independent of the particular
decimal digits used or whether the number format is a decimal. A normal decimal
number can also be displayed as a currency or as a percentage.

```
1234.5       //Decimal number
$1234.50     //U.S. currency
1.234,57€    //German currency
123457%      //Percent
```

### Usage

#### Formatting for a `Locale`

To format a number for the current `Locale`, use one of the static factory methods
to create a format, then call a format method to format it. To format a number
for a different `Locale`, specify the `Locale` in the call to `createInstance()`. You
can control the numbering system to be used for number formatting by creating a
`Locale` that uses the `@numbers` keyword defined. For example, by default, the Thai
locale "th" uses the western digits 0-9. To create a number format that uses the
native Thai digits instead, first create a locale with `"@numbers=thai"` defined.
See [the description on Locales](../../locale/index.md) for details.

> :point_right: **Note**: If you are formatting multiple numbers, save processing time
> by constructing the formatter once and then using it several times.

#### Instantiating a `NumberFormat`

The following methods are used for instantiating `NumberFormat` objects:

1.  **`createInstance()`**
    Returns the normal number format for the current locale or for a specified
    locale.

2.  **`createCurrencyInstance()`**
    Returns the currency format for the current locale or for a specified
    locale.

3.  **`createPercentInstance()`**
    Returns the percentage format for the current locale or for a specified
    locale.

4.  **`createScientificInstance()`**
    Returns the scientific number format for the current locale or for a
    specified locale.

To create a format for spelled-out numbers, use a constructor on `RuleBasedNumberFormat`.

#### Currency Formatting

Currency formatting, i.e., the formatting of monetary values, combines a number
with a suitable display symbol or name for a currency. By default, the currency
is set from the locale data from when the currency format instance is created,
based on the country code in the locale ID. However, for all but trivial uses,
this is fragile because countries change currencies over time, and the locale
data for a particular country may not be available.

For proper currency formatting, both number and currency must be
specified. Aside from achieving reliably correct results, this also allows to
format monetary values in any currency with the format of any locale, like in
exchange rate lists. If the locale data does not contain display symbols or
names for a currency, then the 3-letter ISO code itself is displayed.

The locale ID and the currency code are effectively independent: The locale ID
defines the general format for the numbers, and whether the currency symbol or
name is displayed before or after the number, while the currency code selects
the actual currency with its symbol, name, number of digits, and [rounding
mode](rounding-modes.md).

In ICU and Java, the currency is specified in the form of a 3-letter ISO 4217
code. For example, the code "USD" represents the US Dollar and "EUR" represents
the Euro currency.

In terms of APIs, the currency code is set as an attribute on a number format
object (on a currency instance), while the number value is passed into each
`format()` call or returned from `parse()` as usual.

1.  ICU4C (C++) `NumberFormat.setCurrency()` takes a Unicode string (`const UChar*`) with the 3-letter code.

2.  ICU4C (C API) allows to set the currency code via `unum_setTextAttribute()`
    using the `UNUM_CURRENCY_CODE` selector.

3.  ICU4J `NumberFormat.setCurrency()` takes an ICU Currency object which
    encapsulates the 3-letter code.

4.  The base JDK's `NumberFormat.setCurrency()` takes a JDK Currency object which
    encapsulates the 3-letter code.

The functionality of `Currency` and `setCurrency()` is more advanced in ICU than in
the base JDK. When using ICU, setting the currency automatically adjusts the
number format object appropriately, i.e., it sets not only the currency symbol
and display name, but also the correct number of fraction digits and the correct
[rounding mode](rounding-modes.md). This is not the case with the base JDK. See
the API references for more details.

There is ICU4C sample code at
[icu4c/source/samples/numfmt/main.cpp](https://github.com/unicode-org/icu/blob/master/icu4c/source/samples/numfmt/main.cpp)
which illustrates the use of `NumberFormat.setCurrency()`.

#### Displaying Numbers

You can also control the display of numbers with methods such as
`getMinimumFractionDigits()`. If you want even more control over the format or
parsing, or want to give your users more control, cast the `NumberFormat` returned
from the factory methods to a `DecimalNumberFormat`. This works for the vast
majority of countries.

#### Working with Positions

You can also use forms of the parse and format methods with `ParsePosition` and
`UFieldPosition` to enable you to:

1.  progressively parse through pieces of a string.

2.  align the decimal point and other areas.

For example, you can align numbers in two ways:

1.  If you are using a mono-spaced font with spacing for alignment, pass the
    `FieldPosition` in your format call with `field = INTEGER_FIELD`. On output,
    `getEndIndex` is set to the offset between the last character of the integer
    and the decimal. Add `(desiredSpaceCount - getEndIndex)` spaces at the front
    of the string. You can also use the space padding feature available in
    `DecimalFormat`.

2.  If you are using proportional fonts, instead of padding with spaces, measure
    the width of the string in pixels from the start to `getEndIndex`. Then move
    the pen by `(desiredPixelWidth - widthToAlignmentPoint)` before drawing the
    text. It also works where there is no decimal, but additional characters at
    the end (that is, with parentheses in negative numbers: "(12)" for -12).

#### Emulating `printf`

`NumberFormat` can produce many of the same formats as printf.

| printf | ICU |
|--------|-----|
| Width specifier, e.g., `"%5d"` has a width of 5. | Use `DecimalFormat`. Either specify the padding, with can pad with any character, or specify a minimum integer count and a minimum fraction count, which will emit a specific number of digits, with zero padded to the left and right. |
| Precision specifier for `%f` and `%e`, e.g. `"%.6f"` or `"%.6e"`. This defines the number of digits to the right of the decimal point. | Use `DecimalFormat`. Specify the maximum fraction digits. |
| General scientific notation, `%g`. This format uses either `%f` or `%e`, depending on the magnitude of the number being displayed. | Use `ChoiceFormat` with `DecimalFormat`. For example, for a typical `%g`, which has 6 significant digits, use a `ChoiceFormat` with thresholds of 1e-4 and 1e6. For values between the two thresholds, use a fixed `DecimalFormat` with the pattern `"@#####"`. For values outside the thresholds, use a `DecimalFormat` with the pattern `"@#####E0"`. |

## `DecimalFormat`

`DecimalFormat` is a `NumberFormat` that converts numbers into strings using the
decimal numbering system. This is the formatter that provides standard number
formatting and parsing services for most usage scenarios in most locales. In
order to access features of `DecimalFormat` not exposed in the `NumberFormat` API,
you may need to cast your `NumberFormat` object to a `DecimalFormat`. You may also
construct a `DecimalFormat` directly, but this is not recommended because it can
hinder proper localization.

For a complete description of `DecimalFormat`, including the pattern syntax,
formatting and parsing behavior, and available API, see the [ICU4J `DecimalFormat`
API](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/DecimalFormat.html) or
[ICU4C `DecimalFormat`
API](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classDecimalFormat.html) documentation.

## `DecimalFormatSymbols`

[`DecimalFormatSymbols`](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classDecimalFormatSymbols.html)
specifies the exact characters a `DecimalFormat` uses for various parts of a
number (such as the characters to use for the digits, the character to use as
the decimal point, or the character to use as the minus sign).

This class represents the set of symbols needed by `DecimalFormat` to format
numbers. `DecimalFormat` creates its own instance of `DecimalFormatSymbols` from its
locale data. The `DecimalFormatSymbols` can be adopted by a `DecimalFormat`
instance, or it can be specified when a `DecimalFormat` is created. If you need to
change any of these symbols, can get the `DecimalFormatSymbols` object from your
`DecimalFormat` and then modify it.

## Additional Sample Code

C/C++: See
[icu4c/source/samples/numfmt/](https://github.com/unicode-org/icu/blob/master/icu4c/source/samples/numfmt/)
in the ICU source distribution for code samples showing the use of ICU number
formatting.
