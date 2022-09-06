---
layout: default
title: Boundary Analysis
nav_order: 1300
has_children: true
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Boundary Analysis
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview of Text Boundary Analysis

Text boundary analysis is the process of locating linguistic boundaries while
formatting and handling text. Examples of this process include:

1. Locating appropriate points to word-wrap text to fit within specific margins
   while displaying or printing.

2. Locating the beginning of a word that the user has selected.

3. Counting characters, words, sentences, or paragraphs.

4. Determining how far to move the text cursor when the user hits an arrow key
    (Some characters require more than one position in the text store and some
    characters in the text store do not display at all).

5. Making a list of the unique words in a document.

6. Figuring out if a given range of text contains only whole words.

7. Capitalizing the first letter of each word.

8. Locating a particular unit of the text (For example, finding the third word
    in the document).

The `BreakIterator` classes were designed to support these kinds of tasks. The
BreakIterator objects maintain a location between two characters in the text.
This location will always be a text boundary. Clients can move the location
forward to the next boundary or backward to the previous boundary. Clients can
also check if a particular location within a source text is on a boundary or
find the boundary which is before or after a particular location.

## Four Types of BreakIterator

ICU `BreakIterator`s can be used to locate the following kinds of text boundaries:

1. Character Boundary

2. Word Boundary

3. Line-break Boundary

4. Sentence Boundary

