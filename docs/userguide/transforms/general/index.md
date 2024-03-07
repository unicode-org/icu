---
layout: default
title: Transforms
nav_order: 4
parent: Transforms
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# General Transforms
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

General transforms provide a general-purpose package for processing Unicode
text. They are a powerful and flexible mechanism for handling a variety of
different tasks, including:

1.  Uppercase, Lowercase, Titlecase, Full/Halfwidth conversions
2.  Normalization
3.  Hex and Character Name conversions
4.  Script to Script conversion

Originally, Transforms were designed to convert characters from one script to
another (for example, from Greek to Latin, or Japanese Katakana to Latin). This
is still reflected in the class name, which remains **Transliterator**. However,
the services performed by that class now represent a much more general mechanism
capable of handling a much broader range of tasks. In particular, the Transforms
include pre-built transformations for case conversions, for normalization
conversions, for the removal of given characters, and also for a variety of
language and script transliterations. Transforms can be chained together to
perform a series of operations and each step of the process can use a UnicodeSet
to restrict the characters that are affected.

For example, to remove accents from characters, use the following transform:

```
NFD; [:Nonspacing Mark:] Remove; NFC.
```

This transform separates accents from their base characters, removes the
accents, and then puts the remaining text into an unaccented form.

A transliteration either can be applied to a complete string of text or can be
used incrementally for typing or buffering input. In the latter case, the
transform provides the correct time delay to process characters when there is an
unambiguous mapping. Transliterators can also be used with more complex text,
such as styled text, to maintain the style information where possible. For
example, "~~Αλφaβ~~ητικός" will retain the strikethrough in transliterating to
"~~Alphab~~ētikós".

> :point_right: **Note**: *The transliteration process not only retains font size, but also other
characteristics such as font type and color.*

For an online demonstration of ICU transliteration, see
<https://icu4c-demos.unicode.org/icu-bin/translit> .

## Script Transliteration

Script Transliteration is the general process of converting characters from one
script to another. For example, it can convert characters from Greek to Latin,
or Japanese katakana to Latin. The user must understand that script
transliteration is not translation. Rather, script transliteration it is the
conversion of letters from one script to another without translating the
underlying words. The following shows a sample of script transliteration:

| Source | Transliteration |
|---|---|
| キャンパス | kyanpasu |
| Αλφαβητικός Κατάλογος | Alphabētikós Katálogos |
| биологическом | biologichyeskom |

