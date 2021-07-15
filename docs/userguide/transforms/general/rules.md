---
layout: default
title: Transform Rule Tutorial
nav_order: 5
parent: Transforms
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Transform Rule Tutorial
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

This tutorial describes the process of building a custom transform based on a
set of rules. The tutorial does not describe, in detail, the features of
transform; instead, it explains the process of building rules and describes the
features needed to perform different tasks. The focus is on building a script
transform since this process provides concrete examples that incorporates most
of the rules.

## Script Transliterators

The first task in building a script transform is to determine which system of
transliteration to use as a model. There are dozens of different systems for
each language and script.

The International Organization for Standardization
([ISO](http://www.elot.gr/tc46sc2/)) uses a strict definition of
transliteration, which requires it to be reversible. Although the goal for ICU
script transforms is to be reversible, they do not have to adhere to this
definition. In general, most transliteration systems in use are not reversible.
This tutorial will describe the process for building a reversible transform
since it illustrates more of the issues involved in the rules. (For guidelines
in building transforms, see "Guidelines for Designing Script Transliterations"
(§) in the [General Transforms](index.md) chapter. For external sources for
script transforms, see Script Transliterator Sources (§) in that same chapter)

> :point_right: **Note**: See *[*Properties and ICU Rule Syntax*](../../strings/properties.md) *for
information regarding syntax characters.*

In this example, we start with a set of rules for Greek since they provide a
real example based on mathematics. We will use the rules that do not involve the
pronunciation of Modern Greek; instead, we will use rules that correspond to the
way that Greek words were incorporated into the English language. For example,
we will transliterate "Βιολογία-Φυσιολογία" as "Biología-Physiología", not as
"Violohía-Fisiolohía". To illustrate some of the trickier cases, we will also
transliterate the Greek accents that are no longer in use in modern Greek.

> :point_right: **Note**: *Some of the characters may not be visible on the screen unless you have a
Unicode font with all the Greek letters. If you have a licensed copy of
Microsoft® Office, you can use the "Arial Unicode MS" font, or you can download
the [CODE2000](http://www.code2000.net/) font for free. For more information,
see [Display Problems?](http://www.unicode.org/help/display_problems.html) on
the Unicode web site.*

We will also verify that every Latin letter maps to a Greek letter. This insures
that when we reverse the transliteration that the process can handle all the
Latin letters.

> :point_right: **Note**: *This direction is not reversible. The following table illustrates this
situation:* 

| Source→Target | Reversible | φ → ph → φ |
|---------------|------------|------------|
| Target→Source | Not (Necessarily) Reversible | f → φ → ph |


## Basics

In non-complex cases, we have a one-to-one relationship between letters in both
Greek and Latin. These rules map between a source string and a target string.
The following shows this relationship:

```
π <> p;
```

This rule states that when you transliterate from Greek to Latin, convert π to p
and when you transliterate from Latin to Greek, convert p to π. The syntax is

```
string1 <> string2 ;
```

We will start by adding a whole batch of simple mappings. These mappings will
not work yet, but we will start with them. For now, we will not use the
uppercase versions of characters.

    # One to One Mappings
    α <> a;
    β <> b;
    γ <> g;
    δ <> d;
    ε <> e;

We will also add rules for completeness. These provide fallback mappings for
Latin characters that do not normally result from transliterating Greek
characters.

    # Completeness Mappings
    κ < c;
    κ < q;

## Context and Range

We have completed the simple one-to-one mappings and the rules for completeness.
The next step is to look at the characters in context. In Greek, for example,
the transform converts a "γ" to an "n" if it is before any of the following
characters: γ, κ, ξ, or χ. Otherwise the transform converts it to a "g". The
following list a all of the possibilities:

    γγ > ng;
    γκ > nk;
    γξ > nx;
    γχ > nch;
    γ > g;

All the rules are evaluated in the order they are listed. The transform will
first try to match the first four rules. If all of these rules fail, it will use
the last one.

However, this method quickly becomes tiresome when you consider all the possible
uppercase and lowercase combinations. An alternative is to use two additional
features: context and range.

### Context

First, we will consider the impact of context on a transform. We already have
rules for converting γ, κ, ξ, and χ. We must consider how to convert the γ
character when it is followed by γ, κ, ξ, and χ. Otherwise we must permit
those characters to be converted using their specific rules. This is done with
the following:

    γ } γ > n;
    γ } κ > n;
    γ } ξ > n;
    γ } χ > n;
    γ > g;

