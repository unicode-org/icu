# Formatting Numbers

NOTE: This page is largely out of date and here for archive only. Please see
[NumberFormatter](http://icu-project.org/apiref/icu4c/numberformatter_8h.html)
([Java](http://icu-project.org/apiref/icu4j/com/ibm/icu/number/NumberFormatter.html))
and
[NumberRangeFormatter](http://icu-project.org/apiref/icu4c/numberrangeformatter_8h.html)
([Java](http://icu-project.org/apiref/icu4j/com/ibm/icu/number/NumberRangeFormatter.html))
for newer ways to format numbers in ICU 60+. This includes options for
scientific notation, compact notation, measurement units, currencies, ranges,
and other options.

*A related class is the ChoiceFormat (§) class described in the [Formatting
Messages](../messages/index.md) chapter. It maps ranges of numeric values to
strings.*

## NumberFormat

[NumberFormat](http://icu-project.org/apiref/icu4c/classNumberFormat.html) is
the abstract base class for all number formats. It provides an interface for
formatting and parsing numbers. It also provides methods to determine which
locales have number formats, and what their names are. NumberFormat helps format
and parse numbers for any locale. Your program can be written to be completely
independent of the locale conventions for decimal points or
thousands-separators. It can also be written to be independent of the particular
decimal digits used or whether the number format is a decimal. A normal decimal
number can also be displayed as a currency or as a percentage.

1234.5 //Decimal number
$1234.50 //U.S. currency
1.234,57€ //German currency
123457% //Percent

### Usage

#### Formatting for a Locale

To format a number for the current Locale, use one of the static factory methods
to create a format, then call a format method to format it. To format a number
for a different Locale, specify the Locale in the call to createInstance(). You
can control the numbering system to be used for number formatting by creating a
Locale that uses the @numbers keyword defined. For example, by default, the Thai
locale "th" uses the western digits 0-9. To create a number format that uses the
native Thai digits instead, first create a locale with "@numbers=thai" defined.
See [the description on Locales](../../locale/index.md) for details.

*If you are formatting multiple numbers, save processing time by constructing
the formatter once and then using it several times.*

#### Instantiating a NumberFormat

The following methods are used for instantiating NumberFormat objects:

1.  **createInstance()**
    Returns the normal number format for the current locale or for a specified
    locale.

2.  **createCurrencyInstance()**
    Returns the currency format for the current locale or for a specified
    locale.

3.  **createPercentInstance()**
    Returns the percentage format for the current locale or for a specified
    locale.

4.  **createScientificInstance()**
    Returns the scientific number format for the current locale or for a
    specified locale.

To create a format for spelled-out numbers, use a constructor on
RuleBasedNumberFormat (§).

#### Currency Formatting

Currency formatting, i.e., the formatting of monetary values, combines a number
with a suitable display symbol or name for a currency. By default, the currency
is set from the locale data from when the currency format instance is created,
based on the country code in the locale ID. However, for all but trivial uses,
this is fragile because countries change currencies over time, and the locale
data for a particular country may not be available.

For proper currency formatting, both the number and the currency must be
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
format() call or returned from parse() as usual.

1.  ICU4C (C++) NumberFormat.setCurrency() takes a Unicode string (const UChar
    \*) with the 3-letter code.

2.  ICU4C (C API) allows to set the currency code via unum_setTextAttribute()
    using the UNUM_CURRENCY_CODE selector.

3.  ICU4J NumberFormat.setCurrency() takes an ICU Currency object which
    encapsulates the 3-letter code.

4.  The base JDK's NumberFormat.setCurrency() takes a JDK Currency object which
    encapsulates the 3-letter code.

The functionality of Currency and setCurrency() is more advanced in ICU than in
the base JDK. When using ICU, setting the currency automatically adjusts the
number format object appropriately, i.e., it sets not only the currency symbol
and display name, but also the correct number of fraction digits and the correct
[rounding mode](rounding-modes.md). This is not the case with the base JDK. See
the API references for more details.

There is ICU4C sample code at
[icu/source/samples/numfmt/main.cpp](http://source.icu-project.org/repos/icu/trunk/icu4c/source/samples/numfmt/main.cpp)
which illustrates the use of NumberFormat.setCurrency().

#### Displaying Numbers

You can also control the display of numbers with methods such as
getMinimumFractionDigits. If you want even more control over the format or
parsing, or want to give your users more control, cast the NumberFormat returned
from the factory methods to a DecimalNumberFormat. This works for the vast
majority of countries.

#### Working with Positions

You can also use forms of the parse and format methods with ParsePosition and
UFieldPosition to enable you to:

1.  progressively parse through pieces of a string.

2.  align the decimal point and other areas.

For example, you can align numbers in two ways:

1.  If you are using a mono-spaced font with spacing for alignment, pass the
    FieldPosition in your format call with field = INTEGER_FIELD. On output,
    getEndIndex is set to the offset between the last character of the integer
    and the decimal. Add (desiredSpaceCount - getEndIndex) spaces at the front
    of the string. You can also use the space padding feature available in
    DecimalFormat.

2.  If you are using proportional fonts, instead of padding with spaces, measure
    the width of the string in pixels from the start to getEndIndex. Then move
    the pen by (desiredPixelWidth - widthToAlignmentPoint) before drawing the
    text. It also works where there is no decimal, but additional characters at
    the end (that is, with parentheses in negative numbers: "(12)" for -12).

#### Emulating printf

NumberFormat can produce many of the same formats as printf.

printf ICU Width specifier, e.g., "%5d" has a width of 5. Use DecimalFormat.
Either specify the padding, with can pad with any character, or specify a
minimum integer count and a minimum fraction count, which will emit a specific
number of digits, with zero padded to the left and right. Precision specifier
for %f and %e, e.g. "%.6f" or "%.6e". This defines the number of digits to the
right of the decimal point. Use DecimalFormat. Specify the maximum fraction
digits. General scientific notation, %g. This format uses either %f or %e,
depending on the magnitude of the number being displayed. Use ChoiceFormat with
DecimalFormat. For example, for a typical %g, which has 6 significant digits,
use a ChoiceFormat with thresholds of 1e-4 and 1e6. For values between the two
thresholds, use a fixed DecimalFormat with the pattern "@#####". For values
outside the thresholds, use a DecimalFormat with the pattern "@#####E0".

## DecimalFormat

DecimalFormat is a NumberFormat that converts numbers into strings using the
decimal numbering system. This is the formatter that provides standard number
formatting and parsing services for most usage scenarios in most locales. In
order to access features of DecimalFormat not exposed in the NumberFormat API,
you may need to cast your NumberFormat object to a DecimalFormat. You may also
construct a DecimalFormat directly, but this is not recommended because it can
hinder proper localization.

For a complete description of DecimalFormat, including the pattern syntax,
formatting and parsing behavior, and available API, see the [ICU4J DecimalFormat
API](http://icu-project.org/apiref/icu4j/com/ibm/icu/text/DecimalFormat.html) or
[ICU4C DecimalFormat
API](http://icu-project.org/apiref/icu4c/classDecimalFormat.html) documentation.

## DecimalFormatSymbols

[DecimalFormatSymbols](http://icu-project.org/apiref/icu4c/classDecimalFormatSymbols.html)
specifies the exact characters a DecimalFormat uses for various parts of a
number (such as the characters to use for the digits, the character to use as
the decimal point, or the character to use as the minus sign).

This class represents the set of symbols needed by DecimalFormat to format
numbers. DecimalFormat creates its own instance of DecimalFormatSymbols from its
locale data. The DecimalFormatSymbols can be adopted by a DecimalFormat
instance, or it can be specified when a DecimalFormat is created. If you need to
change any of these symbols, can get the DecimalFormatSymbols object from your
DecimalFormat and then modify it.

## RuleBasedNumberFormat

[RuleBasedNumberFormat](http://icu-project.org/apiref/icu4c/classRuleBasedNumberFormat.html)
can format and parse numbers in spelled-out format, e.g. "one hundred and
thirty-four". For example:

"one hundred and thirty-four" // 134 using en_US spellout
"one hundred and thirty-fourth" // 134 using en_US ordinal
"hundertvierunddreissig" // 134 using de_DE spellout
"MCMLVIII" // custom, 1958 in roman numerals

RuleBasedNumberFormat is based on rules describing how to format a number. The
rule syntax is designed primarily for formatting and parsing numbers as
spelled-out text, though other kinds of formatting are possible. As a
convenience, custom API is provided to allow selection from three predefined
rule definitions, when available: SPELLOUT, ORDINAL, and DURATION. Users can
request formatters either by providing a locale and one of these predefined rule
selectors, or by specifying the rule definitions directly.

*ICU provides number spellout rules for several locales, but not for all of the
locales that ICU supports, and not all of the predefined rule types. Also, as of
release 2.6, some of the provided rules are known to be incomplete.*

### Instantiation

Unlike the other standard number formats, there is no corresponding factory
method on NumberFormat. Instead, RuleBasedNumberFormat objects are instantiated
via constructors. Constructors come in two flavors, ones that take rule text,
and ones that take one of the predefined selectors. Constructors that do not
take a Locale parameter use the current default locale.

The following constructors are available:

1.  **RuleBasedNumberFormat(int)**
    Returns a format using predefined rules of the selected type from the
    current locale.

2.  **RuleBasedNumberFormat(Locale, int)**
    As above, but specifies locale.

3.  **RuleBasedNumberFormat(String)**
    Returns a format using the provided rules, and symbols (if required) from
    the current locale.

4.  **RuleBasedNumberFormat(String, Locale)**
    As above, but specifies locale.

### Usage

RuleBasedNumberFormat can be used like other NumberFormats. For example, in
Java:

double num = 2718.28;
NumberFormat formatter =
new RuleBasedNumberFormat(RuleBasedNumberFormat.SPELLOUT);
String result = formatter.format(num);
System.out.println(result);
// output (in en_US locale):
// two thousand seven hundred and eighteen point two eight

### Rule Sets

Rule descriptions can provide multiple named rule sets, for example, the rules
for en_US spellout provides a '%simplified' rule set that displays text without
commas or the word 'and'. Rule sets can be queried and set on a
RuleBasedNumberFormat. This lets you customize a RuleBasedNumberFormat for use
through its inherited NumberFormat API. For example, in Java:

You can also format a number specifying the ruleset directly, using an
additional overload of format provided by RuleBasedNumberFormat. For example, in
Java:

*There is no standardization of rule set names, so you must either query the
names, as in the first example above, or know the names that are defined in the
rules for that formatter.*

### Rules

The following example provides a quick look at the RuleBasedNumberFormat rule
syntax.

These rules format a number using standard decimal place-value notation, but
using words instead of digits, e.g. 123.4 formats as 'one two three point four':

"-x: minus >>;\\n"
+ "x.x: << point >>;\\n"
+ "zero; one; two; three; four; five; six;\\n"
+ " seven; eight; nine;\\n"
+ "10: << >>;\\n"
+ "100: << >>>;\\n"
+ "1000: <<, >>>;\\n"
+ "1,000,000: <<, >>>;\\n"
+ "1,000,000,000: <<, >>>;\\n"
+ "1,000,000,000,000: <<, >>>;\\n"
+ "1,000,000,000,000,000: =#,##0=;\\n";

Rulesets are invoked by first applying negative and fractional rules, and then
using a recursive process. It starts by finding the rule whose range includes
the current value and applying that rule. If the rule so directs, it emits text,
including text obtained by recursing on new values as directed by the rule. As
you can see, the rules are designed to accomodate recursive processing of
numbers, and so are best suited for formatting numbers in ways that are
inherently recursive.

A full explanation of this example can be found in the [RuleBasedNumberFormat
examples](rbnf-examples.md) . A complete description of the rule syntax can be
found in the [RuleBasedNumberFormat API
Documentation](http://icu-project.org/apiref/icu4c/classRuleBasedNumberFormat.html)
.

## Additional Sample Code

C/C++: See
[icu/source/samples/numfmt/](http://source.icu-project.org/repos/icu/trunk/icu4c/source/samples/numfmt/)
in the ICU source distribution for code samples showing the use of ICU number
formatting.
