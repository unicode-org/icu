---
layout: default
title: Concepts
nav_order: 1
parent: Collation
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Collation Concepts
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

The previous section demonstrated many of the requirements imposed on string
comparison routines that try to correctly collate strings according to
conventions of more than a hundred different languages, written in many
different scripts. This section describes the principles and architecture behind
the ICU Collation Service.

## Sortkeys vs Comparison

Sort keys are most useful in databases, where the overhead of calling a function
for each comparison is very large.

Generating a sort key from a Collator is many times more expensive than doing a
compare with the Collator (for common use cases). That's if the two functions
are called from Java or C. So for those languages, unless there is a very large
number of comparisons, it is better to call the compare function.

Here is an example, with a little back-of-the-envelope calculation. Let's
suppose that with a given language on a given platform, the compare performance
(CP) is 100 faster than sortKey performance (SP), and that you are doing a
binary search of a list with 1,000 elements. The binary comparison performance
is BP. We'd do about 10 comparisons, getting:

compare: 10 \* CP

sortkey: 1 \* SP + 10 \* BP

Even if BP is free, compare would be better. One has to get up to where log2(n)
= 100 before they break even.

But even this calculation is only a rough guide. First, the binary comparison is
not completely free. Secondly, the performance of compare function varies
radically with the source data. We optimized for maximizing performance of
collation in sorting and binary search, so comparing strings that are "close" is
optimized to be much faster than comparing strings that are "far away". That
optimization is important because normal sort/lookup operations compare close
strings far more often -- think of binary search, where the last few comparisons
are always with the closest strings. So even the above calculation is not very
accurate.

## Comparison Levels

In general, when comparing and sorting objects, some properties can take
precedence over others. For example, in geometry, you might consider first the
number of sides a shape has, followed by the number of sides of equal length.
This causes triangles to be sorted together, then rectangles, then pentagons,
etc. Within each category, the shapes would be ordered according to whether they
had 0, 2, 3 or more sides of the same length. However, this is not the only way
the shapes can be sorted. For example, it might be preferable to sort shapes by
color first, so that all red shapes are grouped together, then blue, etc.
Another approach would be to sort the shapes by the amount of area they enclose.

Similarly, character strings have properties, some of which can take precedence
over others. There is more than one way to prioritize the properties.

For example, a common approach is to distinguish characters first by their
unadorned base letter (for example, without accents, vowels or tone marks), then
by accents, and then by the case of the letter (upper vs. lower). Ideographic
characters might be sorted by their component radicals and then by the number of
strokes it takes to draw the character.
An alternative ordering would be to sort these characters by strokes first and
then by their radicals.

The ICU Collation Service supports many levels of comparison (named "Levels",
but also known as "Strengths"). Having these categories enables ICU to sort
strings precisely according to local conventions. However, by allowing the
levels to be selectively employed, searching for a string in text can be
performed with various matching conditions.

Performance optimizations have been made for ICU collation with the default
level settings. Performance specific impacts are discussed in the Performance
section below.

Following is a list of the names for each level and an example usage:

1.  Primary Level: Typically, this is used to denote differences between base
    characters (for example, "a" < "b"). It is the strongest difference. For
    example, dictionaries are divided into different sections by base character.
    This is also called the level-1 strength.

2.  Secondary Level: Accents in the characters are considered secondary
    differences (for example, "as" < "às" < "at"). Other differences between
    letters can also be considered secondary differences, depending on the
    language. A secondary difference is ignored when there is a primary
    difference anywhere in the strings. This is also called the level-2
    strength.
    Note: In some languages (such as Danish), certain accented letters are
    considered to be separate base characters. In most languages, however, an
    accented letter only has a secondary difference from the unaccented version
    of that letter.

3.  Tertiary Level: Upper and lower case differences in characters are
    distinguished at the tertiary level (for example, "ao" < "Ao" < "aò"). In
    addition, a variant of a letter differs from the base form on the tertiary
    level (such as "A" and "Ⓐ"). Another example is the difference between large
    and small Kana. A tertiary difference is ignored when there is a primary or
    secondary difference anywhere in the strings. This is also called the
    level-3 strength.

