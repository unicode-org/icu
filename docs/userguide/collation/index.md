---
layout: default
title: Collation
nav_order: 9
has_children: true
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Collation

## Overview

Information is displayed in sorted order to enable users to easily find the
items they are looking for. However, users of different languages might have
very different expectations of what a "sorted" list should look like. Not only
does the alphabetical order vary from one language to another, but it also can
vary from document to document within the same language. For example, phonebook
ordering might be different than dictionary ordering. String comparison is one
of the basic functions most applications require, and yet implementations often
do not match local conventions. The ICU Collation Service provides string
comparison capability with support for appropriate sort orderings for each of
the locales you need. In the event that you have a very unusual requirement, you
are also provided the facilities to customize orderings.

Starting in release 1.8, the ICU Collation Service is compliant to the Unicode
Collation Algorithm (UCA) ([Unicode Technical Standard
#10](http://www.unicode.org/reports/tr10/)) and based on the Default
Unicode Collation Element Table (DUCET) which defines the same sort order as ISO
14651.

The ICU Collation Service also contains several enhancements that are not
available in UCA. These have been adopted into the [CLDR Collation
Algorithm](http://www.unicode.org/reports/tr35/tr35-collation.html#CLDR_Collation_Algorithm).
For example:

*   Additional case handling (as specified by CLDR): ICU allows case differences
    to be ignored or flipped. Uppercase letters can be sorted before lowercase
    letters, or vice-versa.
*   Easy customization (as specified by CLDR): Services can be easily tailored
    to address a wide range of collation requirements.
*   The [default (root) sort
    order](http://www.unicode.org/reports/tr35/tr35-collation.html#Root_Collation)
    has been tailored slightly for improved functionality and performance.

In other words, ICU implements the CLDR Collation Algorithm which is an
extension of the Unicode Collation Algorithm (UCA) which is an extension of ISO
14651.

There are several benefits to using the collation algorithms defined in these
standards, including:

*   The algorithms have been designed and reviewed by experts in multilingual
    collation, and therefore are robust and comprehensive.

*   Applications that share sorted data but do not agree on how the data should
    be ordered fail to perform correctly. By conforming to the CLDR/UCA/14651
    standards for collation and using CLDR language-specific collation data,
    independently developed applications sort data identically and perform
    properly.

In addition, Unicode contains a large set of characters. This can make it
difficult for collation to be a fast operation or require collation to use
significant memory or disk resources. The ICU collation implementation is
designed to be fast, have a small memory footprint and be highly customizable.

There are many challenges when accommodating the world's languages and writing
systems and the different orderings that are used. However, the ICU Collation
Service provides an excellent means for comparing strings in a locale-sensitive
fashion.

For example, here are some of the ways languages vary in ordering strings:

*   The letters A-Z can be sorted in a different order than in English. For
    example, in Lithuanian, "y" is sorted between "i" and "k".

*   Combinations of letters can be treated as if they were one letter. For
    example, in traditional Spanish "ch" is treated as a single letter, and
    sorted between "c" and "d".

*   Accented letters can be treated as minor variants of the unaccented letter.
    For example, "é" can be treated equivalent to "e".

*   Accented letters can be treated as distinct letters. For example, "Å" in
    Danish is treated as a separate letter that sorts just after "Z".

*   Unaccented letters that are considered distinct in one language can be
    indistinct in another. For example, the letters "v" and "w" are two
    different letters according to English. However, "v" and "w" are
    traditionally considered variant forms of the same letter in Swedish.

*   A letter can be treated as if it were two letters. For example, in German
    phonebook (or "lists of names") order "ä" is compared as if it were "ae".

*   Thai requires that the order of certain letters be reversed.

*   Some French dictionary ordering traditions sort accents in backwards order,
    from the end of the string. For example, the word "côte" sorts before "coté"
    because the acute accent on the final "e" is more significant than the
    circumflex on the "o".

*   Sometimes lowercase letters sort before uppercase letters. The reverse is
    required in other situations. For example, lowercase letters are usually
    sorted before uppercase letters in English. Danish letters are the exact
    opposite.

*   Even in the same language, different applications might require different
    sorting orders. For example, in German dictionaries, "öf" would come before
    "of". In phone books the situation is the exact opposite.

*   Sorting orders can change over time due to government regulations or new
    characters/scripts in Unicode.

To accommodate the many languages and differing requirements, ICU collation
supports customizing sort orderings - also known as **tailoring**. More details
regarding tailoring are discussed in the [Customization
chapter.](customization/index.md)

The basic ICU Collation Service is provided by two main categories of APIs:

*   String comparison - most commonly used: APIs return result of comparing two
    strings (greater than, equal or less than). This is used as a comparator
    when sorting lists, building tree maps, etc.

*   Sort key generation - used when a very large set of strings are
    compared/sorted repeatedly: APIs return a zero-terminated array of bytes per
    string known as a sort key. The keys can be compared directly using strcmp
    or memcmp standard library functions, saving repeated lookup and computation
    of each string's collation properties. For example, database applications
    use index tables of sort keys to index strings quickly. Note, however, that
    this only improves performance for large numbers of strings because sorting
    via the comparison functions is very fast. For more information, see
    [Sortkeys vs Comparison](concepts#sortkeys-vs-comparison).

ICU provides an AlphabeticIndex API for generating language-appropriate
sorted-section labels like in dictionaries and phone books.

ICU also provides a higher-level [string search](string-search)
API which can be used, for example, for case-insensitive or accent-insensitive
search in an editor or in a web page. ICU string search is based on the
low-level [collation element iteration](architecture).

## Programming Examples

Here are some [API usage conventions](api.md) for the ICU Collation Service
APIs.
