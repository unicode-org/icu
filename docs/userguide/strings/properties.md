---
layout: default
title: Properties
nav_order: 2
parent: Chars and Strings
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Properties

## Overview

Text processing requires that a program treat text appropriately. If text is
exchanged between several systems, it is important for them to process the text
consistently. This is done by assigning each character, or a range of
characters, attributes or properties used for text processing, and by defining
standard algorithms for at least the basic text operations.

Traditionally, such attributes and algorithms have not been well-defined for
most character sets, and text processing had to rely on ad-hoc solutions. Over
time, standards were created for querying properties of the system codepage.
However, the set of these properties was limited. Their data was not coordinated
among implementations, and standard algorithms were not available.

It is one of the strengths of Unicode that it not only defines a very large
character set, but also assigns a comprehensive set of properties and usage
notes to all characters. It defines standard algorithms for critical text
processing, and the data is publicly provided and kept up-to-date. See
https://www.unicode.org/ and https://www.unicode.org/main.html for more information.

Sample code is available in the ICU source code library at
[icu4c/source/samples/props/props.cpp](https://github.com/unicode-org/icu/blob/master/icu4c/source/samples/props/props.cpp).
See also the source code for the [Unicode
browser](https://github.com/unicode-org/icu-demos/tree/master/ubrowse) demo
application, which can be used
[online](http://demo.icu-project.org/icu-bin/ubrowse) to browse Unicode
characters with their properties.

## Unicode Character Database properties in ICU APIs

The following table shows all Unicode Character Database properties (except for
purely "extracted" ones and Unihan properties) and the corresponding ICU APIs.
Most of the time, ICU4C provides functions in
icu4c/source/common/unicode/uchar.h and ICU4J provides parallel functions in the
com.ibm.icu.lang.UCharacter class. Properties of a single Unicode character are
accessed by its 21-bit code point value (type: UChar32=int32_t in C/C++, int in
Java).

[Surrogate code points](https://www.unicode.org/glossary/#surrogate_code_point)
mostly have default property values, except for the General_Category (gc=Cs).

For integer values outside the Unicode code point range (negative or ≥
0x110000), most API functions return null values (false, 0, etc.). API functions
that map a code point to another (e.g., u_foldCase()/UCharacter.foldCase())
normally return out-of-range values (i.e., map them to themselves), just like
for unassigned code points or generally code points that have no specific
mappings. In particular, -1 (=U_SENTINEL in ICU4C) is mapped to -1.

Most properties are also available via UnicodeSet APIs and patterns. See the
Lookup section below.

See [UAX #44, Unicode Character
Database](https://www.unicode.org/reports/tr44/#Properties) itself for
comparison. The UCD files
[PropertyAliases.txt](https://www.unicode.org/Public/UCD/latest/ucd/PropertyAliases.txt)
and
[PropertyValueAliases.txt](https://www.unicode.org/Public/UCD/latest/ucd/PropertyValueAliases.txt)
list all properties and their values by name and type.

UAX #44 also shows which UCD files have data for which properties,
and many other useful details.

Most properties that use binary, integer, or enumerated values are available via
functions u_hasBinaryProperty and u_getIntPropertyValue which take UProperty
enum constants to select the property. (ICU4J UCharacter member functions do not
have the "u_" prefix.) The constant names include the long property name
according to PropertyAliases.txt, e.g., UCHAR_LINE_BREAK. Corresponding property
value enum constant names often contain the short property name and the long
value name, e.g., U_LB_LINE_FEED. For enumeration/integer type properties, the
enumeration result type is also listed here.

Some UnicodeSet APIs use the same UProperty constants. Other UnicodeSet APIs and
UnicodeSet and regular expression patterns use the long or short property
aliases and property value aliases (see PropertyAliases.txt and
PropertyValueAliases.txt).

There is one pseudo-property, UCHAR_GENERAL_CATEGORY_MASK for which the APIs do
not use a single value but a bit-set (a mask) of zero or more values, with each
bit corresponding to one UCHAR_GENERAL_CATEGORY value. This allows ICU to
represent property value aliases for multiple general categories, like "Letters"
(which stands for "Uppercase Letters", "Lowercase Letters", etc.). In other
words, there are two ICU properties for the same Unicode property, one
delivering single values (for per-code point lookup) and the other delivering
sets of values (for use with value aliases and UnicodeSet).

| UCD Name | Type |  | ICU4C uchar.h / ICU4J UCharacter |
|--------------|--------|-----|------------------------------|
| Age | Unicode version | (U) | C: u_charAge fills in UVersionInfo<br>Java: getAge returns a VersionInfo reference |
| Alphabetic | binary | (U) | u_isUAlphabetic, UCHAR_ALPHABETIC |
| ASCII_Hex_Digit | binary | (U) | UCHAR_ASCII_HEX_DIGIT |
| Bidi_Class | enum | (U) | u_charDirection, UCHAR_BIDI_CLASS<br>returns enum UCharDirection |
| Bidi_Control | binary | (U) | UCHAR_BIDI_CONTROL |
| Bidi_Mirrored | binary | (U) | u_isMirrored, UCHAR_BIDI_MIRRORED |
| Bidi_Mirroring_Glyph | code point |  | u_charMirror |
| Block | enum | (U) | ublock_getCode, UCHAR_BLOCK<br>returns enum UBlockCode |
| Canonical_Combining_Class | 0..255 | (U) | u_getCombiningClass, UCHAR_CANONICAL_COMBINING_CLASS |
| Case_Folding | Unicode string |  | u_strFoldCase (ustring.h) |
| Case_Ignorable | binary | (U) | UCHAR_CASE_IGNORABLE |
| Cased | binary | (U) | UCHAR_CASED |
| Changes_When_Casefolded | binary | (U) | UCHAR_CHANGES_WHEN_CASEFOLDED |
| Changes_When_Casemapped | binary | (U) | UCHAR_CHANGES_WHEN_CASEMAPPED |
| Changes_When_NFKC_Casefolded | binary | (U) | UCHAR_CHANGES_WHEN_NFKC_CASEFOLDED |
| Changes_When_Lowercased | binary | (U) | UCHAR_CHANGES_WHEN_LOWERCASED |
| Changes_When_Titlecased | binary | (U) | UCHAR_CHANGES_WHEN_TITLECASED |
| Changes_When_Uppercased | binary | (U) | UCHAR_CHANGES_WHEN_UPPERCASED |
| Composition_Exclusion | binary | (c) | contributes to Full_Composition_Exclusion |
| Dash | binary | (U) | UCHAR_DASH |
| Decomposition_Mapping | Unicode string |  | NFKC Normalizer2::getRawDecomposition() |
| Decomposition_Type | enum | (U) | UCHAR_DECOMPOSITION_TYPE<br>returns enum UDecompositionType |
| Default_Ignorable_Code_Point | binary | (U) | UCHAR_DEFAULT​_IGNORABLE_CODE_POINT |
| Deprecated | binary | (U) | UCHAR_DEPRECATED |
| Diacritic | binary | (U) | UCHAR_DIACRITIC |
| East_Asian_Width | enum | (U) | UCHAR_EAST_ASIAN_WIDTH<br>returns enum UEastAsianWidth |
| Expands_On_NF* | binary |  | available via normalization API (normalizer2.h) |
| Extender | binary | (U) | UCHAR_EXTENDER |
| FC_NFKC_Closure | Unicode string |  | u_getFC_NFKC_Closure |
| Full_Composition_Exclusion | binary | (U) | UCHAR_FULL​_COMPOSITION_EXCLUSION |
| General_Category | enum | (U) | u_charType, UCHAR_GENERAL_CATEGORY, UCHAR_GENERAL_CATEGORY_MASK<br>returns enum UCharCategory |
| Grapheme_Base | binary | (U) | UCHAR_GRAPHEME_BASE |
| Grapheme_Cluster_Break | enum | (U) | UCHAR_GRAPHEME_CLUSTER_BREAK<br>returns enum UGraphemeClusterBreak |
| Grapheme_Extend | binary | (U) | UCHAR_GRAPHEME_EXTEND |
| Grapheme_Link | binary | (U) | UCHAR_GRAPHEME_LINK |
| Hangul_Syllable_Type | enum | (U) | UCHAR_HANGUL_SYLLABLE_TYPE<br>returns enum UHangulSyllableType |
| Hex_Digit | binary | (U) | UCHAR_HEX_DIGIT |
| Hyphen | binary | (U) | UCHAR_HYPHEN |
| ID_Continue | binary | (U) | UCHAR_ID_CONTINUE |
| ID_Start | binary | (U) | UCHAR_ID_START |
| Ideographic | binary | (U) | UCHAR_IDEOGRAPHIC |
| IDS_Binary_Operator | binary | (U) | UCHAR_IDS_BINARY_OPERATOR |
| IDS_Triary_Operator | binary | (U) | UCHAR_IDS_TRINARY_OPERATOR |
| Indic_Positional_Category | enum | (U) | UCHAR_INDIC_POSITIONAL_CATEGORY<br>returns enum UIndicPositionalCategory |
| Indic_Syllabic_Category | enum | (U) | UCHAR_INDIC_SYLLABIC_CATEGORY<br>returns enum UIndicSyllabicCategory |
| ISO_Comment | ASCII string |  | u_getISOComment |
| Jamo_Short_Name | ASCII string | (c) | contributes to Name |
| Join_Control | binary | (U) | UCHAR_JOIN_CONTROL |
| Joining_Group | enum | (U) | UCHAR_JOINING_GROUP<br>returns enum UJoiningGroup |
| Joining_Type | enum | (U) | UCHAR_JOINING_TYPE<br>returns enum UJoiningType |
| Line_Break | enum | (U) | UCHAR_LINE_BREAK<br>returns enum ULineBreak |
| Logical_Order_Exception | binary | (U) | UCHAR_LOGICAL_ORDER_EXCEPTION |
| Lowercase | binary | (U) | u_isULowercase, UCHAR_LOWERCASE |
| Lowercase_Mapping | Unicode string |  | available via u_strToLower (ustring.h) |
| Math | binary | (U) | UCHAR_MATH |
| Name | ASCII string | (U) | u_charName(U_UNICODE_CHAR_NAME or U_EXTENDED_CHAR_NAME) |
| Name_Alias | ASCII string |  | u_charName(U_CHAR_NAME_ALIAS) |
| NF*_QuickCheck | enum | (U) | UCHAR_NF*_QUICK_CHECK and available via quickCheck (normalizer2.h)<br>returns UNormalizationCheckResult (no/maybe/yes) |
| NFKC_Casefold | Unicode string |  | available via normalization API (normalizer2.h "nfkc_cf") |
| Noncharacter_Code_Point | binary | (U) | UCHAR_NONCHARACTER​_CODE_POINT, <br /> U_IS_UNICODE_NONCHAR (utf.h) |
| Numeric_Type | enum | (U) | UCHAR_NUMERIC_TYPE<br>returns enum UNumericType |
| Numeric_Value | double | (U) | u_getNumericValueJava/UnicodeSet: only non-negative integers, no fractions |
| Other_Alphabetic | binary | (c) | contributes to Alphabetic |
| Other_Default_Ignorable​_Code_Point | binary | (c) | contributes to Default_Ignorable​_Code_Point |
| Other_Grapheme_Extend | binary | (c) | contributes to Grapheme_Extend |
| Other_Lowercase | binary | (c) | contributes to Lowercase |
| Other_Math | binary | (c) | contributes to Math |
| Other_Uppercase | binary | (c) | contributes to Uppercase |
| Pattern_Syntax | binary | (U) | UCHAR_PATTERN_SYNTAX |
| Pattern_White_Space | binary | (U) | UCHAR_PATTERN_WHITE_SPACE |
| Quotation_Mark | binary | (U) | UCHAR_QUOTATION_MARK |
| Radical | binary | (U) | UCHAR_RADICAL |
| Script | enum | (U) | uscript_getCode (uscript.h), UCHAR_SCRIPT<br>returns enum UScriptCode |
| Script_Extensions | list | (U) | uscript_getScriptExtensions & uscript_hasScript (uscript.h), UCHAR_SCRIPT_EXTENSIONS<br>returns a list of enum UScriptCode values |
| Sentence_Break | enum | (U) | UCHAR_SENTENCE_BREAK<br>returns enum USentenceBreak |
| Simple_Case_Folding | code point |  | u_foldCase |
| Simple_Lowercase_ Mapping | code point |  | u_tolower |
| Simple_Titlecase_ Mapping | code point |  | u_totitle |
| Simple_Uppercase_ Mapping | code point |  | u_toupper |
| Soft_Dotted | binary | (U) | UCHAR_SOFT_DOTTED |
| STerm | binary | (U) | UCHAR_S_TERM |
| Terminal_Punctuation | binary | (U) | UCHAR_TERMINAL_PUNCTUATION |
| Titlecase_Mapping | Unicode string |  | u_strToTitle (ustring.h) |
| Unicode_1_Name | ASCII string | (U) | u_charName(U_UNICODE_10_CHAR_NAME or U_EXTENDED_CHAR_NAME) |
| Unified_Ideograph | binary | (U) | UCHAR_UNIFIED_IDEOGRAPH |
| Uppercase | binary | (U) | u_isUUppercase, UCHAR_UPPERCASE |
| Uppercase_Mapping | Unicode string |  | u_strToUpper (ustring.h) |
| Vertical_Orientation | enum | (U) | UCHAR_VERTICAL_ORIENTATION<br>returns enum UVerticalOrientation |
| White_Space | binary | (U) | u_isUWhiteSpace, UCHAR_WHITE_SPACE |
| Word_Break | enum | (U) | UCHAR_WORD_BREAK<br>returns enum UWordBreakValues |
| XID_Continue | binary | (U) | UCHAR_XID_CONTINUE |
| XID_Start | binary | (U) | UCHAR_XID_START |

Notes:

1.  (c) - This property only **contributes** to "real" properties (mostly
    "Other_..." properties), so there is no direct support for this property in
    ICU.

2.  (U) - This property is available via the UnicodeSet APIs and patterns. Any
    property available in UnicodeSet is also available in regular expressions.
    Properties which are not available in UnicodeSet are generally those that
    are not available through a UProperty selector.

3.  UnicodeSet `[:scx=Arab:]` is a superset of `[:sc=Arab:]`;
    see https://www.unicode.org/reports/tr18/#Script_Property

4.  Full case mapping properties (e.g., Lowercase_Mapping) are complex.
    The string case mapping functions that implement them handle language-specific
    and/or context-sensitive mappings.
    The output may have more code points or fewer code points than the input.

## Customization

ICU does not provide the means to modify properties at runtime. The properties
are provided exactly as specified by a recent version of the Unicode Standard
(as published in the [Character
Database](http://www.unicode.org/onlinedat/online.html)).

For custom sets and maps, it is easiest to make UnicodeSet or
UCPTrie/CodePointTrie objects with the desired values.

However, if an application requires custom properties (for example, for [Private
Use](http://www.unicode.org/glossary/) characters), then it is possible to
change or add them at build-time. This is doable but not easy.

It is done by modifying the Character Database files copied into the ICU source
tree at
[icu4c/source/data/unidata](https://github.com/unicode-org/icu/tree/master/icu4c/source/data/unidata).
Since ICU 49, most of the properties have been combined into one file,
unidata/ppucd.txt (see the [Preparsed
UCD](http://site.icu-project.org/design/props/ppucd) design doc). Some of the
remaining UCD files are still inputs, others are only used for unit tests.

To add a character to such a file, a line must be inserted into the file with
the format used in that file (see the online documentation on the [Unicode
site](http://www.unicode.org/reports/tr44/) for more information). After
modifying one or more of these files, the ICU data needs to be rebuilt, and the
resulting files need to be checked into the ICU source tree. The files are
processed by special ICU tools outside of the normal ICU build. The
[unidata/changes.txt](https://github.com/unicode-org/icu/blob/master/icu4c/source/data/unidata/changes.txt)
file documents the process that has been used for the last several Unicode
version updates; skip the file preparation and API update steps.

Any available Unicode code point (0 to 10FFFF<sub>16</sub>) can be used.
Code point values
should be written with either 4, 5, or 6 hex digits. The minimum number of
digits possible should be used (but no fewer than 4). Note that the Unicode
Standard specifies that the 32 code points U+FDD0..U+FDEF and the 34 code points
U+...xFFFE and U+...xFFFF (where x=0, 1, 2, ..., F, 10) are not characters,
therefore they should not be added to any of the character database files.

## Lookup

For lookup by code point, iterate through the string, fetch code points, and
either call the unicode/uchar.h / UCharacter or similar functions, or use
dedicated sets and maps. For binary properties, and sets in general, there are
also more efficient methods for iterating over substrings.

### Binary property from code point

Call one of the binary-property functions. Alternatively, make a UnicodeSet for
the property (remember to freeze() it) or for a custom set of characters, and
call contains().

### Binary property over string

It is often useful to partition a string into substrings where every character
has the property, and substrings where every character does not have the
property. For example, to split the string at separator characters, remove
certain types of characters, trim white space, etc. Use a UnicodeSet with its
span() and spanBack() methods (available in C++ in UTF-8 versions). In Java, you
can also use a UnicodeSetSpanner.

### Enumerated property from code point

Call one of the int-property functions. Alternatively, build a UCPTrie /
CodePointTrie (new in ICU 63) via its mutable version and build method, then use
that to get the int value for each code point.

### Enumerated property over string

Easiest is to iterate over code points of the string and call per-code point
lookup methods (or use a code point trie).

The UCPTrie / CodePointTrie (new in ICU 63) also offers C macros and a Java
String iterator class where the iteration and data lookup are integrated to
avoid redundancies in validation and range checks.

The UTF-16 code point macros and the Java String iterator also provide the code
point as output, because it has to be fetched or assembled anyway.

The UTF-8 macros do not assemble the code point because that would be some
amount of extra work, but often only the lookup value is used and the code point
is not needed. When it is needed after all, it is possible to take advantage of
the macros having validated the byte sequence: If the sequence was ill-formed,
then the trie's error value is set. Therefore, if a value other than the trie
error value was returned, then the sequence was well-formed, and the code point
can be fetched without revalidating the sequence (e.g., via U8_NEXT_UNSAFE()).
Since the length of the sequence (1..4 bytes) is also known from the iteration
(string index before/after next() call), an even simpler piece of code can be
used. (See for example the ICU-internal function codePointFromValidUTF8() in
normalizer2impl.cpp.)

### Code point trie most-optimized UTF-16 access

UTF-16 text processing can be further optimized by detecting surrogate pairs and
assembling supplementary code points only when there is non-trivial data
available.

At build time, iterate over all supplementary code points
(umutablecptrie_getRange() / MutableCodePointTrie.getRange() starting from
U+10000) to see if there is non-trivial data for any of the supplementary code
points associated with a lead surrogate. If so, then set a special
(application-specific) value for the lead surrogate.

At runtime, use UCPTRIE_FAST_BMP_GET() per code *unit*. If there is non-trivial
data and the code unit is a lead surrogate, then check if a trail surrogate
follows. If so, assemble the supplementary code point with
U16_GET_SUPPLEMENTARY() and look up its value with UCPTRIE_FAST_SUPP_GET();
otherwise deal with the unpaired surrogate in some way. (Java CodePointTrie.Fast
and java.lang.Character have equivalent methods.)

If there is only trivial data for lead and trail surrogates, then processing can
often skip them. (In this case, there will be two data lookups, one for the lead
surrogate and one for the trail surrogate, but they are fast, and this
optimization speeds up the more common BMP characters by not checking for
surrogates each time.)

For example, in normalization or case mapping all characters that do not have
any mappings are simply copied as is.

## Properties in ICU Rule Syntax

ICU rule syntaxes should use the Unicode Pattern_White_Space set as syntactic
"spaces" to allow for the usage of white space characters outside of the normal
ASCII range while still maintaining backward compatibility. See
<https://www.unicode.org/reports/tr31/#Pattern_Syntax> for more information.
