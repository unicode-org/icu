---
layout: default
title: Architecture
nav_order: 2
parent: Collation
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Collation Service Architecture
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

This section describes the design principles, architecture and coding
conventions of the ICU Collation Service.

## Collator

To use the Collation Service, a Collator must first be instantiated. An
Collator is a data structure or object that maintains all of the property
and state information necessary to define and support the specific collation
behavior provided. Examples of properties described in the Collator are the
locale, whether normalization is to be performed, and how many levels of
collation are to be evaluated. Examples of the state information described in
the Collator include the direction of a Collation Element Iterator (forward
or backward) and the status of the last API executed.

The Collator is instantiated either by referencing a locale or by defining a
custom set of rules (a tailoring).

The Collation Service uses the paradigm:

1.  Open a Collator,

2.  Use while necessary,

3.  Close the Collator.

Collator instances cannot be shared among threads. You should open them
instead, and use a different collator for each separate thread. The safe clone
function is supported for cloning collators in a thread-safe fashion.

The Collation Service follows the ICU conventions for locale designation
when opening collators:

1.  NULL means the default locale.

2.  The empty locale name ("") means the root locale.
    The Collation Service adheres to the ICU conventions described in the
    "[ICU Architectural Design](../design.md) " section of the users guide.
    In particular:

3.  The standard error code convention is usually followed. (Functions that do
    not take an error code parameter do so for backward compatibility.)

4.  The string length convention is followed: when passing a `UChar *`, the
    length is required in a separate argument. If -1 is passed for the length,
    it is assumed that the string is zero terminated.

### Collation locale and keyword handling

When a collator is created from a locale, the collation service (like all ICU
services) must map the requested locale to the localized collation data
available to ICU at the time. It does so using the standard ICU locale fallback
mechanism. See the fallback section of the [locale
chapter](../locale/index.md) for more details.

If you pass a regular locale in, like "en_US", the collation service first
searches with fallback for "collations/default" key. The first such key it finds
will have an associated string value; this is the keyword name for the collation
that is default for this locale. If the search falls all the way back to the
root locale, the collation service will us the "collations/default" key there,
which has the value "standard".

If there is a locale with a keyword, like "de-u-co-phonebk" or "de@collation=phonebook", the
collation service searches with fallback for "collations/phonebook". If the
search is successful, the collation service uses the string value it finds to
instantiate a Collator. If the search fails because no such key is present in
any of ICU's locale data (e.g., "de@collation=funky"), the service returns a
collator implementing the default tailoring of the locale.
If the fallback is all the way to the root locale, then
the return `UErrorCode` is `U_USING_DEFAULT_WARNING`.

## Input values for collation

Collation deals with processing strings. ICU generally requires that all the
strings should be in UTF-16 format, and that all the required conversion should
done before ICU functions are used. In the case of collation, there are APIs
that can also take instances of character iterators (`UCharIterator`)
or UTF-8 directly.

Theoretically, character iterators can iterate strings
in any encoding. ICU currently provides character iterator implementations for
UTF-8 and UTF-16BE (useful when processing data from a big endian platform on an
little endian machine). It should be noted, however, that using iterators for
collation APIs has a performance impact. It should be used in situations when it
is not desirable to convert whole strings before the operation - such as when
using a string compare function.

## Collation Elements

As discussed in the introduction, there are many possible orderings for sorted
text, depending on language and other factors. Ideally, there is a way to
describe each ordering as a set of rules for calculating numeric values for each
string of text. The collation process then becomes one of simply comparing these
numeric values.

This essentially describes the way the Collation Service works. To implement
a particular sort ordering, first the relationship between each character or
character sequence is derived. For example, a Spanish ordering defines the
letter sequence "CH" to be between the letters "C" and "D". As also discussed in
the introduction, to order strings properly requires that comparison of base
letters must be considered separately from comparison of accents. Letter case
must also be considered separately from either base letters or accents. Any
ordering specification language must provide a way to define the relationships
between characters or character sequences on multiple levels. ICU supports this
by using "<" to describe a relationship at the primary level, using "<<" to
describe a relationship at the secondary level, and using "<<<" to describe a
relationship at the tertiary level. Here are some example usages:

