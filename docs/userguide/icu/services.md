---
layout: default
title: ICU Services
nav_order: 4
parent: ICU
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU Services
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview of the ICU Services

ICU enables you to write language-independent C/C++ and Java code that is used on
separate, localized resources to get language-specific results. ICU supports
many features, including language-sensitive text, dates, time, numbers,
currency, message sorting, and searching. ICU provides language-specific results
for a broad range of languages.

### Strings, Properties and CharacterIterator

ICU provides basic Unicode support for the following:

*   [Unicode strings](strings/index.md)

    ICU includes type definitions for UTF-16 strings and code points. It also
    contains many C `u_string` functions and the C++ `UnicodeString` class with many
    additional string functions.

*   [Unicode properties](strings/properties.md)

    ICU includes the C definitions and functions found in `uchar.h` as well as
    some macros found in `utf.h`. It also includes the C++ Unicode class.

*   [Unicode string iteration](strings/characteriterator.md)

    In C, ICU uses the macros in `utf.h` for the iteration of strings. In C++, ICU
    uses the characterIterator and its subclasses.

### Conversion Basics

A converter is used to transform text from one encoding type to another. In the
case of Unicode, ICU transforms text from one encoding codepage to Unicode and
back. An encoding is a mapping from a given character set definition to the
actual bits used to represent the data.

### Locale and Resources

The ICU package contains the locale and resource bundles as well as the classes
that implement them. Also, the ICU package contains the locale data (plain text
resource bundles) and provides APIs to access and make use of that data in
various services. Users need to understand these terms and the relationship
between them.

A locale identifies a group of users who have similar cultural and linguistic
expectations for how their computers interact with them and process data. This
is an abstract concept that is typically expressed by one of the following:

A locale ID specifies a language and region enabling the software to support
culturally and linguistically appropriate information for each user. A locale
object represents a specific geographical, political, or cultural region. As a
programmatic expression of locale IDs, ICU provides the C++ `Locale` class. In C,
Application Programming Interfaces (APIs) use simple C `string` for locale IDs.

ICU stores locale-specific data in resource bundles, which provide a general
mechanism to access strings and other objects for ICU services to perform
according to locale conventions. ICU contains data for its services to support
many locales. Resource bundles contain the locale data of applications that use
ICU. In C++, the `**ResourceBundle**` implements the locale data. In C, this
feature is provided by the `**ures_**` interface.

In addition to storing system-level data in ICU's resource bundles, applications
typically also need to use resource bundles of their own to store
locale-dependent application data. ICU provides the generic resource bundle APIs
to access these bundles and also provides the tools to build them.

> :point_right: **Note**: *Display strings, which are displayed to a user of a program, are bundled in a
separate file instead of being embedded in the lines of the program.*

### Locales and Services