A left curly brace marks the start of a context rule. The context rule will be
followed when the transform matches the rules against the source text, but
itself will not be converted. For example, if we had the sequence γγ, the
transform converts the first γ into an "n" using the first rule, then the second
γ is unaffected by that rule. The "γ" matches a "k" rule and is converts it into
a "k". The result is "nk".

### Range

Using context, we have the same number of rules. But, by using range, we can
collapse the first four rules into one. The following shows how we can use
range:

    {γ}[γκξχ] > n;
    γ > g;

Any list of characters within square braces will match any one of the
characters. We can then add the uppercase variants for completeness, to get:

    γ } [ΓΚΞΧγκξχ] > n;
    γ > g;

Remember that we can use spaces for clarity. We can also write this rule as the
following:

    γ } [ Γ Κ Ξ Χ γ κ ξ χ ] > n ;
    γ > g ;

If a range of characters happens to have adjacent code numbers, we can just use
a hyphen to abbreviate it. For example, instead of writing `[a b c d e f g m n o]`,
we can simplify the range by writing `[a-g m-o]`.

## Styled Text

Another reason to use context is that transforms will convert styled text. When
transforms convert styled text, they copy the style source text to the target
text. However, the transforms are limited in that they can only convert whole
replacements since it is impossible to know how any boundaries within the source
text will correspond to the target text. Thus the following shows the effects of
the two types of rules on some sample text:

For example, suppose that we were to convert "γγ" to "ng". By using context, if
there is a different style on the first gamma than on the second (such as font,
size, color, etc), then that style difference is preserved in the resulting two
characters. That is, the "n" will have the style of the first gamma, while the
"g" will have the style of the second gamma.

> :point_right: **Note**: *Contexts preserve the styles at a much finer granularity.*

## Case

When converting from Greek to Latin, we can just convert "θ" to and from "th".
But what happens with the uppercase theta (Θ)? Sometimes we need to convert it
to uppercase "TH", and sometimes to uppercase "T" and lowercase "h". We can
choose between these based on the letters before and afterwards. If there is a
lowercase letter after an uppercase letter, we can choose "Th", otherwise we
will use "TH".

We could manually list all the lowercase letters, but we also can use ranges.
Ranges not only list characters explicitly, but they also give you access to all
the characters that have a given Unicode property. Although the abbreviations
are a bit arcane, we can specify common sets of characters such as all the
uppercase letters. The following example shows how case and range can be used
together:

    Θ } [:LowercaseLetter:] <> Th;
    Θ <> TH;

The example allows words like Θεολογικές‚ to map to Theologikés and not
THeologikés

> :point_right: **Note**: *You either can specify properties with the POSIX-style syntax, such as
[:LowercaseLetter:], or with the Perl-style syntax, such as
\\p{LowercaseLetter}.*

## Properties and Values

A Greek sigma is written as "ς" if it is at the end of a word (but not
completely separate) and as "σ" otherwise. When we convert characters from Greek
to Latin, this is not a problem. However, it is a problem when we convert the
character back to Greek from Latin. We need to convert an s depending on the
context. While we could list all the possible letters in a range, we can also
use a character property. Although the range `[:Letter:]` stands for all
letters, we really want all the characters that aren't letters. To accomplish
this, we can use a negated range: `[:^Letter:]`. The following shows a negated
range:

    σ < [:^Letter:] { s } [:^Letter:] ;
    ς < s } [:^Letter:] ;
    σ < s ;

These rules state that if an "s" is surrounded by non-letters, convert it to
"σ". Otherwise, if the "s" is followed by a non-letter, convert it to "ς". If
all else fails, convert it to "σ"

> :point_right: **Note**: *Negated ranges [^...] will match at the beginning and the end of a string.
This makes the rules much easier to write. *

To make the rules clearer, you can use variables. Instead of the example above,
we can write the following:

    $nonletter = [:^Letter:] ;
    σ < $nonletter { s } $nonletter ;
    ς < s } $nonletter ;
    σ < s ;