4.  Quaternary Level: When punctuation is ignored (see Ignoring Punctuations
    (§)) at level 1-3, an additional level can be used to distinguish words with
    and without punctuation (for example, "ab" < "a-b" < "aB"). This difference
    is ignored when there is a primary, secondary or tertiary difference. This
    is also known as the level-4 strength. The quaternary level should only be
    used if ignoring punctuation is required or when processing Japanese text
    (see Hiragana processing (§)).

5.  Identical Level: When all other levels are equal, the identical level is
    used as a tiebreaker. The Unicode code point values of the NFD form of each
    string are compared at this level, just in case there is no difference at
    levels 1-4. For example, Hebrew cantillation marks are only distinguished
    at this level. This level should be used sparingly, as only code point
    value differences between two strings is an extremely rare occurrence.
    Using this level substantially decreases the performance for
    both incremental comparison and sort key generation (as well as increasing
    the sort key length). It is also known as level 5 strength.

## Backward Secondary Sorting

Some languages require words to be ordered on the secondary level according to
the *last* accent difference, as opposed to the *first* accent difference. This
was previously the default for all French locales, based on some French
dictionary ordering traditions, but is currently only applicable to Canadian
French (locale **fr_CA**), for conformance with the [Canadian sorting
standard](http://www.unicode.org/reports/tr10/#CanStd). The difference in
ordering is only noticeable for a small number of pairs of real words. For more
information see [UCA: Contextual
Sensitivity](http://www.unicode.org/reports/tr10/#Contextual_Sensitivity).

Example:

Forward secondary | Backward secondary
----------------- | ------------------
cote              | cote
coté              | côte
côte              | coté
côté              | côté

## Contractions

A contraction is a sequence consisting of two or more letters. It is considered
a single letter in sorting.

For example, in the traditional Spanish sorting order, "ch" is considered a
single letter. All words that begin with "ch" sort after all other words
beginning with "c", but before words starting with "d".

Other examples of contractions are "ch" in Czech, which sorts after "h", and
"lj" and "nj" in Croatian and Latin Serbian, which sort after "l" and "n"
respectively.

Example:

Order without contraction | Order with contraction "lj" sorting after letter "l"
------------------------- | ----------------------------------------------------
la                        | la
li                        | li
lj                        | lk
lja                       | lz
ljz                       | lj
lk                        | lja
lz                        | ljz
ma                        | ma

Contracting sequences such as the above are not very common in most languages.

> :point_right: **Note** Since ICU 2.2, and as required by the UCA,
> if a completely ignorable code point
> appears in text in the middle of contraction, it will not break the contraction.
> For example, in Czech sorting, cU+0000h will sort as it were ch.

## Expansions

If a letter sorts as if it were a sequence of more than one letter, it is called
an expansion.

For example, in German phonebook sorting (de@collation=phonebook or BCP 47
de-u-co-phonebk), "ä" sorts as though it were equivalent to the sequence "ae."
All words starting with "ä" will sort between words starting with "ad" and words
starting with "af".

In the case of Unicode encoding, characters can often be represented either as
pre-composed characters or in decomposed form. For example, the letter "à" can
be represented in its decomposed (a+\`) and pre-composed (à) form. Most
applications do not want to distinguish text by the way it is encoded. A search
for "à" should find all instances of the letter, regardless of whether the
instance is in pre-composed or decomposed form. Therefore, either form of the
letter must result in the same sort ordering. The architecture of the ICU
Collation Service supports this.

## Contractions Producing Expansions

It is possible to have contractions that produce expansions.

One example occurs in Japanese, where the vowel with a prolonged sound mark is
treated to be equivalent to the long vowel version:

カアー<<< カイー and\
キイー<<< キイー

> :point_right: **Note** Since ICU 2.0 Japanese tailoring uses
> [prefix analysis](http://www.unicode.org/reports/tr35/tr35-collation.html#Context_Sensitive_Mappings)
> instead of contraction producing expansions.

## Normalization

In the section on expansions, we discussed that text in Unicode can often be
represented in either pre-composed or decomposed forms. There are other types of
equivalences possible with Unicode, including Canonical and Compatibility. The
process of
Normalization ensures that text is written in a predictable way so that searches
are not made unnecessarily complicated by having to match on equivalences. Not
all text is normalized, however, so it is useful to have a collation service
that can address text that is not normalized, but do so with efficiency.

The ICU Collation Service handles un-normalized text properly, producing the
same results as if the text were normalized.

In practice, most data that is encountered is in normalized or semi-normalized
form already. The ICU Collation Service is designed so that it can process a
wide range of normalized or un-normalized text without a need for normalization
processing. When a case is encountered that requires normalization, the ICU
Collation Service drops into code specific to this purpose. This maximizes
performance for the majority of text that does not require normalization.

In addition, if the text is known with certainty not to contain un-normalized
text, then even the overhead of checking for normalization can be eliminated.
The ICU Collation Service has the ability to turn Normalization Checking either
on or off. If Normalization Checking is turned off, it is the user's
responsibility to insure that all text is already in the appropriate form. This
is true in a great majority of the world languages, so normalization checking is
turned off by default for most locales.

If the text requires normalization processing, Normalization Checking should be
on. Any language that uses multiple combining characters such as Arabic, ancient
Greek, Hebrew, Hindi, Thai or Vietnamese either requires Normalization Checking
to be on, or the text to go through a normalization process before collation.

For more information about Normalization related reordering please see
[Unicode Technical Note #5](http://www.unicode.org/notes/tn5/) and
[UAX #15.](http://www.unicode.org/reports/tr15/)

> :point_right: **Note** ICU supports two modes of normalization: on and off.
> Java.text.\* classes offer compatibility decomposition mode, which is not supported in ICU.

## Ignoring Punctuation

In some cases, punctuation can be ignored while searching or sorting data. For
example, this enables a search for "biweekly" to also return instances of
"bi-weekly". In other cases, it is desirable for punctuated text to be
distinguished from text without punctuation, but to have the text sort close
together.

These two behaviors can be accomplished if there is a way for a character to be
ignored on all levels except for the quaternary level. If this is the case, then
two strings which compare as identical on the first three levels (base letter,
accents, and case) are then distinguished at the fourth level based on their
punctuation (if any). If the comparison function ignores differences at the
fourth level, then strings that differ by punctuation only are compared as
equal.

The following table shows the results of sorting a list of terms in 3 different
ways. In the first column, punctuation characters (space " ", and hyphen "-")
are not ignored (" " < "-" < "b"). In the second column, punctuation characters
are ignored in the first 3 levels and compared only in the fourth level. In the
third column, punctuation characters are ignored in the first 3 levels and the
fourth level is not considered. In the last column, punctuated terms are
equivalent to the identical terms without punctuation.

For more options and details see the [“Ignore Punctuation”
Options](customization/ignorepunct.md) page.

Non-ignorable | Ignorable and Quaternary strength | Ignorable and Tertiary strength
------------- | --------------------------------- | -------------------------------
black bird    | black bird                        | **black bird**
black Bird    | black-bird                        | **black-bird**
black birds   | blackbird                         | **blackbird**
black-bird    | black Bird                        | black Bird
black-Bird    | black-Bird                        | black-Bird
black-birds   | blackBird                         | blackBird
blackbird     | black birds                       | black birds
blackBird     | black-birds                       | black-birds
blackbirds    | blackbirds                        | blackbirds

> :point_right: **Note** The strings with the same font format in the last column are
compared as equal by ICU Collator.\
> Since ICU 2.2 and as prescribed by the UCA, primary ignorable code points that
> follow shifted code points will be completely ignored. This means that an accent
> following a space will compare as if it was a space alone.

## Case Ordering

The tertiary level is used to distinguish text by case, by small versus large
Kana, and other letter variants as noted above.

Some applications prefer to emphasize case differences so that words starting
with the same case sort together. Some Japanese applications require the
difference between small and large Kana be emphasized over other tertiary
differences.

The UCA does not provide means to separate out either case or Kana differences
from the remaining tertiary differences. However, the ICU Collation Service has
two options that help in customize case and/or Kana differences. Both options
are turned off by default.

### CaseFirst

The Case-first option makes case the most significant part of the tertiary
level. Primary and secondary levels are unaffected. With this option, words
starting with the same case sort together. The Case-first option can be set to
make either lowercase sort before
uppercase or uppercase sort before lowercase.

Note: The case-first option does not constitute a separate level; it is simply a
reordering of the tertiary level.

ICU makes use of the following three case categories for sorting

1.  uppercase: "ABC"

2.  mixed case: "Abc", "aBc"

3.  normal (lowercase or no case): "abc", "123"

Mixed case is always sorted between uppercase and normal case when the
"case-first" option is set.

### CaseLevel

The Case Level option makes a separate level for case differences. This is an
extra level positioned between secondary and tertiary. The case level is used in
Japanese to make the difference between small and large Kana more important than
the other tertiary differences. It also can be used to ignore other tertiary
differences, or even secondary differences. This is especially useful in
matching. For example, if the strength is set to primary only (level-1) and the
case level is turned on, the comparison ignores accents and tertiary differences
except for case. The contents of the case level are affected by the case-first
option.

The case level is independent from the strength of comparison. It is possible to
have a collator set to primary strength with the case level turned on. This
provides for comparison that takes into account the case differences, while at
the same time ignoring accents and tertiary differences other than case. This
may be used in searching.

Example:

**Case-first off, Case level off**

apple\
ⓐⓟⓟⓛⓔ\
Abernathy\
ⒶⒷⒺⓇⓃⒶⓉⒽⓎ\
ähnlich\
Ähnlichkeit

**Lowercase-first, Case level off**

apple\
ⓐⓟⓟⓛⓔ\
ähnlich\
Abernathy\
ⒶⒷⒺⓇⓃⒶⓉⒽⓎ\
Ähnlichkeit

**Uppercase-first, Case level off**

Abernathy\
ⒶⒷⒺⓇⓃⒶⓉⒽⓎ\
Ähnlichkeit\
apple\
ⓐⓟⓟⓛⓔ\
ähnlich

**Lowercase-first, Case level on**

apple\
Abernathy\
ⓐⓟⓟⓛⓔ\
ⒶⒷⒺⓇⓃⒶⓉⒽⓎ\
ähnlich\
Ähnlichkeit

**Uppercase-first, Case level on**

Abernathy\
apple\
ⒶⒷⒺⓇⓃⒶⓉⒽⓎ\
ⓐⓟⓟⓛⓔ\
Ähnlichkeit\
ähnlich

## Script Reordering

Script reordering allows scripts and some other groups of characters to be moved
relative to each other. This reordering is done on top of the DUCET/CLDR
standard collation order. Reordering can specify groups to be placed at the
start and/or the end of the collation order.

By default, reordering codes specified for the start of the order are placed in
the order given after several special non-script blocks. These special groups of
characters are space, punctuation, symbol, currency, and digit. Script groups
can be intermingled with these special non-script groups if those special groups
are explicitly specified in the reordering.

The special code `others` stands for any script that is not explicitly mentioned
in the list. Anything that is after others will go at the very end of the list
in the order given. For example, `[Grek, others, Latn]` will result in an
ordering that puts all scripts other than Greek and Latin between them.

### Examples:

Note: All examples below use the string equivalents for the scripts and reorder
codes that would be used in collator rules. The script and reorder code
constants that would be used in API calls will be different.

**Example 1:**\
set reorder code - `[Grek]`\
result - `[space, punctuation, symbol, currency, digit, Grek, others]`

**Example 2:**\
set reorder code - `[Grek]`\
result - `[space, punctuation, symbol, currency, digit, Grek, others]`

followed by: set reorder code - `[Hani]`\
result -` [space, punctuation, symbol, currency, digit, Hani, others]`

That is, setting a reordering always modifies
the DUCET/CLDR order, replacing whatever was previously set, rather than adding
on to it. In order to cumulatively modify an ordering, you have to retrieve the
existing ordering, modify it, and then set it.

**Example 3:**\
set reorder code - `[others, digit]`\
result - `[space, punctuation, symbol, currency, others, digit]`

**Example 4:**\
set reorder code - `[space, Grek, punctuation]`\
result - `[symbol, currency, digit, space, Grek, punctuation, others]`

**Example 5:**\
set reorder code - `[Grek, others, Hani]`\
result - `[space, punctuation, symbol, currency, digit, Grek, others, Hani]`

**Example 6:**\
set reorder code - `[Grek, others, Hani, symbol, Tglg]`\
result - `[space, punctuation, currency, digit, Grek, others, Hani, symbol, Tglg]`

followed by:\
set reorder code - `[NONE]`\
result - DUCET/CLDR

**Example 7:**\
set reorder code - `[Grek, others, Hani, symbol, Tglg]`\
result - `[space, punctuation, currency, digit, Grek, others, Hani, symbol, Tglg]`

followed by:\
set reorder code - `[DEFAULT]`\
result - original reordering for the locale which may or may not be DUCET/CLDR

**Example 8:**\
set reorder code - `[Grek, others, Hani, symbol, Tglg]`\
result - `[space, punctuation, currency, digit, Grek, others, Hani, symbol, Tglg]`

followed by:\
set reorder code - `[]`\
result - original reordering for the locale which may or may not be DUCET/CLDR

**Example 9:**\
set reorder code - `[Hebr, Phnx]`\
result - error

Beginning with ICU 55, scripts only reorder together if they are primary-equal,
for example Hiragana and Katakana.

ICU 4.8-54:

*   Scripts were reordered in groups, each normally starting with a [Recommended
    Script](http://www.unicode.org/reports/tr31/#Table_Recommended_Scripts).
*   Reorder codes moved as a group (were “equivalent”) if their scripts shared a
    primary-weight lead byte.
*   For example, Hebr and Phnx were “equivalent” reordering codes and were
    reordered together. Their order relative to each other could not be changed.
*   Only any one code out of any group could be reordered, not multiple of the
    same group.

## Sorting of Japanese Text (JIS X 4061)

Japanese standard JIS X 4061 requires two changes to the collation procedures:
special processing of Hiragana characters and (for performance reasons) prefix
analysis of text.

### Hiragana Processing

JIS X 4061 standard requires more levels than provided by the UCA. To offer
conformant sorting order, ICU uses the quaternary level to distinguish between
Hiragana and Katakana. Hiragana symbols are given smaller values than Katakana
symbols on quaternary level, thus causing Hiragana sequences to sort before
corresponding Katakana sequences.

### Prefix Analysis

Another characteristics of sorting according to the JIS X 4061 is a large number
of contractions followed by expansions (see
[Contractions Producing Expansions](#contractions-producing-expansions)).
This causes all the Hiragana and Katakana codepoints to be treated as
contractions, which reduces performance. The solution we adopted introduces the
prefix concept which allows us to improve the performance of Japanese sorting.
More about this can be found in the [customization
chapter](customization/index.md) .

## Thai/Lao reordering

UCA requires that certain Thai and Lao prevowels be reordered with a code point
following them. This option is always on in the ICU implementation, as
prescribed by the UCA.

This rule takes effect when:

1.  A Thai vowel of the range \\U0E40-\\U0E44 precedes a Thai consonant of the
    range \\U0E01-\\U0E2E
    or

2.  A Lao vowel of the range \\U0EC0-\\U0EC4 precedes a Lao consonant of the
    range \\U0E81-\\U0EAE. In these cases the vowel is placed after the
    consonant for collation purposes.

> :point_right: **Note** There is a difference between java.text.\* classes and ICU in regard to Thai
> reordering. Java.text.\* classes allow tailorings to turn off reordering by
> using the '!' modifier. ICU ignores the '!' modifier and always reorders Thai
> prevowels.

## Space Padding

In many database products, fields are padded with null. To get correct results,
the input to a Collator should omit any superfluous trailing padding spaces. The
problem arises with contractions, expansions, or normalization. Suppose that
there are two fields, one containing "aed" and the other with "äd". German
phonebook sorting (de@collation=phonebook or BCP 47 de-u-co-phonebk) will
compare "ä" as if it were "ae" (on a primary level), so the order will be "äd" <
"aed". But if both fields are padded with spaces to a length of 3, then this
will reverse the order, since the first will compare as if it were one character
longer. In other words, when you start with strings 1 and 2

1  | a  | e  | d         | \<space\>
-- | -- | -- | --------- | ---------
2  | ä  | d  | \<space\> | \<space\>

they end up being compared on a primary level as if they were 1' and 2'

1' | a  | e  | d  | \<space\> | &nbsp;
-- | -- | -- | -- | --------- | ---------
2' | a  | e  | d  | \<space\> | \<space\>

Since 2' has an extra character (the extra space), it counts as having a primary
difference when it shouldn't. The correct result occurs when the trailing
padding spaces are removed, as in 1" and 2"

1" | a  | e  | d
-- | -- | -- | --
2" | a  | e  | d

## Collator naming scheme

***Starting with ICU 54, the following naming scheme and its API functions are deprecated.***
Use `ucol_open()` with language tag collation keywords instead
(see [Collation API Details](api.md)). For example,
`ucol_open("de-u-co-phonebk-ka-shifted", &errorCode)` for German Phonebook order
with "ignore punctuation" mode.

When collating or matching text, a number of attributes can be used to affect
the desired result. The following describes the attributes, their values, their
effects, their normal usage, and the string comparison performance and sort key
length implications. It also includes single-letter abbreviations for both the
attributes and their values. These abbreviations allow a 'short-form'
specification of a set of collation options, such as "UCA4.0.0_AS_LSV_S", which
can be used to specific that the desired options are: UCA version 4.0.0; ignore
spaces, punctuation and symbols; use Swedish linguistic conventions; compare
case-insensitively.

A number of attribute values are common across different attributes; these
include **Default** (abbreviated as D), **On** (O), and **Off** (X). Unless
otherwise stated, the examples use the UCA alone with default settings.

> :point_right: **Note** In order to achieve uniqueness, a collator name always
> has the attribute abbreviations sorted.

### Main References

1.  For a full list of supported locales in ICU, see [Locale
    Explorer](http://demo.icu-project.org/icu-bin/locexp) , which also contains
    an on-line demo showing sorting for each locale. The demo allows you to try
    different attribute values, to see how they affect sorting.

2.  To see tabular results for the UCA table itself, see the [Unicode Collation
    Charts](http://www.unicode.org/charts/collation/) .

3.  For the UCA specification, see [UTS #10: Unicode Collation
    Algorithm](http://www.unicode.org/reports/tr10/) .

4.  For more detail on the precise effects of these options, see [Collation
    Customization](customization/index.md) .

#### Collator Naming Attributes

Attribute              | Abbreviation | Possible Values
---------------------- | ------------ | ---------------
Locale                 | L            | \<language\>
Script                 | Z            | \<script\>
Region                 | R            | \<region\>
Variant                | V            | \<variant\>
Keyword                | K            | \<keyword\>
&nbsp;                 | &nbsp;       | &nbsp;
Strength               | S            | 1, 2, 3, 4, I, D
Case_Level             | E            | X, O, D
Case_First             | C            | X, L, U, D
Alternate              | A            | N, S, D
Variable_Top           | T            | \<hex digits\>
Normalization Checking | N            | X, O, D
French                 | F            | X, O, D
Hiragana               | H            | X, O, D

#### Collator Naming Attribute Descriptions

The **Locale** attribute is typically the most
important attribute for correct sorting and matching, according to the user
expectations in different countries and regions. The default UCA ordering will
only sort a few languages such as Dutch and Portuguese correctly ("correctly"
meaning according to the normal expectations for users of the languages).
Otherwise, you need to supply the locale to UCA in order to properly collate
text for a given language. Thus a locale needs to be supplied so as to choose a
collator that is correctly **tailored** for that locale. The choice of a locale
will automatically preset the values for all of the attributes to something that
is reasonable for that locale. Thus most of the time the other attributes do not
need to be explicitly set. In some cases, the choice of locale will make a
difference in string comparison performance and/or sort key length.

In short attribute names,
`<language>_<script>_<region>_<variant>@collation=<keyword>` is
represented by: `L<language>_Z<script>_R<region>_V<variant>_K<keyword>`. Not
all the elements are required. Valid values for locale elements are general
valid values for RFC 3066 locale naming.

**Example:**\
**Locale="sv" (Swedish)** "Kypper" < "Köpfe"\
**Locale="de" (German)** "Köpfe" < "Kypper"

The **Strength** attribute determines whether accents or
case are taken into account when collating or matching text. ( (In writing
systems without case or accents, it controls similarly important features). The
default strength setting usually does not need to be changed for collating
(sorting), but often needs to be changed when **matching** (e.g. SELECT). The
possible values include Default (D), Primary (1), Secondary (2), Tertiary (3),
Quaternary (4), and Identical (I).

For example, people may choose to ignore accents or ignore accents and case when
searching for text.

Almost all characters are distinguished by the first three levels, and in most
locales the default value is thus Tertiary. However, if Alternate is set to be
Shifted, then the Quaternary strength (4) can be used to break ties among
whitespace, punctuation, and symbols that would otherwise be ignored. If very
fine distinctions among characters are required, then the Identical strength (I)
can be used (for example, Identical Strength distinguishes between the
**Mathematical Bold Small A** and the **Mathematical Italic Small A.** For more
examples, look at the cells with white backgrounds in the collation charts).
However, using levels higher than Tertiary - the Identical strength - result in
significantly longer sort keys, and slower string comparison performance for
equal strings.

**Example:**\
**S=1** role = Role = rôle\
**S=2** role = Role < rôle\
**S=3** role < Role < rôle

The **Case_Level** attribute is used when ignoring accents
**but not** case. In such a situation, set Strength to be Primary, and
Case_Level to be On. In most locales, this setting is Off by default. There is a
small string comparison performance and sort key impact if this attribute is set
to be On.

**Example:**\
**S=1, E=X** role = Role = rôle\
**S=1, E=O** role = rôle < Role

The **Case_First** attribute is used to control whether
uppercase letters come before lowercase letters or vice versa, in the absence of
other differences in the strings. The possible values are Uppercase_First (U)
and Lowercase_First (L), plus the standard Default and Off. There is almost no
difference between the Off and Lowercase_First options in terms of results, so
typically users will not use Lowercase_First: only Off or Uppercase_First.
(People interested in the detailed differences between X and L should consult
the [Collation Customization](customization/index.md) ).
Specifying either L or U won't affect string comparison performance, but will
affect the sort key length.

**Example:**\
**C=X or C=L** "china" < "China" < "denmark" < "Denmark"\
**C=U** "China" < "china" < "Denmark" < "denmark"

The **Alternate** attribute is used to control the handling of
the so-called **variable **characters in the UCA: whitespace, punctuation and
symbols. If Alternate is set to Non-Ignorable (N), then differences among these
characters are of the same importance as differences among letters. If Alternate
is set to Shifted (S), then these characters are of only minor importance. The
Shifted value is often used in combination with Strength set to Quaternary. In
such a case, white-space, punctuation, and symbols are considered when comparing
strings, but only if all other aspects of the strings (base letters, accents,
and case) are identical. If Alternate is not set to Shifted, then there is no
difference between a Strength of 3 and a Strength of 4.

For more information and examples, see
[Variable_Weighting](http://www.unicode.org/reports/tr10/#Variable_Weighting) in
the UCA.

The reason the Alternate values are not simply On and Off is that
additional Alternate values may be added in the future.

The UCA option
**Blanked** is expressed with Strength set to 3, and Alternate set to Shifted.

The default for most locales is Non-Ignorable. If Shifted is selected, it may be
slower if there are many strings that are the same except for punctuation; sort
key length will not be affected unless the strength level is also increased.

**Example:**\
**S=3, A=N** di Silva < Di Silva < diSilva < U.S.A. < USA\
**S=3, A=S** di Silva = diSilva < Di Silva < U.S.A. = USA\
**S=4, A=S** di Silva < diSilva < Di Silva < U.S.A. < USA

The **Variable_Top** attribute is only meaningful if the
Alternate attribute is not set to Non-Ignorable. In such a case, it controls
which characters count as ignorable. The \<hex\> value specifies the "highest"
character sequence (in UCA order) weight that is to be considered ignorable.

Thus, for example, if a user wanted white-space to be ignorable, but not any
visible characters, then s/he would use the value Variable_Top=0020 (space). The
digits should only be a single character. All characters of the same primary
weight are equivalent, so Variable_Top=3000 (ideographic space) has the same
effect as Variable_Top=0020.

This setting (alone) has little impact on string comparison performance; setting
it lower or higher will make sort keys slightly shorter or longer respectively.

**Example:**\
**S=3, A=S** di Silva = diSilva < U.S.A. = USA\
**S=3, A=S, T=0020** di Silva = diSilva < U.S.A. < USA

The **Normalization** setting determines whether
text is thoroughly normalized or not in comparison. Even if the setting is off
(which is the default for many locales), text as represented in common usage
will compare correctly (for details, see [UTN
#5](http://www.unicode.org/notes/tn5/)). Only if the accent marks are in
non-canonical order will there be a problem. If the setting is On, then the best
results are guaranteed for all possible text input.There is a medium string
comparison performance cost if this attribute is On, depending on the frequency
of sequences that require normalization. There is no significant effect on sort
key length.If the input text is known to be in NFD or NFKD normalization forms,
there is no need to enable this Normalization option.

**Example:**\
**N=X** ä = a + ◌̈ < ä + ◌̣ < ạ + ◌̈\
**N=O** ä = a + ◌̈ < ä + ◌̣ = ạ + ◌̈

Some **French** dictionary ordering traditions sort strings with
different accents from the back of the string. This attribute is automatically
set to On for the Canadian French locale (fr_CA). Users normally would not need
to explicitly set this attribute. There is a string comparison performance cost
when it is set On, but sort key length is unaffected.

**Example:**\
**F=X** cote < coté < côte < côté\
**F=O** cote < côte < coté < côté

Compatibility with JIS x 4061 requires the introduction of an
additional level to distinguish **Hiragana** and Katakana characters. If
compatibility with that standard is required, then this attribute is set On, and
the strength should be set to at least Quaternary.

This attribute is an implementation detail of the CLDR Japanese tailoring. The
implementation might change to use a different mechanism to achieve the same
Japanese sort order. Since ICU 50, this attribute is not settable any more.

**Example:**\
**H=X, S=4** きゅう = キュウ < きゆう = キユウ\
**H=O, S=4** きゅう < キュウ < きゆう < キユウ

> :point_right: **Note** If attributes in collator name are not overridden,
> it is assumed that they are the same as for the given locale.
> For example, a collator opened with an empty
> string has the same attribute settings as **AN_CX_EX_FX_HX_KX_NX_S3_T0000**.*

### Summary of Value Abbreviations

Value         | Abbreviation
------------- | ------------
Default       | D
On            | O
Off           | X
Primary       | 1
Secondary     | 2
Tertiary      | 3
Quaternary    | 4
Identical     | I
Shifted       | S
Non-Ignorable | N
Lower-First   | L
Upper-First   | U