> :point_right: **Note**: *Some of the characters may not be
visible on the screen unless you have a Unicode font with all the Greek letters.
If you have a licensed copy of Microsoft® Office, you can use the "Arial Unicode
MS" font, or you can download the [CODE2000](http://www.code2000.net/) font for
free. For more information, see [Display
Problems?](http://www.unicode.org/help/display_problems.html) on the Unicode web
site.*

While the user may not recognize that the Japanese word "kyanpasu" is equivalent
to the English word "campus," it is easier to recognize and interpret the word
in text than if the letters were left in the original script. There are several
situations where this transliteration is especially useful. For example, when a
user views names that are entered in a world-wide database, it is extremely
helpful to view and refer to the names in the user's native script. It is also
useful for product support. For example, if a service engineer is sent a program
dump that is filled with characters from foreign scripts, it is much easier to
diagnose the problem when the text is transliterated and the service engineer
can recognize the characters. Also, when the user performs searching and
indexing tasks, transliteration can retrieve information in a different script.
The following shows these retrieval capabilities:

| Source | Transliteration |
|---|---|
| 김, 국삼 | Gim, Gugsam |
| 김, 명희 | Gim, Myeonghyi |
| 정, 병호 | Jeong, Byeongho |
| ... | ... |
| たけだ, まさゆき | Takeda, Masayuki |
| ますだ, よしひこ | Masuda, Yoshihiko |
| やまもと, のぼる | Yamamoto, Noboru |
| ... | ... |
| Ρούτση, Άννα | Roútsē, Ánna |
| Καλούδης, Χρήστος | Kaloúdēs, Chrḗstos |
| Θεοδωράτου, Ελένη | Theodōrátou, Elénē |

Transliteration can also be used to convert unfamiliar letters within the same
script, such as converting Icelandic THORN (þ) to th.

## Transliterator Identifiers

Transliterators are not created directly using C++ or Java constructors.
Instead, the are created by giving an **identifier**—a name string in a specific
format—to one of the Transliterator factory methods, such as
`Transliterator.getInstance()` (Java) or `Transliterator::createInstance()`. The
following are some examples of identifiers:

1.  `Latin-Cyrillic`
2.  `[:Lu:] Latin-Greek (Greek-Latin/UNGEGN)`
3.  `[A-Za-z]; Lower(); Latin-Katakana; Katakana-Hiragana; ([:Hiragana:])`

It is important to understand identifiers and their syntax, since it is through
the use of identifiers that one creates transforms, restricts their effective
range, and combines them together. This section describes transform identifiers
in detail. Throughout this section, it is important to distinguish between
**identifiers** and the **actual transforms** that they refer to. All actual
transforms are named by well-formed identifiers, but not all well-formed
identifiers refer to actual transforms. An analogy is C++ method names. I can
write the syntactially well-formed method name "void
Cursor::getPosition(Position& pos)", but whether or not this refers to an actual
method in an actual class is a different matter.

### Basic IDs

The simplest identifier is a 'basic ID'.

```
basicID := (<source> "-")? <target> ("/" <variant>)?
```

A basic ID typically names a source and target. In "Katakana-Latin", "Katakana"
is the source and "Latin" is the target. The source specifier describes the
characters or strings that the transform will modify. The target specifier
describes the result of the modification. If the source is not given, then the
source is "Any", the set of all characters. Source and Target specifiers can be
[Script IDs](http://www.unicode.org/cldr/utility/properties.jsp#Script) (long like
"Latin" or short like "Latn"), [Unicode language
Identifiers](http://www.unicode.org/reports/tr35/#Unicode_Language_and_Locale_Identifiers)
(like fr, en_US, or zh_Hant), or special tags (like Any or Hex). For example:

1.  Katakana-Latin
2.  Null
3.  Hex-Any/Perl
4.  Latin-el
5.  Greek-en_US/UNGEGN

Some basic IDs contain a further specifier following a forward slash. This is
the variant, and it further specifies the transform when several versions of a
single transformation are possible. For example, ICU provides several transforms
that convert from Unicode characters to escaped representations. These include
standard Unicode syntax "U+4E01", Perl syntax "\\x{4E01}", XML syntax
"\&#x4E01;", and others. The transforms for these operations are named
"Any-Hex/Unicode", "Any-Hex/Perl", and "Any-Hex/XML", respectively. If no
variant is specified, then the default variant is selected. In the example of
"Any-Hex", this is the Java variant (for historical reasons), so "Any-Hex" is
equivalent to "Any-Hex/Java".

### Filtered IDs

A filtered IDs is a basic IDs constrained by a filter. For example, to specify a
transform that converts only ASCII vowels to uppercase, use the ID:

```
[aeiou] Upper
```

The filter is a valid UnicodeSet pattern prefixed to the basic ID. Only
characters within the set will be modified by the transform. Some transforms are
only useful with filters, for example, the Remove transform, which deletes all
input characters. Specifying `"[:Nonspacing Mark:] Remove"` gives a transform
that removes non-spacing marks from input text.

> :point_right: **Note**: *As of ICU 2.0, the filter pattern must be enclosed in brackets. Perl-syntax
patterns of the form `"\p{Lu}"` cannot be used directly; instead they must be
enclosed, e.g. `"[\p{Lu}]"`.*

### Inverses

Any transform ID can be modified to form an "inverse" ID. This is the ID of a
related transform that performs an inverse operation. For basic IDs, this is
done by exchanging the source and target names. For example, the inverse of
"Latin-Greek/UNGEGN" is "Greek-Latin/UNGEGN", and vice versa. The variant, if
any, is unaffected.

If there is no named source, the same rule still applies, using the implicit
source "Any". So the inverse of "Hex/Perl" is "Hex-Any/Perl", since the former
is really shorthand for "Any-Hex/Perl".

The notion of inverses carries two important caveats. The first involves the
semantics of inverses. Consider a transform "A-B". Its inverse, "B-A", is
thought of as reversing the transformation accomplished by "A-B". The degree and
completeness of the reversal, however, is not guaranteed.

For example, consider the "Lower" transform. It has an inverse of "Upper" (this
is a special, non-standard inverse relationship that the transliteration service
knows about). Applying "Lower" to the string "Hello There" yields the string
"hello there". Applying "Upper" to this result then yields "HELLO THERE", which
is not the same as the original string.

Complete and exact reversal **is** possible if the transform has been explicitly
designed to support this. Examples of transforms that support this are "Any-Hex"
and "SCRIPT-Latin", where SCRIPT is a supported transliteration script. The
"SCRIPT-Latin" transforms support exact reversal of well-formed text in SCRIPT
to Latin (via "SCRIPT-Latin") and back to SCRIPT (via "Latin-SCRIPT"). This is
called **round-trip integrity**. They do not, however, support round-trip
integrity from Latin to SCRIPT and back to Latin.

> :point_right: **Note**: *Do not assume that a transform's inverse will provide a complete or exact
reversal.*

The second caveat with inverses has to do with existence. Although any ID can be
inverted, this does not guarantee that the inverse ID actually exists. For
example, if I create a custom translitertor `Latin-Antarean` and register it with
the system, I can then pass the string "Latin-Antarean" to `createInstance()` or
`getInstance()` to get that transform. If I then ask for its inverse, however, the
request will fail, since I have not created and registered "Antarean-Latin" with
the system.

> :point_right: **Note**: *Any transform ID can be inverted, but the inverse ID may not name an actual
registered transform.*

### Custom Inverses

Consider the transforms "Any-Lower" and "Any-Upper": It is convenient to
associate these as inverses of one another. However, using the standard
procedure for ID inversion on "Any-Lower" yields "Lower-Any", which is not what
we want. To override the standard ID inversion, the inverse ID can be explicitly
stated within the ID string as follows:

`"Any-Lower (Any-Upper)"` or equivalently `"Lower (Upper)"`

When this ID is inverted, the result is "Any-Upper (Any-Lower)". Using this
mechanism, the user can form arbitrary inverse relations when necessary.

When using custom inverses of the form "A-B (C-D)", either "A-B" or "C-D" may be
empty. An empty element is the same as "Null". That is, "A-B ( )" is the same as
"A-B (Null)", and it inverts to the null transform (which does nothing). The
null transform it inverts to has the ID "(A-B)", also written "Null (A-B)", and
inverts back to "A-B ( )". Note that "A-B ( )" is very different from both "A-B"
and "(A-B)":

| ID | Inverse of ID |
|---|---|
| A-B | B-A |
| A-B ( ) | (A-B) |
| (A-B) | A-B ( ) |

For some system transforms, special inverse mappings exists automatically. These
mappings are symmetrical, that is, the right column is the inverse of the left
column, and vice versa. The mappings are:

| | |
|---|---|
| Any-Null | Any-Null |
| Any-NFD | Any-NFC |
| Any-NFKD | Any-NFKC |
| Any-Lower | Any-Upper |

In other words, writing "Any-NFD" is exactly equivalent to writing "Any-NFD
(Any-NFC)" since the system maps the former to the latter internally. However,
one can still alter the mapping of these transforms by specifying an explicit
custom inverse, e.g. "NFD (Lower)".

### Compound IDs

Transliterators are often combined in sequence to achieve a desired
transformation. This is analogous to the composition of mathematical functions.
For example, given a script that converts lowercase ASCII characters from Latin
script to Katakana script, it is convenient to first (1) separate input base
characters and accents, and then (2) convert uppercase to lowercase. (Katakana
is caseless, so it is best to write rules that operate only on the lowercase
Latin base characters and produce corresponding Katakana.) To achieve this, a
**compound transform** can be specified as follows:

```
NFKD; Lower; Latin-Katakana;
```

(In real life, we would probably use "NFD", but we use "NFKD" for explanatory
purposes here.) It is also desirable to modify only Latin script characters. To
do so, a filter may be prefixed to the entire compound transform. This is called
a **global filter** to distinguish it from filters on the individual transforms
within the compound:

```
[:Latin:]; NFKD; Lower; Latin-Katakana;
```

The inverse of such a transform is formed by reversing the list and inverting
each element. In this example, this would be:

```
Katkana-Latin; Upper; NFKC; ([:Latin:]);
```

Note that two special mappings take effect: "Lower" to "Upper" and "NFKD" to
"NFKC". Note also that the global filter is enclosed in parentheses, rendering
it inoperative in the reverse direction.

In this example we probably don't really want to map Latin characters to
uppercase in the reverse direction, so we need to modify the original transform
as follows:

```
[:Latin:]; NFKD; Lower(); Latin-Katakana;
```

Recall that the empty parentheses in "Lower ( )" are shorthand for "Lower
(Null)" where "Null" is the null transform, that is, the transform that leaves
text unchanged. The inverse of this is "Null (Lower)", also written "(Lower)".
Now the inverse of the entire compound is:

```
Katakana-Latin; (Lower); NFKC; ([:Latin:]);
```

This still isn't quite right, since we really want to recompose our output, in
both directions. We also want to only touch Katakana characters in the reverse
direction. Our final example, modified to address these two concerns, is as
follows:

```
[:Latin:]; NFKD; Lower(); Latin-Katakana; NFC; ([:Katakana:]);
```

This inverts to:

```
[:Katakana:]; NFD; Katakana-Latin; (Lower); NFKC; ([:Latin:]);
```

(In real life, we would probably use only "NFD" and "NFC", but we use the
compatibility normalizers in this example so they can be distinguished.)

Compound IDs are the most complex identifiers that can be formed. Many system
transforms are actually compound transforms that have been aliased to basic IDs.
It is also possible to write a transform rule with embedded instructions for
generating a compound transform; system transforms use this approach as well.

### Formal ID Syntax

Here is a formal description of the identifier syntax. The 'ID' entity can be
passed to `getInstance()` or `createInstance()`.

| ID | := Single_ID \| Compound_ID |
|---|---|
| Single_ID | := filter? Basic_ID ( '(' Basic_ID? ')' )? \| filter? '(' Basic_ID ')'
| Compound_ID | := ( filter ';' )? ( Single_ID ';' )+ ( '(' filter ');' )?
| Basic_ID | := Spec \| Spec '-' Spec \| Spec '/' Identifier \| Spec '-' Spec '/' Identifier
| Spec | := script-name \| locale-name \| Identifier
| Identifier | := identifier-start identifier-part\*

Elements enclosed in single quotes are literals. Parentheses group elements.
Vertical bars represent exclusive alternatives. The '?' suffix repeats the
preceding element zero or one times. The '+' suffix repeats the preceding
element one or more times.

A 'script-name' is a string acceptable to the UScript API that specifies a
script. It may be a full script name such as "Latin" or a script abbreviation
such as "Latn". A 'locale-name' is a standard locale name such as "hi_IN". The
'identifier-start' and 'identifier-part' elements are characters defined by the
UCharacter API to start and continue identifier names. Finally, 'filter' is a
valid UnicodeSet pattern.

> :point_right: **Note**: *As of ICU 2.0, the filter must be enclosed in brackets. Top-level Perl-style
patterns are unsupported in 2.0.*

## ICU Transliterators

Currently, there are a number of basic transliterations supplied with ICU. The
following table shows these basic transforms:

### General

| | |
|---|---|
| → Any-Null | Has no effect; leaves input text unchanged. |
| → Any-Remove | Deletes input characters. This is useful when combined with a filter that restricts the characters to be deleted. |
| → Any-Lower, Any-Upper, Any-Title | Converts to the specified case. See [Case Mappings](../casemappings.md) for more information. |
| → Any-NFD, Any-NFC, Any-NFKD, Any-NFKC, Any-FCD, Any-FCC | Converts to the specified normalized form. See [Normalization](../normalization/index.md) for more information. |
| Any-Name | Converts between characters and their Unicode names in curly braces using Perl syntax. For example: ., \\N{FULL STOP}\\N{COMMA} |
| Any-Hex | Converts between characters and their Unicode code point values. For example: ., \\u002E\\u002C Any-Hex/XML uses the &#xXXXX; format. For example: ., &#x2E;&#x2C; Variants include Any-Hex/C, Any-Hex/Java, Any-Hex/Perl, Any-Hex/XML, and Any-Hex/XML10. Any-Hex, with no variant, is equivalent to Any-Hex/Java, for historical reasons. |
| → Any-Accents | Lets you type e- for e-macron, etc. For example: o' ó |
| Any-Publishing | Converts between real punctuation and typewriter punctuation. For example: “a” — ‘b’ "a" -- 'b' |
| → Latin-ASCII | Converts non-ASCII-range punctuation, symbols, and Latin letters in an approximate ASCII-range equivalent. For example: « → '<<', © → '(C)', Æ → AE. Can be combined with Any-Latin to produce a transform that will convert as much as possible to an ASCII-range representation: “Any-Latin; Latin-ASCII”. |
| IPA-XSampa | Convert between IPA characters and the XSampa ASCII-range representation of IPA characters. |
| Fullwidth-Halfwidth | Converts between narrow or half-width characters and full-width. For example: ﻿ｱﾙｱﾉﾘｳ tech アルアノリウ　ｔｅｃｈ |
| Latin-NumericPinyin | Converts between a Pinyin Latin representation using tone marks and one using numeric tone indicators. |

### Script/Language

The ICU script/language transforms are based on common standards for the
particular scripts, where possible. In some cases, the transforms are augmented
to support reversibility.

> :point_right: **Note**: *Standard transliteration methods often do not follow the pronunciation rules of
any particular language in the target script. For more information on the design
of transliterations, see the following Guidelines (§) section. *

The built-in script transforms are:

| | |
|---|---|
| Latin | Arabic, Armenian, Bopomofo, Cyrillic, Georgian, Greek (with UNGEGN variant), Han (with Names variant → Latin), Hangul, Hebrew, Hiragana, Indic, Jamo, Katakana, Syriac, Thaana, Thai |
| Indic | Indic |
| Hiragana | Katakana |
| Simplified (Hans) | Traditional (Hant) |

Indic includes Devanagari, Gujarati, Gurmukhi, Kannada, Malayalam, Oriya, Tamil,
and Telegu. ICU can transliterate from Latin to any of these dialects and back,
and from Indic script to any other Indic script. For example, you can
transliterate from Kannada to Gujarati, or from Latin to Oriya.

In addition, ICU may supply transliterations that are specific to language
pairs, or between a language and a script. For example, ICU could have a ru-en
(Russian-English) transform.

As with locales, there is a fallback mechanism. If the Russian-English transform
is requested and is not available, then ICU will search for a Russian-Latin
transform. If the Russian-Latin transform is not available, ICU will search for
a Cyrillic-Latin transform.

For information on the precise makeup of each of the script transforms, see
Script Transliterator Sources (§) section below.

## Guidelines for Script/Language Transliterations

There are a number of generally desirable guidelines for script
transliterations. These guidelines are rarely satisfied simultaneously, so
constructing a reasonable transliteration is always a process of balancing
different requirements. These requirements are most important for people who are
building transliterations, but are also useful as background information for
users. The following lists the general guidelines for transliterations:

1.  complete: every well-formed sequence of characters in the source script
    should transliterate to a sequence of characters from the target script.

2.  predictable: the letters themselves (without any knowledge of the languages
    written in that script) should be sufficient for the transliteration, based
    on a relatively small number of rules. This allows the transliteration to be
    performed mechanically.

3.  pronounceable: transliteration is not as useful if the process simply maps
    the characters without any regard to their pronunciation. Simply mapping
    "αβγδεζηθ..." to "abcdefgh..." would yield strings that might be complete
    and unambiguous, but cannot be pronounced.

4.  unambiguous: it is always possible to recover the text in the source script
    from the transliteration in the target script. Someone that knows the
    transliteration rules will be able to recover the precise spelling of the
    original source text (for example, it is possible to go from Elláda back to
    the original Ελλάδα). It is possible to define an reverse (or inverse)
    mapping. Thus, this property is sometimes called reversibility (or
    invertibility).

### Ambiguity

In transliteration, multiple characters may produce ambiguities unless the rules
are carefully designed. For example, the Greek character PSI (ψ) maps to ps, but
ps could also (theoretically) result from the sequence PI, SIGMA (πσ) since PI
(π) maps to p and SIGMA (σ) maps to s.

The Japanese transliteration standards provide a good mechanism for handling
similar ambiguities. Using the Japanese transliteration standards, whenever an
ambiguous sequence in the target script does not result from a single letter,
the transform uses an apostrophe to disambiguate it. For example, it uses that
procedure to distinguish between man'ichi and manichi. Using this procedure, the
Greek character PI SIGMA (πσ) maps to p's. This method is recommended for all
script transliteration methods.

> :point_right: **Note**: *Some characters in a target script are not normally found outside of certain
contexts. For example, the small Japanese "ya" character, as in "kya" (キャ), is
not normally found in isolation. To handle such characters, ICU uses a tilde.
For example, to display an isolated small "ya", type "~ya". To represent a
non-final Greek sigma (ασ) at the end of a word, use "a~s". To represent a final
sigma in a non-final position (ςα), type "~sa". *

For the general script transforms, a common technique for reversibility is to
use extra accents to distinguish between letters that may not be otherwise
distinguished. For example, the following shows Greek text that is mapped to
fully reversible Latin:

> **`Greek-Latin`**
> | | |
> |---|---|
> | τί φῄς; γραφὴν σέ τις, ὡς ἔοικε, γέγραπται: οὐ γὰρ ἐκεῖνό γε καταγνώσομαι, ὡς σὺ ἕτερον. | tí phḗis; graphḕn sé tis, hōs éoike, gégraptai: ou gàr ekeînó ge katagnṓsomai, hōs sỳ héteron. |

If the user wants a version without certain accents, then a transform can be
used to remove the accents. For example, the following transliterates to Latin
but removes the macron accents on the long vowels.

> **`Greek-Latin; nfd; [\u0304] remove; nfc`**
> | | |
> |---|---|
> | τί φῄς; γραφὴν σέ τις, ὡς ἔοικε, γέγραπται: οὐ γὰρ ἐκεῖνό γε καταγνώσομαι, ὡς σὺ ἕτερον. | tí phéis; graphèn sé tis, hos éoike, gégraptai: ou gàr ekeînó ge katagnósomai, hos sỳ héteron.

The following transliterates to Latin but removes all accents:

> **`Greek-Latin; nfd; [:nonspacing marks:] remove; nfc`**
> | | |
> |---|---|
> | τί φῄς; γραφὴν σέ τις, ὡς ἔοικε, γέγραπται: οὐ γὰρ ἐκεῖνό γε καταγνώσομαι, ὡς σὺ ἕτερον. | ti pheis; graphen se tis, hos eoike, gegraptai: ou gar ekeino ge katagnosomai, hos sy heteron. |

### Pronunciation

Standard transliteration methods often do not follow the pronunciation rules of
any particular language in the target script. For example, the Japanese Hepburn
system uses a "j" that has the English phonetic value (as opposed to French,
German, or Spanish), but uses vowels that do not have the standard English
sounds. A transliteration method might also require some special knowledge to
have the correct pronunciation. For example, in the Japanese kunrei-siki system,
"tu" is pronounced as "tsu". This is similar to situations where there are
different languages within the same script. For example, knowing that the word
Gewalt comes from German allows a knowledgeable reader to pronounce the "w" as a
"v".

In some cases, transliteration may be heavily influenced by tradition. For
example, the modern Greek letter beta (β) sounds like a "v", but a transform may
continue to use a b (as in biology). In that case, the user would need to know
that a "b" in the transliterated word corresponded to beta (β) and is to be
pronounced as a "v" in modern Greek. Letters may also be transliterated
differently according to their context to make the pronunciation more
predictable. For example, since the Greek sequence GAMMA GAMMA (γγ) is
pronounced as "ng", the first GAMMA can be transcribed as an "n".

> :point_right: **Note**: *In general, predictability means that when transliterating Latin script to
other scripts, English text will not produce phonetic results. This is because
the pronunciation of English cannot be predicted easily from the letters in a
word: e.g. grove, move, and love all end with "ove", but are pronounced very
differently.*

### Cautions

Reversibility may require modifications of traditional transcription methods.
For example, there are two standard methods for transliterating Japanese
katakana and hiragana into Latin letters. The kunrei-siki method is unambiguous.
The Hepburn method can be more easily pronounced by foreigners but is ambiguous.
In the Hepburn method, both ZI (ジ) and DI (ヂ) are represented by "ji" and both
ZU (ズ) and DU (ヅ) are represented by "zu". A slightly amended version of
Hepburn, that uses "dji" for DI and "dzu" for DU, is unambiguous.

When a sequence of two letters map to one, case mappings (uppercase and
lowercase) must be handled carefully to ensure reversibility. For cased scripts,
the two letters may need to have different cases, depending on the next letter.
For example, the Greek letter PHI (Φ) maps to PH in Latin, but Φο maps to Pho,
and not to PHo.

Some scripts have characters that take on different shapes depending on their
context. Usually, this is done at the display level (such as with Arabic) and
does not require special transliteration support. However, in a few cases this
is represented with different character codes, such as in Greek and Hebrew. For
example, a Greek SIGMA is written in a final form (ς) at the end of words, and a
non-final form (σ) in other locations. This requires the transform to map
different characters based on the context.

> :point_right: **Note**: *It is useful for the reverse mapping to be complete so that arbitrary strings
in the target script can be reasonably mapped back to the source script.
Complete reverse mapping makes it much easier to do mechanical quality checks
and so on. For example, even though the letter "q" might not be necessary in a
transliteration of Greek, it can be mapped to a KAPPA (κ). Such reverse mappings
will not, in general, be unambiguous.*

## Using Transliterators

Transliterators have APIs in C, C++, and Java™. Only the C++ APIs are listed
here. For more information on the C, Java, and other APIs, see the relevant API
docs.

To list the available Transliterators, use code like the following:

```
count = Transliterator:: countAvailableIDs();
myID = Transliterator::getAvailableID(n);
```

The ID should not be displayed to users as it is for internal use only. A
separate string, one that can be localized to different languages, is obtained
with a static method. (This method is static to allow the translated names to be
augmented without changing the code.) To get a localized name for use in a GUI,
use the following:

```
Transliterator::getDisplayName(myID, france, nameForUser);
```

To create a Transliterator, use the following:

```
UErrorCode status = U_ZERO_ERROR;
Transliterator *myTrans = Transliterator::createInstance("Latin-Greek",
UTRANS_FORWARD, status);
```

To get a pre-made compound transform, use a series of IDs separated by ";". For
example:

```
myTrans = Transliterator::createInstance(
    "any-NFD; [:nonspacing mark:] any-remove; any-NFC", UTRANS_FORWARD, status);
```

To convert an entire string, use the following:

```
myTrans.transliterate(myString);
```

For more complex cases, such a keyboard input, the following full method
provides more control:

```
myTrans.transliterate(replaceable, positions, complete);
```

The Replaceable interface (or abstract class in C++) allows more complex text to
be used with Transliterators, such as styled text. In ICU4J, a wrapper is
supplied for StringBuffer. A wrapper is an interface to text that handles a very
few operations. For example, the interface can access characters and replace one
substring with another. By using this interface, replacement text can take on
the same style as the text it is replacing, so that style information is not
lost. With a replaceable interface to HTML or XML, even higher level structure
can be preserved.

The positions parameter contains information about the range of text that should
be transliterated, plus the possibly larger range of text that can serve as
context.

The `complete` parameter indicates whether or not you are to consider the text up
to the limit to be complete or not. For keyboard input, the `complete` parameter
should normally be false. Only when the conversion is complete is that parameter
set to true. For example, suppose that a transform converts "sh" to X, and "s"
in other cases to Y. If the complete parameter is true, then a dangling "s"
converts to Y; when the complete parameter is false, then the dangling "s"
should not be converted, since there is more text to come.

In keyboard input, normally start/cursor and limit/end are set to the selection
at the time the transform is chosen. The following shows how the selection is
chosen:

```
positions.start = positions.cursor = selection.getStart();
positions.limit = positions.end = selection.getEnd();
```

As the user types or inserts `inputChars`, call the following:

```
replacable.replace(positions.limit, positions.limit, inputChars); // update the
text
positions.limit += inputChars.length(); // update the positions
myTrans.transliterate(replaceable, positions, false);
```

If the user performs an action that indicates he or she is done with the text,
then transliterate is called one last time using the following:

```
myTrans.transliterate(replaceable, positions, false);
```

Transliterator objects are stateless. They retain no information between calls
to `transliterate()`.

The statelessness might seem to limit the complexity of the operations that can
be performed. In practice, complex transliterations happen by delaying the
replacement of text until it is known that no other replacements are possible.
In other words, although the Transliterator objects are stateless, the source
text itself embodies all the needed information and delayed operation allows
arbitrary complexity.

## Designing Transliterators

Many people use the supplied transforms. However, there are two different ways
of designing transforms. Many transforms can be produced without subclassing,
simply by designing rules for a RuleBasedTransliterator. If conversions can be
done algorithmically much more compactly than with a long list of rules, then
consider subclassing Transliterator directly. For example, ICU itself supplies
specialized subclasses for the following:

1.  Hangul Jamo

2.  Any Hex

3.  Wrapping the string functions for normalization, case mapping, etc.

### Subclassing Transliterators

Subclassers must override `handleTransliterate(Replaceable text, Positions
positions, boolean complete)`. They can override some of the other methods for
efficiency, but ensure that the results are identical. In `handleTransliterate`
convert the text from `positions.cursor` up to `positions.limit`. The context from
`positions.start` to `positions.end` may be taken into account as context when doing
this conversion, but should not be converted themselves. Never look at any
characters before `positions.start` or after `positions.end`.

The `complete` parameter indicates whether or not the text up to limit is
complete. For example, suppose that you would convert "sh" to X, and "s" in
other cases to Y. If the complete parameter is true, then a dangling "s"
converts to Y; when the complete parameter is false, then the dangling "s"
should not be converted. When you return from the method, `positions.cursor`
should be set to the furthest position you processed. Typically this will be up
to `limit`; in case there was an incomplete sequence at the end, `cursor` should be
set to the position just before that sequence.

### Rule-Based Transliterators

ICU supplies the foundation for producing well-behaved transliterations and
supplies a number of typing transliterations for different scripts. The simplest
mechanism for producing transliterations is called a RuleBasedTransliterator.
The RuleBasedTransliterator is a data-based class that allows transliterations
to be built up with a series of rules. These rules provide a specialized set of
context-sensitive matching operations. The operations are similar to
regular-expression rules, but adapted to the specific domain of
transliterations.

The simplest rule is a conversion rule, which replaces one string of characters
with another. The conversion rule takes the following form:

```
xy > z ;
```

This converts any substring "xy" into "z". Rules are executed in order, so:

```
sch > sh ;
ss > z ;
```

This conversion rule transforms "bass school" into "baz shool". The transform
walks through the string from start to finish. Thus given the rules above
"bassch" will convert to "bazch", because the "ss" rule is found before the
"sch" rule in the string (later, we'll see a way to override this behavior). If
two rules can both apply at a given point in the string, then the transform
applies the first rule in the list.

All of the ASCII characters except numbers and letters are reserved for use in
the rule syntax. Normally, these characters do not need to be converted.
However, to convert them use either a pair of single quotes or a slash. The pair
of single quotes can be used to surround a whole string of text. The slash
affects only the character immediately after it. For example, to convert from
two less-than signs to the word "much less than", use one of the following
rules:

```
\<\< > much\ less\ than ;
'<<' > 'much less than' ;
'<<' > much' 'less\ than ;
```

*Spaces may be inserted anywhere without any effect on the rules. Use extra space to separate items out for clarity without worrying about the effects. This feature is particularly useful with combining marks; it is handy to put some spaces around it to separate it from the surrounding text. The following is an example:*

```
 ͅ> i ; # an iota-subscript diacritic turns into an i.
```

*For a real space in the rules, place quotes around it. For a real backslash,
either double it \, or quote it '\'. For a real single quote, double it '',
or place a backslash before it \'. Each of the following means the same thing:*

```
'can''t go'
'can\'t go'
can\'t\ go
can''t' 'go
```

*Any text that starts with a hash mark and concludes a line is a comment. Comments help document how the rules work. The following shows a comment in a rule:*

```
x > ks ; # change every x into ks
```

We can use "\\u" notation instead of any letter. For instance, instead of using
the Greek πp, we could write:

```
\u03C0 > p ;
```

We can also define and use variables, such as:

```
$pi = \u03C0 ; $pi > p ;
```

#### Dual Rules

Rules can also specify what happens when an inverse transform is formed. To do
this, we reverse the direction of the "<" sign. Thus the above example becomes:

```
$pi < p ;
```

With the inverse transform, "p" will convert to the Greek p. These two
directions can be combined together into a dual conversion rule by using the
"<>" operator, yielding:

```
$pi <> p ;
```

#### Context

Context can be used to have the results of a transformation be different
depending on the characters before or after. The following means "Remove
hyphens, but only when they follow lowercase letters":

```
[:lowercase letter:] } '-' > '' ;
```

> :point_right: **Note**: *The context itself (`[:lowercase letter:]`) is unaffected by the replacement;
only the text between the curly braces is changed. *

#### Revisiting

If the resulting text contains a vertical bar "|", then that means that
processing will proceed from that point and that the transform will revisit part
of the resulting text. For example, if we have:

```
x > y | z ;
z a > w;
```

then the string "xa" will convert to "yw". First, "xa" is converted to "yza".
Then the processing will continue from after the character "y", pick up the
"za", and convert it. Had we not had the "|", the result would have been simply
"yza".

#### Example

The following shows how these features are combined together in the
Transliterator "Any-Publishing". This transform converts the ASCII typewriter
conventions into text more suitable for desktop publishing (in English). It
turns straight quotation marks or UNIX style quotation marks into curly
quotation marks, fixes multiple spaces, and converts double-hyphens into a dash.

```
# Variables
$single = \' ;
$space = ' ' ;
$double = \" ;
$back = \` ;
$tab = '\u0008' ;

# the following is for spaces, line ends, (, [, {, ...
$makeRight = [[:separator:][:start punctuation:][:initial punctuation:]] ;

# fix UNIX quotes
$back $back > “ ; # generate right d.q.m. (double quotation mark)
$back > ‘ ;

# fix typewriter quotes, by context
$makeRight { $double <> “ ; # convert a double to right d.q.m. after certain chars
^ { $double > “ ; # convert a double at the start of the line.
$double <> ” ; # otherwise convert to a left q.m.

$makeRight {$single} <> ‘ ; # do the same for s.q.m.s
^ {$single} > ‘ ;
$single <> ’;

# fix multiple spaces and hyphens
$space {$space} > ; # collapse multiple spaces
'--' <> — ; # convert fake dash into real one
```

### Rule Syntax

The following describes the full format of the list of rules used to create a
RuleBasedTransliterator. Each rule in the list is terminated by a semicolon. The
list consists of the following:

1.  an optional filter rule
2.  zero or more transform rules
3.  zero or more variable-definition rules
4.  zero or more conversion rules
5.  an optional inverse filter rule

The filter rule, if present, must appear at the beginning of the list, before
any of the other rules. The inverse filter rule, if present, must appear at the
end of the list, after all of the other rules. The other rules may occur in any
order and be freely intermixed.

The rule list can also generate the inverse of the transform. In that case, the
inverse of each of the rules is used, as described below.

#### Transform Rules

Each transform rule consists of two colons followed by a transform name. For
example:

```
:: NFD ;
```

The inverse of a transform rule follows the same conventions as when we create a
transform by name. For example:

```
:: lower () ; # only executed for the normal
:: (lower) ; # only executed for the inverse
:: lower ; # executed for both the normal and the inverse
```

#### Variable Definition Rules

Each variable definition is of the following form:

```
$variableName = contents ;
```

The variable name can contain letters and digits, but must start with a letter.
More precisely, the variable names use Unicode identifiers as defined by the
identifier properties in ICU. The identifier properties allow for the use of
foreign letters and numbers. See the Unicode class for C++ and the UCharacter
class for Java.

The contents of a variable definition is any sequence of Unicode sets and
characters or characters. For example:

```
$mac = M [aA] [cC] ;
```

Variables are only replaced within other variable definition rules and within
conversion rules. They have no effect on transliteration rules.

#### Filter Rules

A filter rule consists of two colons followed by a UnicodeSet. This filter is
global in that only the characters matching the filter will be affected by any
transform rules or conversion rules. The inverse filter rule consists of two
colons followed by a UnicodeSet in parentheses. This filter is also global for
the inverse transform.

For example, the Hiragana-Latin transform can be implemented by "pivoting"
through the Katakana converter, as follows:

```
# don't touch any katakana that was in the text!
:: [:^Katakana:] ;

:: Hiragana-Katakana;
:: Katakana-Latin;

# don't touch any katakana that was in the text
# for the inverse either!
:: ([:^Katakana:]) ;
```

The filters keep the transform from mistakenly converting any of the "pivot"
characters. Note that this is a case where a rule list contains no conversion
rules at all, just transform rules and filters.

#### Conversion Rules

Conversion rules can be forward, backward, or double. The complete conversion
rule syntax is described below:

##### Forward

A forward conversion rule is of the following form:

```
before_context { text_to_replace } after_context > completed_result | result_to_revisit ;
```

If there is no before_context, then the "{" can be omitted. If there is no
after_context, then the "}" can be omitted. If there is no result_to_revisit,
then the "|" can be omitted. A forward conversion rule is only executed for the
normal transform and is ignored when generating the inverse transform.

##### Backward

A backward conversion rule is of the following form:

```
completed_result | result_to_revisit < before_context { text_to_replace } after_context ;
```

The same omission rules apply as in the case of forward conversion rules. A
backward conversion rule is only executed for the inverse transform and is
ignored when generating the normal transform.

##### Dual

A dual conversion rule combines a forward conversion rule and a backward
conversion rule into one, as discussed above. It is of the form:

```
a { b | c } d <> e { f | g } h ;
```

When generating the normal transform and the inverse, the revisit mark "|" and
the before and after contexts are ignored on the sides where they don't belong.
Thus, the above is exactly equivalent to the sequence of the following two
rules:

```
a { b c } d > f | g ;
b | c < e { f g } h ;
```

#### Intermixing Transform Rules and Conversion Rules

Starting in ICU 3.4, transform rules and conversion rules may be freely
intermixed. (In earlier versions of ICU, transform rules were only allowed at
the beginning or end of the rule set, immediately after the global filter or
immediately before the reverse global filter.) Inserting a transform rule into
the middle of a set of conversion rules has an important side effect.

Normally, conversion rules are considered together as a group. The only time
their order in the rule set is important is when more than one rule matches at
the same point in the string. In that case, the one that occurs earlier in the
rule set wins. In all other situations, when multiple rules match overlapping
parts of the string, the one that matches earlier wins.

Transform rules apply to the whole string. If you have several transform rules
in a row, the first one is applied to the whole string, then the second one is
applied to the whole string, and so on. To reconcile this behavior with the
behavior of conversion rules, transform rules have the side effect of breaking a
surrounding set of conversion rules into two groups: First all of the conversion
rules before the transform rule are applied as a group to the whole string in
the usual way, then the transform rule is applied to the whole string, and then
the conversion rules after the transform rule are applied as a group to the
whole string. For example, consider the following rules:

```
abc > xyz;
xyz > def;
::Upper;
```

If you apply these rules to “abcxyz”, you get “XYZDEF”. If you move the
“::Upper;” to the middle of the rule set and change the cases accordingly...

```
abc > xyz;
::Upper;
XYZ > DEF;
```

...applying this to “abcxyz” produces “DEFDEF”. This is because “::Upper;”
causes the transliterator to reset to the beginning of the string: The first
rule turns the string into “xyzxyz”, the second rule uppercases the whole thing
to “XYZXYZ”, and the third rule turns this into “DEFDEF”.

This can be useful when a transform naturally occurs in multiple “passes.”
Consider this rule set:

```
[:Separator:]* > ' ';
'high school' > 'H.S.';
'middle school' > 'M.S.';
'elementary school' > 'E.S.';
```

If you apply this rule to “high school”, you get “H.S.”, but if you apply it to
“high school” (with two spaces), you just get “high school” (with one space). To
have “high school” (with two spaces) turn into “H.S.”, you'd either have to have
the first rule back up some arbitrary distance (far enough to see “elementary”,
if you want all the rules to work), or you have to include the whole left-hand
side of the first rule in the other rules, which can make them hard to read and
maintain:

```
$space = [:Separator:]*;
high $space school > 'H.S.';
middle $space school > 'M.S.';
elementary $space school > 'E.S.';
```

Instead, you can simply insert “::Null;” in order to get things to work right:

```
[:Separator:]* > ' ';
::Null;
'high school' > 'H.S.';
'middle school' > 'M.S.';
'elementary school' > 'E.S.';
```

The “::Null;” has no effect of its own (the null transliterator, by definition,
doesn't do anything), but it splits the other rules into two “passes”: The first
rule is applied to the whole string, normalizing all runs of whitespace into
single spaces, and then we start over at the beginning of the string to look for
the phrases. “high school” (with four spaces) gets correctly converted to
“H.S.”.

This can also sometimes be useful with rules that have overlapping domains.
Consider this rule set from before:

```
sch > sh ;
ss > z ;
```

Apply this rule to “bassch” results in “bazch” because “ss” matches earlier in
the string than “sch”. If you really wanted “bassh”-- that is, if you wanted the
first rule to win even when the second rule matches earlier in the string, you'd
either have to add another rule for this special case...

```
sch > sh ;
ssch > ssh;
ss > z ;
```

...or you could use a transform rule to apply the conversions in two passes:

```
sch > sh ;
::Null;
ss > z ;
```

#### Masking

When transforms are built, a warning is returned if rules are masked. This
happens when a rule could not be executed because the earlier one would always
match.

```
a > b ;
ac > d ; # masked!
```

In this case, for example, every string that could have a match for "ac" will
already match "a", because the rules are executed in order. However, the
transform compiler will not currently catch cases that would be masked because
of the use of UnicodeSets or regular expression operators, such as the
following:

```
a } [:L:] > b ;
ac > d ; # masked, but not caught by the compiler
```

#### Inverse Summary

The following table shows how the same rule list generates two different
transforms, where the inverse is restated in terms of forward rules (this is a
contrived example, simply to show the reordering):

##### Original Rules

```
:: [:Uppercase Letter:] ;
:: latin-greek ;
:: greek-japanese ;
x <> y ;
z > w ;
r < m ;
:: upper;
a > b ;
c <> d ;
:: any-publishing ;
:: ([:Number:]) ;
```

##### Forward

```
:: [:Uppercase Letter:] ;
:: latin-greek ;
:: greek-japanese ;
x > y ;
z > w ;
:: upper ;
a > b ;
c > d ;
:: any-publishing ;
```

##### Inverse

```
:: [:Number:] ;
:: publishing-any ;
d > c ;
:: lower ;
y > x ;
m > r ;
:: japanese-greek ;
:: greek-latin ;
```

> :point_right: **Note**: *Note how the irrelevant rules (the inverse filter rule and the rules containing
<) are omitted (ignored, actually) in the forward direction, and notice how
things are reversed: the transform rules are inverted and happen in the opposite
order, and the groups of conversion rules are also executed in the opposite
relative order (although the rules within each group are executed in the same
order).*

#### Function Calls

As of ICU 2.1, rule-based transforms can invoke other transforms. The transform
being invoked must be registered with the system before it can be used in a
rule. The syntax for a function call resembles a Perl subroutine call:

```
( [a-zA-Z] ) ( [a-zA-Z]* ) > &Any-Upper($1) &Any-Lower($2) ;
```

This example transforms strings of ASCII letters to have an initial uppercase
letter followed by lowercase letters. (In practice, you would use the `Any-Title`
to do proper titlecasing.)

The formal syntax is:

```
'&' Basic-id '(' Text-arg ')'
```

Elements in single quotes are literals. Basic-id is a basic ID, as described
earlier. It specifies a source, target, and optional variant, but does not
include a filter, explicit reverse, or compound elements. Text-arg is any text
that may appear on the output side of a rule. This means nested function calls
are supported.

For more information on the use of rules, and more examples of the syntax in
use, see the [tutorial](./rules.md).

### Regular Expression

The rules are similar to Regular Expressions in offering: Variables, Property
matches, Contextual matches, Rearrangement ($1, $2…), and Quantifiers (\*, +,
?). They are more powerful in offering: Ordered Rules, Cursor Backup,
Buffered/Keyboard support. They are less powerful in that they have only greedy
quantifiers, no backup (so no X | Y), and no input-side back references.

Here is a simple example that shows the difference between a set of
Transliterator rules, and successively applying regular expression replacements.

Since the transform processes each of its rules at each point, it catches the yx
before the xy in the second case. Since each of the regular expressions is
evaluated over the whole string, that isn't possible. Simply using multiple
regular expressions can't account for the interaction and ordering of characters
and rules. (You can, however, simulate the regex behavior with transform rules
by using a transform rule to split the conversion rules into passes.)

For more details on constructing rules, see the [Transliterator Rule Tutorial](./rules.md).

## Script Transliterator Sources

Currently ICU offers script transliterations between Latin and certain other
scripts (such script transliterations are called romanizations), plus
transliterations between the Indic scripts (excluding Urdu). Additional
romanizations and other script transliterations will be added in the future. In
general, ICU follows the [UNGEGN: Working Group on Romanization
Systems](http://www.eki.ee/wgrs/) where possible. The following describes the
sources used.

Except where otherwise noted, all of these systems are designed to be
reversible. For bicameral scripts (those with upper and lower case), case may
not be completely preserved. The transliterations are also designed to be
complete for the letters a-z. A fallback is used for a letter that is not used
in the transliteration.

### Korean

There are many romanizations of Korean. The default transliteration follows the
[National Institute of Korean Language](https://www.korean.go.kr/front_eng/roman/roman_01.do)
guidelines on Romanization of Korean with the clause 8 variant for reversibility:

8. When it is necessary to convert Romanized Korean back to Hangeul in special
cases such as in academic articles, Romanization is done according to Hangeul
spelling and not pronunciation. Each Hangeul letter is Romanized as explained in
section 2 except that ㄱ, ㄷ, ㅂ, ㄹ are always written as g, d, b, l. When ㅇ has no
sound value, it is replaced by a hyphen. It may also be used when it is necessary to
distinguish between syllables.

There is one other variation: an apostrophe is used instead of a hyphen, since
it has better title casing behavior. To change this, see the Modifications (§)
section below.

### Japanese

The default transliteration for Japanese uses the a slight variant of the
Hepburn system. With Hepburn system, both ZI (ジ) and DI (ヂ) are represented by
"ji" and both ZU (ズ) and DU (ヅ) are represented by "zu". This is amended
slightly for reversibility by using "dji" for DI and "dzu" for DU.

The Katakana transliteration is reversible. Hiragana-Katakana transliteration is
not completely reversible since there are several Katakana letters that do not
have corresponding Hiragana equivalents. Also, the length mark is not used with
Hiragana. The Hiragana-Latin transliteration is also not reversible since
internally it is a combination of Katakana-Hiragana and Hiragana-Latin.

### Greek

The default transliteration uses a standard transcription for Greek. The
transliterations is one that is aimed at preserving etymology. The ISO 843
variant has the following differences:

| Greek | Default | ISO 843 |
|---|---|---|
| β  | b  | v |
| γ\* | n | g |
| η | ē | ī |
| ̔ | h | (omitted) |
| ̀ | ̀ | (omitted) |
| ~ | ~ | (omitted) |

\* before γ, κ, ξ, χ

### Cyrillic

Cyrillic generally follows ISO 9 for the base Cyrillic set. There are tentative
plans to add extended Cyrillic characters in the future, plus variants for GOST
and other national standards.

### Indic

The default romanization uses the ISCII standard with some minor modifications
for reversibility. Internally, all Indic scripts are transliterated by
converting first to an internal form, called Interindic, then from Interindic to
the target script.

Transliteration of Indic scripts in ICU follows the ISO 15919 standard for
Romanization of Indic scripts using diacritics. Internally, all Indic scripts
are transliterated by converting first to an internal form, called Inter-Indic,
then from Inter-Indic to the target script. ISO 15919 differs from ISCII 91 in
application of diacritics for certain characters. These differences are shown in
the following example (illustrated with Devanagari, although the same principles
apply to the other Indic scripts):

| Devanagari | ISCII 91 | ISO 15919 |
|---|---|---|
| ऋ | ṛ | r̥ |
| ऌ | ḻ | l̥ |
| ॠ | ṝ | r̥̄ |
| ॡ | ḻ̄ | l̥̄ |
| ढ़ | d̂ha | ṛha |
| ड़ |d̂a | ṛa |

> :point_right: **Note**: *With some fonts the diacritics will not be correctly placed on the base
letters. The macron on a lowercase L may look particularly bad.*

Transliteration rules in Indic are reversible with the exception of the ZWJ and
ZWNJ used to request explicit rendering effects. For example:

| Devanagari | Romanization | Note |
|---|---|---|
| क्ष | kṣa | normal |
| क्‍ष | kṣa | explicit halant requested |
| क्‌ष | kṣa | half-consonant requested |

There are two particular instances where transliterations may produce unexpected
results: (1) where a halant after a consonant is implied by the romanization (in
such cases the vowel needs to be explicitly written out), and (2) with the
transliteration of 'c'.

For example:

| Devanagari | Romanization |
|---|---|
| सेन्गुप्त | Sēngupta |
| सेनगुप्त | Sēnagupta |
| मोनिच | Monica |
| मोनिक | Monika |

### Modifications

It is easy using transforms to create variants of the defaults. For example, to
create a variant of Korean that uses hyphens instead of apostrophes, use the
following rules:

```
:: Latin-Hangul ;
'' <> '-' ;
```

### More Information

For more information, see:

1.  [UNGEGN: Working Group on Romanization Systems](http://www.eki.ee/wgrs/)

2.  [Transliteration of Non-Roman Alphabets and Scripts (Søren
    Binks)](http://transliteration.eki.ee/)

3.  [Standards for Archival Description:
    Romanization](http://www.archivists.org/catalog/stds99/chapter8.html)

4.  [ISO-15915
    (Hindi)](http://transliteration.eki.ee/pdf/Hindi-Marathi-Nepali.pdf)

5.  [ISO-15915 (Gujarati)](http://transliteration.eki.ee/pdf/Gujarati.pdf)

6.  [ISO-15915 (Kannada)](http://transliteration.eki.ee/pdf/Kannada.pdf)

7.  [ISCII-91](http://www.cdacindia.com/html/gist/down/iscii_d.asp)