There are many more properties available that can be used in combination. For
following table lists some examples:

| Combination | Example | Description: All code points that are: |
|----------------|--------------------------|--------------------------------------------|
| Union | [[:Greek:] [:letter:]] | either in the Greek script, or are letters |
| Intersection | [[:Greek:] & [:letter:]] | are both Greek and letters |
| Set Difference | [[:Greek:] - [:letter:]] | are Greek but not letters |
| Complement | [^[:Greek:] [:letter:]] | are neither Greek nor letters |

For more on properties, see the [UnicodeSet](../../strings/unicodeset.md) and
[Properties](../../strings/properties.md) chapters.

## Repetition

Elements in a rule can also repeat. For example, in the following rules, the
transform converts an iota-subscript into a capital I if the preceding base
letter is an uppercase character. Otherwise, the transform converts the
iota-subscript into a lowercase character.

    [:Uppercase Letter:] { ͅ } > I;
    ͅ > i;

However, this is not sufficient, since the base letter may be optionally
followed by non-spacing marks. To capture that, we can use the \* syntax, which
means repeat zero or more times. The following shows this syntax:

    [:Uppercase Letter:] [:Nonspacing Mark:] \* { ͅ } > I ;
    ͅ > i ;

The following operators can be used for repetition:

| Repetition Operators |  |
|----------------------|------------------|
| X* | zero or more X's |
| X+ | one or more X's |
| X? | Zero or one X |

We can also use these operators as sequences with parentheses for grouping. For
example, "a ( b c ) \* d" will match against "ad" or "abcd" or "abcbcd".

*Currently, any repetition will cause the sequence to match as many times as allowed even if that causes the rest of the rule to fail. For example, suppose we have the following (contrived) rules:*
*The intent was to transform a sequence like "able blue" into "ablæ blué". The rule does not work as it produces "ablé blué". The problem is that when the left side is matched against the text in the first rule, the `[:Letter:]*` matches all the way back through the "al" characters. Then there is no "a" left to match. To have it match properly, we must subtract the 'a' as in the following example:*

## Æther

The start and end of a string are treated specially. Essentially, characters off
the end of the string are handled as if they were the noncharacter \\uFFFF,
which is called "æther". (The code point \\uFFFF will never occur in any valid
Unicode text). In particular, a negative Unicode set will generally also match
against the start/end of a string. For example, the following rule will execute
on the first **a** in a string, as well as an **a** that is actually preceded by
a non-letter.

