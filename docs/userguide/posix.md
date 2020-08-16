---
layout: default
title: C/POSIX Migration
nav_order: 6
parent: ICU
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# C/POSIX Migration
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Migration from Standard C and POSIX APIs

The ISO C and POSIX standards define a number of APIs for string handling and
internationalization in C. They do not support Unicode well because they were
initially designed before Unicode/ISO 10646 were developed, and the POSIX APIs
are also problematic for other internationalization aspects.

This chapter discusses C/POSIX APIs with their problems, and shows which ICU
APIs to use instead.

> :point_right:  **Note**: *We use the term "POSIX" to mean the POSIX.1 standard (IEEE Std 1003.1) which
defines system interfaces and headers with relevance for string handling and
internationalization. The XPG3, XPG4, Single Unix Specification (SUS) and other
standards include POSIX.1 as a subset, adding other specifications that are
irrelevant for this topic.*

> :construction: This chapter is not complete yet – more POSIX APIs are expected to be discussed
in the future.

## Strings and Characters

### Character Sets and Encodings

#### ISO C

The ISO C standard provides two basic character types (`char` and `wchar_t`) and
defines strings as arrays of units of these types. The standard allows nearly
arbitrary character and string character sets and encodings, which was necessary
when there was no single character set that worked everywhere.

For portable C programs, characters and strings are opaque, i.e., a program
cannot assume that any particular character is represented by any particular
code or sequence of codes. Programs use standard library functions to handle
characters and strings. Only a small set of characters — usually the set of
graphic characters available in US-ASCII — can be reliably accessed via
character and string literals.

#### Problems

1.  Many different encodings are used on each platform, making it difficult for
    multiple programs and libraries to process the same text.

2.  Programs often need to know the codes of special characters. For example,
    code that parses a filename needs to know how the path and file separators
    are encoded; this is commonly possible because filenames deliberately use
    US-ASCII characters, but any software that uses non-ASCII characters becomes
    platform-dependent. It is practically impossible to provide sophisticated
    text processing without knowledge of the character set, its string encoding,
    and other detailed features.

3.  The C/POSIX standards only provide a very limited set of useful functions
    for character and string handling; many functions that are provided do not
    work for non-trivial cases.

4.  While the size of the char type is in practice fixed to 8 bits in modern
    compilers, and its common encodings are reasonably well documented, the size
    of wchar_t varies between 8/16/32 bits depending on the compiler, and only
    few of the string encodings used with it are documented.