Symbol | Example  | Description
------ | -------- | -----------
`<`    | `c < ch` | Make a primary (base letter) difference between "c" and the character sequence "ch"
`<<`   | `a << ä` | Make a secondary (accent) difference between "a" and "ä"
`<<<`  | `a<<<A`  | Make a tertiary difference between "a" and "A"

A more complete description of the ordering specification symbols and their
meanings is provided in the section on Collation Tailoring.

Once a sort ordering is defined by specifying the desired relationships between
characters and character sequences, ICU can convert these relationships to a
series of numerical values (one for each level) that satisfy these same
relationships.

This series of numeric values, representing the relative weighting of a
character or character sequence, is called a Collation Element (CE).
One possible encoding of a Collation Element is a 32-bit value consisting of
a 16-bit primary weight, a 8-bit secondary weight,
2 case bits, and a 6-bit tertiary weight.

The sort weight of a string is represented by the collation elements of its
component characters and character sequences. For example, the sort weight of
the string "apple" would consist of its component Collation Elements, as shown
here:

"Apple" | "Apple" Collation Elements
------- | --------------------------
a       | `[1900.05.05]`
p       | `[3700.05.05]`
p       | `[3700.05.05]`
l       | `[2F00.05.05]`
e       | `[2100.05.05]`

In this example, the letter "a" has a 16-bit primary weight of 1900 (hex), an
8-bit secondary weight of 05 (hex), and a combined 8-bit case-tertiary weight of
05 (hex).

String comparison is performed by comparing the collation elements of each
string. Each of the primary weights are compared. If a difference is found, that
difference determines the relationship between the two strings. If no
differences are found, the secondary weights are compared and so forth.

With ICU it is possible to specify how many levels should be compared. For some
applications, it can be desirable to compare only primary levels or to compare
only primary and secondary levels.

## Sort Keys

If a string is to be compared thousands or millions of times,
it can be more efficient to use sort keys.
Sort keys are useful in situations where a large amount of data is indexed
and frequently searched. The sort key is generated once and used in subsequent
comparisons, rather than repeatedly generating the string's Collation Elements.
The comparison of sort keys is a very efficient and simple binary compare of strings of
unsigned bytes.

An important property of ICU sort keys is that you can obtain the same results
by comparing 2 strings as you do by comparing the sort keys of the 2 strings
(provided that the same ordering and related collation attributes are used).

An ICU sort key is a pre-processed sequence of bytes generated from a Unicode
string. The weights for each comparison level are concatenated, separated by a
"0x01" byte between levels.
The entire sequence is terminated with a 0x00 byte for convenience in C APIs.
(This 0x00 terminator is counted in the sort key length —
unlike regular strings where the NUL terminator is excluded from the string length.)

ICU actually compresses the sort keys so that they take the
minimum storage in memory and in databases.

<!-- TODO: (diagram was missing in Google Sites already)
    The diagram below represents an uncompressed sort key in ICU for ease of understanding.  -->

### Sort key size

One of the more important issues when considering using sort keys is the sort
key size. Unfortunately, it is very hard to give a fast exact answer to the
following question: "What is the maximum size for sort keys generated for
strings of size X". This problem is twofold:

1.  The maximum size of the sort key depends on the size of the collation
    elements that are used to build it. Size of collation elements vary greatly
    and depends both on the alphabet in question and on the locale used.

2.  Compression is used in building sort keys. Most 'regular' sequences of
    characters produce very compact sort keys.

