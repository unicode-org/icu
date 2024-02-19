---
layout: default
title: Customization
nav_order: 3
parent: Collation
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Collation Customization
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

ICU uses the [CLDR root collation
order](http://www.unicode.org/reports/tr35/tr35-collation.html#Root_Collation)
as a default starting point for ordering. (The CLDR root collation is based on
the [UCA
DUCET](http://www.unicode.org/reports/tr10/#Default_Unicode_Collation_Element_Table).)
Not all languages have sorting sequences that correspond with the root collation
order because no single sort order can simultaneously encompass the specifics of
all the languages. In particular, languages that share a script may sort the
same letters differently.

Therefore, ICU provides a data-driven, flexible, and run-time-customizable
mechanism called "tailoring". Tailoring overrides the default order of code
points and the values of the ICU Collation Service attributes.

## Collation Rule

A `RuleBasedCollator` is built from a rule string which changes the sort order of
some characters and strings relative to the default order. An empty string (or
one with only white space and comments) results in a collator that behaves like
the root collator.

A tailoring is specified via a string containing a set of rules. ICU implements
the (CLDR) [LDML collation rule
syntax](http://www.unicode.org/reports/tr35/tr35-collation.html#Rules). For more
details see there.

Each rule contains a string of ordered characters that starts with an **anchor
point** or a **reset value**. For example, `"&a < g"`, places "g"
after "a" and before "b", and the "a" does not change place. This rule has the
following sorting consequences:

Without rule | With rule
------------ | ---------
Abernathy    | Abernathy
apple        | apple
bird         | green
Boston       | bird
Graham       | Boston
green        | Graham

Note that only the word that starts with "g" has changed place. All the words
sorted after "a" and "A" are sorted after "g".
This includes "Graham"; "G" would have to be tailored separately, such as with
`"&a < g <<< G"`.

This is a non-complex example of a tailoring rule. Tailoring rules consist of
zero or more rules and zero or more options. There must be at least one rule or
at least one option. The rule syntax is discussed in more detail in the
following sections.

Note that the tailoring rules override the UCA ordering. In addition, if a
character is reordered, it automatically reorders any other equivalent
characters. For example, if the rule "&e<a" is used to reorder "a" in the list,
"á" is also greater than "é".

## Syntax

The following table summarizes the basic syntax necessary for most usages:

Symbol | Example&nbsp; | Description
------ | ------------- | ----------------------------------
`<`    | `a < b`       | Identifies a primary (base letter) difference between "a" and "b"
`<<`   | `a << ä`      | Signifies a secondary (accent) difference between "a" and "ä"
`<<<`  | `a<<<A`       | Identifies a tertiary difference between "a" and "A"
`<<<<` | `か<<<<カ`     | Identifies a quaternary difference between "か" and "カ". (New in ICU 53.)
`=`    | `x = y`       | Signifies no difference between "x" and "y".
`&`    | `&Z`          | Instructs ICU to reset at this letter. These rules will be relative to this letter from here on, but will not affect the position of Z itself.

> :point_right: **Note**: ICU permits up to three quaternary relations in a row
> (except for intervening "=" identity relations).

> :point_right: **Note**: In releases prior to 1.8,
> ICU used the notations `;` to represent secondary relations and `,` to represent tertiary relations.
> Starting in release 1.8, use `<<` symbols to represent secondary relations and
> `<<<` symbols to represent tertiary relations.
> Rules that use the `;` and `,` notations are still processed by ICU for compatibility;
> also, some of the data used for tailoring to particular locales
> has not yet been updated to the new syntax.
> However, one should consider these symbols deprecated.

> :point_right: **Note**: See the [LDML collation rule syntax](http://www.unicode.org/reports/tr35/tr35-collation.html#Rules)
> and [Properties and ICU Rule Syntax](../../strings/properties.md) for
> information regarding syntax characters.

Repeated use of the same relation can be abbreviated, for example
`&a <* bcd-gp-s` for `&a < b < c < d < e < f < g < p < q < r < s`.
For details see the
[LDML collation spec, section
Orderings](http://www.unicode.org/reports/tr35/tr35-collation.html#Orderings).

### Escaping Rules

Most of the characters can be used as parts of rules. However, whitespace
characters will be skipped over, and all ASCII characters that are not digits or
letters are considered to be part of syntax. In order to use these characters in
rules, they need to be escaped. Escaping can be done in several ways:

*   Single characters can be escaped using backslash **\\** (U+005C).

*   Strings can be escaped by putting them between single quotes **'like
    this'**.

*   The single quote (ASCII apostrophe) can be quoted using two single quotes
    **''**, both inside and outside single-quote-escaped strings.

### Simple Tailoring Examples

Serbian (Latin) or Croatian: `& C < č <<< Č < ć <<< Ć`

This rule is needed because the root collation order usually considers accents
to have secondary differences in order to base character. This rule ensures that 'ć'
'č' are treated as base letters.

UCA             | Tailoring: `& C < č <<< Č < ć <<< Ć`
--------------- | --------------
CUKIĆ RADOJICA  | CUKIĆ RADOJICA
ČUKIĆ SLOBODAN  | CUKIĆ SVETOZAR
CUKIĆ SVETOZAR  | CURIĆ MILOŠ
ČUKIĆ ZORAN     | CVRKALJ ÐURO
CURIĆ MILOŠ     | ČUKIĆ SLOBODAN
ĆURIĆ MILOŠ     | ČUKIĆ ZORAN
CVRKALJ ÐURO    | ĆURIĆ MILOŠ

Serbian (Latin) or Croatian: `& Ð < dž <<< Dž <<< DŽ`

This rule is an example of a contraction. "D" alone is sorted after "C" and "Ž"
is sorted after "Z", but "DŽ", due to the tailoring rule, is treated as a single
letter that gets sorted after "Đ" and before "E" ("Đ" sorts as a base letter
after "D" in the UCA). Another thing to note in this example is capitalization
of the letter "DŽ". There are three versions, since all three can legally appear
in text. The fourth version "dŽ" is omitted since it does not occur.

UCA      | Tailoring: `& Ð < dž <<< Dž <<< DŽ`
-------- | ---------
dan      | dan
dubok    | dubok
džabe    | đak
džin     | džabe
Džin     | džin
DŽIN     | Džin
đak      | DŽIN
Evropa   | Evropa

Danish: `&V <<< w <<< W`

The letter 'W' is sorted after 'V', but is treated as a tertiary difference
similar to the difference between 'v' and 'V'.

UCA | `&V <<< w <<< W`
--- | ----------------
va  | va
Va  | Va
VA  | VA
vb  | wa
Vb  | Wa
VB  | WA
vz  | vb
Vz  | Vb
VZ  | VB
wa  | wb
Wa  | Wb
WA  | WB
wb  | vz
Wb  | Vz
WB  | VZ
wz  | wz
Wz  | Wz
WZ  | WZ

### Default Options

ICU implements the [LDML collation
options/settings](http://www.unicode.org/reports/tr35/tr35-collation.html#Setting_Options).
For more information see there.

The tailoring inherits all the attribute values from the root collator unless
they are explicitly redefined in the tailoring. The following summarizes
the option settings. Default options are **in emphasis**.

#### alternate
- **`[alternate non-ignorable]`**
- `[alternate shifted]`

Sets the default value of the UCOL_ALTERNATE_HANDLING attribute. If
set to shifted, variable code points will be ignored on the primary level.
For details see the [“Ignore Punctuation” Options](ignorepunct.md) page.

#### maxVariable
- **`[maxVariable punct]`**
- `[maxVariable space]`

Sets the variable top to the top of the specified
reordering group. (New in ICU 53.) All code points with primary weights less
than or equal to the variable top will be considered variable, and thus affected
by the alternate handling.

#### variable top
(deprecated)
- `& X < [variable top]`

Sets the default value for the variable top. All the code points with primary
strengths less than variable top will be considered variable.
*Changing the variable top via this rule syntax is deprecated since ICU 53.*
It has been replaced by the maxVariable option.

#### normalization
- **`[normalization off]`**
- `[normalization on]`

Turns on or off the UCOL_NORMALIZATION_MODE attribute.
If set to on, a quick check and necessary normalization will be performed.

#### strength
- `[strength 1]`
- `[strength 2]`
- **`[strength 3]`**
- `[strength 4]`
- `[strength I]`

Sets the default strength for the collator.

#### backwards
- `[backwards 2]`

Sets the default value of the UCOL_FRENCH_COLLATION attribute. If set to on,
weights on the secondary level will be reversed.

#### caseLevel
- **`[caseLevel off]`**
- `[caseLevel on]`

Turns on or off the UCOL_CASE_LEVEL attribute. If set to on a
level consisting only of case characteristics will be inserted in front of
tertiary level. To ignore accents but take cases into account, set strength to
primary and case level to on.

#### caseFirst
- **`[caseFirst off]`**
- `[caseFirst upper]`
- `[caseFirst lower]`

Sets the value for the UCOL_CASE_FIRST attribute. If set to
upper, causes upper case to sort before lower case. If set to lower, lower case
will sort before upper case. Useful for locales that have an already supported
ordering but require different order of cases. Affects case and tertiary levels.

#### numericOrdering
- **`[numericOrdering off]`**
- `[numericOrdering on]`

Turns on or off the UCOL_NUMERIC_COLLATION attribute. If
set to on, then sequences of decimal digits (gc=Nd) sort by their numeric value.

#### hiraganaQ
(deprecated)
- **`[hiraganaQ off]`**
- `[hiraganaQ on]`

Controls special treatment of Hiragana code points on
quaternary level. If turned on, Hiragana code points will get lower values than
all the other non-variable code points. Strength must be greater or equal than
quaternary if you want this attribute to take effect.
*hiraganaQ is deprecated since ICU 50.* It was an implementation detail of the
Japanese tailoring. In CLDR 25/ICU 53, the Japanese tailoring expresses the
differences between Hiragana and Katakana via explicit quaternary (`<<<<`)
relations.

#### suppressContractions
- `[suppressContractions [Љ-ґ]]`

Removes context-sensitive mappings (contractions and prefix/context-before mappings)
associated with each of the code points in the given UnicodeSet. It works on the
current set of rules: It removes mappings from the root collation as well as
from previous rules.

This is the only way to *remove* mappings: The rule syntax otherwise only adds
and overrides mappings. This special command is used in CLDR tailoring data to
remove Cyrillic root collation contractions that are not necessary in several
languages.

#### optimize
- `[optimize [Ά-ώ]]`

Performance optimization for the code points in the UnicodeSet.
In ICU, where tailoring data only contains the
mappings that are different from the root collation (otherwise the data would be
too large), falling back to root collation mappings for the rest of Unicode is
slightly slower. The optimize command copies mappings for additional characters
into the tailoring data.

#### reorder
followed by one or more reorder codes
- `[reorder Grek Hani space]` 

Reorders scripts relative to each other and relative to a special set of
non-script blocks (space, punctuation, symbol, currency, and digit). The default
order is the same as in the DUCET and in the CLDR root collator.

----

A tailoring that consists only of options is also valid and has the same basic
ordering as the root collation. For example, the Greek tailoring has option
settings only: `[normalization on][reorder Grek]`

(The examples in this chapter might refer to older versions of data for
particular languages. Check CLDR or ICU for actual, current tailorings.)

The following tailoring example reorders uppercase and lowercase and uses
backwards-secondary ordering:

```
[caseFirst upper]
[backwards 2]
& C < č , Č
& G < ģ , Ģ
& I < y, Y
& K < ķ , Ķ
& L < ļ , Ļ
& N < ņ , Ņ
& S < š , Š
& Z < ž , Ž
```

#### Values for Reorder Codes

Reordering Group                         | Rule Value
---------------------------------------- | ----------
Unicode white space characters           | space
Unicode punctuation                      | punct
Unicode symbols except currency symbols  | symbol
Unicode currency symbols                 | currency
Unicode decimal digits                   | digit
Unicode scripts not mentioned ("others") |Zzzz (= Unknown script)

In addition, ISO **4-letter script codes** can be used. Codes for scripts that
do not have Unicode characters (according to the Unicode Script property values)
are ignored.

Limitations of ICU 4.8-52: (Except `Kore` is still not usable because it refers
to multiple scripts that do not sort primary-equal.)

*   For Chinese, use script code `Hani`, *not* `Hans` or `Hant`.
*   For Japanese, use both `Kana` and `Hani` (*not* `Hira`).
*   For Korean, use both `Hang` and `Hani` (*not* `Kore`).

#### Semantics of a List of Reorder Codes

This section is relevant for both the `[reorder ...]` rule syntax and the
`Collator.setReorderCodes()` API.

For an introduction and examples see the section “Script Reordering” in the
[Collation Concepts chapter](../concepts.md).

On the API, the special groups are represented with `Collator.ReorderCode`s
(`UColReorderCode`) values rather than `UScript` (`UScriptCode`) values.

In ICU 4.8-54, not every script could be reordered independently. CLDR and ICU
supported reordering of groups of scripts, each of which started with one of the
[Recommended
Scripts](http://www.unicode.org/reports/tr31/#Table_Recommended_Scripts). A
script that is not Recommended always moved together with the Recommended Script
that precedes it in DUCET order. (Hiragana sorts together with Katakana, Coptic
with Greek, etc.) ICU allowed any one script of a (Recommended Script +
DUCET-following) group in the `[reorder]` list, moving the whole set of scripts
together. However, it was strongly recommended that only Recommended Scripts be
used.

Beginning with ICU 55, scripts only reorder together if they are primary-equal,
for example Hiragana and Katakana.

Zyyy=Common and Zinh=Inherited cannot be reordered.

The special code Zzzz (= Unknown script = `UScript.UNKNOWN` =
`Collator.ReorderCodes.OTHERS` = "others") stands for any script that is not
explicitly mentioned in the list of reordering codes. If Zzzz is mentioned in
the list, then any groups and scripts mentioned later in the list will go at the
very end of the reordering, in the order given. If Zzzz is not mentioned, then
all scripts that are not explicitly listed follow at the end in DUCET order.

The special reorder code `Collator.ReorderCodes.NONE` (= `UScript.UNKNOWN`), when
used alone (same as `[reorder Zzzz]` or not specifying a `[reorder]` rule in a
tailoring), will remove any reordering for this collator. The result of setting
no reordering will be to use the DUCET/CLDR order.

On the API (not applicable to rule syntax), the special reorder code
`Collator.ReorderCodes.DEFAULT` (= `UScript.INHERITED`) will reset the reordering
for the collator to its default order. The default reordering may be the
DUCET/CLDR order or may be a reordering that was specified when this collator
was created from resource data or from rules. The DEFAULT code must be the sole
code supplied when it used.

For details see the [section “Collation Reordering” in the LDML collation
spec](http://www.unicode.org/reports/tr35/tr35-collation.html#Script_Reordering).

### Advanced Syntactical Elements

Several other syntactical elements are needed in more specific situations.

#### Order before

- Syntax: `[before 1|2|3]`
- Example: `&[before 2]a<ā<á<ǎ<à`

Enables users to order characters **before **a given character. In UCA 3.0, the
example is equivalent to & ㍡<ā<á<ǎ<à (㍡= \\u3361, ideographic telegraph symbol
for hour nine) and makes accented 'a' letters sort before 'a'. Accents are often
used to indicate the intonations in Pinyin. In this case, the non-accented
letters sort after the accented letters.

#### Expansion

- Syntax: `/`
- Example: `æ/e`

Adds the collation element for 'e' to the collation element for æ.
After a reset `&ae << æ` is equivalent to `&a << æ/e`. See the Expansion example
below.

#### Prefix processing

- Syntax: `|`
- Example: `a|b`

If 'b' is encountered and it follows 'a',
output the appropriate collation element. If 'b' follows any other letter,
output the normal collation element for 'b'.
The collation element for 'a' is not affected.

This element is used to speed up sorting under JIS X 4061. See the
Prefix example below.

#### Reset to top

- Syntax: `[top]`
- Example: `&[top] < a < b < c …`

**Deprecated, use indirect positioning instead**
(`&[last regular]`, see section below)
Reorders a set of characters 'above' the UCA. `[top]` is a virtual code point having the
biggest primary weight value that will ever be assigned in the UCA. Above top,
there is a large number of unassigned primary weights that can be used for a
'large' tailoring, such as the reordering of the CJK characters according to a
Far Eastern code page. The first difference after the top is always primary.

### Indirect Positioning of Collation Elements

Since ICU version 2.0, ICU allows for indirect positioning of collation elements
(CE). Similar to the reset anchor `top`, these reset anchors allow for positioning of the
tailoring relative to significant sections of the UCA table. You can use the
`[before]` reset option to position before these sections.

Name                      | Example CE value  | Note
------------------------- | ----------------- | ------------
first tertiary ignorable  | `[,,]`            | Start of the UCA table. This value will never change unless CEs are extended with higher level values.
last tertiary ignorable   | `[,,]`            | This value will never change unless CEs are extended with higher level values.
first secondary ignorable | `[,, 05]`         | Currently there are no secondary ignorables in the UCA table.
last secondary ignorable  | `[,, 05]`         | Currently there are no secondary ignorables in the UCA table.
first primary ignorable   | `[, 87, 05]`      | Mostly for non-spacing combining marks.
last primary ignorable    | `[, E1 B1, 05]`   | Currently this value points to a non-existing code point, used to facilitate sorting of compatibility characters.
first variable            | `[05 07, 05, 05]` | The lowest CE that is not primary-ignorable. (see below)
last variable             | `[17 9B, 05, 05]` | End of variable section.
first regular             | `[1A 20, 05, 05]` | This is the first regular CE (not primary ignorable and not variable). The majority of code points have regular CEs.
last regular              | `[78 AA B2, 05, 05]` | Use `&[last regular]` instead of `&[top]`. (see below)
first implicit            | `[E0 03 03, 05, 05]` | Section of implicitly generated collation elements. (see below)
last implicit             | `[E3 DC 70 C0, 05, 05]` | End of implicit section. This is the CE of the last unassigned code point (U+10FFFD). (see below)
first trailing            | `[E5, 05, 05]`    | Start of trailing section. (see below)
last trailing             | `[FF FF, 05, 05]` | End of trailing collation elements section. This is the highest possible CE, and is the CE for U+FFFF. Not available for tailoring, see `[first trailing]`.

"first variable": The current code point is TAB=U+0009. This is the start of the variable section. "Variable" characters will be ignored on primary/secondary/tertiary levels when the "shifted" option is on.

Tailoring after "last regular" will effectively position characters
between regular code points and "implicit" CEs (the next section).
This should be used (only) for tailoring Han characters
which tends to affect thousands of characters.
The script reordering implementation assumes that CEs in this section
are for "Hani" script characters.

"Implicit" means that the UCA default ordering table (DUCET)
does not explicitly specify CEs for CJK ideographs and unassigned code points;
instead, their CEs are computed at runtime.

Beginning with ICU 53, tailoring to any unassigned code point,
including "last implicit", is not supported any more.

"trailing": Tailoring characters after `[first trailing]`
makes them sort after all other non-tailored code points except for U+FFFD and U+FFFF.

The "trailing" section is reserved for future use, such as for non starting Jamos. See
<http://www.unicode.org/reports/tr10/#Trailing_Weights>.
CLDR 1.9/ICU 4.6 and later map U+FFFF to the very end of the trailing section.
UCA 6.3/CLDR 24/ICU 52 and later map U+FFFD to just before U+FFFF.
U+FFFD..U+FFFF are not tailorable, and nothing can tailor to them.
<http://www.unicode.org/reports/tr35/tr35-collation.html#tailored_noncharacter_weights>

Before ICU 4.6, U+FFFF mapped to a completely ignorable CE, and `[last trailing]`
was the same as `[first trailing]`.

Not all of the indirect-positioning anchors are useful. Most of the 'first'
elements should be used with the `[before]` directive, in order to make sure
that your tailoring will sort before an interesting section.

### Complex Tailoring Examples

The following are several fragments of real tailorings, illustrating some of the
advanced syntactical elements:

#### Expansion Example:

**Swedish:**
```
&t<<<þ/h
&T<<<Þ/H
```

The letter 'þ' (THORN) is normally treated by UCA/root collation as a separate
letter that has primary-level sorting after 'z'. However, in Swedish and some
other Scandinavian languages, 'þ' and 'Þ' should be treated as just a
tertiary-level difference from the letters "th" and "TH" respectively. This is
an example of an expansion.

UCA | `&t<<<þ/h, &T<<<Þ/H`
--- | --------------------
az  | az
Az  | Az
tha | tha
Tha | þa
THa | Tha
thz | THa
za  | Þa
Za  | thz
zz  | þz
þa  | za
Þa  | Za
þz  | zz

#### Prefix Example:

Prefixes are used in Japanese tailorings to reduce the number of contractions. A
big number of contractions is a performance burden on the commonly-used base
characters, as their processing is much more complicated than the processing of
regular elements.

A prefix rule conditionally changes the CE of the character or string (e.g., ー)
after the | symbol; unlike a contraction, it does not affect the CE of the
preceding text (e.g., ァ). (By contrast, a contraction like ァー consumes both
characters and can assign them a CE or expansion unrelated to ァ's CE.) A prefix
rule is especially useful if the character or string (ー) after the | symbol
occurs significantly less often than the first character of the prefix (ァ).

```
&[before 3]ァ <<< ァ|ー = ｧ|ー = ぁ|ー
```

This could have been written as a series of contractions followed by expansion:

```
&[before 3]ァー <<< ァー = ｧー = ぁー
```

However, in that case ァ, ｧ and ぁ would start contractions. Since the prolonged
sound mark (ー) occurs much less frequently than the other letters of Japanese
Katakana and Hiragana, it is much more prudent to put the extra processing on it
by using prefixes.

#### Reset example:

A "reset" always uses only the base character as the insertion point even if
there is an expansion. So the following rule,

```
& J <<< K / B & K <<< M
```

is equivalent to

```
& J <<< K / B <<< M
```

Which produces the following sort order:

"JA"

"MA"

"KA"

"KC"

"JC"

"MC"

> :point_right: **Note**: Assuming the letters "J", "K" and "M" have equal primary weights, the second
> letter contains the differences among these strings. However, the letter "K" is
> treated as if it always has a letter "B" following it while the letters "J" and
> "M" do not.

The following is an example of collation elements for these strings resulting
from the specified rules:

Strings | Collation Elements | &nbsp;         | &nbsp;
------- | ------------------ | -------------- | ------
"JA"    | `[005C.00.01]`     | `[0052.00.01]` |
"MA"    | `[005C.00.03]`     | `[0052.00.01]` |
"KA"    | `[005C.00.02]`     | `[0053.00.01]` | `[0052.00.01]`
"KC"    | `[005C.00.02]`     | `[0053.00.01]` | `[0054.00.01]`
"JC"    | `[005C.00.01]`     | `[0054.00.01]` |
"MC"    | `[005C.00.03]`     | `[0054.00.01]` |

## Tailoring Issues

ICU uses canonical closure. This means that for each code point in Unicode, if
the canonically composed form of a tailored string produces different collation
elements than the canonically decomposed form, then the canonically composed
form is effectively added to the ordering. If 'a' is tailored, for example, all
of the accented 'a' characters are also tailored. Canonical closure allows
collators to process Unicode strings in the FCD form as well as in NFD. (Note:
Most but not all NFC strings are also in FCD. See
<http://www.unicode.org/notes/tn5/#FCD>)

However, *compatibility* equivalents are NOT automatically added. If the rule
"&b < a" is in tailoring, and the order of **ⓐ (circled a)** is important, it
needs to be tailored **explicitly**.

Redundant tailoring rules are removed, with later rules "winning". The strengths
around the removed rules are also fixed.

### Example:

The following table summarizes effects of different redundant rules.

&nbsp; | Original                                                  | Equivalent
------ | --------------------------------------------------------- | ----------
1      | `& a < b < c < d` `& r < c`                               | `& a < b < d` `& r < c`
2      | `& a < b < c < d` `& c < m`                               | `& a < b < c < m < d`
3      | `& a < b < c < d` `& a < m`                               | `& a < m < b < c < d`
4      | `& a <<< b << c < d` `& a < m`                            | `& a <<< b << c < m < d`
5      | `& a < b < c < d` `& [before 1] c < m`                    | `& a < b < m < c < d`
6      | `& a < b <<< c << d <<< e` `& [before 3] e <<< x`         | `& a < b <<< c << d <<< x <<< e`
7      | `& a < b <<< c << d <<< e` `& [before 2] e <<< x`         | `& a < b <<< c <<< x << d <<< e`
8      | `& a < b <<< c << d <<< e` `& [before 1] e <<< x`         | `& a <<< x < b <<< c << d <<< e`
9      | `& a < b <<< c << d <<< e <<< f < g` `& [before 1] g < x` | `& a < b <<< c << d <<< e <<< f < x < g`

If two different reset lists tailor the same character, then it is removed from the first
one (see 1 in the table above).
If the second list resets to a character tailored in the first list, then the second
list is inserted in the first (see 2).
If both lists reset to the same character, then the same thing
happens (see 3). Whenever such an insertion occurs, the second strength
"postpones" the position (see 4).

If there is a `[before N]` on the reset, then the reset character is
effectively replaced by the item that would be before it, either in a previous
tailoring (if the letter occurs in one - see 5) or in the UCA. The N determines
the 'distance' before, based on the strength of the difference (see 6-8).
However, this is subject to postponement (see 9), so be careful!

### Reset semantics

The reset semantic in ICU 1.8 and above is different from the previous ICU
releases. Prior to version 1.8, the reset relation modifier was applicable only
to the entry immediately following the reset entry. Also, the relation modifier
applied to all entries that occurred until the next reset or primary relation.

For example,

```
&xyz << e <<< f
```

was equivalent to

```
&x << e/yz <<< f
```

prior to ICU version 1.8.

Starting with ICU version 1.8, the modifier is equivalent to

```
&x << e/yz <<< f/yz
```

The new semantic produces more intuitive results, especially when the character
after the reset is decomposable. Since all rules are converted to NFD before
they are interpreted, this can result in contractions that the rule-writer might
not be aware of. Expansion propagates only until the next reset or primary
relation occurs.

For example, the following rule:

```
&ab = c <<< d << e <<< f < g <<< h
```

was equivalent to the following prior to ICU 1.8 and in Java:

```
&a = c/b <<< d << e <<< f < g <<< h
```

Starting with 1.8, it is equivalent to

```
&a = c / b <<< d / b << e / b <<< f / b < g <<< h
```

## Known Limitations

The following are known limitations of the ICU collation implementation. These
are theoretical limitations, however, since there are no known languages for
which these limitations are an issue. However, for completeness they should be
fixed in a future version after 1.8.1. The examples given are designed for
simplicity in testing, and do not match any real languages.

### Expansion

The goal of expansion is to sort as if the expansion text were inserted right
after the character. For example, with the rule

```
&a <<< c / e
```

The text "...**c**..." should sort as if it were right after "...**ae**..." with
a tertiary difference. There are a few cases where this is not currently true.

#### Recursive Expansion

Given the rules

```
&a <<< c / e
&g <<< e / I
```

Expansion should sort the text "...**c**..." as if it were just after
"...**ae**...", and that should also sort as if it were just after
"...**agi**...". This requires that the compilation of expansions be recursive
(and check for loops as well!). ICU currently does not do this.

Rules         | Desired Order | Current Order
------------- | ------------- | -------------
`& a = b / c` | add           | b
`& d = c / e` | b             | add
&nbsp;        | adf           | adf

#### Contractions Spanning Expansions

ICU currently always pre-compiles the expansion into an internal format (a list
of one or more collation elements) when the rule is compiled. If there is a
contraction that spans the end of the expanded text and the start of the
original text, however, that contraction will not match. A text case that
illustrates this is:

Rules           | Desired Order | Current Order
--------------- | ------------- | -------------
`& a <<< c / e` | ad            | ad
`& g <<< eh`    | c             | c
&nbsp;          | af            | ch
&nbsp;          | g             | af
&nbsp;          | ch            | g
&nbsp;          | h             | h

Since the pre-compiled expansions are a huge performance gain, we will probably
keep the implementation the way it is, but in the future allow additional syntax
to indicate those few expansions that need to behave as if the text were
inserted because of the existence of another contraction. Note that such
expansions need to be recursively expanded (as in #1), but rather than at
pre-compile time, these need to be done at runtime.

While it is possible to automatically detect these cases, it would be better to
allow explicit control in case spanning is not desired. An example of such
syntax might be something like:

```
&a <<< c // e
```

**Notes:** ICU does handle the case where there is a contraction that is
completely inside the expansion.

Suppose that someone had the rules:

```
&a = c / e
&x = ae
```

These do not cause **c** to sort as if it were **ae**, nor should they.

### Normalization

The Unicode Collation Algorithm specifies that all text sort as if it were first
normalized into NFD. For performance reasons, ICU collation data is
pre-processed so that there is no need to perform normalization on strings that
are in [FCD](http://www.unicode.org/notes/tn5/#FCD) and do not contain any composite
combining marks. Composite combining marks are: { U+0344, U+0F73, U+0F75, U+0F81
}
[`[[:^lccc=0:]&[:toNFD=/../:]]`](http://www.unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B%3A%5Elccc%3D0%3A%5D%26%5B%3AtoNFD%3D%2F..%2F%3A%5D&abb=on&g=)
(These characters must be decomposed for discontiguous contractions to work
properly. Use of these characters is discouraged by the Unicode Standard.). The
vast majority of strings are in this form.

#### Nulls in Contractions

Nulls should not be used in contractions that could invoke normalization.

Rules                | Desired Order | Current Order
-------------------- | ------------- | -------------
`& a <<< '\u0000'^`  | a             | '\\u0000'^
&nbsp;               | '\\u0000'^    | a

#### Contractions Spanning Normalization

The following rule specifies that a grave accent followed by a **b** is a
contraction, and sorts as if it were an **e**.

```
& e <<< ` b
```

On this basis, "...àb..." should sort as if it were just after "...ae...".
Because of the preprocessing, however, the contraction will not match if this
text is represented with the pre-composed character à, but **will** match if
given the decomposed sequence **a + grave accent**. The same thing happens if
the contraction spans the start of a normalized sequence.

Rules        | Desired Order | Current Order
------------ | ------------- | -------------
& e <<< \` b | à             | à
&nbsp;       | ad            | àb
&nbsp;       | àb            | ad
&nbsp;       | af            | af
&nbsp;       | &nbsp;        |
`& g <<< ca` | f             | cà
&nbsp;       | ca            | f
&nbsp;       | cà            | ca
&nbsp;       | h             | h

### Variable Top

ICU lets you set the top of the variable range. This can be done, for example,
to allow you to ignore just SPACES, and not punctuation.

#### Variable Top Exclusion

There is currently a limitation that causes variable top to (perhaps) exclude
more characters than it should. This happens if you not only set variable top,
but also tailor a number of characters around it with primary differences. The
exact number that you can tailor depends on the internal "gaps" between the
characters in the pre-compiled UCA table. Normally there is a gap of one. There
are larger gaps between scripts (such as between Latin and Greek), and after
certain other special characters. For example, if variable top is set to be at
SPACE ('\\u0020'), then it works correctly with up to 70 characters also
tailored after space. However, if variable top is set to be equal to HYPHEN
('\\u2010'), only one other value can be accommodated.

In the following, the goal is for x to be ignored and z not to be ignored.

Rules              | Desired Order SHIFTED = ON | Current Order
------------------ | -------------------------- | -------------
`& \u2010`         | -                          | -
`< x`              | z                          | z
`< [variable top]` | zb                         | zb
`< z`              | a                          | xb
&nbsp;             | b                          | a
&nbsp;             | -b                         | b
&nbsp;             | xb                         | -b
&nbsp;             | c                          | c

> :point_right: **Note**: With ICU 1.8.1, the
> user is advised not to tailor the variable top to customize more than two
> primary relations (for example, `"& x < y < [variable top]"`). Starting in ICU
> 2.0, setVariableTop() allows the user to set the variable top programmatically
> to a legal single character or a valid contracting sequence. In addition, the
> string that variable top is set to should not be treated as either inclusive or
> exclusive in the rules.

### Case Level/First/Second

In ICU, it is possible to override the tertiary settings programmatically. This
is used to change the default case behavior to be all upper first or all lower
first. It can also be used for a separate case level, or to ignore all other
tertiary differences (such as between circled and non-circled letters, or
between half-width and full-width katakana). The case values are derived
directly from the Unicode character properties, and not set by the rules.

#### Mixed Case Contractions

There is currently a limitation that all contractions of multiple characters can
only have three special case values: upper, lower, and mixed. All mixed-case
contractions are grouped together, and are not affected by the upper first vs.
lower first flag.

Rules      | Desired Order UPPER_FIRST | Current Order
---------- | ------------------------- | -------------
`& c < ch` | C                         | c
`<<< cH`   | CH                        | CH
`<<< Ch`   | Ch                        | cH
`<<< CH`   | cH                        | Ch
&nbsp;     | ch                        | ch

## Building on Existing Locales

All of the collation rules are additive; that is, they override what any
previous rule expressed. That means that you can build on existing rules for
given locales. Here is an example of this, which fetches the rules for a
particular locale (Danish), then overrides some part (sorting '%' after 'm').
The syntax is Java, but C/C++ has similar features.

```java
ULocale myLocale = new ULocale("da");
try {

    RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(myLocale);
    String rules = col.getRules();
    String myRules = "& m < '%'";
    RuleBasedCollator col2 = new RuleBasedCollator(rules + myRules);

    // check the values

    List<String> expected = Arrays.asList("a;m;%;z;aa".split(";"));
    TreeSet<String> sorted = new TreeSet<String>(col2);
    sorted.addAll(expected);
    ArrayList<String> actual = new ArrayList<String>(sorted);
    assertEquals("Customized rules with %", expected, actual);

} catch (Exception e) {
    throw new IllegalArgumentException("Failed to create customized rules", e);
}
```

The root collator has an empty rules string (`getRules()` returns `""`): Any
collator's tailoring rules string defines how a collator *differs* from the root
collator, and the tailoring rules string was the input for building the
tailoring collator. By contrast, the root collator itself is built from a file
with explicit mappings (ICU4C source/data/unidata/FractionalUCA.txt)
from characters/contractions to collation elements. This file represents the
[DUCET](http://www.unicode.org/reports/tr10/#Default_Unicode_Collation_Element_Table)
as [modified by
CLDR](http://www.unicode.org/reports/tr35/tr35-collation.html#Root_Collation).

There are "extended" versions of `getRules()` which, when called with
`delta=UCOL_FULL_RULES` (C/C++) or `fullrules=true` (Java), return "full rules"
which are a concatenation of the "UCA rules" and the collator's tailoring. The
"UCA rules" are published as UCA_Rules.txt in every [UCA
release](http://www.unicode.org/Public/UCA/).

*   "UCA rules" is a historical misnomer. The UCA specifies an Algorithm which
    applies to all collators, and provides the DUCET as its Default table.
*   ICU's root collator implements the CLDR-modified collation element table.
    The "UCA rules" returned from ICU functions are equivalently modified rules
    compared with those for the DUCET.

The "UCA rules" are an *approximation* of the root collator's sort order, but
there are some differences because not all of the details of the root collator
mappings can be expressed in rule syntax. In particular, a collator built from
ICU4C source/data/unidata/UCARules.txt
has at least the following issues compared with the real root collator:

*   inefficient (long) collation element weights
*   CODAN (numeric collation) will not work (the 0 digit's primary weight is
    hardcoded, or specified in FractionalUCA.txt)
*   script reordering will not work
*   alternate=shifted will not work
*   the sort order has some differences from the regular root collator,
    including additional tertiary differences

The "full rules" are almost never used, or useful, at runtime. They are included
in ICU for historical reasons and for UCA consistency tests. They might be
usable for emulating the CLDR/ICU sort order with a collation implementation not
based on CLDR/ICU.

Collation rule strings in general are not commonly used but are a significant
portion of the data size in ICU collation resource bundles, especially for CJK
languages. The rule strings can be omitted from those resource bundles by adding
the `--omitCollationRules` option to the relevant `genrb` invocations
(for ICU 53..63, in icu4c/source/data/Makefile.in)
or, since ICU 64, with a [data filter config file](../../icu_data/buildtool.md).
(See for example the relevant
[ICU integration test instructions](https://icu.unicode.org/processes/release/tasks/integration#TOC-Verify-that-ICU4C-tests-pass-without-collation-rule-strings).)

If the tailoring rules are needed but the 150kB or so of "UCA rules" are not,
then the line

```
UCARules:process(uca_rules){"../unidata/UCARules.txt"}
```

in
[source/data/coll/root.txt](https://github.com/unicode-org/icu/blob/main/icu4c/source/data/coll/root.txt)
can be commented out or deleted.

## Cautions

The following are not known rule limitations, but rather cautions.

### Resets

Since resets always work on the existing state, the user is required to make
sure that the rule entries are in the proper order.

Rules     | Order | Comment
--------- | ----- | -------
`& a < b` | a     | The rules mean: put **b** after **a**, then put **c** after **a** (inserting **before** the **b**).
`& a < c` | c     |
&nbsp;    | b     |

### Postpone Insertion

When using a reset to insert a value X with a certain strength difference after
a value Y, it actually is inserted just before the next item of the same
strength or higher following Y. Thus, the following are equivalent:

```
... m < a = c <<< d << e <<< f < g <<< h & a << x
... m < a = c <<< d << x << e <<< f < g <<< h
```

> :point_right: **Note**: This is different from the Java semantics.
> In Java, the value is inserted immediately after the reset character.

### Jamo Tailoring

If Jamo characters are tailored, that causes the code to go through a slow path,
which will have a significant effect on performance.

### Compatibility Decompositions

When tailoring a letter, the customization affects all of its canonical
equivalents. That is, if tailoring rule sorts an **'a'** after**'e '**, for
example, then "**"à", "á", ...** are also sorted after '**e**'.his is not true
for compatibility equivalents. If the desired sorting order is for a
**superscript-a** ("ª") to be after "**e"**, it is necessary to specify the rule
for that.

### Case Differences

Similarly, when tailoring an "**a" to be sorted** after "**e"**, including
"**A"** to be after "**e" **as well, it is required to have a specific rule for
that sorting sequence.

### Automatic Expansions

ICU will automatically form expansions whenever a reset is to a multi-character
value that is not a contraction. For example, `& ab <<< c` is equivalent to
`& a <<< c / b`. The user may be unaware of this happening, since it may not be
obvious that the reset is to a multi-character value. For example, `& à<<< d` is
equivalent to & a <<< d / \`
