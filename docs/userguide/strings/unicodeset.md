---
layout: default
title: UnicodeSet
nav_order: 50
parent: Chars and Strings
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# UnicodeSet

## Overview

A UnicodeSet is an object that represents a finite set of Unicode code point
sequences.  The contents of that object can be specified either by
patterns using the UnicodeSet syntax defined in 
[Draft Unicode Technical Standard #61, Unicode Set Notation](https://www.unicode.org/reports/tr61/),
or by building them programmatically.

Here are a few examples of sets:

| Pattern | Description |
|--------------|-------------------------------------------------------------|
| `[a-z]` | The lower case letters a through z |
| `[abc123]` | The six characters a,b,c,1,2 and 3 |
| `[\p{Letter}]` | All characters with the Unicode General Category of Letter. |

### String Values

In addition to being a set of characters (of Unicode code points),
a UnicodeSet may also contain string values. Conceptually, the UnicodeSet is
always a set of strings, not a set of characters, although in many common use
cases the strings are all of length one, which reduces to being a set of
characters.

This concept can be confusing when first encountered, probably because similar
set constructs from other environments
(e.g., character classes in most regular expression implementations)
can only contain characters.

Until ICU 68, it was not possible for a UnicodeSet to contain the empty string.
In Java, an exception was thrown. In C++, the empty string was silently ignored.

Starting with ICU 69 [ICU-13702](https://unicode-org.atlassian.net/browse/ICU-13702)
the empty string is supported as a set element;
however, it is ignored in matching functions such as `span(string)`.

## UnicodeSet Patterns

UnicodeSet objects can be constructed from patterns in the notation defined in
[Draft Unicode Technical Standard #61, Unicode Set Notation](https://www.unicode.org/reports/tr61/);
see the [#Conformance] section for specifics.

### General
At a high level, these are built up from lists of elements and Unicode property queries.

Element lists are sequences of characters,
character ranges indicated by a '-' between two characters, as in
`a-z`, and strings enclosed in curly brackets, as in `{abc}`.
For example, `[a c d-f m {cat}]` is equivalent to `[a c d e f m {cat}]`;
this set contains six letters, as well as the three-letter string "cat".
Whitespace can be freely used for clarity: `[a c d-f m]` means the same
as `[acd-fm]`.

Unicode property queries are specified
by a Unicode property, such as `[:Letter:]`.
The table below shows the two kinds of syntax: POSIX and Perl style, as well as the
equivalent API calls.
Also, the table shows the "Negative", which is a property that excludes all characters of
a given kind. For example, `[:^Letter:]` matches all characters that are not
`[:Letter:]`.

|  | POSIX-style Syntax | Perl-style Syntax | Corresponding method
|--------------------|------------------|-------------------|
| Positive | `[:type=value:]` | `\p{type=value}` | .applyPropertyAlias​(type, value)
| Negative  | `[:^type=value:]` or `[:type=≠value:]` | `\P{type=value}` or `\p{type=≠value}` | .applyPropertyAlias​(type, value).complement().removeAllStrings() |

These low-level lists or properties then can be freely combined with
the normal set operations (union, intersection, difference, and complement).


|  | Example | Corresponding Method | Meaning |
|-------|-------------------------|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A B | `[[:letter:] [:number:]]` | `A.addAll(B)` | To union two sets A and B, simply concatenate them |
| A & B | `[[:letter:] & [a-z]]` | `A.retainAll(B)` | To intersect two sets A and B, use the '&' operator. |
| A - B | `[[:letter:] - [a-z]]` | `A.removeAll(B)` | To take the set-difference of two sets  A and B, use the '-' operator. |
| [^A] | `[^a-z]` | `A.complement(B).removeAllStrings()` | To invert a set A, place a '^' immediately after the opening '['.  Note that this is a code point complement: `[^[𝐴]]` is equivalent to `[[\x{0000}-\x{10FFFF}]-[𝐴]]`, and contains no strings, regardless of whether 𝐴 contains strings. |

#### Precedence

The binary operators of union, intersection, and set-difference have equal
precedence and bind left-to-right. Thus the following are equivalent:

*   `[[:letter:] - [a-z] [:number:] & [\u0100-\u01FF]]`
*   `[[[[[:letter:] - [a-z]] [:number:]] & [\u0100-\u01FF]]`

Another example is that the set `[[ace][bdf] - [abc][def]]` is **not**
the empty set, but instead the set `[def]`. That is because the syntax
corresponds to the following UnicodeSet operations:

1.  start with `[ace]`
2.  addAll `[bdf]` *-- we now have `[abcdef]`*
3.  removeAll `[abc]` *-- we now have `[def]`*
4.  addAll `[def]` *-- no effect, we still have `[def]`*

This only really matters when the union and intersection operation are used together,
operation, or when the difference operation is used, as union and intersection operation are
associative. To make sure that the - is
the main operator, add brackets to group the operations as desired, such as
`[[ace][bdf] - [[abc][def]]]`.

Another caveat with the '&' and '-' operators is that they operate between
**sets**. That is, they must be immediately preceded and immediately followed by
a set. For example, the pattern `[[:Lu:]-A]` is illegal. To specify
the set of uppercase letters except for 'A', enclose the 'A' in a set:
`[[:Lu:]-[A]]`.


### Conformance

The ICU UnicodeSet class is a conformant and consistent implementation of the
UnicodeSet notation as defined in
[Draft Unicode Technical Standard #61, Unicode Set Notation](https://www.unicode.org/reports/tr61/).
It imposes some restrictions to the set of lexical elements defined in that standard, as described
below.

#### Restrictions

ICU supports only property queries that are recommended for general-purpose APIs:
the productions with a gray background in the [property-query](https://www.unicode.org/reports/tr61/#property-query)
grammar are not supported.


For a list of supported properties, see the [Properties](properties.md) chapter.
For details on the syntax, see the section of the UnicodeSet standard
on [property queries](https://www.unicode.org/reports/tr61/#Property-Queries).

#### Extensions

## Using a UnicodeSet

For best performance, once the set contents is complete, freeze() the set to
make it immutable and to speed up contains() and span() operations (for which it
builds a small additional data structure).

The most basic operation is contains(code point) or, if relevant,
contains(string).

For splitting and partitioning strings, it is simpler and faster to use span()
and spanBack() rather than iterate over code points and calling contains(). In
Java, there is also a class UnicodeSetSpanner for somewhat higher-level
operations. See also the “Lookup” section of the [Properties](properties.md)
chapter.

## Programmatically Building UnicodeSets

ICU users can programmatically build a UnicodeSet by adding or removing ranges
of characters or by using the retain (intersection), remove (difference), and
add (union) operations.

## Getting UnicodeSet from Script

ICU provides the functionality of getting UnicodeSet from the script. Here is an
example of generating a pattern from all the scripts that are associated to a
Locale and then getting the UnicodeSet based on the generated pattern.

**In C:**

    UErrorCode err = U_ZERO_ERROR;
    const int32_t capacity = 10;
    const char * shortname = NULL;
    int32_t num, j;
    int32_t strLength =4;
    UChar32 c = 0x00003096 ;
    UScriptCode script[10] = {USCRIPT_INVALID_CODE};
    UScriptCode scriptcode = USCRIPT_INVALID_CODE;
    num = uscript_getCode("ja",script,capacity, &err);
    printf("%s %d \n", "Number of script code associated are :", num);
    UnicodeString temp = UnicodeString("[", 1, US_INV);
    UnicodeString pattern;
    for(j=0;j<num;j++){
        shortname = uscript_getShortName(script[j]);
        UnicodeString str(shortname,strLength,US_INV);
        temp.append("[:");
        temp.append(str);
        temp.append(":]+");
    }
    pattern = temp.remove(temp.length()-1,1);
    pattern.append("]");
    UnicodeSet cnvSet(pattern, err);
    printf("%d\n", cnvSet.size());
    printf("%d\n", cnvSet.contains(c));

**In Java:**

    ULocale ul = new ULocale("ja");
    int script[] = UScript.getCode(ul);
    String str ="[";
    for(int i=0;i<script.length;i++){
        str = str + "[:"+UScript.getShortName(script[i])+":]+";
    }
    String pattern =str.substring(0, (str.length()-1));
    pattern = pattern + "]";
    System.out.println(pattern);
    UnicodeSet ucs = new UnicodeSet(pattern);
    System.out.println(ucs.size());
    System.out.println(ucs.contains(0x00003096));
