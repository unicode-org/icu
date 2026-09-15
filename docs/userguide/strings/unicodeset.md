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
sequences, optimized for single code points.  The contents of that object can be specified either by
pattern strings using the UnicodeSet syntax defined in 
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

UnicodeSet objects can be constructed from pattern strings using the notation defined in
[Draft Unicode Technical Standard #61, Unicode Set Notation](https://www.unicode.org/reports/tr61/);
see the [Conformance](#Conformance) section for specifics.

### General
At a high level, these are built up from lists of elements and Unicode property queries.

Element lists are sequences of characters,
character ranges indicated by a '-' between two characters, as in
`a-z`, and strings enclosed in curly brackets, as in `{abc}`.
For example, `[a c d-f m {cat}]` is equivalent to `[a c d e f m {cat}]`;
this set contains six letters, as well as the three-letter string "cat".
By default, whitespace can be freely used for clarity: `[a c d-f m]` means the same
as `[acd-fm]`.

> Note: When explicit options are passed to UnicodeSet,
> if IGNORE_SPACE is not set, whitespace is not ignored,
> but instead is interpreted literally.
> See the [Space-sensitive parsing](#space-sensitive) section
> for specifics.

Unicode [property queries](https://www.unicode.org/reports/tr61/#Property-Queries)
refer to the set of characters that have a Unicode property value, such as `[:Letter:]`.
The table below shows the two kinds of syntax: POSIX and Perl style, as well as the
equivalent API calls.
Also, the table shows the "Negative", which is a property that excludes all characters of
a given kind. For example, `[:^Letter:]` matches all characters that are not
`[:Letter:]`.  The property name can be omitted when it is General_Category (as
for Letter) or Script; the property value Yes can be omitted for a binary property,
as in `[:White_Space:]`.

|          | POSIX-style Syntax       | Perl-style Syntax        | Corresponding method                     |
|----------|--------------------------|--------------------------|------------------------------------------|
| Positive | `[:propertyName=value:]` | `\p{propertyName=value}` | .applyPropertyAlias​(propertyName, value) |
| Negative | `[:^propertyName=value:]` or `[:propertyName≠value:]` | `\P{propertyName=value}` or `\p{propertyName≠value}` | .applyPropertyAlias(propertyName, value).complement().removeAllStrings() |

These low-level lists or properties then can be freely combined with
the normal set operations (union, intersection, difference, and complement).

|  | Example | Corresponding Method | Meaning |
|-------|-------------------------|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A B | `[[:letter:] [:number:]]` | `A.addAll(B)` | To union two sets A and B, simply concatenate them |
| A & B | `[[:letter:] & [a-z]]` | `A.retainAll(B)` | To intersect two sets A and B, use the '&' operator. |
| A - B | `[[:letter:] - [a-z]]` | `A.removeAll(B)` | To take the set-difference of two sets  A and B, use the '-' operator. |
| [^A] | `[^a-z]` | `A.complement(B).removeAllStrings()` | To invert a set A, place a '^' immediately after the opening '['.  Note that this is a code point complement: `[^[𝐴]]` is equivalent to `[[\x{0000}-\x{10FFFF}]-[𝐴]]`, and contains no strings, regardless of whether 𝐴 contains strings. |

> :point_right: **Note**: *ICU Regular Expression set expressions have a different (but similar) syntax,
and a different set of recognized backslash escapes. \[Sets\] in ICU Regular
Expressions follow the conventions from Perl and Java regular expressions rather
than the pattern syntax from ICU UnicodeSet.*

#### Precedence of set operations

As described [in the UnicodeSet grammar](https://www.unicode.org/reports/tr61/#SetOperation),
the binary operators of union, intersection, and set-difference have equal
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

This only really matters when the union and intersection operations are used together,
or when the difference operation is used, as union and intersection are
each associative. To make sure that the - is
the main operator, add brackets to group the operations as desired, such as
`[[ace][bdf] - [[abc][def]]]`.

Another caveat with the '&' and '-' operators is that they operate between
**sets**. That is, they must be immediately preceded and immediately followed by
a set. For example, the pattern `[[:Lu:]-A]` is illegal. To specify
the set of uppercase letters except for 'A', enclose the 'A' in a set:
`[[:Lu:]-[A]]`.

### Recent changes

In ICU 79, as part of the work on standardizing UnicodeSet notation in
[Draft Unicode Technical Standard #61, Unicode Set Notation](https://www.unicode.org/reports/tr61/),
a number of changes were made to UnicodeSet parsing in ICU.

These changes add some new features, such as `\N{hex:name}` escapes.

In some corner cases, they change the behaviour of the UnicodeSet class on
pattern strings that were previously accepted:
1. Spaces are no longer ignored in string literals.
   This change comes with a migration period:
   * In ICU 78, the sets `[{a b}]` and `[{ab}]` were equal, both contaning the
     two-character string `ab`.
   * In ICU 79 the set `[{a b}]` is ill-formed.
   * In ICU 81, the pattern string `[{a b}]` will be accepted, representing a
     set that contains the three-character string `a b`.
2. `\N` escapes now represent characters, rather than sets containing a single
   character:
   * In ICU 78, `\N{LATIN SMALL LETTER A}` was a well-formed pattern string
     representing the one-element set containing the character `a`.  
     In ICU 79, it is ill-formed; `[\N{LATIN SMALL LETTER A}]` should be used.
   * In ICU 78, `[[a-z]-\N{LATIN SMALL LETTER A}]` was a well-formed pattern
     string representing the twenty-five element set `[b-z]`.  
     In ICU 79, it is ill-formed; `[[a-z]-[\N{LATIN SMALL LETTER A}]]` should be used.
   * In ICU 78, `[\N{LATIN SMALL LETTER A}-\N{LATIN SMALL LETTER Z}]` was a
     well-formed pattern string representing the empty set
     (equivalent to `[[a]-[z]]`).  
     In ICU 79, it represents the twenty-six element set `[a-z]`.
3. Variables are now grammatical.
   * In ICU 78, the following transform rules were valid, equivalent to `[a-z] > A;`:
     ```
     $a = a;
     $z = z;
     $hyphen = '-';
     [$a$hyphen$z] > A;
     ```
   * In ICU 79, this is ill-formed: variables must represent elements or sets;
     they cannot expand to operators nor any other substring.
     See the formal syntax of [variable](#variable) in the
     [Extensions](#Extensions) section below.
4. String ranges are disallowed.
   * In ICU4J 78 (but not ICU4C), `[{aa}-{zz}]` was a well-formed pattern string
     containing 26×26=676 two-character strings (`aa`, `ab`, `ac`, …, `az`, `ba`, `bb`, …, `zy`, `zz`).
   * In ICU 79 (both C and J), it is disallowed.
5. `\p` and `\P` are disallowed in string literals.
   * In ICU 78, `[\p]` was ill-formed, but `[{\p}]` was well-formed, equal to `[p]`.
   * In ICU 79, `[{\p}]` is ill-formed.
6. `\N` in string literal now starts a named-element.
   * In ICU 78, `[\N]` was ill-formed, but `[{\N}]` was well-formed, equal to `[N]`.  
     In ICU 79, `[{\N}]` is ill-formed.
   * In ICU 78, `[{\N{LATIN SMALL LETTER A}\N{LATIN SMALL LETTER B}}]` was well-formed containing three elements: U+007D RIGHT CURLY BRACKET, U+0062 LATIN SMALL LETTER B, and the 19-character string `N{LATINSMALLLETTERA`.  
     In ICU 79, `[{\N{LATIN SMALL LETTER A}\N{LATIN SMALL LETTER B}}]` contains a single element, the two-character string `ab`.
7. Ranges cannot contain an unescaped HYPHEN-MINUS.
   * In ICU 78, `[--a]` was a well-formed pattern string equal to `[\--a]`,
     but `[\0--]` and `[b--a]` were ill-formed.
   * In ICU 79, `[--a]` becomes ill-formed.
8. Spaces are disallowed between `[:` and `^`, line breaks and tabs are disallowed inside `[::]`.
   * In ICU 78, `[: ^XID_Continue:]` was well-formed, equivalent to `[:^XID_Continue:]`.  
     In ICU 79, it is ill-formed. Use `[:^XID_Continue]`.
   * In ICU 78,
     ```
     [:
     XID continue:]
     ```
     was well-formed, equivalent to `[:XID_Continue:]`.  
     In ICU 79, it is ill-formed. Use `[:XID continue:]`.
9. A trailing equals sign does not mean =Yes, nor does it mean =gc nor =sc.
   * In ICU 78, `\p{XID_Continue=}` is well-formed, equivalent to `\p{XID_Continue=Yes}` or `\p{XID_Continue}`.  
     In ICU 79, it is ill-formed; use `\p{XID_Continue=Yes}` or `\p{XID_Continue}`.
   * In ICU 78, `\p{Uppercase_Letter=}` is well-formed, equivalent to `\p{General_Category=Uppercase_Letter}` or `\p{Uppercase_Letter}`.  
     In ICU 79, it is ill-formed; use `\p{General_Category=Uppercase_Letter}` or `\p{Uppercase_Letter}`.
10. Escapes for surrogate pairs in formats other than `\u` are deprecated.
    * In ICU4C 78, `[\x{DBFF}\x{DFFF}]` was a two-element set containing the surrogate
      code points U+DBFF and U+DFFF.  
      In ICU4C 79, it is ill-formed; use `[\x{DBFF} \x{DFFF}]`.
    * In ICU4J 78 and 79, `[\x{DBFF}\x{DFFF}]` is the one-element set containing the
      supplementary code point U+10FFFF.
      In a future version of ICU, this may be made ill-formed in Java as well.
    * In both ICU4C and ICU4J, `[\uDBFF\uDFFF]` has long been equivalent to
      `[\x{10FFFF}]`. This remains the case.
11. Implicit Directional Marks can no longer separate lexical elements.
    * In ICU 78, `[\xD‎F]` (that’s `[\xD`&lt;U+200E&gt;`F]`,
      with a LEFT-TO-RIGHT MARK between the D and the F), was the two-element set
      containing U+000D (CARRIAGE RETURN) and U+0046 F LATIN CAPITAL LETTER F.
      In ICU 79, it is ill-formed. Use `[\xD F]`.  
    * In ICU 78, `[\00‎7]` (that’s `[\00`&lt;U+200E&gt;`7]`,
      with a LEFT-TO-RIGHT MARK between the 0 and the 7), was the two-element set
      containing U+0000 (NULL) and U+0037 7 DIGIT SEVEN.  
      In ICU 79, it is ill-formed. Use `[\x00 7]`.

### Conformance

The ICU UnicodeSet class is a conformant and consistent implementation of the
UnicodeSet notation as defined in
[Draft Unicode Technical Standard #61, Unicode Set Notation](https://www.unicode.org/reports/tr61/).
It imposes some restrictions to the set of lexical elements defined in that standard, as described
below.
It also implements some pure extensions: some expressions that are
ill-formed according the UnicodeSet standard are defined by ICU. 

#### Restrictions

ICU supports only property queries that are recommended for general-purpose APIs:
the productions with a gray background in the [property-query](https://www.unicode.org/reports/tr61/#property-query)
grammar are not supported.

The list of supported properties is given in the [Properties](properties.md) chapter.

Doubly-negated property queries, as defined in the section on
[Negations](https://www.unicode.org/reports/tr61/#Negations),
are disallowed.

When matching property names and property values in property queries,
ICU uses an older version of rule UAX44-LM3 which does not ignore the prefix `is`:
thus `\p{isSpaceSeparator}` is ill-formed.
The remainder of rule UAX44-LM3 is supported:
`[:general-category = SPACE SEPARATOR:]` is accepted, and equivalent to
`[:General_Category=Space_Separator:]`.

When querying the Age property, only values matching `[0-9]+(\.[0-9]+(\.[0-9]+)?)?`
are supported: other values matching aliases for the Age property under UAX44-LM3,
such as `V17_0`, `v170`, or `1 7.0`, are not supported.

When querying numeric properties, rational values are not supported;
see section [Valid Values and Resolved Sets](https://www.unicode.org/reports/tr61#Valid-Values-and-Resolved-Sets) of DUTS #61.
Only 64-bit floating-point values are supported (IEEE 754 binary64).

When matching character names in property queries for the `Name` property
and in [named-element](https://www.unicode.org/reports/tr61/#named-element)s,
formal aliases of type other than `correction` are ignored.
For instance, `\N{BEL}` is not supported. This is a known defect tracked by ticket
[ICU-8963](https://unicode-org.atlassian.net/browse/ICU-8963).

#### Extensions

ICU interprets some expressions that are ill-formed according to the UnicodeSet standard:

* Some non-UCD properties, such as RGI_Emoji, are supported in
  [property-query](https://www.unicode.org/reports/tr61/#property-query).
  See the list in the [Properties](properties.md) chapter.
* `\u` [four-hexadecimal-digits](https://www.unicode.org/reports/tr61/#four-hexadecimal-digits)
  `\u` [four-hexadecimal-digits](https://www.unicode.org/reports/tr61/#four-hexadecimal-digits)
  where the first constituent  [four-hexadecimal-digits](https://www.unicode.org/reports/tr61/#four-hexadecimal-digits)
  represent a high surrogate and the second constituent
  [four-hexadecimal-digits](https://www.unicode.org/reports/tr61/#four-hexadecimal-digits)
  represent a low surrogate is an [escaped-element](https://www.unicode.org/reports/tr61/#escaped-element)
  representing the supplementary code point whose UTF-16 encoding is that sequence of
  surrogates.
* A [string-literal](https://www.unicode.org/reports/tr61/#string-literal) is
  allowed to contain [escaped-element](https://www.unicode.org/reports/tr61/#escaped-element)s
  representing surrogate code points.
* U+007D } RIGHT CURLY BRACKET is allowed as a [literal-element](https://www.unicode.org/reports/tr61/#literal-element), thus `[}]` is equal to `[\}]`.
* A `$` at the end of [Content](https://www.unicode.org/reports/tr61/#Content) (that is, preceding a
  closing bracket `]`) represents the noncharacter code point U+FFFF.
  This is used to represent the start or end of text in transform rules, see Section
  [Æther](../transforms/general/rules.md#%C3%A6ther) of
  the transform rule tutorial.

  Anywhere else, a `$` is allowed as if it were a
  [literal-element](https://www.unicode.org/reports/tr61/#literal-element),
  representing the character U+0024 `$` DOLLAR SIGN either alone in element lists or in a range.

  Formally, the situation is a little more complicated: adding `$` representing itself to
  [literal-element](https://www.unicode.org/reports/tr61/#literal-element) would allow it at the end
  of [Content](https://www.unicode.org/reports/tr61/#Content), and adding alternatives to the
  [Content](https://www.unicode.org/reports/tr61/#Content) production with a final `$` representing
  U+FFFF would make the grammar ambiguous.

  Instead, `$` is added as a [set-operator](https://www.unicode.org/reports/tr61/#set-operator),
  and the grammar is modified as follows.

  The following syntactic categories are introduced:
  > <a id="Æther"></a>[Æther](#Æther) ⩴ `$`  
  > <a id="DollarElements"></a>[DollarElements](#DollarElements) ⩴ `$` | [RangeElement](https://www.unicode.org/reports/tr61/#RangeElement) `-` `$`

  The following alternatives are added to the
  [Content](https://www.unicode.org/reports/tr61/#Content) production:
  > | [Æther](#Æther)  
  > | [ElementList](https://www.unicode.org/reports/tr61/#ElementList) [Æther](#Æther)  
  > | [DollarElements](https://www.unicode.org/reports/tr61/#DollarElements) [UnescapedHyphenMinus](https://www.unicode.org/reports/tr61/#UnescapedHyphenMinus)  
  > | [DollarElements](https://www.unicode.org/reports/tr61/#DollarElements) [Æther](https://www.unicode.org/reports/tr61/#Æther)  
  > | [ElementList](https://www.unicode.org/reports/tr61/#ElementList) [DollarElements](https://www.unicode.org/reports/tr61/#DollarElements) [UnescapedHyphenMinus](https://www.unicode.org/reports/tr61/#UnescapedHyphenMinus)  
  > |  [ElementList](https://www.unicode.org/reports/tr61/#ElementList) [DollarElements](https://www.unicode.org/reports/tr61/#DollarElements) [Æther](https://www.unicode.org/reports/tr61/#Æther)  

  The following alternative is added to the [ElementList](https://www.unicode.org/reports/tr61/#ElementList) production:
  > | [ElementList](https://www.unicode.org/reports/tr61/#ElementList) [DollarElements](#DollarElements) [Elements](https://www.unicode.org/reports/tr61/#Elements)

  The following alternative is added to the [Union](https://www.unicode.org/reports/tr61/#Union) production:
  > | [ElementList](https://www.unicode.org/reports/tr61/#ElementList) [DollarElements](#DollarElements) [UnicodeSet](https://www.unicode.org/reports/tr61/#UnicodeSet)

  The following alternative is added to the [Range](https://www.unicode.org/reports/tr61/#Range) production:
  > | `$` - [RangeElement](https://www.unicode.org/reports/tr61/#RangeElement)

  When the [set-operator](https://www.unicode.org/reports/tr61/#set-operator) `$` occurs
  as an immediate constituent of an [Æther](#Æther)</a>,
  it represents the noncharacter code point U+FFFF.

  When it occurs anywhere else, it represents the character U+0024 $ DOLLAR SIGN.

  A [DollarElements](#DollarElements) construct of the form 𝑥 `-` `$`, where 𝑥 is a
  [RangeElement](https://www.unicode.org/reports/tr61/#RangeElement),
  is equivalent to the [Range](#Range) 𝑥 `-` `\N{0024:$:DOLLAR SIGN}`.

* If a `SymbolTable` is passed to the constructor of `UnicodeSet`, a new lexical
  element is introduced:
  > <a id="variable"></a>[variable](#variable) ⩴ $ [reference](#reference)

  where the function `SymbolTable::parseReference` defines the syntactic category
  <a id="reference"></a>[reference](#reference).

  The expansion of a [variable](#variable) is defined by `SymbolTable::lookup`; it
  disambiguates the syntactic category as follows:
  * If the expansion is a [UnicodeSet](https://www.unicode.org/reports/tr61/#UnicodeSet), the variable
    is a *set-valued*-[variable](#variable).
  * If the expansion is a [RangeElement](https://www.unicode.org/reports/tr61/#RangeElement), the
    variable is a *code-point-valued*-[variable](#variable).
  * If the expansion is a [string-literal](https://www.unicode.org/reports/tr61/#string-literal), the
    variable is a *string-valued*-[variable](#variable).
  * Otherwise, the variable is ill-defined, and the `UnicodeSet` constructor fails.
  The following alternative is added to the [UnicodeSet](https://www.unicode.org/reports/tr61/#UnicodeSet) production:
  > | *set-valued*-[variable](#variable)

  The following alternative is added to the [RangeElement](https://www.unicode.org/reports/tr61/#RangeElement) production:
  > | *code-point-valued*-[variable](#variable)

  The following alternative is added to the [Element](https://www.unicode.org/reports/tr61/#Element):
  > | *string-valued*-[variable](#variable)

  The [variable](#variable) represents the same set of code point sequences as its expansion.

#### Space-sensitive parsing {#space-sensitive}

When sets are parsed with explicit options and the IGNORE_SPACE bit is not set,
all characters in the
[white-space](https://www.unicode.org/reports/tr61/#white-space)
syntactic category of the UnicodeSet grammar are removed from that syntactic
category, and are added to the
[literal-element](https://www.unicode.org/reports/tr61/#literal-element)
syntactic category.

In that configuration, the UnicodeSet class is a conformant but inconsistent
implementation of UnicodeSet notation: those expressions that contain a
[literal-element](https://www.unicode.org/reports/tr61/#literal-element)
with the Pattern_Syntax property are interpreted differently from the standard.

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
