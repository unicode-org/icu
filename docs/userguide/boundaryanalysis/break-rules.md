---
layout: default
title: Break Rules
nav_order: 1
parent: Boundary Analysis
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Break Rules
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Introduction

ICU locates boundary positions within text by means of rules, which are a form
of regular expressions. The form of the rules is similar, but not identical,
to the boundary rules from the Unicode specifications
[[UAX-14](https://www.unicode.org/reports/tr14/), 
[UAX-29](https://www.unicode.org/reports/tr29/)], and there is a reasonably close
correspondence between the two.

Taken as a set, the ICU rules describe how to move forward to the next boundary,
starting from a known boundary.
ICU includes rules for the standard boundary types (word, line, etc.).
Applications may also create customized break iterators from their own rules.

ICU's built-in rules are located at
[icu/icu4c/source/data/brkitr/rules/](https://github.com/unicode-org/icu/tree/master/icu4c/source/data/brkitr/rules).
These can serve as examples when writing your own, and as starting point for
customizations.

### Rule Tutorial

Rules most commonly describe a range of text that should remain together,
unbroken. For example, this rule

```
    [\p{Letter}]+;
```

matches a run of one or more letters, and would cause them to remain unbroken.

The part within `[`brackets`]` follows normal ICU [UnicodeSet pattern syntax](../strings/unicodeset.md).

The qualifier, '`+`' in this case, can be one of

| Qualifier | Meaning                  |
| --------- | ------------------------ |
| empty     | Match exactly once       |
| `?`       | Match zero or one time   |
| `+`       | Match one or more times  |
| `*`       | Match zero or more times |

#### Variables

A variable names a set or rule sub-expression. They are useful for documenting
what something represents, and for simplifying complex expressions by breaking
them up.

"Variable" is something if a misnomer; they cannot be reassigned, but are more
of a constant expression.

They start with a '`$`', both in the definition and use.

```
    # Variable Definition
    $ASCIILetNum = [A-Za-z0-9];
    # Variable Use
    $ASCIILetNum+;
```

#### Comments and Semicolons

'`#`' begins a comment, which extends to the end of a line.

Comments may stand alone, or appear after another statement on a line.

All rule statements or expressions are terminated by semicolons.

#### Chained Matching

Most ICU rule sets use the concept of "chained matching". The idea is that
complete match can be composed from multiple pieces, with each piece coming from
an individual rule of a rule set.

This idea is unique to ICU break rules, it is not a concept found in other
regular expression based matchers. Some of the Unicode standard break rules
would be difficult to implement without it.

Starting with an example,

```
    !!chain;
    word_char = [\p{Letter}];
    word_joiner = [_-];
    $word_char+;
    $word_char $word_joiner $word_char;
```

These rules will match "`abc`", "`hello_world`", `"hi-there"`,
"`a-bunch_of-joiners-here`".

They will not match "`-abc`", "`multiple__joiners`", "`tail-`"

A full match is composed of pieces or submatches, possibly from different rules,
with adjacent submatches linked by at least one overlapping character.

In the example below, matching "`hello_world`",

* '`1`' shows matches of the first rule, `word_char+`

* '`2`' shows matches of the second rule, `$word_char $word_joiner $word_char`

```
      hello_world
      11111 11111
          222
```

There is an overlap of the matched regions, which causes the chaining mechanism
to join them into a single overall match.

The mechanism is a good match to, for example, [Unicode's word break
rules](http://www.unicode.org/reports/tr29/#Word_Boundary_Rules), where rules
WB5 through WB13 combine to piece together longer words from multiple short
segments.

`!!chain;` enables chaining in a rule set. It is disabled by default for back
compatibility—very old versions of ICU did not support it, and it was
originally introduced as an option.

#### Parentheses and Alternation

Rule expressions can contain parentheses and '`|`' operators, representing
alternation or "or" operations. This follows conventional regular expression
behavior.

For example, the following would match a simplified identifier:

```
    $Letter ($Letter | $Digit)*;
```

#### String and Character Literals

Similarly to common regular expressions, literal characters that do not have
other special meaning represent themselves. So the rule

```
    Hello;
```

would match the literal input "`Hello`".

In practice, nearly all break rules are composed from `[`sets`]` based on Unicode
character properties; literal characters in rules are very rare.

To prevent random typos in rules from being treated as literals, use this
option:

```
    !!quoted_literals_only;
```

With the option, the naked `Hello` becomes a rule syntax error while a quoted
`"hello"` still matches a literal hello.

`!!quoted_literals_only` is strongly recommended for all rule sets. The random
typo problem is very real, and surprisingly hard to recognize and debug.

#### Explicit Break Rules

A rule containing a slash (`/`) will force a boundary when it matches, even when
other rules or chaining would otherwise lead to a longer match. Also called Hard
Break Rules, these have the form

```
    pre-context / post-context;
```

where the pre and post-context look like normal break rules. Both the pre and
post context are required, and must not allow a zero-length match. There should
be no overlap between characters that end a match of the pre-context and those
that begin a match of the post-context.

Chaining into a hard break rule operates normally. There is no chaining out of a
hard break rule; when the post-context matches a break is forced immediately.

Note: future versions of ICU may loosen the restrictions on explicit break
rules. The behavior of rules with missing or overlapping contexts is subject to
change.

#### Chaining Control

Chaining into a rule can be dis-allowed by beginning that rule with a '`^`'. Rules
so marked can begin a match after a preceding boundary or at the start of text,
but cannot extend a match via chaining from another rule.

~~The !!LBCMNoChain; statement modifies chaining behavior by preventing chaining
from one rule to another from occurring on any character whose Line Break
property is Combining Mark. This option is subject to change or removal, and
should not be used in general. Within ICU, it is used only with the line break
rules. We hope to replace it with something more general.~~

> :point_right: **Note**: `!!LBCMNoChain` is deprecated, and will be removed
> completely from a future version of ICU.

## Rule Status Values

Break rules can be tagged with a number, which is called the *rule status*.
After a boundary has been located, the status number of the specific rule that
determined the boundary position is available to the application through the
function `getRuleStatus()`.

For the predefined word boundary rules, status values are available to
distinguish between boundaries associated with words, numbers, and those around
spaces or punctuation. Similarly for line break boundaries, status values
distinguish between mandatory line endings (new line characters) and break
opportunities that are appropriate points for line wrapping. Refer to the ICU
API documentation for the C header file `ubrk.h` or to Java class
`RuleBasedBreakIterator` for a complete list of the predefined boundary
classifications.

When creating custom sets of break rules, integer status values can be
associated with boundary rules in whatever way will be convenient for the
application. There is no need to remain restricted to the predefined values and
classifications from the standard rules.

It is possible for a set of break rules to contain more than a single rule that
produces some boundary in an input text. In this event, `getRuleStatus()` will
return the numerically largest status value from the matching rules, and the
alternate function `getRuleStatusVec()` will return a vector of the values from
all of the matching rules.

In the source form of the break rules, status numbers appear at end of a rule,
and are enclosed in `{`braces`}`.

Hard break rules that also have a status value place the status at the end, for
example

```
    pre-context / post-context {1234};
```

### Word Dictionaries

For some languages that don't normally use spaces between words, break iterators
are able to supplement the rules with dictionary based breaking. Some languages,
Thai or Lao, for example, use a dictionary for both word and line breaking.
Others, such as Japanese, use a dictionary for word breaking, but not for line
breaking.

To enable dictionary use,

1. The break rules must select, as unbroken chunks, ranges of text to be passed
   off to the word dictionary for further subdivision.
2. The break rules must define a character class named `$dictionary` that
   contains the characters (letters) to be handled by the dictionary.

The dictionary implementation, on receiving a range of text, will map it to a
specific dictionary based on script, and then delegate to that dictionary for
subdividing the range into words.

See, for example, this snippet from the [line break rules](https://github.com/unicode-org/icu/blob/master/icu4c/source/data/brkitr/rules/line.txt):

```
    #  Dictionary character set, for triggering language-based break engines. Currently
    #  limited to LineBreak=Complex_Context (SA).
    $dictionary = [$SA];
```

## Rule Options

| Option          | Description |
| --------------- | ----------- |
| `!!chain`       |  Enable rule chaining. Default is no chaining. |
| `!!forward`     |  The rules that follow are for forward iteration. Forward rules are now the only type of rules needed or used.   |

### Deprecated Rule Options

| Deprecated Option          | Description |
| --------------- | ----------- |
| ~~`!!reverse`~~     | ~~*[deprecated]* The rules that follow are for reverse iteration. No longer needed; any rules in a Reverse rule section are ignored.~~ |
| ~~`!!safe_forward`~~ | ~~*[deprecated]* The rules that follow are for safe forward iteration. No longer needed; any rules in such a section are ignored.~~ |
| ~~`!!safe_reverse`~~ | ~~*[deprecated]* The rules that follow are for safe reverse iteration. No longer needed; any rules in such a section are ignored.~~ |
| ~~`!!LBCMNoChain`~~ | ~~*[deprecated]* Disable chaining when the overlap character matches `\p{Line_Break=Combining_Mark}`~~ |

## Rule Syntax

Here is the syntax for the boundary rules. (The EBNF Syntax is given below.)

| Rule Name | Rule Values | Notes |
| ---------- | ----------- | ----- |
| rules | statement+ | |
| statement | assignment \| rule \| control |
| control | (`!!forward` \| `!!reverse` \| `!!safe_forward` \| `!!safe_reverse` \| `!!chain`) `;`
| assignment | variable `=` expr `;` | 5 |
| rule | `^`? expr (`{`number`}`)? `;` | 8,9 |
| number | [0-9]+ | 1 |
| break-point | `/` | 10 |
| expr | expr-q \| expr `\|` expr \| expr expr | 3 |
| expr-q | term \| term `*` \| term `?` \| term `+` |
| term | rule-char \| unicode-set \| variable \| quoted-sequence \| `(` expr `)` \| break-point |
| rule-special | *any printing ascii character except letters or numbers* \| white-space |
| rule-char | *any non-escaped character that is not rule-special* \| `.` \| *any escaped character except* `\p` *or* `\P` |
| variable | `$` name-start-char name-char* | 7 |
| name-start-char | `_` \| \p{L} |
| name-char | name-start-char \| \\p{N} |
| quoted-sequence | `'` *(any char except single quote or line terminator or two adjacent single quotes)*+ `'` |
| escaped-char | *See “Character Quoting and Escaping” in the [UnicodeSet](../strings/unicodeset.md) chapter* |
| unicode-set | See [UnicodeSet](../strings/unicodeset.md) | 4 |
| comment | unescaped `#` *(any char except new-line)** new-line | 2 |
| s | unescaped \p{Z}, tab, LF, FF, CR, NEL | 6 |
| new-line | LF, CR, NEL | 2 |

### Rule Syntax Notes

1. The number associated with a rule that actually determined a break position
   is available to the application after the break has been returned. These
   numbers are *not* Perl regular expression repeat counts.

2. Comments are recognized and removed separately from otherwise parsing the
   rules. They may appear wherever a space would be allowed (and ignored.)

3. The implicit concatenation of adjacent terms has higher precedence than the
   `|` operation. "`ab|cd`" is interpreted as "`(ab)|(cd)`", not as "`a(b|c)d`" or
   "`(((ab)|c)d)`"

4. The syntax for [unicode-set](../strings/unicodeset.md) is defined (and parsed) by the `UnicodeSet` class.
   It is not repeated here.

5. For `$`variables that will be referenced from inside of a `UnicodeSet`, the
   definition must consist only of a Unicode Set. For example, when variable `$a`
   is used in a rule like `[$a$b$c]`, then this definition of `$a` is ok:
   “`$a=[:Lu:];`” while this one “`$a=abcd;`” would cause an error when `$a` was
   used.

6. Spaces are allowed nearly anywhere, and are not significant unless escaped.
   Exceptions to this are noted.

7. No spaces are allowed within a variable name. The variable name `$dictionary`
   is special. If defined, it must be a Unicode Set, the characters of which
   will trigger the use of word dictionary based boundaries.

8. A leading `^` on a rule prevents chaining into that rule. It can only match
   immediately after a preceding boundary, or at the start of text.

9. `{`nnn`}` appearing at the end of a rule is a Rule Status number, not a repeat
   count as it would be with conventional regular expression syntax.

10. A `/` in a rule specifies a hard break point. If the rule matches, a
    boundary will be forced at the position of the `/` within the match.

### EBNF Syntax used for the RBBI rules syntax description

| syntax | description |
| -- | ------------------------- |
| a? | zero or one instance of a |
| a+ | one or more instances of a |
| a* | zero or more instances of a |
| a \| b | either a or b, but not both |
| `a` "`a`" | the literal string between the quotes or displayed as `monospace` |

## Planned Changes and Removed or Deprecated Rule Features

1. Reverse rules could formerly be indicated by beginning them with an
   exclamation `!`. This syntax is deprecated, and will be removed from a
   future version of ICU.

2. `!!LBCMNoChain` was a global option that specified that characters with the
   line break property of "Combining Character" would not participate in rule
   chaining. This option was always considered internal, is deprecated and will
   be removed from a future version of ICU.

3. Naked rule characters. Plain text, in the context of a rule, is treated as
   literal text to be matched, much like normal regular expressions. This turns
   out to be very error prone, has been the source of bugs in released versions
   of ICU, and is not useful in implementing normal text boundary rules. A
   future version will reject literal text that is not escaped.

4. Exact reverse rules and safe forward rules: planned changes to the break
   engine implementation will remove the need for exact reverse rules and safe
   forward rules.

5. `{bof}` and `{eof}`, appearing within `[`sets`]`, match the beginning or ending of
   the input text, respectively. This is an internal (not documented) feature
   that will probably be removed in a future version of ICU. They are currently
   used by the standard rules for word, line and sentence breaking. An
   alternative is probably needed. The existing implementation is incomplete.

## Additional Sample Code

**C/C++**
See [icu/source/samples/break/](https://github.com/unicode-org/icu/tree/master/icu4c/source/samples/break/)
in the ICU source distribution for code samples showing the use of ICU boundary analysis.

## Details about Dictionary-Based Break Iteration

> :point_right: **Note**: This section below is originally from August 2012.
> It is probably out of date, for example `brkfiles.mk` does not exist anymore.

Certain Unicode characters have a "dictionary" bit set in the break iteration
rules, and text made up of these characters cannot be handled by the rules-based
break iteration code for lines or words. Rather, they must be handled by a
dictionary-based approach. The ICU approach is as follows:

Once the Dictionary bit is detected, the set of characters with that bit is
handed off to "dictionary code." This code then inspects the characters more
carefully, and splits them by script (Thai, Khmer, Chinese, Japanese, Korean).
If text in this script has not yet been handled, it loads the appropriate
dictionary from disk, and initializes a specialized "BreakEngine" class for that
script.

There are three such specialized classes: Thai, Khmer and CJK.

Thai and Khmer use very similar approaches. They look through a dictionary that
is not weighted by word frequency, and attempt to find the longest total "match"
that can be made in the text.

For Chinese and Japanese text, on the other hand, we have a unified dictionary
(due to the fact that both use some of the same characters, it is difficult to
distinguish them) that contains information about word frequencies. The
algorithm to match text then uses dynamic programming to find the set of breaks
it considers "most likely" based on the frequency of the words created by the
breaks. This algorithm could also be used for Thai and Khmer, but we do not have
sufficient data to do so. This algorithm could also be used for Korean, but once
again we do not have the data to do so.

Code of interest is in `source/common/dictbe.{h, cpp}`, `source/common/brkeng.{h,
cpp}`, `source/common/dictionarydata.{h, cpp}`. The dictionaries use the `BytesTrie`
and `UCharsTrie` as their data store. The binary form of these dictionaries is
produced by the `gendict` tool, which has source in `source/tools/gendict`.

In order to add new dictionary implementations, a few changes have to be made.
First, you should create a new subclass of `DictionaryBreakEngine` or
`LanguageBreakEngine` in `dictbe.cpp` that implements your algorithm. Then, in
`brkeng.cpp`, you should add logic to create this dictionary break engine if we
strike the appropriate script - which should only be 3 or so lines of code at
the most. Lastly, you should add the correct data file. If your data is to be
represented as a `.dict` file - as is recommended, and in fact required if you
don't want to make substantial code changes to the engine loader - you need to
simply add a file in the correct format for gendict to the `source/data/brkitr`
directory, and add its name to the list of `BRK_DICT_SOURCE` in
`source/data/brkitr/brkfiles.mk`. This will cause your dictionary (say, `foo.txt`)
to be added as a `UCharsTrie` dictionary with the name foo.dict. If you want your
dictionary to be a `BytesTrie` dictionary, you will need to specify a transform
within the `Makefile`. To do so, find the part of `source/data/Makefile.in` and
`source/data/makedata.mak` that deals with `thaidict.dict` and `khmerdict.dict` and
add a similar set of lines for your script. Lastly, in
`source/data/brkitr/root.txt`, add a line to the dictionaries `{}` section of the
form:

```
    shortscriptname:process(dependency){"dictionaryname.dict"}
```

For example, for Katakana:

```
    Kata:process(dependency){"cjdict.dict"}
```

Make sure to add appropriate tests for the new implementation.