Each type of boundary is found in accordance with the rules specified by Unicode
Standard Annex #29, *Unicode Text Segmentation*
(<https://www.unicode.org/reports/tr29/> ) or Unicode Standard Annex #14, *Unicode
Line Breaking Algorithm* (<https://www.unicode.org/reports/tr14/>)

### Character Boundary

The character-boundary iterator locates the boundaries according to the rules
defined in <http://www.unicode.org/reports/tr29/#Grapheme_Cluster_Boundaries>.
These boundaries try to match what a user would think of as a "character"—a
basic unit of a writing system for a language—which may be more than just a
single Unicode code point.

The letter `Ä`, for example, can be represented in Unicode either with a single
code-point value or with two code-point values (one representing the `A` and
another representing the umlaut `¨`). The character-boundary iterator will treat
either representation as a single character.

End-user characters, as described above, are also called grapheme clusters, in
an attempt to limit the confusion caused by multiple meanings for the word
"character".

### Word Boundary

The word-boundary iterator locates the boundaries of words, for purposes such as
double click selection or "Find whole words" operations.

Words boundaries are identified according to the rules in
<https://www.unicode.org/reports/tr29/#Word_Boundaries>, supplemented by a word
dictionary for text in Chinese, Japanese, Thai or Khmer. The rules used for
locating word breaks take into account the alphabets and conventions used by
different languages.

Here's an example of a sentence, showing the boundary locations that will be
identified by a word break iterator:

> :point_right: **Note**: TODO: An example needs to be added here.

### Line-break Boundary

The line-break iterator locates positions that would be appropriate points to
wrap lines when displaying the text. The boundary rules are define here:
<https://www.unicode.org/reports/tr14/>

This example shows the differences in the break locations produced by word and
line break iterators:

> :point_right: **Note**: TODO: An example needs to be added here.

### Sentence Boundary

A sentence-break iterator locates sentence boundaries according to the rules
defined here: <https://www.unicode.org/reports/tr29/#Sentence_Boundaries>

## Dictionary-Based BreakIterator

Some languages are written without spaces, and word and line breaking requires
more than rules over character sequences. ICU provides dictionary support for
word boundaries in Chinese, Japanese, Thai, Lao, Khmer and Burmese.

Use of the dictionaries is automatic when text in one of the dictionary
languages is encountered. There is no separate API, and no extra programming
steps required by applications making use of the dictionaries.

## Usage

To locate boundaries in a document, create a BreakIterator using the
`BreakIterator::create***Instance` family of methods in C++, or the `ubrk_open()`
function (C), where "`***`" is `Character`, `Word`, `Line` or `Sentence`,
depending on the type of iterator wanted. These factory methods also take a
parameter that specifies the locale for the language of the text to be processed.

When creating a `BreakIterator`, a locale is also specified, and the behavior of
the BreakIterator obtained may be specialized in some way for that locale. For
most locales the default break iterator behavior is used.

Applications also may register customized BreakIterators for use in specific
locales. Once such a break iterator has been registered, any requests for break
iterators for that locale will return copies of the registered break iterator.

ICU may cache service instances. Therefore, registration should be done during
startup, before opening services by locale ID.

In the general-usage-model, applications will use the following basic steps to
analyze a piece of text for boundaries:

1. Create a `BreakIterator` with the desired behavior

2. Use the `setText()` method to set the iterator to analyze a particular piece
   of text.

3. Locate the desired boundaries using the appropriate combination of `first()`,
   `last()`, `next()`, `previous()`, `preceding()`, and `following()` methods.

The `setText()` method can be called more than once, allowing reuse of a
BreakIterator on new pieces of text. Because the creation of a `BreakIterator` can
be relatively time-consuming, it makes good sense to reuse them when practical.

The iterator always points to a boundary position between two characters. The
numerical value of the position, as returned by `current()` is the zero-based
index of the character following the boundary. Thus a position of zero
represents a boundary preceding the first character of the text, and a position
of one represents a boundary between the first and second characters.

The `first()` and `last()` methods reset the iterator's current position to the
beginning or end of the text (the beginning and the end are always considered
boundaries). The `next()` and `previous()` methods advance the iterator one boundary
forward or backward from the current position. If the `next()` or `previous()`
methods run off the beginning or end of the text, it returns DONE. The `current()`
method returns the current position.

The `following()` and `preceding()` methods are used for random access, to move the
iterator to an arbitrary position within the text. Since a BreakIterator always
points to a boundary position, the `following()` and `preceding()` methods will
never set the iterator to point to the position specified by the caller (even if
it is, in fact, a boundary position). `BreakIterator` will, however, set the
iterator to the nearest boundary position before or after the specified
position.

`isBoundary()` returns true if the specified position is a boundary.

### Thread Safety

`BreakIterator`s are not thread safe. This is inherit in their design—break
iterators are stateful, holding a reference to and position in the text, meaning
that a single instance cannot operate in parallel on multiple texts.

For concurrent break iteration, each thread must use its own break iterator.
These can be obtained by creating separate break iterators of the desired type,
or by initially creating a main break iterator and then creating a clone for
each thread.

### Line Breaking Strictness, a CSS Property

CSS has the concept of "[Line Breaking
Strictness](https://www.w3.org/TR/css-text-3/#line-break-property)". This
property specifies the strictness of line-breaking rules applied within an
element: especially how wrapping interacts with punctuation and symbols. ICU
line break iterators can choose a strictness using locale tags:

| Locale       | Behavior    |
| ------------ | ----------- |
| `en@lb=strict` <br/> `ja@lb=strict`  | Breaks text using the most stringent set of line-breaking rules |
| `en@lb=normal` <br/> `ja@lb=normal`  | Breaks text using the most common set of line-breaking rules. |
| `en@lb=loose`  <br/> `ja@lb=loose`   | Breaks text using the least restrictive set of line-breaking rules. Typically used for short lines, such as in newspapers. |

### Sentence Break Filters

Sentence breaking can return false positives - an indication that sentence ends
in an incorrect position - in the presence of abbreviations. For example,
consider the sentence

> In the meantime Mr. Weston arrived with his small ship.

Default sentence break shows a false boundary following the "Mr."

ICU includes lists of common abbreviations that can be used to filter, to
ignore, these false sentence boundaries. Filtering is enabled by the presence of
the `ss` locale tag when creating the break iterator.

| Locale           | Behavior                                                |
| ---------------- | ------------------------------------------------------- |
| `en`             |  no filtering                                           |
| `en@ss=standard` |  Filter based on common English language abbreviations. |
| `es@ss=standard` |  Filter with common Spanish abbreviations.              |

Abbreviation lists are available (as of ICU 64) for English, German, Spanish,
French, Italian and Portuguese.

## Accuracy

ICU's break iterators are based on the default boundary rules described in the
Unicode Standard Annexes [14](https://www.unicode.org/reports/tr14/) and
[29](https://www.unicode.org/reports/tr29/). These are relatively
simple boundary rules that can be implemented efficiently, and are sufficient
for many purposes and languages. However, some languages and applications will
require a more sophisticated linguistic analysis of the text in order to find
boundaries with good accuracy. Such an analysis is not directly available from
ICU at this time.

Break Iterators based on custom, user-supplied boundary rules can be created and
used by applications with requirements that are not met by the standard default
boundary rules.

## BreakIterator Boundary Analysis Examples

### Print out all the word-boundary positions in a UnicodeString

**In C++:**

```c++
void listWordBoundaries(const UnicodeString& s) {
    UErrorCode status = U_ZERO_ERROR;
    BreakIterator* bi = BreakIterator::createWordInstance(Locale::getUS(), status);
    bi->setText(s);
    int32_t p = bi->first();
    while (p != BreakIterator::DONE) {
        printf("Boundary at position %d\n", p);
        p = bi->next();
    }
    delete bi;
}
```

**In C:**

```c
void listWordBoundaries(const UChar* s, int32_t len) {
    UBreakIterator* bi;
    int32_t p;
    UErrorCode err = U_ZERO_ERROR;
    bi = ubrk_open(UBRK_WORD, 0, s, len, &err);
    if (U_FAILURE(err)) return;
    p = ubrk_first(bi);
    while (p != UBRK_DONE) {
        printf("Boundary at position %d\n", p);
        p = ubrk_next(bi);
    }
    ubrk_close(bi);
}
```

### Get the boundaries of the word that contains a double-click position

**In C++:**

```c++
void wordContaining(BreakIterator& wordBrk,
        int32_t idx,
        const UnicodeString& s,
        int32_t& start,
        int32_t& end) {
    // this function is written to assume that we have an
    // appropriate BreakIterator stored in an object or a
    // global variable somewhere-- When possible, programmers
    // should avoid having the create() and delete calls in
    // a function of this nature.
    if (s.isEmpty())
        return;
    wordBrk.setText(s);
    start = wordBrk.preceding(idx + 1);
    end = wordBrk.next();
    // NOTE: for this and similar operations, use preceding() and next()
    // as shown here, not following() and previous(). preceding() is
    // faster than following() and next() is faster than previous()
    // NOTE: By using preceding(idx + 1) above, we're adopting the convention
    // that if the double-click comes right on top of a word boundary, it
    // selects the word that _begins_ on that boundary (preceding(idx) would
    // instead select the word that _ends_ on that boundary).
}
```

**In C:**

```c
void wordContaining(UBreakIterator* wordBrk,
    int32_t idx,
    const UChar* s,
    int32_t sLen,
    int32_t* start,
    int32_t* end,
    UErrorCode* err) {
    if (wordBrk == NULL || s == NULL || start == NULL || end == NULL) {
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    ubrk_setText(wordBrk, s, sLen, err);
    if (U_SUCCESS(*err)) {
        *start = ubrk_preceding(wordBrk, idx + 1);
        *end = ubrk_next(wordBrk);
    }
}
```

### Check for Whole Words

Use the following to check if a range of text is a "whole word":

**In C++:**

```c++
UBool isWholeWord(BreakIterator& wordBrk,
    const UnicodeString& s,
    int32_t start,
    int32_t end) {
    if (s.isEmpty())
        return false;
    wordBrk.setText(s);
    if (!wordBrk.isBoundary(start))
        return false;
    return wordBrk.isBoundary(end);
}
```

**In C:**

```c
UBool isWholeWord(UBreakIterator* wordBrk,
    const UChar* s,
    int32_t sLen,
    int32_t start,
    int32_t end,
    UErrorCode* err) {
    UBool result = false;
    if (wordBrk == NULL || s == NULL) {
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return false;
    }
    ubrk_setText(wordBrk, s, sLen, err);
    if (U_SUCCESS(*err)) {
        result = ubrk_isBoundary(wordBrk, start) && ubrk_isBoundary(wordBrk, end);
    }
    return result;
}
```

Count the words in a document (C++ only):

```c++
int32_t containsLetters(RuleBasedBreakIterator& bi, const UnicodeString& s, int32_t start) {
    bi.setText(s);
    int32_t count = 0;
    while (start != BreakIterator::DONE) {
        int breakType = bi.getRuleStatus();
        if (breakType != UBRK_WORD_NONE) {
            // Exclude spaces, punctuation, and the like.
            // A status value UBRK_WORD_NONE indicates that the boundary does
            // not start a word or number.
            //
            ++count;
        }
        start = bi.next();
    }
    return count;
}
```

The function `getRuleStatus()` returns an enum giving additional information on
the text preceding the last break position found. Using this value, it is
possible to distinguish between numbers, words, words containing kana
characters, words containing ideographic characters, and non-word characters,
such as spaces or punctuation. The sample uses the break status value to filter
out, and not count, boundaries associated with non-word characters.

### Word-wrap a document (C++ only)

The sample function below wraps a paragraph so that each line is less than or
equal to 72 characters. The function fills in an array passed in by the caller
with the starting offsets of
each line in the document. Also, it fills in a second array to track how many
trailing white space characters there are in the line. For simplicity, it is
assumed that an outside process has already broken the document into paragraphs.
For example, it is assumed that every string the function is passed has a single
newline at the end only.

```c++
int32_t wrapParagraph(const UnicodeString& s,
                   const Locale& locale,
                   int32_t lineStarts[],
                   int32_t trailingwhitespace[],
                   int32_t maxLines,
                   UErrorCode &status) {

    int32_t        numLines = 0;
    int32_t        p, q;
    const int32_t MAX_CHARS_PER_LINE = 72;
    UChar          c;

    BreakIterator *bi = BreakIterator::createLineInstance(locale, status);
    if (U_FAILURE(status)) {
        delete bi;
        return 0;
    }
    bi->setText(s);


    p = 0;
    while (p < s.length()) {
        // jump ahead in the paragraph by the maximum number of
        // characters that will fit
        q = p + MAX_CHARS_PER_LINE;

        // if this puts us on a white space character, a control character
        // (which includes newlines), or a non-spacing mark, seek forward
        // and stop on the next character that is not any of these things
        // since none of these characters will be visible at the end of a
        // line, we can ignore them for the purposes of figuring out how
        // many characters will fit on the line)
        if (q < s.length()) {
            c = s[q];
            while (q < s.length()
                   && (u_isspace(c)
                       || u_charType(c) == U_CONTROL_CHAR
                       || u_charType(c) == U_NON_SPACING_MARK
            )) {
                ++q;
                c = s[q];
            }
        }

        // then locate the last legal line-break decision at or before
        // the current position ("at or before" is what causes the "+ 1")
        q = bi->preceding(q + 1);

        // if this causes us to wind back to where we started, then the
        // line has no legal line-break positions. Break the line at
        // the maximum number of characters
        if (q == p) {
            p += MAX_CHARS_PER_LINE;
            lineStarts[numLines] = p;
            trailingwhitespace[numLines] = 0;
            ++numLines;
        }
        // otherwise, we got a good line-break position. Record the start of this
        // line (p) and then seek back from the end of this line (q) until you find
        // a non-white space character (same criteria as above) and
        // record the number of white space characters at the end of the
        // line in the other results array
        else {
            lineStarts[numLines] = p;
            int32_t nextLineStart = q;

            for (q--; q > p; q--) {
                c = s[q];
                if (!(u_isspace(c)
                       || u_charType(c) == U_CONTROL_CHAR
                       || u_charType(c) == U_NON_SPACING_MARK)) {
                    break;
                }
            }
            trailingwhitespace[numLines] = nextLineStart - q -1;
            p = nextLineStart;
           ++numLines;
        }
        if (numLines >= maxLines) {
            break;
        }
    }
    delete bi;
    return numLines;
}
```

Most text editors would not break lines based on the number of characters on a
line. Even with a monospaced font, there are still many Unicode characters that
are not displayed and therefore should be filtered out of the calculation. With
a proportional font, character widths are added up until a maximum line width is
exceeded or an end of the paragraph marker is reached.

Trailing white space does not need to be counted in the line-width measurement
because it does not need to be displayed at the end of a line. The sample code
above returns an array of trailing white space values because an external
rendering process needs to be able to measure the length of the line (without
the trailing white space) to justify the lines. For example, if the text is
right-justified, the invisible white space would be drawn outside the margin.
The line would actually end with the last visible character.

In either case, the basic principle is to jump ahead in the text to the location
where the line would break (without taking word breaks into account). Then, move
backwards using the preceding() method to find the last legal breaking position
before that location. Iterating straight through the text with next() method
will generally be slower.

## ICU BreakIterator Data Files

The source code for the ICU break rules for the standard boundary types is
located in the directory
[icu4c/source/data/brkitr/rules](https://github.com/unicode-org/icu/tree/main/icu4c/source/data/brkitr/rules).
These files will be built, and the corresponding binary state tables
incorporated into ICU's data, by the standard ICU4C build process.

The dictionary word lists used by word break, and for some languages, line break
are in
[icu4c/source/data/brkitr/dictionaries](https://github.com/unicode-org/icu/tree/main/icu4c/source/data/brkitr/dictionaries).

The same data is used by both ICU4C and ICU4J. In the normal ICU build process,
the source data is processed into a binary form using ICU4C, and the resulting
binary tables are incorporated into ICU4J.