The interaction between locales and services is fundamental to ICU. Please refer
to [Locales and Services](./locale/index#locales-and-services).

### Transliteration

Transliteration was originally designed to convert characters from one script to
another (for example, from Greek to Latin, or Japanese Katakana to Latin). Now,
transliteration is a more flexible mechanism that has pre-built transformations
for case conversions, normalization conversions, the removal of given
characters, and also for a variety of language and script transliterations.
Transliterations can be chained together to perform a series of operations and
each step of the process can use a UnicodeSet to restrict the characters that
are affected. There are two basic types of transliterators:

Most natural language transliterators (such as Greek-Latin) are written a
rule-based transliterators.

Transliterators can be written as text files using a
simple language that is similar to regular expression syntax.

### `Date` and `Time` Classes

Date and time routines manage independent date and time functions in
milliseconds since January 1, 1970 (0:00:00.000 UTC). Points in time before then
are represented as negative numbers.

ICU provides the following [classes](datetime/index.md) to support calendars and
time zones:

*   [`Calendar`](datetime/calendar/index#calendar)
    
    The abstract superclass for extracting calendar-related attributes from a `Date` value.

*   [`GregorianCalendar`](datetime/calendar/index#gregoriancalendar)
    
    A concrete class for representing a Gregorian calendar.

*   [`TimeZone`](datetime/timezone/index.md)
    
    An abstract superclass for representing a time zone.

*   [`SimpleTimeZone`](datetime/timezone/index.md)
    
    A concrete class for representing a time zone for use with a Gregorian calendar.

> :point_right: **Note**: *C classes provide the same functionality as the C++ classes with the exception
of subclassing.*

### Format and Parse

Formatters translate between non-text data values and textual representations of
those values. The result is a string of text that represents the internal value.
A formatter can parse a string and convert a textual representation of some
value (if it finds one it understands) back into its internal representation.
For example, when the formatter reads the characters 1, 0, and 3 followed by
something other than a digit, it produces the value 103 in its internal binary
representation.

A formatter takes a value and produces a user-readable string that represents
that value or takes a string and parses it to produce a value.

ICU provides the following areas and classes for general formatting, formatting
numbers, formatting dates and times, and formatting messages:

#### General Formatting

See [Formatting and Parsing Classes](format_parse/index#formatting-and-parsing-classes) for an introduction to the following:

* `Format`
* `FieldPosition`
* `ParsePosition`
* `Formattable`

#### Formatting Numbers

*   [`NumberFormat`](format_parse/numbers/index#formatting-numbers)
    NumberFormat provides the basic fields and methods to format number objects
    and number primitives into localized strings and parse localized strings to
    number objects.

*   [`DecimalFormat`](format_parse/numbers/index#decimalformat)
    DecimalFormat provides the methods used to format number objects and number
    primitives into localized strings and parse localized strings into number
    objects in base 10.

*   [`DecimalFormatSymbols`](format_parse/numbers/index#decimalformatsymbols)
    DecimalFormatSymbols is a concrete class used by DecimalFormat to access
    localized number strings such as the grouping separators, the decimal
    separator, and the percent sign.

#### Formatting Dates and Times

*   [`DateFormat`](format_parse/datetime/index.md)
    
    `DateFormat` provides the basic fields and methods for formatting date objects
    to localized strings and parsing date and time strings to date objects.

*   [`SimpleDateFormat`](format_parse/datetime/index.md)
    
    `SimpleDateFormat` is a concrete class used to format date objects to
    localized strings and to parse date and time strings to date objects using a
    `GregorianCalendar`.

*   [`DateFormatSymbols`](format_parse/datetime/index.md)
    
    `DateFormatSymbols` is a concrete class used to access localized date and time
    formatting strings, such as names of the months, days of the week, and the
    time zone.

#### Formatting Messages

*   [`MessageFormat`](format_parse/messages/index.md)
    
    `MessageFormat` is a concrete class used to produce a language-specific user
    message that contains numbers, currency, percentages, date, time, and string
    variables.

*   [`ChoiceFormat`](format_parse/messages/index.md)
    
    `ChoiceFormat` is a concrete class used to map strings to ranges of numbers
    and to handle plural words and name series in user messages.

> :point_right: **Note**: *C classes provide the same functionality as the C++ classes with the exception
of subclassing.*

### Searching and Sorting

Sorting and searching non-English text presents a number of challenges that many
English speakers are unaware of. The primary source of difficulty is accents,
which have very different meanings in different languages, and sometimes even
within the same language:

*   Many accented letters, such as the é in café, are treated as minor variants
    on the letter that is accented.

*   Sometimes the accented form of a letter is treated as a distinct letter for
    the purposes of comparison. For example, Å in Danish is treated as a
    separate letter that sorts just after Z.

*   In some cases, an accented letter is treated as if it were two letters. In
    traditional German, for example, ä is compared as if it were ae.

Searching and sorting is done through collation using the `Collator` class and its
sub-classes `RuleBasedCollator` and `CollationElementIterator` as well as the
`CollationKey` object. Collation determines the proper sort sequence for two or
more natural language strings. It also can determine if two strings are
equivalent for the purpose of searching.

The `Collator` class and its sub-class `RuleBasedCollator` perform locale-sensitive
string comparisons to create sorting and searching routines for natural language
text. `Collator` and `RuleBasedCollator` can distinguish between characters
associated with base characters (such as 'a' and 'b'), accent marks (such as
'ò', 'ó'), and uppercase or lowercase properties (such as 'a' and 'A').

ICU provides the following collation classes for sorting and searching natural
language text according to locale-specific rules:

*   [`Collator`](collation/architecture.md) is the abstract base class of all classes that compare strings.

*   [`CollationElementIterator`](collation/architecture.md) is a concrete iterator class that provides an
    iterator for stepping through each character of a locale-specific string
    according to the rules of a specific collator object.

*   [`RuleBasedCollator`](collation/architecture.md) is the only built-in 
    implementation of the collator. It
    provides a sophisticated mechanism for comparing strings in a
    language-specific manner, and an interface that allows the user to
    specifically customize the sorting order.

*   [`CollationKey`](collation/architecture.md)  is an object that enables the fast sorting of strings by
    representing a string as a sort key under the rules of a specific collator
    object.

> :point_right: **Note**: *C classes provide the same functionality as the C++ classes with the exception
of subclassing.*

### Text Analysis

The BreakIterator services can be used for formatting and handling text;
locating the beginning and ending points of a word; counting words, sentences,
and paragraphs; and listing unique words. Specifically, text operations can be
done to locate the following linguistic boundaries:

*   Display text on the screen and locate places in the text where the
    BreakIterator can perform word-wrapping to fit the text within the margins

*   Locate the beginning and end of a word that the user has selected

*   Count graphemes (or characters), words, sentences, or paragraphs

*   Determine how far to move in the text store when the user hits an arrow key
    to move forward or backward one grapheme

*   Make a list of all the unique words in a document

*   Figure out whether or not a range of text contains only whole words

*   Capitalize the first letter of each word

*   Extract a particular unit from the text such as "find me the third grapheme
    in this document"

The BreakIterator services were designed and developed around an "iterator" or
"cursor" style of interface. The object points to a particular place in the
text. You can move the pointer forward or backward to search the text for
boundaries.

The `BreakIterator` class makes it possible to iterate over user characters. A
`BreakIterator` can find the location of a character, word, sentence or potential
line-break boundary. This makes it possible for a software program to properly
select characters for text operations such as highlighting a character, cutting
a word, moving to the next sentence, or wrapping words at a line ending.
`BreakIterator` performs these operations in a locale-sensitive manner, meaning
that it recognizes text boundaries according to the particular locale ID.

ICU provides the following classes for iterating over locale-specific text:

*   [`BreakIterator`](boundaryanalysis/index.md)
    
    The abstract base class that defines the operations for finding and getting
    the positions of logical breaks in a string of text: characters, words,
    sentences, and potential line breaks.

*   [`CharacterIterator`](strings/characteriterator.md)
    
    The abstract base class for forward and backward iteration over a string of
    Unicode characters.

*   [`StringCharacterIterator`](strings/index.md)
    
    A concrete class for forward and backward iteration over a string of Unicode
    characters. `StringCharacterIterator` inherits from `CharacterIterator`.

### Paragraph Layout

See [Paragraph Layout](./layoutengine/paragraph.md) for more details.

## Locale-Dependent Operations

Many of the ICU classes are locale-sensitive, meaning that you have to create a
different one for each locale.

| C API | C++ Class | Description |
|----------|--------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ubrk_` | `BreakIterator` | The `BreakIterator` class implements methods to find the location of boundaries in the text. |
| `ucal_` | `Calendar` | The `Calendar` class is an abstract base class that converts between a `UDate` object and a set of integer fields such as `YEAR`, `MONTH`, `DAY`, `HOUR`, and so on. |
| `umsg.h` | `ChoiceFormat` | A `ChoiceFormat` class enables you to attach a format to a range of numbers. |
| `ucol_` | `CollationElementIterator` | The `CollationElementIterator` class is used as an iterator to walk through each character of an international string. |
| `ucol_` | `CollationKey` | The `Collator` class generates the Collation keys. |
| `ucol_` | `Collator` | The `Collator` class performs locale-sensitive string comparison. |
| `udat_` | `DateFormat` | `DateFormat` is an abstract class for a family of classes. `DateFormat` converts dates and times from their internal representations to a textual form that is language-independent, and then back to their internal representations. |
| `udat_` | `DateFormatSymbols` | `DateFormatSymbols` is a public class that encapsulates localized date and time formatting data. This information includes time zone information. |
| `unum_` | `DecimalFormatSymbols` | This class represents the set of symbols needed by `DecimalFormat` to format numbers. |
| `umsg.h` | `Format` | The `Format` class is the base class for all formats. |
| `ucal_` | `GregorianCalendar` | `GregorianCalendar` is a concrete class that provides the standard calendar used in many locations. |
| `uloc_` | `Locale` | A `Locale` object represents a specific geographical, political, or cultural region. |
| `umsg.h` | `MessageFormat` | `MessageFormat` provides a means to produce concatenated messages in language-neutral way. |
| `unum_` | `NumberFormat` | `NumberFormat` is an abstract base class for all number formats. |
| `ures_` | `ResourceBundle` | `ResourceBundle` provides a means to access a collection of locale-specific information. |
| `ucol_` | `RuleBasedCollator` | The `RuleBasedCollator` provides the implementation of the `Collator` class using data-driven tables. |
| `udat_` | `SimpleDateFormat` | `SimpleDateFormat` is a concrete class used to format and parse dates in a language-independent way. |
| `ucal_` | `SimpleTimeZone` | `SimpleTimeZone` is a concrete subclass of `TimeZone` that represents a time zone for use with a Gregorian calendar. |
| `usearch_` | `StringSearch` | `StringSearch` provides a way to search text in a locale sensitive manner. |
| `ucal_` | `TimeZone` | `TimeZone` represents a time zone offset, and also determines daylight savings time settings. |

## Locale-Independent Operations

The following ICU services can be used in all locales as they provide
locale-independent services and users do not need to specify a locale ID:

| C API | C++ Class | Description |
|-----------|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ubidi_` |  | `UBiDi` is used for implementing the Unicode BiDi algorithm. |
| `utf.h` | `CharacterIterator` | `CharacterIterator` is an abstract class that defines an API for iteration on text objects. It is an interface for forward and backward iteration and for the random access of a text object. Also, it provides backward compatibility to the Java and older ICU `CharacterIterator` classes. |
| n/a | `Formattable` | `Formattable` is a thin wrapper class that converts between the primitive numeric types (`double`, `long`, and so on) and the `UDate` and `UnicodeString` classes. `Formattable` objects can be passed to the `Format` class or its subclasses for formatting. |
| `unorm_` | `Normalizer` | `Normalizer` transforms Unicode text into an equivalent composed or decomposed form to allow for easier sorting and searching of text. |
| n/a | `ParsePosition` | `ParsePosition` is a simple class used by the `Format` class and its subclasses to keep track of the current position during parsing. |
| `uidna_` |  | An implementation of the IDNA protocol as defined in RFC 3490. |
| `utf.h` | `StringCharacterIterator` | A concrete subclass of `CharacterIterator` that iterates over the characters (code units or code points) in a `UnicodeString`. |
| `utf.h` | `UCharCharacterIterator` | A concrete subclass of `CharacterIterator` that iterates over the characters (code units or code points) in a `UChar` array. |
| `uchar.h` |  | The Unicode character properties API allows you to query the properties associated with individual Unicode character values. |
| `uregex_` | `RegexMatcher` | `RegexMatcher` is a regular expressions implementation. This allows you to perform string matching based upon a pattern. |
| `utrans_` | `Transliterator` | `Transliterator` is an abstract class that transliterates text from one format to another. The most common type of transliterator is a script, or an alphabet. |
| `uset_` | `UnicodeSet` | Objects of the `UnicodeSet` class represent character classes used in regular expressions. These classes specify a subset of the set of all Unicode characters. This is a mutable set of Unicode characters. |
| `ustring.h` | `UnicodeString` | `UnicodeString` is a string class that stores Unicode characters directly. This class is a concrete implementation of the abstract class `Replaceable`. |
| `ushape.h` |  | Provides operations to transform (shape) between Arabic characters and their presentation forms. |
| `ucnv_` |  | The Unicode conversion API allows you to convert data written in one codepage/encoding to and from UTF-16. |