| Rule | [:^L:] { a > b ; |
|---------|------------------|
| Source | a xa a |
| Results | b xa b |

This is because \\uFFFF is an element of `[:^L:]`, which includes all codepoints
that do not represent letters. To refer explicitly to æther, you can use a **$**
at the end of a range, such as in the following rules:

| Rules | [0-9$] { a > b ; a } [0-9$] > b ;|
|------------------|------------------|
| Source | a 5a a |
| Results | b 5b a |

In these rules, an **a** before or after a number -- or at the start or end of a
string -- will be matched. (You could also use \\uFFFF explicitly, but the $ is
recommended).

Thus to disallow a match against æther in a negation, you need to add the $ to
the list of negated items. For example, the first rule and results from above
would change to the following (notice that the first a is not replaced):

| Rule | [^[:L:]$] { a > b ; |
|---------|---------------------|
| Source | a xa a |
| Results | a xa b |

> :point_right: **Note**: *Characters that are outside the context limits -- contextStart to contextEnd -- are also treated as
æther.*

The property `[:any:]` can be used to match all code points, including æther.
Thus the following are equivalent:

| Rule1 | [\u0000-\U0010FFFF] { a > A ; |
|-------|-------------------------------|
| Rule2 | [:any:] { a > A ; |

However, since the transform is always greedy with no backup, this property is
not very useful in practice. What is more often required is dealing with the end
of lines. If you want to match the start or end of a line, then you can define a
variable that includes all the line separator characters, and then use it in the
context of your rules. For example:

| Rules | $break = [[:Zp:][:Zl:] \u000A-\u000D \u0085 $] ; $break { a > A ;|
|------------------|--------------------------------------------------|
| Source | a a a a |
| Results | A a A a |

There is also a special character, the period (.), that is equivalent to the
**negation** of the $break variable we defined above. It can be used to match
any characters excluding those for linebreaks or æther. However, it cannot be
used within a range: you can't have `[[.] - \u000A]`, for example. If you
want to have different behavior you can define your own variables and use them
instead of the period.

> :point_right: **Note**: *There are a few other special escapes, that can be used in ranges. These are
listed in the table below. However, instead of the latter two it is safest to
use the above $break definition since it works for line endings across different
platforms.* 

| Escape | Meaning | Code |
|--------|-----------------|--------|
| \t | Tab | \u0009 |
| \n | Linefeed | \u000A |
| \r | Carriage Return | \u000D |

## Accents

We could handle each accented character by itself with rules such as the
following:

    ά > á;
    έ > é;
    ...

This procedure is very complicated when we consider all the possible
combinations of accents and the fact that the text might not be normalized. In
ICU 1.8, we can add other transforms as rules either before or after all the
other rules. We then can modify the rules to the following:

    :: NFD (NFC) ;
    α <> a;
    ...
    ω <> ō;
    :: NFC (NFD);

These modified rules first separate accents from their base characters and then
put them in a canonical order. We can then deal with the individual components,
as desired. We can use NFC (NFC) at the end to put the entire result into
standard canonical form. The inverse uses the transform rules in reverse order,
so the (NFD) goes at the bottom and (NFC) at the top.

A global filter can also be used with the transform rules. The following example
shows a filter used in the rules:

    :: [[:Greek:][:Inherited:]];
    :: NFD (NFC) ;
    α <> a;
    ...
    ω <> ō;
    :: NFC (NFD);
    :: ([[:Latin:][:Inherited:]]);

The global filter will cause any other characters to be unaffected. In
particular, the NFD then only applies to Greek characters and accents, leaving
all other characters

## Disambiguation

If the transliteration is to be completely reversible, what would happen if we
happened to have the Greek combination νγ? Because ν converts to n, both νγ and
γγ convert to "ng" and we have an ambiguity. Normally, this sequence does not
occur in the Greek language. However, for consistency -- and especially to aid
in mechanical testing– we must consider this situation. (There are other cases
in this and other languages where both sequences occur.)

To resolve this ambiguity, use the mechanism recommended by the Japanese and
Korean transliteration standards by inserting an apostrophe or hyphen to
disambiguate the results. We can add a rule like the following that inserts an
apostrophe after an "n" if we need to reverse the transliteration process:

    ν } [ΓΚΞΧγκξχ] > n\';

In ICU, there are several of these mechanisms for the Greek rules. The ICU rules
undergo some fairly rigorous mechanical testing to ensure reversibility. Adding
these disambiguation rules ensure that the rules can pass these tests and handle
all possible sequences of characters correctly.

There are some character forms that never occur in normal context. By
convention, we use tilde (\~) for such cases to allow for reverse
transliteration. Thus, if you had the text "Θεολογικές (ς)", it would
transliterate to "Theologikés (\~s)". Using the tilde allows the reverse
transliteration to detect the character and convert correctly back to the
original: "Θεολογικές (ς)". Similarly, if we had the phrase "Θεολογικέσ", it
would transliterate to "Theologiké~s". These are called anomalous characters.

## Revisiting

Rules allow for characters to be revisited after they are replaced. For example,
the following converts "C" back "S" in front of "E", "I" or "Y". The vertical
bar means that the character will be revisited, so that the "S" or "K" in a
Greek transform will be applied to the result and will eventually produce a
sigma (Σ, σ, or ς) or kappa (Κ or κ).

    $softener = [eiyEIY] ;
    | S < C } $softener ;
    | K < C ;
    | s < c } $softener ;
    | k < c ;

The ability to revisit is particularly useful in reducing the number of rules
required for a given language. For example, in Japanese there are a large number
of cases that follow the same pattern: "kyo" maps to a large hiragana for "ki"
(き) followed by a small hiragana for "yo" (ょ). This can be done with a small
number of rules with the following pattern:

First, the ASCII punctuation mark, tilde "~", represents characters that never
normally occur in isolation. This is a general convention for anomalous
characters within the ICU rules in any event.

    '~yu' > ゅ;
    '~ye' > ぇ;
    '~yo' > ょ;

Second, any syllables that use this pattern are broken into the first hiragana
and are followed by letters that will form the small hiragana.

    by > び|'~y';
    ch > ち|'~y';
    dj > ぢ|'~y';
    gy > ぎ|'~y';
    j > じ|'~y';
    ky > き|'~y';
    my > み|'~y';
    ny > に|'~y';
    py > ぴ|'~y';
    ry > り|'~y';
    sh > し|'~y';

Using these rules, "kyo" is first converted into "き~yo". Since the "~yo" is then
revisited, this produces the desired final result, "きょ". Thus, a small number of
rules (3 + 11 = 14) provide for a large number of cases. If all of the
combinations of rules were used instead, it would require 3 x 11 = 33 rules.

You can set the new revisit point (called the cursor) anywhere in the
replacement text. You can even set the revisit point before or after the target
text. The at-sign, as in the following example, is used as a filler to indicate
the position, for those cases:

    [aeiou] { x > | @ ks ;
    ak > ack ;

The first rule will convert "x", when preceded by a vowel, into "ks". The
transform will then backup to the position before the vowel and continue. In the
next pass, the "ak" will match and be invoked. Thus, if the source text is "ax",
the result will be "ack".

> :point_right: **Note**: *Although you can move the cursor forward or backward, it is limited in two
ways: (a) to the text that is matched, (b) within the original substring that is
to be converted. For example, if we have the rule "a b\* {x} > |@@@@@y" and it
matches in the text "mabbx", the result will be "m|abby" (| represents the
cursor position). Even though there are five @ signs, the cursor will only
backup to the first character that is matched.*

## Copying

We can copy part of the matched string to the target text. Use parenthesis to
group the text to copy and use "$n" (where n is a number from 1 to 99) to
indicate which group. For example, in Korean, any vowel that does not have a
consonant before it gets the null consonant (?) inserted before it. The
following example shows this rule:

    ([aeiouwy]) > ?| $1 ;

To revisit the vowel again, insert the null consonant, insert the vowel, and
then backup before the vowel to reconsider it. Similarly, we have a following
rule that inserts a null vowel (?), if no real vowel is found after a consonant:

    ([b-dg-hj-km-npr-t]) > | $1 eu;

In this case, since we are going to reconsider the text again, we put in the
Latin equivalent of the Korean null vowel, which is "eu".

## Order Matters

Two rules overlap when there is a string that both rules could match at the
start. For example, the first part of the following rule does not overlap, but
the last two parts do overlap:

    β > b;
    γ } [ Γ Κ Ξ Χ γ κ ξ χ ] > n ;
    γ > g ;

When rules do not overlap, they will produce the same result no matter what
order they are in. It does not matter whether we have either of the following:

    β > b;
    γ > g ;
    or
    γ > g ;
    β > b;

When rules do overlap, order is important. In fact, a rule could be rendered
completely useless. Suppose we have:

    β } [aeiou] > b;
    β } [^aeiou] > v;
    β > p;