If one is to assume the worst case and use too-big buffers, a lot of space will
be wasted. However, if you use too-small buffers, you will lose performance if
generated sort keys are longer than supplied buffers too often
(and you have to reallocate for each of those).
A good strategy
for this problem would be to manually manage a large buffer for storing sortkeys
and keep a list of indices to sort keys in this buffer (see the "large buffers"
[Collation Example](examples#using-large-buffers-to-manage-sort-keys)
for more details).

Here are some rules of a thumb, please do not rely on them. If you are looking
at the East Asian locales, you probably want to go with 5 bytes per code point.
For Thai, 3 bytes per code point should be sufficient. For all the other locales
(mostly Latin and Cyrillic), you should be fine with 2 bytes per code point.
These values are based on average lengths of sort keys generated with tertiary
strength. If you need quaternary and identical strength (you should not), add 3
bytes per code point to each of these.

### Partial sort keys

In some cases, most notably when implementing [radix
sorting](http://en.wikipedia.org/wiki/Radix_sort), it is useful to produce only
parts of sort keys at a time. ICU4C 2.6+ provides an API that allows producing
parts of sort keys (`ucol_nextSortKeyPart` API). These sort keys may or may not be
compressed; that is, they may or may not be compatible with regular sort keys.

### Merging sort keys

Sometimes, it is useful to be able to merge sort keys. One example is having
separate sort keys for first and last names. If you need to perform an operation
that requires a sort key generated on the whole name, instead of concatenating
strings and regenerating sort keys, you should merge the sort keys. The merging
is done by merging the corresponding levels while inserting a terminator between
merged parts. The reserved sort key byte value for the merge terminator is 0x02.
For more details see [UCA section 1.6, Merging Sort
Keys](http://www.unicode.org/reports/tr10/#Interleaved_Levels).

*   C API: unicode/ucol.h `ucol_mergeSortkeys()`
*   Java API: `com.ibm.icu.text.CollationKey merge(CollationKey source)`

CLDR 1.9/ICU 4.6 and later map U+FFFE to a special collation element that is
intended to allow concatenating strings like firstName+\\uFFFE+lastName to yield
the same results as merging their individual sort keys.
This has been fully implemented in ICU since version 53.

### Generating bounds for a sort key (prefix matching)

Having sort keys for strings allows for easy creation of bounds - sort keys that
are guaranteed to be smaller or larger than any sort key from a give range. For
example, if bounds are produced for a sortkey of string "smith", strings between
upper and lower bounds with one level would include "Smith", "SMITH", "sMiTh".
Two kinds of upper bounds can be generated - the first one will match only
strings of equal length, while the second one will match all the strings with
the same initial prefix.

CLDR 1.9/ICU 4.6 and later map U+FFFF to a collation element with the maximum
primary weight, so that for example the string "smith\\uFFFF" can be used as the
upper bound rather than modifying the sort key for "smith".

## Collation Element Iterator

The collation element iterator is used for traversing Unicode string collation
elements one at a time. It can be used to implement language-sensitive text
search algorithms like Boyer-Moore.

For most applications, the two API categories, compare and sort key, are
sufficient. Most people do not need to manipulate collation elements directly.

Example:

Consider iterating over "apple" and "äpple". Here are sequences of collation
elements:

String 1 | String 1 Collation Elements
-------- | ---------------------------
a        | `[1900.05.05]`
p        | `[3700.05.05]`
p        | `[3700.05.05]`
l        | `[2F00.05.05]`
e        | `[2100.05.05]`

String 2 | String 2 Collation Elements
-------- | ---------------------------
a        | `[1900.05.05]`
\\u0308  | `[0000.9D.05]`
p        | `[3700.05.05]`
p        | `[3700.05.05]`
l        | `[2F00.05.05]`
e        | `[2100.05.05]`

The resulting CEs are typically masked according to the desired strength, and
zero CEs are discarded. In the above example, masking with 0xFFFF0000 (for primary strength)
produces the results of NULL secondary and tertiary differences. The collator then
ignores the NULL differences and declares a match. For more details see the
paper "Efficient text searching in Java™: Finding the right string in any
language" by Laura Werner (
<http://icu-project.org/docs/papers/efficient_text_searching_in_java.html>).

## Collation Attributes

The Collation Service has a number of attributes whose values can be changed
during run time. These attributes affect both the functionality and the
performance of the Collation Service. This section describes these
attributes and, where possible, their performance impact. Performance
indications are only approximate and timings may vary significantly depending on
the CPU, compiler, etc.

Although string comparison by ICU and comparison of each string's sort key give
the same results, attribute settings can impact the execution time of each
method differently. To be precise in the discussion of performance, this section
refers to the API employed in the measurement. The `ucol_strcoll` function is the
API for string comparison. The `ucol_getSortKey` function is used to create sort
keys.

> :point_right: **Note** There is a special attribute value, `UCOL_DEFAULT`,
> that can be used to set any attribute to its default value
> (which is inherited from the UCA and the tailoring).

### Attribute Types

#### Strength level

Collation strength, or the maximum collation level used for comparison, is set
by using the `UCOL_STRENGTH` attribute. Valid values are:

1.  `UCOL_PRIMARY`

2.  `UCOL_SECONDARY`

3.  `UCOL_TERTIARY` (default)

4.  `UCOL_QUATERNARY`

5.  `UCOL_IDENTICAL`

#### French collation

The `UCOL_FRENCH_COLLATION` attribute determines whether to sort the secondary
differences in reverse order. Valid values are:

1.  `UCOL_OFF` (default): compares secondary differences in the order they appear
    in the string.

2.  `UCOL_ON`: causes secondary differences to be considered in reverse order, as
    it is done in the French language.

#### Normalization mode

The `UCOL_NORMALIZATION_MODE` attribute, or its alias `UCOL_DECOMPOSITION_MODE`,
controls whether text normalization is performed on the input strings. Valid
values are:

1.  `UCOL_OFF` (default): turns off normalization check

2.  `UCOL_ON` : normalization is checked and the collator performs normalization
    if it is needed.

X                     | FCD | NFC | NFD
--------------------- | --- | --- | ---
A-ring                | Y   | Y   |
Angstrom              | Y   |     |
A + ring              | Y   |     | Y
A + grave             | Y   | Y   |
A-ring + grave        | Y   |     |
A + cedilla + ring    | Y   |     | Y
A + ring + cedilla    |     |     |
A-ring + cedilla      |     | Y   |

With normalization mode turned on, the `ucol_strcoll` function slows down by 10%.
In addition, the time to generate a sort key also increases by about 25%.

#### Alternate handling

This attribute allows shifting of the variable characters (usually spaces and
punctuation, in the UCA also most symbols) from the primary to the quaternary
strength level. This is set by using the `UCOL_ALTERNATE_HANDLING` attribute. For
details see [UCA: Variable
Weighting](http://www.unicode.org/reports/tr10/#Variable_Weighting), [LDML:
Collation
Settings](http://www.unicode.org/reports/tr35/tr35-collation.html#Collation_Settings),
and [“Ignore Punctuation” Options](customization/ignorepunct.md).

1.  `UCOL_NON_IGNORABLE` (CLDR/ICU default): variable characters are treated as
    all the other characters

2.  `UCOL_SHIFTED` (UCA default): all the variable characters will be ignored at
    the primary, secondary and tertiary levels and their primary strengths will
    be shifted to the quaternary level.

#### Case Ordering

Some conventions require uppercase letters to sort before lowercase ones, while
others require the opposite. This attribute is controlled by the value of the
`UCOL_CASE_FIRST`. The case difference in the UCA is contained in the tertiary
weights along with other appearance characteristics (like circling of letters).
The case-first attribute allows for emphasizing of the case property of the
letters by reordering the tertiary weights with either upper-first, and/or
lowercase-first. This difference gets the most significant bit in the weight.
Valid values for this attribute are:

1.  `UCOL_OFF` (default): leave tertiary weights unaffected

2.  `UCOL_LOWER_FIRST`: causes lowercase letters and uncased characters to sort
    before uppercase

3.  `UCOL_UPPER_FIRST` : causes uppercase letters to sort first

The case-first attribute does not affect the performance substantially.

#### Case level

When this attribute is set, an additional level is formed between the secondary
and tertiary levels, known as the Case Level. The case level is used to
distinguish large and small Japanese Kana characters. Case level could also be
used in other situations. for example to distinguish certain Pinyin characters.
Case level is controlled by `UCOL_CASE_LEVEL` attribute. Valid values for this
attribute are

1.  `UCOL_OFF` (default): no additional case level

2.  `UCOL_ON` : adds a case level

#### Hiragana Quaternary

*This setting is deprecated and ignored in recent versions of ICU.*

Hiragana Quaternary can be set to `UCOL_ON`, in which case Hiragana code points
will sort before everything else on the quaternary level. If set to `UCOL_OFF`
Hiragana letters are treated the same as all the other code points. This setting
can be changed on run-time using the `UCOL_HIRAGANA_QUATERNARY_MODE` attribute.
You probably won't need to use it.

#### Variable Top

Variable Top is a boundary which decides whether the code points will be treated
as variable (shifted to quaternary level in the **shifted** mode) or
non-ignorable. Special APIs are used for setting of variable top. It can
basically be set either to a codepoint or a primary strength value.

## Performance

ICU collation is designed to be fast, small and customizable. Several techniques
are used to enhance the performance:

1.  Providing optimized processing for Latin characters.

2.  Comparing strings incrementally and stopping at the first significant
    difference.

3.  Tuning to eliminate unnecessary file access or memory allocation.

4.  Providing efficient preflight functions that allows fast sort key size
    generation.

5.  Using a single, shared copy of UCA in memory for the read-only default sort
    order. Only small tailoring tables are kept in memory for locale-specific
    customization.

6.  Compressing sort keys efficiently.

7.  Making the sort order be data-driven.

In general, the best performance from the Collation Service is expected by
doing the following:

1.  After opening a collator, keep and reuse it until done. Do not open new
    collators for the same sort order. (Note the restriction on
    multi-threading.)

2.  Use `ucol_strcoll` etc. when comparing strings. If it is necessary to
    compare strings thousands or millions of times,
    create the sort keys first and compare the sort keys instead.
    Generating the sort keys of two strings is about 5-10
    times slower than just comparing them directly.

3.  Follow the best practice guidelines for generating sort keys. Do not call
    `ucol_getSortKey` twice to first size the key and then allocate the sort key
    buffer and repeat the call to the function to fill in the buffer.

### Performance and Storage Implications of Attributes

Most people use the default attributes when comparing strings or when creating
sort keys. When they do want to customize the ordering, the most common options
are the following :

`UCOL_ALTERNATE_HANDLING == UCOL_SHIFTED`\
Used to ignore space and punctuation characters

`UCOL_ALTERNATE_HANDLING == UCOL_SHIFTED` **and** `UCOL_STRENGTH == UCOL_QUATERNARY`\
Used to ignore the space and punctuation characters except when there are no previous letter, accent, or case/variable differences.

`UCOL_CASE_FIRST == UCOL_LOWER_FIRST` **or** `UCOL_CASE_FIRST == UCOL_UPPER_FIRST`\
Used to change the ordering of upper vs. lower case letters (as
well as small vs. large kana)

`UCOL_CASE_LEVEL == UCOL_ON` **and** `UCOL_STRENGTH == UCOL_PRIMARY`\
Used to ignore only the accent differences.

`UCOL_NORMALIZATION_MODE == UCOL_ON`\
Force to always check for normalization. This
is used if the input text may not be in FCD form.

`UCOL_FRENCH_COLLATION == UCOL_OFF`\
This is only useful for languages like French and Catalan that may turn this attribute on.
(It is the default only for Canadian French ("fr-CA").)

In String Comparison, most of these options have little or no effect on
performance. The only noticeable one is normalization, which can cost 10%-40% in
performance.

For Sort Keys, most of these options either leave the storage alone or reduce
it. Shifting can reduce the storage by about 10%-20%; case level + primary-only
can decrease it about 20% to 40%. Using no French accents can reduce the storage
by about 38% , but only for languages like French and Catalan that turn it on by
default. On the other hand, using Shifted + Quaternary can increase the storage by
10%-15%. (The Identical Level also increases the length, but this option is not
recommended).

> :point_right: **Note** All of the above numbers are based on
> tests run on a particular machine, with a particular set of data.
> (The data for each language is a large number of names
> in that language in the format <first_name>, <last name>.)
> The performance and storage may vary, depending on the particular computer,
> operating system, and data.

## Versioning

Sort keys are often stored on disk for later reuse. A common example is the use
of keys to build indexes in databases. When comparing keys, it is important to
know that both keys were generated by the same algorithms and weightings.
Otherwise, identical strings with keys generated on two different dates, for
example, might compare as unequal. Sort keys can be affected by new versions of
ICU or its data tables, new sort key formats, or changes to the Collator.
Starting with release 1.8.1, ICU provides a versioning mechanism to identify the
version information of the following (but not limited to),

1.  The run-time executable

2.  The collation element content

3.  The Unicode/UCA database

4.  The tailoring table

The version information of Collator is a 32-bit integer. If a new version of ICU
has changes affecting the content of collation elements, the version information
will be changed. In that case, to use the new version of ICU collator will
require regenerating any saved or stored sort keys.

However, it is possible to modify ICU code or data without changing relevant version numbers,
so it is safer to regenerate sort keys any time after any part of ICU has been updated.

Since ICU4C 1.8.1.
it is possible to build your program so that it uses more than one version of
ICU (only in C/C++, not in Java). Therefore, you could use the current version
for the features you need and use the older version for collation.

## Programming Examples

See the [Collation Examples](examples.md) chapter for an example of how to
compare and create sort keys with the default locale in C, C++ and Java.
