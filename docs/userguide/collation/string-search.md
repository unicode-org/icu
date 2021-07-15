---
layout: default
title: String Search
nav_order: 4
parent: Collation
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# String Search Service
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

String searching, also known as string matching, is a very important subject in
the wider domain of text processing and analysis. Many software applications use
the basic string search algorithm in the implementations on most operating
systems. With the popularity of Internet, the quantity of available data from
different parts of the world has increased dramatically within a short time.
Therefore, a string search algorithm that is language-aware has become more
important. A bitwise match that uses the `u_strstr` (C), `UnicodeString::indexOf`
(C++) or `String.indexOf` (Java) APIs will not yield the correct result specific
to a particular language's requirements. The APIs will not yield the correct
result because all the issues that are important to language-sensitive collation
are also applicable to text searching. The following lists those issues which
are applicable to text searching:

1.  Accented letters\
    In English, accents are treated as minor variations of a letter. In French,
    accented letters have much more significance as they can actually change the
    meaning of a word. Very often, an accented letter is actually a distinct
    letter. For example, letter 'å' (\\u00e5) may be just a letter 'a' with an
    accent symbol to English speakers. However, it is actually a distinct letter
    in Danish; in Danish searching for 'a' should generally not match 'å' and
    vice versa. In some cases, such as in traditional German, an accented letter
    is short-hand for something longer. In sorting, an 'ä' (\\u00e4) is treated
    as 'ae'. Note that primary- and secondary-level distinctions for *searching*
    may not be the same as those for sorting; in ICU, many languages provide a
    special "search" collator with the appropriate level settings for search.

2.  Conjoined letters\
    Special handling is required when a single letter is treated equivalent to
    two distinct letters and vice versa. For example, in German, the letter 'ß'
    (\\u00df) is treated as 'ss' in sorting. Also, in most languages, 'æ'
    (\\u00e6) is considered equivalent to the letter 'a' followed by the letter
    'e'. Also, the ligatures are often treated as distinct letters by
    themselves. For example, 'ch' is treated as a distinct letter between the
    letter 'c' and the letter 'd' in Spanish.

3.  Ignorable punctuation\
    As in collation, it is important that the user is able to choose to ignore
    punctuation symbols while the user searches for a pattern in the string. For
    example, a user may search for "blackbird" and want to include entries such
    as "black-bird".

## ICU String Search Model

The ICU string search service provides similar APIs to the other text iterating
services. Allowing users to specify the starting position and direction within
the text string to be searched. For more information, please see the [Boundary
Analysis](../boundaryanalysis/index.md) chapter. The user can locate one or all
occurrences of a pattern in a string. For a given collator, a pattern match is
located at the offsets <start, end> in a string if the collator finds that the
sub-string between the start and end is equal.

The string search service supports two different types of canonical match
behavior.

Let S' be the sub-string of a text string S between the offsets start and end
<start, end>.
A pattern string P matches a text string S at the offsets <start, end> if

1.  option 1. P matches some canonical equivalent string of S'. Suppose the
    collator used for searching has a tertiary collation strength, all accents
    are non-ignorable. If the pattern "a\\u0300" is searched in the target text
    "a\\u0325\\u0300", a match will be found, since the target text is
    canonically equivalent to "a\\u0300\\u0325"

2.  option 2. P matches S' and if P starts or ends with a combining mark, there
    exists no non-ignorable combining mark before or after S' in S respectively.
    Following the example above, the pattern "a\\u0300" will not find a match in
    "a\\u0325\\u0300", since there exists a non-ignorable accent '\\u0325' in
    the middle of 'a' and '\\u0300'. Even with a target text of
    "a\\u0300\\u0325" a match will not be found because of the non-ignorable
    trailing accent \\u0325.

One restriction is to be noted for option 1. Currently there are no composite
characters that consists of a character with combining class greater than 0
before a character with combining class equals to 0. However, if such a
character exists in the future, the string search service may not work correctly
with option 1 when such characters are encountered.

Furthermore, option 1 could generate more than one "encompassing" matches. For
example, in Danish, 'å' (\\u00e5) and 'aa' are considered equivalent. So the
pattern "baad" will match "a--båd--man" (a--b\\u00e5d--man) at the start offset
at 3 and the end offset 5. However, the start offset can be 1 or 2 and the end
offset can be 6 or 7, because "-" (hyphen) is ignorable for a certain collation.
The ICU implementation always returns the offsets of the shortest match
sub-string. To be more exact, the string search added a "tightest" match
condition. In other words, if the pattern matches at offsets <start, end> as
well as offsets <start + 1, end>, the offsets <start, end> are not considered a
match. Likewise, if the pattern matches at offsets <start, end> as well as
offsets <start, end + 1>, the offsets <start, end + 1> are not considered a
match. Therefore, when the option 1 is chosen in Danish collator, 'baad' will
match in the string "a--båd--man" (a--b\\u00e5d--man) ONLY at offsets <3,5>.