In this case, the last rule is masked as none of the text that will match the
rule will already be matched by previous rules. If a rule is masked, then a
warning will be issued when you attempt to build a transform with the rules.

## Combinations

In Greek, a rough breathing mark on one of the first two vowels in a word
represents an "H". This mark is invalid anywhere else in the language. In the
normalize (NFD) form, the rough-breathing mark will be first accent after the
vowel (with perhaps other accents following). So, we will start with the
following variables and rule. The rule transforms a rough breathing mark into an
"H", and moves it to before the vowels.

    $gvowel = [ΑΕΗΙΟΥΩαεηιουω];
    ($gvowel + ) ̔ > H | $1;

A word like ὍΤΑΝ" is transformed into "HOTAN". This transformation does not work
with a lowercase word like "ὅταν". To handle lowercase words, we insert another
rule that moves the "H" over lowercase vowels and changes it to lowercase. The
following shows this rule:

    $gvowel = [ΑΕΗΙΟΥΩαεηιουω];
    $lcgvowel = [αεηιουω];
    ($lcgvowel +) ̔ > h | $1; # fix lowercase
    ($gvowel + ) ̔ > H | $1;

This rule provides the correct results as the lowercase word "ὅταν" is
transformed into "hotan".

There are also titlecase words such as "Ὅταν". For this situation, we need to
lowercase the uppercase letters as the transform passes over them. We need to do
that in two circumstances: (a) the breathing mark is on a capital letter
followed by a lowercase, or (b) the breathing mark is on a lowercase vowel. The
following shows how to write a rule for this situation:

    $gvowel = [ΑΕΗΙΟΥΩαεηιουω];
    $lcgvowel = [αεηιουω];

    # fix Titlecase
    {Ο ̔ } [:Nonspacing Mark:]* [:Ll:] > H | ο;

    # fix Titlecase
    {Ο ( $lcgvowel * ) ̔ } > H | ο $1;

    # fix lowercase
    ( $lcgvowel + ) ̔ > h | $1 ;
    ($gvowel + ) ̔ > H | $1 ;

