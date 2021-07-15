---
layout: default
title: Formatting
nav_order: 7
has_children: true
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Formatting and Parsing
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

Formatters translate between binary data and human-readable textual
representations of these values. For example, you cannot display the computer
representation of the number 103. You can only display the numeral 103 as a
textual representation (using three text characters). The result from a
formatter is a string that contains text that the user will recognize as
representing the internal value. A formatter can also parse a string by
converting a textual representation of some value back into its internal
representation. For example, it reads the characters 1, 0 and 3 followed by
something other than a digit, and produces the value 103 as an internal binary
representation.

These classes encapsulate information about the display of localized times,
days, numbers, currencies, and messages. Formatting classes do both formatting
and parsing and allow the separation of the data that the end-user sees from the
code. Separating the program code from the data allows a program to be more
easily localized. Formatting is converting a date, time, number, message or
other object from its internal representation into a string. Parsing is the
reverse operation. It is the process of converting a string to an internal
representation of the date, time, number, message or other object.

Using the formatting classes is an important step in internationalizing your
software because the `format()` and `parse()` methods in each of the classes make
your software language neutral, by replacing implicit conversions with explicit
formatting calls.

## Internationalization Formatting Tips

This section discusses some of the ways you can format and parse numbers,
currencies, dates, times and text messages in your program so that the data is
separate from the code and can be easily localized. This is the information your
users see on their computer screens, so it needs to be in a language and format
that conforms to their local conventions.

Some things you need to keep in mind while you are creating your code are the
following:

*   Keep your code and your data separate

*   Format the data in a locale-sensitive manner

*   Keep your code locale-independent

*   Avoid writing special routines to handle specific locales

*   String objects formatted by `format()` are parseable by the `parse()` method\*

