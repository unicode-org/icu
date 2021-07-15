---
layout: default
title: Case Mappings
nav_order: 1
parent: Transforms
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Case Mappings
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

Case mapping is used to handle the mapping of upper-case, lower-case, and title
case characters for a given language. Case is a normative property of characters
in specific alphabets (e.g. Latin, Greek, Cyrillic, Armenian, and Georgian)
whereby characters are considered to be variants of a single letter. ICU refers
to these variants, which may differ markedly in shape and size, as uppercase
letters (also known as capital or majuscule) and lower-case letters (also known
as small or minuscule). Alphabets with case differences are called bicameral and
alphabets without case differences are called unicameral.

Due to the inclusion of certain composite characters for compatibility, such as
the Latin capital letter 'DZ' (\\u01F1 'DZ'), there is a third case called title
case. Title case is used to capitalize the first character of a word such as the
Latin capital letter 'D' with small letter 'z' ( \\u01F2 'Dz'). The term "title
case" can also be used to refer to words whose first letter is an uppercase or
title case letter and the rest are lowercase letters. However, not all words in
the title of a document or first words in a sentence will be title case. The use
of title case words is language dependent. For example, in English, "Taming of
the Shrew" would be the appropriate capitalization and not "Taming Of The
Shrew".

> :point_right: **Note**: *As of Unicode 11, Georgian now has Mkhedruli (lowercase) and Mtavruli
(uppercase) which form case pairs, but are not used in title case.*

Sample code is available in the ICU source code library at
[icu/source/samples/ustring/ustring.cpp](https://github.com/unicode-org/icu/blob/master/icu4c/source/samples/ustring/ustring.cpp)
.

Please refer to the following sections in the [The Unicode Standard](http://www.unicode.org/versions/latest/)
for more information about case mapping:

*   3.13 Default Case Algorithms
*   4.2 Case
*   5.18 Case Mappings

## Simple (Single-Character) Case Mapping

The general case mapping in ICU is non-language based and a 1 to 1 generic
character map.

A character is considered to have a lowercase, uppercase, or title case
equivalent if there is a respective "simple" case mapping specified for the
character in the [Unicode Character Database](http://www.unicode.org/ucd/) (UnicodeData.txt).
If a character has no mapping equivalent, the result is the character itself.

The APIs provided for the general case mapping, located in `uchar.h` file, handles
only single characters of type `UChar32` and returns only single characters. To
convert a string to a non-language based specific case, use the APIs in either
the `unistr.h` or `ustring.h` files with a `NULL` argument locale.

## Full (Language-Specific) Case Mapping

There are different case mappings for different locales. For instance, unlike
English, the character Latin small letter 'i' in Turkish has an equivalent Latin
capital letter 'I' with dot above ( \\u0130 'İ').

Similar to the simple case mapping API, a character is considered to have a
lowercase, uppercase or title case equivalent if there is a respective mapping
specified for the character in the Unicode Character database (UnicodeData.txt).
In the case where a character has no mapping equivalent, the result is the
character itself.

To convert a string to a language based specific case, use the APIs in `ustring.h`
and `unistr.h` with an intended argument locale.

ICU implements full Unicode string case mappings.

**In general:**

*   **case mapping can change the number of code points and/or code units of a
    string,**
*   **is language-sensitive (results may differ depending on language), and**
*   **is context-sensitive (a character in the input string may map differently
    depending on surrounding characters).**

## Case Folding

Case folding maps strings to a canonical form where case differences are erased.
Using the case folding API, ICU supports fast matches without regard to case in
lookups, since only binary comparison is required.

The CaseFolding.txt file in the Unicode Character Database is used for
performing locale-independent case folding. This text file is generated from the
case mappings in the Unicode Character Database, using both the single-character
and the multi-character mappings. The CaseFolding.txt file transforms all
characters having different case forms into a common form. To compare two
strings for non-case-sensitive matching, you can transform each string and then
use a binary comparison. There are also functions to compare two strings
case-insensitively using the same case folding data.

Unicode case folding is not context-sensitive. It is also not
language-sensitive, although there is a flag for whether to apply special
mappings for use with Turkic (Turkish/Azerbaijani) text data.

Character case folding APIs implementations are located in:

1.  `uchar.h` for single character folding

2.  `ustring.h` and `unistr.h` for character string folding.
