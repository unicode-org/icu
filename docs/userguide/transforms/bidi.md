---
layout: default
title: BiDi Algorithm
nav_order: 2
parent: Transforms
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# BiDi Algorithm
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

Bidirectional text consists of mainly right-to-left text with some left-to-right
nested segments (such as an Arabic text with some information in English), or
vice versa (such as an English letter with a Hebrew address nested within it.)
The predominant direction is called the global orientation.

Languages involving bidirectional text are used mainly in the Middle East. They
include Arabic, Urdu, Persian, Hebrew, and Yiddish.

In such a language, the general flow of text proceeds horizontally from right to
left, but numbers are written from left to right, the same way as they are
written in English. In addition, if some text (addresses, acronyms, or
quotations) in English or another left-to-right language is embedded, it is also
written from left to right.

* Libraries that perform a bidirectional algorithm and reorder strings
accordingly are sometimes called "Storage Layout Engines". ICU's BiDi (ubidi.h)
and shaping (ushape.h) APIs can be used at the core of such "Storage Layout
Engines". *

## Countries with Languages that Require Bidirectional Scripting

There are over 600 million people whose languages are written right-to-left, including
Persian and Urdu which use the Arabic script with additional characters.

| Language | Countries (examples) |
|----------|------------------------------------------------------|
| Arabic   | Egypt, Jordan, Morocco, Saudi Arabia, ... Middle East & North Africa |
| Persian  | Iran, Afghanistan |
| Urdu     | India, Pakistan |
| Hebrew   | Israel |
| Yiddish  | Israel, North America, South America, Russia, Europe |

This list of languages is far from complete. Other languages with RTL scripts include
Divehi (Maldives), Kurdish (Iraq), Kashmiri (India), Sindhi (Pakistan and India), Uighur (China), and Pashto (Afghanistan), etc.

## Logical Order versus Visual Order

When reading bidirectional text, whenever the eye of the experienced reader
encounters an embedded segment, it "automatically" jumps to the other end of the
segment and reads it in the opposite direction. The sequence in which the
characters are pronounced is thus a logical sequence which differs from the
visual sequence in which they are presented on the screen or page.

The logical order of bidirectional text is also the order in which it is usually
keyed, and in which it is stored in memory.

Consider the following example, where Arabic or Hebrew letters are represented
by uppercase English letters and English text is represented by lowercase
letters:

    english CIBARA text

The English letter h is visually followed by the Arabic letter C, but logically
h is followed by the rightmost letter A. The next letter, in logical order, will
be R. In other words, the logical and storage order of the same text would be:

    english ARABIC text

Text is stored and processed in logical order to make processing feasible: A
contiguous substring of logical-order text (e.g., from a copy&paste operation)
contains a logically contiguous piece of the text. For example, "ish ARA" is a
logically contiguous piece of the sample text above. By contrast, a contiguous
substring of visual-order text may contain pieces of the text from distant parts
of a paragraph. ("ish" and "CIB" from the sample text above are not logically
adjacent.) Sorting and searching in text (establishing lexical order among
strings) as well as any other kind of context-sensitive text analysis also rely
on the storage of text in logical order because such processing must match user
expectations.

When text is displayed or printed, it must be "reordered" into visual order with
some parts of the text laid out left-to-right, and other parts laid out
right-to-left. The Unicode standard specifies an algorithm for this
logical-to-visual reordering. It always works on a paragraph as a whole; the
actual positioning of the text on the screen or paper must then take line breaks
into account, based on the output of the bidirectional algorithm. The reordering
output is also used for cursor movement and selection.

Legacy systems frequently stored text in visual order to avoid reordering for
display. When exchanging data with such systems for processing in Unicode it is
necessary to reorder the data from visual order to logical order and back. Such
not-for-display transformations are sometimes referred to as "storage layout"
transformations.

The are two problems with an "inverse reordering" from visual to logical order:
There may be more than one logical order of text that results in the same
display (logical-to-visual reordering is a many-to-one function), and there is
no standard algorithm for it. ICU's BiDi API provides a setting for "inverse"
operation that modifies the standard Unicode Bidi algorithm. However, it may not
always produce the expected results. Bidirectional data should be converted to
Unicode and reordered to logical order only once to avoid roundtrip losses. Just
as it is best to never convert to non-Unicode charsets, data should not be
reordered from logical to visual order except for display and printing.

## References

ICU provides an implementation of the Unicode BiDi algorithm, as well as simple
functions to write a reordered version of the string using the generated
meta-data. An "inverse" flag can be set to **approximate** visual-to-logical
reordering. See the ubidi.h header file and the [BiDi API
References](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/ubidi_8h.html) .

See [Unicode Standard Annex #9: The Bidirectional
Algorithm](http://www.unicode.org/reports/tr9/) .

## Programming Examples in C and C++

See the [BiDi API reference](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/ubidi_8h.html)
for more information.