> :point_right: **Note**: Although parsing is supported in several legacy ICU APIs,
it is generally considered bad practice to parse localized strings.
For more information, read [Why You Should Not Parse
Localized Strings](https://blog.sffc.xyz/post/190943794505/why-you-should-not-parse-localized-strings).

### Numbers and Currencies

Programs store and operate on numbers using a locale-independent binary
representation. When displaying or printing a number it is converted to a
locale-specific string. For example, the number 12345.67 is "12,345.67" in the
US, "12 345,67" in France and "12.345,67" in Germany.

By invoking the methods provided by the `NumberFormat` class, you can format
numbers, currencies, and percentages according to the specified or default
locale. `NumberFormat` is locale-sensitive so you need to create a new
`NumberFormat` for each locale. `NumberFormat` methods format primitive-type
numbers, such as double and output the number as a locale-specific string.

For currencies you call `getCurrencyInstance` to create a formatter that returns a
string with the formatted number and the appropriate currency sign. Of course,
the `NumberFormat` class is unaware of exchange rates so, the number output is the
same regardless of the specified currency. This means that the same number has
different monetary values depending on the currency locale. If the number is
9988776.65 the results will be:

*   9 988 776,65 € in France

*   9.988.776,65 € in Germany

*   $9,988,776.65 in the United States

In order to format percentages, create a locale-specific formatter and call the
`getPercentInstance` method. With this formatter, a decimal fraction such as 0.75
is displayed as 75%.

#### Customizing Number Formats

If you need to customize a number format you can use the `DecimalFormat` and
the `DecimalFormatSymbols` classes in the [Formatting
Numbers](numbers/index#formatting-numbers) chapter. This not usually necessary and
it makes your code much more complex, but it is available for those rare
instances where you need it. In general, you would do this by explicitly
specifying the number format pattern.

If you need to format or parse spelled-out numbers, you can use the
`RuleBasedNumberFormat` class (see the [Formatting Numbers](numbers/index#formatting-numbers) chapter).
You can instantiate a default formatter for a locale, or by using the 
`RuleBasedNumberFormat` rule syntax, specify your own.

Using `NumberFormat` class methods (see the [Formatting Numbers](numbers/index#formatting-numbers) chapter)
with a predefined locale is the easiest and the most accurate way to format numbers, and currencies.

> :point_right: **Note**: *See [Properties and ICU Rule Syntax](../strings/properties) for
information regarding syntax characters.*

### Date and Times

You display or print a Date by first converting it to a locale-specific string
that conforms to the conventions of the end user's Locale. For example, Germans
recognize 20.4.98 as a valid date, and Americans recognize 4/20/98.

> :point_right: **Note**: *The appropriate Calendar support is required for different locales. For
example, the Buddhist calendar is the official calendar in Thailand so the
typical assumption of Gregorian Calendar usage should not be used. ICU will pick
the appropriate Calendar based on the locale you supply when opening a `Calendar`
or `DateFormat`.*

### Messages

Message format helps make the order of display elements localizable. It helps
address problems of grammatical differences in languages. For example, consider
the sentence, "I go to work by car everyday." In Japanese, the grammar
equivalent can be "Everyday, I to work by car go." Another example will be the
plurals in text, for example, "no space for rent, one room for rent and many
rooms for rent," where "for rent" is the only constant text among the three.

## Formatting and Parsing Classes

ICU provides four major areas and twelve classes for formatting numbers, dates
and messages:

### General Formatting

*   `Format`:
    
    The abstract superclass of all format classes. It provides the basic methods
    for formatting and parsing numbers, dates, strings and other objects.

*   `FieldPosition`:
    
    A concrete class for holding the field constant and the begin and end
    indices for number and date fields.

*   `ParsePosition`:
    
    A concrete class for holding the parse position in a string during parsing.

*   `Formattable`:
    
    `Formattable` objects can be passed to the `Format` class or its subclasses for
    formatting. It encapsulates a polymorphic piece of data to be formatted and
    is used with `MessageFormat`. `Formattable` is used by some formatting
    operations to provide a single "type" that encompasses all formattable
    values (e.g., it can hold a number, a date, or a string, and so on).

*   `UParseError`:
    
    `UParseError` is used to returned detailed information about parsing errors.
    It is used by the ICU parsing engines that parse long rules, patterns, or
    programs. This is helpful when the text being parsed is long enough that
    more information than a `UErrorCode` is needed to localize the error.

**Formatting Numbers**

*   [`NumberFormat`](numbers/legacy-numberformat#numberformat)
    
    The abstract superclass that provides the basic fields and methods for
    formatting `Number` objects and number primitives to localized strings and
    parsing localized strings to `Number` objects.

*   [`DecimalFormat`](numbers/legacy-numberformat#decimalformat)
    
    A concrete class for formatting `Number` objects and number primitives to
    localized strings and parsing localized strings to `Number` objects, in base 10.

*   [`RuleBasedNumberFormat`](numbers/rbnf)
    
    A concrete class for formatting `Number` objects and number primitives to
    localized text, especially spelled-out format such as found in check writing
    (e.g. "two hundred and thirty-four"), and parsing text into `Number` objects.

*   [`DecimalFormatSymbols`](numbers/legacy-numberformat#decimalformatsymbols)
    
    A concrete class for accessing localized number strings, such as the
    grouping separators, decimal separator, and percent sign. Used by
    `DecimalFormat`.

**Formatting Dates and Times**

*   [`DateFormat`](datetime/index#dateformat)
    
    The abstract superclass that provides the basic fields and methods for
    formatting `Date` objects to localized strings and parsing date and time
    strings to `Date` objects.

*   [`SimpleDateFormat`](datetime/index#simpledateformat)
    
    A concrete class for formatting `Date` objects to localized strings and
    parsing date and time strings to `Date` objects, using a `GregorianCalendar`.

*   [`DateFormatSymbols`](datetime/index#dateformatsymbols)
    
    A concrete class for accessing localized date-time formatting strings, such
    as names of the months, days of the week and the time zone.

**Formatting Messages**

*   [`MessageFormat`](messages/index#messageformat)
    
    A concrete class for producing a language-specific user message that
    contains numbers, currency, percentages, date, time and string variables.

*   [`ChoiceFormat`](messages/examples#choiceformat-class)
    
    A concrete class for mapping strings to ranges of numbers and for handling
    plurals and names series in user messages.