The default behavior is that described in option 2 above. To obtain the behavior
described in option 1, you must set the normalization mode to ON in the collator
used for search.

> :point_right: **Note**: The "tightest match" behavior described above
> is defined as "Minimal Match" in
> [Section 8 Searching and Matching in UTS #10 Unicode Collation Collation Algorithm](http://www.unicode.org/reports/tr10/#Searching).
> "Medial Match" and "Maximal Match" are not yet implemented by the ICU String Search service.

The string search service also supports two varieties of “asymmetric search” as
described in *[Section 8.2 Asymmetric Search in UTS #10 Unicode Collation
Collation Algorithm](http://www.unicode.org/reports/tr10/#Asymmetric_Search)*.
With asymmetric search, for example, unaccented characters are treated as
“wildcards” that may match any character with the same primary weight, this
behavior can be applied just to characters in the search pattern, or to
characters in both the search pattern and the searched text. With the former
behavior, searching with French behavior for 'e' might match 'e', 'è', 'é', 'ê',
and so one, while search for 'é' would only match 'é'.

Both a locale or collator can be used to specify the language-sensitive rules
for searches. When a locale is specified, a collator will be created internally
and the StringSearch instance that is created is responsible for the ownership
of the collator. All the collation attributes will be considered during the
string search operation. However, the users only can set the collator attributes
using the collator APIs. Normalization is usually done within collation and the
process is outside the scope of the string search service.

As in other iterator interfaces, the string search service provides APIs to
perform string matching for the first pattern occurrence, immediate next,
previous match, and the last pattern occurrence. There are also options to allow
for overlapping matching. For example, in English, if the string is "ababab" and
the pattern is "abab", overlapping matching produces results of offsets <0, 3>
and <2, 5>. Otherwise, the mutually exclusive matching produces the result
offset <0, 3> only. To find a whole word match, the user can provide a
locale-specific `BreakIterator` object to a `StringSearch` instance to correctly
locate the word boundaries. For example, if "c" exists in the string "abc", a
match is returned. However, the behavior can be overwritten by supplying a word
`BreakIterator`.

The minimum unit of match is aligned to an extended grapheme cluster in the ICU
string search service implementation defined by [UAX #29 Unicode Text
Segmentation](http://www.unicode.org/reports/tr29/). Therefore, all matches will
begin and end on extended grapheme cluster boundaries. If the given input search
pattern starts with non-base character, no matches will be returned.
When there are contractions in the collation sequence and the contraction
happens to span across the boundary of a match, it is not considered a match.
For example, in traditional Spanish where 'ch' is a contraction, the "har"
pattern will not match in the string "uno charo". Boundaries that are
discontiguous contractions will yield a match result similar to those described
above, where the end of the match returned will be one character before the
immediate following base letter. In addition, only the first match will be
located if a pattern contains only combining marks and the search string
contains more than one occurrences of the pattern consecutively. For example, if
the user searches for the pattern "´" (\\u00b4) in the string "A´´B",
(A\\u00b4\\u00b4B) the result will be offsets <1, 2>.

### Example

**In C:**

```c
    char *tgtstr = "The quick brown fox jumps over the lazy dog.";
    char *patstr = "fox";
    UChar target[64];

    UChar pattern[16];
    int pos = 0;
    UErrorCode status = U_ZERO_ERROR;
    UStringSearch *search = NULL;

    u_uastrcpy(target, tgtstr);
    u_uastrcpy(pattern, patstr);


    search = usearch_open(pattern, -1, target, -1, "en_US", 
                          NULL, &status);


    if (U_FAILURE(status)) {
        fprintf(stderr, "Could not create a UStringSearch.\n");
        return;
    }

    for(pos = usearch_first(search, &status);
        U_SUCCESS(status) && pos != USEARCH_DONE;
        pos = usearch_next(search, &status))
    {
        fprintf(stdout, "Match found at position %d.\n", pos);
    }

    if (U_FAILURE(status)) {
        fprintf(stderr, "Error searching for pattern.\n");
    }
```

**In C++:**

```c++
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString target("Jackdaws love my big sphinx of quartz.");
    UnicodeString pattern("sphinx");
    StringSearch search(pattern, target, Locale::getUS(), NULL, status);


    if (U_FAILURE(status)) {
        fprintf(stderr, "Could not create a StringSearch object.\n");
        return;
    }

    for(int pos = search.first(status);
        U_SUCCESS(status) && pos != USEARCH_DONE;
        pos = search.next(status))
    {
        fprintf(stdout, "Match found at position %d.\n", pos);
    }

    if (U_FAILURE(status)) {
        fprintf(stderr, "Error searching for pattern.\n");
    }
```

**In Java:**

```java
    StringCharacterIterator target = new StringCharacterIterator(
                                         "Pack my box with five dozen liquor jugs.");
    String pattern = "box";

    try {
        StringSearch search = new StringSearch(pattern, target, Locale.US);


        for(int pos = search.first();
            pos != StringSearch.DONE;
            pos = search.next())
        {
            System.out.println("Match found for pattern at position " + pos); 
        }
    } catch (Exception e) {
        System.err.println("StringSearch failure: " + e.toString());
    }
```

## Performance and Other Implications

The ICU string search service is designed to be on top of the ICU collation
service. Therefore, all the performance implications that apply to a collator
are also applicable to the string search service. To obtain the best
performance, use the default collator attributes described in the Performance
and Storage Implications on Attributes section in the [Collation Service
Architecture](architecture#performance-and-storage-implications-of-attributes)
chapter. In addition, users need to be aware of
the following `StringSearch` specific considerations:

### Search Algorithm

ICU4C releases up to 3.8 used the Boyer-Moore search algorithm in the string
search service. There were some known issues in these previous releases.
(See ICU tickets [ICU-5024](https://unicode-org.atlassian.net/browse/ICU-5024),
[ICU-5382](https://unicode-org.atlassian.net/browse/ICU-5382),
[ICU-5420](https://unicode-org.atlassian.net/browse/ICU-5420))

In ICU4C 4.0, the string
search service was updated with the simple linear search algorithm, which
locates a match by shifting a cursor in the target text one by one, and these
issues were fixed. In ICU4C 4.0.1, the Boyer-Moore search code was reintroduced
as a separated API set as a technology preview. In a later release, this code was deleted.

The Boyer-Moore searching
algorithm is based on automata or combinatorial properties of strings and
pre-processes the pattern and known to be much faster than the linear search
when search pattern length is longer. According to performance evaluation
between these two implementations, the Boyer-Moore search is faster than the
linear search when the pattern text is longer than 3 or 4 characters.
However, it is very tricky to get correct results with a collation-based Boyer-Moore search.

### Change Iterating Direction

The ICU string search service provides a set of very dynamic APIs that allow
users to change the iterating direction randomly. For example, users can search
for a particular word going forward by calling the `usearch_next` (C),
`StringSearch::next` (C++) or `StringSearch.next` (Java) APIs and then search
backwards at any point of the search operation by calling the `usearch_previous`
(C), `StringSearch::previous` (C++) or `StringSearch.previous` (Java) APIs. Another
way to change the iterating direction is by calling the `usearch_reset` (C),
`StringSearch::previous` (C++) or `StringSearch.previous` (Java) APIs. Though the
direction change can occur without calling the reset APIs first, this operation
comes with a reduction in speed.

> :point_right: **Note**: The backward search is not available with the
> ICU4C Boyer-Moore search technology preview introduced in ICU4C 4.0.1
> and only available with the linear search implementation.

### Thai and Lao Character Boundaries

In collation, certain Thai and Lao vowels are swapped with the next character.
For example, the text string "A ขเ" (A \\u0e02\\u0e40) is processed internally
in collation as
"A เข" (A \\u0e40\\u0e02). Therefore, if the user searches for the pattern "Aเ"
(A\\u0e40) in "A ขเ" (A \\u0e02\\u0e40) the string search service will match
starting at offset 0. Since this normalization process is internal to collation,
there is no notification that the swapping has happened. The return result
offsets in this example will be <0, 2> even though the range would encompass one
extra character.

### Case Level Search

Case level string search is currently done with the strength set to tertiary.
When searching with the strength set to primary and the case level attribute
turned on, results given may not be correct. The case level attribute is
different from tertiary strength in that accents are ignored but case
differences are not. Suppose you wanted to search for “A” in the text
“ABC\\u00C5a”. The match found should be at 0 and 3 if using the case level
attribute. However, searching with the case level attribute turned on finds
matches at 0, 3, and 4, which includes the lower case 'a'. To ensure that case
level differences are not ignored, string search must be done with at least
tertiary strength.
