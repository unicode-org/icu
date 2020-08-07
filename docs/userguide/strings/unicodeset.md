---
layout: default
title: UnicodeSet
nav_order: 5
parent: Chars and Strings
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# UnicodeSet

## Overview

A UnicodeSet is an object that represents a set of Unicode characters or
character strings. The contents of that object can be specified either by
patterns or by building them programmatically.

Here are a few examples of sets:

| Pattern | Description |
|--------------|-------------------------------------------------------------|
| `[a-z]` | The lower case letters a through z |
| `[abc123]` | The six characters a,b,c,1,2 and 3 |
| `[\p{Letter}]` | All characters with the Unicode General Category of Letter. |

String Values In addition to being a set of characters (of Unicode code points),
a UnicodeSet may also contain string values. Conceptually, the UnicodeSet is
always a set of strings, not a set of characters, although in many common use
cases the strings are all of length one, which reduces to being a set of
characters.

This concept can be confusing when first encountered, probably because similar
set constructs from other environments (regular expressions) can only contain
characters.

## UnicodeSet Patterns

Patterns are a series of characters bounded by square brackets that contain
lists of characters and Unicode property sets. Lists are a sequence of
characters that may have ranges indicated by a '-' between two characters, as in
"a-z". The sequence specifies the range of all characters from the left to the
right, in Unicode order. For example, `[a c d-f m]` is equivalent to `[a c d e f m]`.
Whitespace can be freely used for clarity as `[a c d-f m]` means the same
as `[acd-fm]`.

Unicode property sets are specified by a Unicode property, such as `[:Letter:]`.
For a list of supported properties, see the [Properties](properties.md) chapter.
For details on the use of short vs. long property and property value names, see
the end of this section. The syntax for specifying the property names is an
extension of either POSIX or Perl syntax with the addition of "=value". For
example, you can match letters by using the POSIX syntax `[:Letter:]`, or by
using the Perl-style syntax \\p{Letter}. The type can be omitted for the
Category and Script properties, but is required for other properties.

The table below shows the two kinds of syntax: POSIX and Perl style. Also, the
table shows the "Negative", which is a property that excludes all characters of
a given kind. For example, `[:^Letter:]` matches all characters that are not
`[:Letter:]`.

|  | Positive | Negative |
|--------------------|------------------|-------------------|
| POSIX-style Syntax | `[:type=value:]` | `[:^type=value:]` |
| Perl-style Syntax  | `\p{type=value}` | `\P{type=value}`  |

These following low-level lists or properties then can be freely combined with
the normal set operations (union, inverse, difference, and intersection):

|  | Example | Corresponding Method | Meaning |
|-------|-------------------------|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A B | `[[:letter:] [:number:]]` | `A.addAll(B)` | To union two sets A and B, simply concatenate them |
| A & B | `[[:letter:] & [a-z]]` | `A.retainAll(B)` | To intersect two sets A and B, use the '&' operator. |
| A - B | `[[:letter:] - [a-z]]` | `A.removeAll(B)` | To take the set-difference of two sets  A and B, use the '-' operator. |
| [^A] | `[^a-z]` | `A.complement(B)` | To invert a set A, place a '^' immediately after the opening '['.  Note that the complement only affects code points, not string values. In any other location, the '^' does not have a special meaning. |

### Precedence

The binary operators of union, intersection, and set-difference have equal
precedence and bind left-to-right. Thus the following are equivalent:

*   `[[:letter:] - [a-z] [:number:] & [\u0100-\u01FF]]`
*   `[[[[[:letter:] - [a-z]] [:number:]] & [\u0100-\u01FF]]`

Another example is that the set `[[ace][bdf\] - [abc][def]]` is **not**
the empty set, but instead the set `[def]`. That is because the syntax
corresponds to the following UnicodeSet operations:

1.  start with `[ace]`
2.  addAll `[bdf]` *-- we now have `[abcdef]`*
3.  removeAll `[abc]` *-- we now have `[def]`*
4.  addAll `[def]` *-- no effect, we still have `[def]`*

This only really matters where there are the difference and intersection
operations, as the union operation is commutative. To make sure that the - is
the main operator, add brackets to group the operations as desired, such as
`[[ace][bdf] - [[abc][def]]]`.

Another caveat with the '&' and '-' operators is that they operate between
**sets**. That is, they must be immediately preceded and immediately followed by
a set. For example, the pattern `[[:Lu:]-A]` is illegal, since it is
interpreted as the set `[:Lu:]` followed by the incomplete range `-A`. To specify
the set of uppercase letters except for 'A', enclose the 'A' in a set:
`[[:Lu:]-[A]]`.

### Examples