This rule gives the correct results for lowercase as "Ὅταν" is transformed into
"Hotan". We must copy the above insertion and modify it for each of the vowels
since each has a different lowercase.

We must also write a rule to handle a single letter word like "ὃ". In that case,
we would need to look beyond the word, either forward or backward, to know
whether to transform it to "HO" or to transform it to "Ho". Unlike the case of a
capital theta (Θ), there are cases in the Greek language where single-vowel
words have rough breathing marks. In this case, we would use several rules to
match either before or after the word and ignore certain characters like
punctuation and space (watch out for combining marks).

## Pitfalls

1.  **Case** When executing script conversions, if the source script has
    uppercase and lowercase characters, and the target is lowercase, then
    lowercase everything before your first rule. For example:
    ```
    # lowercase target before applying forward rules
    :: [:Latin:] lower ();
    ```
    This will allow the rules to work even when they are given a mixture of
    upper and lower case character. This procedure is done in the following ICU
    transforms:
    -  Latin-Hangul
    -  Latin-Greek
    -  Latin-Cyrillic
    -  Latin-Devanagari
    -  Latin-Gujarati
    -  etc

1.  **Punctuation** When executing script conversions, remember that scripts
    have different punctuation conventions. For example, in the Greek language,
    the ";" means a question mark. Generally, these punctuation marks also
    should be converted when transliterating scripts.

2.  **Normalization** Always design transform rules so that they work no matter
    whether the source is normalized or not. (This is also true for the target,
    in the case of backwards rules.) Generally, the best way to do this is to
    have `:: NFD (NFC);` as the first line of the rules, and `:: NFC (NFD);` as the
    last line. To supply filters, as described above, break each of these lines
    into two separate lines. Then, apply the filter to either the normal or
    inverse direction. Each of the accents then can be manipulated as separate
    items that are always in a canonical order. If we are not using any accent
    manipulation, we could use `:: NFC (NFC) ;` at the top of the rules instead.

3.  **Ignorable Characters** Letters may have following accents such as the
    following example:
    ```
    # convert z after letters into s
    [:lowercase letter:] } z > s ;
    ```
    Normally, we want to ignore any accents that are on the z in performing the
    rule. To do that, restate the rule as:
    ```
    # convert z after letters into s
    [:lowercase letter:] [:mark:]* } z > s ;
    ```
    Even if we are not using NFD, this is still a good idea since some languages
    use separate accents that cannot be combined.
    Moreover, some languages may have embedded format codes, such as a
    Left-Right Mark, or a Non-Joiner. Because of that, it is even safer to use
    the following:
    ```
    # define at the top of your file
    $ignore = [ [:mark:] [:format:] ] * ;
    ...
    # convert z after letters into sh
    [:letter:] $ignore } z > s ;
    ```


> :point_right: **Note**: *Remember that the rules themselves must be in the same normalization format.
Otherwise, nothing will match. To do this, run NFD on the rules themselves. In
some cases, we must rearrange the order of the rules because of masking. For
example, consider the following rules:*

*If these rules are put in normalized form, then the second rule will mask the first. To avoid this, exchange the order because the NFD representation has the accents separate from the base character. We will not be able to see this on the screen if accents are rendered correctly. The following shows the NFD representation:*