5.  See also [What size wchar_t do I need for
    Unicode?](http://icu-project.org/docs/papers/unicode_wchar_t.html)

6.  A program based on this model must be recompiled for each platform. Usually,
    it must be recompiled for each supported language or family of languages.

7.  The ISO C standard basically requires, by how its standard functions are
    defined, that the data type for a single character code in a large character
    set is the same as the string base unit type (wchar_t). This has led to C
    standard library implementations using Unicode encodings which are either
    limited for single-character functions to only part of Unicode, or suffer
    from reduced interoperability with most Unicode-aware software.

#### ICU

ICU always processes Unicode text. Unicode covers all languages and allows safe
hard coding of character codes, in addition to providing many standard or
recommended algorithms and a lot of useful character property data. See the
chapters about [Unicode Basics](unicode.md) and [Strings](strings/index.md) and others.

ICU uses the 16-bit encoding form of Unicode (UTF-16) for processing, making it
fully interoperable with most Unicode-aware software. See [UTF-16 for
Processing](http://www.unicode.org/notes/tn12/). In the case of ICU4J, this is
naturally the case because the Java language and the JDK use UTF-16.

ICU uses and/or provides direct access to all of the [Unicode
properties](strings/properties.md) which provide a much finer-grained
classification of characters than [C/POSIX character
classes](https://htmlpreview.github.io/?https://github.com/unicode-org/icu-docs/blob/master/design/posix_classes.html).

In C/C++ source code character and string literals, ICU uses only "invariant"
characters. They are the subset of graphic ASCII characters that are almost
always encoded with the same byte values on all systems. (One set of byte values
for ASCII-based systems, and another such set of byte values for EBCDIC
systems.) See
[`utypes.h`](https://github.com/unicode-org/icu/blob/master/icu4c/source/common/unicode/utypes.h)
for the set of "invariant" characters.

With the use of Unicode, the implementation of many of the Unicode standard
algorithms, and its cross-platform availability, ICU provides for consistent,
portable, and reliable text processing.

### Case Mappings

#### ISO C

The standard C functions `tolower()`, `toupper()`, etc. take and return one
character code each.

#### Problems

1.  This does not work for German, where the character "ß" (sharp s) uppercases
    to the two characters "SS". (It "expands".)

2.  It does not work for Greek, where the character "Σ" (capital sigma)
    lowercases to either "ς" (small final sigma) or "σ" (small sigma) depending
    on whether the capital sigma is the last letter in a word. (It is
    context-dependent.)

3.  It does not work for Lithuanian and Turkic languages where a "combining dot
    above" character may need to be removed in certain cases. (It "contracts"
    and is language- and context-dependent.)

4.  There are a number of other such cases.

5.  There are no standard functions for title-casing strings.

6.  There are no standard functions for case-folding strings. (Case-folding is
    used for case-insensitive comparisons; there are C/POSIX functions for
    direct, case-insensitive comparisons of pairs of strings. Case-folding is
    useful when one string is compared to many others, or as part of a chain of
    transformations of a string.)

#### ICU

Case mappings are operations taking and returning strings, to support length
changes and context dependencies. Unicode provides algorithms and data for
proper case mappings, and ICU provides APIs for them. (See the API references
for various string functions and for Transforms/Transliteration.)

### Character Classes

#### ISO C

The standard C functions isalpha(), isdigit(), etc. take a character code each
and return boolean values for whether the character belongs to the current
locale's respective character class.

#### Problems

1.  Character classes are bound to locales, instead of providing consistent
    classifications for characters.

2.  The same character may have different classifications depending on the
    locale and the platform.

3.  There are only very few POSIX character classes, and they are not well
    defined. For example, there is a class for punctuation characters but not
    one for symbols.

4.  For example, the dollar symbol (“$”) may or may not belong to the punct
    class depending on the locale, even on the same system.

5.  The standard allows at most two sets of decimal digits: The digits of the
    “portable character set” (i.e., those in the ASCII repertoire) and one more.
    Some implementations only recognize ASCII digits in the isdigit() function.
    However, there are many sets of decimal digits in a multilingual character
    set like Unicode.

6.  The POSIX standard assumes that each locale definition file carries the
    character class data for all relevant characters. With many locales using
    overlapping character repertoires, this can lead to a lot of duplication.
    For efficiency, many UTF-8 locales define character classes only for very
    few characters instead of for all of Unicode. For example, some de_DE.utf-8
    locales only define character classes for characters used in German, or for
    the repertoire of ISO 8859-1 – in other words, for only a tiny fraction of
    the representable Unicode repertoire. Processing of text using more than
    this repertoire is not possible with such an implementation.

7.  For more about the problems with POSIX character classes in a Unicode
    context see [Annex C: Compatibility Properties in Unicode
    Technical Standard #18: Unicode Regular Expressions](http://www.unicode.org/reports/tr18/#Compatibility_Properties)
    and see the mailing list archives for the unicode list (on unicode.org). See
    also the ICU design document about [C/POSIX character
    classes](https://htmlpreview.github.io/?https://github.com/unicode-org/icu-docs/blob/master/design/posix_classes.html).

#### ICU

ICU provides locale-independent access to all [Unicode
properties](strings/properties.md) (except Unihan.txt properties), as well as to
the POSIX character classes, via functions defined in `uchar.h` and in ICU4J's
`UCharacter` class (see API references) as well as via `UnicodeSet`. The POSIX
character classes are implemented according to the recommendations in UTS #18.

The Unicode Character Database defines more than 70 character properties, their
values are designed for the large character set as well as for real text
processing, and they are updated with each version of Unicode. The UCD is
available online, facilitating industry-wide consistency in the implementation
of Unicode properties.

## Formatting and Parsing

### Currency Formatting

#### POSIX

The `strfmon()` function is used to format monetary values. The default format and
the currency display symbol or display name are selected by the LC_MONETARY
locale ID. The number formatting can also be controlled with a formatting string
resembling what `printf()` uses.

#### Problems

1.  Selection of the currency via a locale ID is unreliable: Countries change
    currencies over time, and the locale data for a particular country may not
    be available. This results in using the wrong currency. For example, an
    application may assume that a country has switched from a previous currency
    to the Euro, but it may run on an OS that predates the switch.

2.  Using a single locale ID for the whole format makes it very difficult to
    format values for multiple currencies with the same number format (for
    example, for an exchange rate list or for showing the price of an item
    adjusted for several currencies). `strfmon()` allows to specify the number
    format fully, but then the application cannot use a country's default number
    format.

3.  The set of formattable currencies is limited to those that are available via
    locale IDs on a particular system.

4.  There does not appear to be a function to parse currency values.

#### ICU

ICU number formatting APIs have separate, orthogonal settings for the number
format, which can be selected with a locale ID, and the currency, which is
specified with an ISO code. See the [Formatting
Numbers](format_parse/numbers/index.md) chapter for details.