| `[a]` | The set containing 'a' |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `[a-z]` | The set containing 'a' through 'z' and all letters in between, in Unicode order |
| `[^a-z]` | The set containing all characters but 'a' through 'z', that is, U+0000 through 'a'-1 and 'z'+1 through U+FFFF |
| `[[pat1][pat2]]` | The union of sets specified by pat1 and pat2 |
| `[[pat1]& [pat2]]` | The intersection of sets specified by pat1 and pat2 |
| `[[pat1]- [pat2]]` | The asymmetric difference of sets specified by pat1 and pat2 |
| `[:Lu:]` | The set of characters belonging to the given Unicode category, as defined by  `Character.getType()`; in this case, Unicode uppercase letters. The long form for this is  `[:UppercaseLetter:]`. |
| `[:L:]` | The set of characters belonging to all Unicode categories starting with 'L', that is,  `[[:Lu:][:Ll:][:Lt:][:Lm:][:Lo:]]`. The long form for this is  `[:Letter:]`. |

### String Values in Sets

String values are enclosed in {curly brackets}.

| Set expression | Description |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `[abc{def}]` | A set containing four members, the single characters a, b and c, and the string “def” |
| `[{abc}{def}]` | A set containing two members, the string “abc” and the string “def”. |
| `[{a}{b}{c}]` `[abc]` | These two sets are equivalent. Each contains three items, the three individual characters a, b and c. A {string} containing a single character is equivalent to that same character specified in any other way. |

### Character Quoting and Escaping in Unicode Set Patterns

#### Single Quote

Two single quotes represents a single quote, either inside or outside single
quotes.

Text within single quotes is not interpreted in any way (except for two adjacent
single quotes). It is taken as literal text (special characters become
non-special).

These quoting conventions for ICU UnicodeSets differ from those of regular
expression character set expressions. In regular expressions, single quotes have
no special meaning and are treated like any other literal character.

#### Backslash Escapes

Outside of single quotes, certain backslashed characters have special meaning:

| `\uhhhh` | Exactly 4 hex digits; h in [0-9A-Fa-f] |
|------------|----------------------------------------|
| `\Uhhhhhhhh` | Exactly 8 hex digits |
| `\xhh` | 1-2 hex digits |
| `\ooo` | 1-3 octal digits; o in [0-7] |
| `\a` | U+0007 (BELL) |
| `\b` | U+0008 (BACKSPACE) |
| `\t` | U+0009 (HORIZONTAL TAB) |
| `\n` | U+000A (LINE FEED) |
| `\v` | U+000B (VERTICAL TAB) |
| `\f` | U+000C (FORM FEED) |
| `\r` | U+000D (CARRIAGE RETURN) |
| `\\` | U+005C (BACKSLASH) |

Anything else following a backslash is mapped to itself, except in an
environment where it is defined to have some special meaning. For example,
`\\p{Lu}` is the set of uppercase letters in UnicodeSet.

Any character formed as the result of a backslash escape loses any special
meaning and is treated as a literal. In particular, note that \\u and \\U
escapes create literal characters. (In contrast, the Java compiler treats
Unicode escapes as just a way to represent arbitrary characters in an ASCII
source file, and any resulting characters are **not** tagged as literals.)

#### Whitespace

Whitespace (as defined by our API) is ignored unless it is quoted or
backslashed.

> :point_right: **Note**: *The rules for quoting and white space handling are common to most ICU APIs that
process rule or expression strings, including UnicodeSet, Transliteration and
Break Iterators.*

> :point_right: **Note**:*ICU Regular Expression set expressions have a different (but similar) syntax,
and a different set of recognized backslash escapes. \[Sets\] in ICU Regular
Expressions follow the conventions from Perl and Java regular expressions rather
than the pattern syntax from ICU UnicodeSet.*

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

## Property Values

The following property value variants are recognized:

| Format | Description | Example |
|--------|-----------------------------------------------------------------------------------------------------|-----------------------------------|
| short | omits the type (used to prevent ambiguity and only allowed with the Category and Script properties) | Lu |
| medium | uses an abbreviated type and value | gc=Lu |
| long | uses a full type and value | General_Category=Uppercase_Letter |

If the type or value is omitted, then the equals sign is also omitted. The short
style is only
used for Category and Script properties because these properties are very common
and their omission is unambiguous.

In actual practice, you can mix type names and values that are omitted,
abbreviated, or full. For example, if Category=Unassigned you could use what is
in the table explicitly, `\p{gc=Unassigned}`, `\p{Category=Cn}`, or
`\p{Unassigned}`.

When these are processed, case and whitespace are ignored so you may use them
for clarity, if desired. For example, `\p{Category = Uppercase Letter}` or
`\p{Category = uppercase letter}`.

For a list of supported properties, see the [Properties](properties.md) chapter.

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
    printf("%s %d \n" ,"Number of script code associated are :", num);
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
