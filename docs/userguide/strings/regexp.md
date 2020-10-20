---
layout: default
title: Regular Expressions
nav_order: 6
parent: Chars and Strings
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Regular Expressions

## Overview

ICU's Regular Expressions package provides applications with the ability to
apply regular expression matching to Unicode string data. The regular expression
patterns and behavior are based on Perl's regular expressions. The C++
programming API for using ICU regular expressions is loosely based on the JDK
1.4 package java.util.regex, with some extensions to adapt it for use in a C++
environment. A plain C API is also provided.

The ICU Regular expression API supports operations including testing for a
pattern match, searching for a pattern match, and replacing matched text.
Capture groups allow subranges within an overall match to be identified, and to
appear within replacement text.

A Perl-inspired split() function that breaks a string into fields based on a
delimiter pattern is also included.

ICU Regular Expressions conform to version 19 of the
[Unicode Technical Standard \#18](http://www.unicode.org/reports/tr18/),
Unicode Regular Expressions, level 1, and in addition include Default Word
boundaries and Name Properties from level 2.

A detailed description of regular expression patterns and pattern matching
behavior is not included in this user guide. The best reference for this topic
is the book "Mastering Regular Expressions, 3rd Edition" by Jeffrey E. F.
Friedl, O'Reilly Media; 3rd edition (August 2006). Matching behavior can
sometimes be surprising, and this book is highly recommended for anyone doing
significant work with regular expressions.

## Using ICU Regular Expressions

The ICU C++ Regular Expression API includes two classes, `RegexPattern` and
`RegexMatcher`, that parallel the classes from the Java JDK package
java.util.regex. A `RegexPattern` represents a compiled regular expression while
`RegexMatcher` associates a `RegexPattern` and an input string to be matched, and
provides API for the various find, match and replace operations. In most cases,
however, only the class `RegexMatcher` is needed, and the existence of class
RegexPattern can safely be ignored.

The first step in using a regular expression is typically the creation of a
`RegexMatcher` object from the source (string) form of the regular expression.

`RegexMatcher` holds a pre-processed (compiled) pattern and a reference to an
input string to be matched, and provides API for the various find, match and
replace operations. `RegexMatchers` can be reset and reused with new input, thus
avoiding object creation overhead when performing the same matching operation
repeatedly on different strings.

The following code will create a `RegexMatcher` from a string containing a regular
expression, and then perform a simple `find()` operation.

        #include <unicode/regex.h>
        UErrorCode status = U_ZERO_ERROR;
        ...
        RegexMatcher *matcher = new RegexMatcher("abc+", 0, status);
        if (U_FAILURE(status)) {
            // Handle any syntax errors in the regular expression here
            ...
        }
        UnicodeString stringToTest = "Find the abc in this string";
        matcher->reset(stringToTest);
        if (matcher->find()) {
            // We found a match.
            int startOfMatch = matcher->start(status); // string index of start of match.
            ...
        }

Several types of matching tests are available

| Function      | Description                                                    |
|:--------------|:---------------------------------------------------------------|
| `matches()`   | True if the pattern matches the entire string, from the start through to the last character.
| `lookingAt()` | True if the pattern matches at the start of the string. The match need not include the entire string.
| `find()`      | True if the pattern matches somewhere within the string.  Successive calls to find() will find additional matches, until the string is exhausted.

If additional text is to be checked for a match with the same pattern, there is
no need to create a new matcher object; just reuse the existing one.

        myMatcher->reset(anotherString);
        if (myMatcher->matches(status)) {
            // We have a match with the new string.
        }

Note that matching happens directly in the string supplied by the application.
This reduces the overhead when resetting a matcher to an absolute minimum – the
matcher need only store a reference to the new string – but it does mean that
the application must be careful not to modify or delete the string while the
matcher is holding a reference to the string.

After finding a match, additional information is available about the range of
the input matched, and the contents of any capture groups. Note that, for
simplicity, any error parameters have been omitted. See the [API
reference](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classRegexMatcher.html) for
complete a complete description of the API.

| Function        | Description                                                    |
|:----------------|:---------------------------------------------------------------|
| `start()`       | Return the index of the start of the matched region in the input string.
| `end()`         | Return the index of the first character following the match.
| `group()`       | Return a UnicodeString containing the text that was matched.
| `start(n)`      | Return the index of the start of the text matched by the nth capture group.
| `end(n)`        | Return the index of the first character following the text matched by the nth capture group.
| `group(n)`      | Return a UnicodeString containing the text that was matched by the nth capture group.

## Regular Expression Metacharacters

| Character | outside of sets | \[inside sets\] |  Description |
|:----------|:----------------|:----------------|:-------------|
| \\a       | ✓               | ✓               | Match a BELL, \\u0007.
| \\A       | ✓               |                 | Match at the beginning of the input. Differs from ^ in that \\A will not match after a new line within the input.
| \\b       | ✓               |                 | Match if the current position is a word boundary. Boundaries occur at the transitions between word (\\w) and non-word (\\W) characters, with combining marks ignored. For better word boundaries, see [ICU Boundary Analysis](../boundaryanalysis/index.md).
| \\B       | ✓               |                 | Match if the current position is not a word boundary.
| \\cX      | ✓               | ✓               | Match a control-X character.
| \\d       | ✓               | ✓               | Match any character with the Unicode General Category of Nd (Number, Decimal Digit.)
| \\D       | ✓               | ✓               | Match any character that is not a decimal digit.
| \\e       | ✓               | ✓               | Match an ESCAPE, \\u001B.
| \\E       | ✓               | ✓               | Terminates a \\Q ...  \\E quoted sequence.
| \\f       | ✓               | ✓               | Match a FORM FEED, \\u000C.
| \\G       | ✓               | ✓               | Match if the current position is at the end of the previous match.
| \\h       | ✓               | ✓               | Match a Horizontal White Space character.  They are characters with Unicode General Category of Space_Separator plus the ASCII tab (\\u0009).
| \\H       | ✓               | ✓               | Match a non-Horizontal White Space character.
| \\k<name> | ✓               |                 | Named Capture Back Reference.
| \\n       | ✓               | ✓               | Match a LINE FEED, \\u000A.
| \\N{UNICODE CHARACTER NAME} | ✓  | ✓          | Match the named character.
| \\p{UNICODE PROPERTY NAME} | ✓   | ✓          | Match any character with the specified Unicode Property.
| \\P{UNICODE PROPERTY NAME} | ✓   | ✓          | Match any character not having the specified Unicode Property.
| \\Q       | ✓               | ✓               | Quotes all following characters until \\E.
| \\r       | ✓               | ✓               | Match a CARRIAGE RETURN, \\u000D.
| \\R       | ✓               |                 | Match a new line character, or the sequence CR LF. The new line characters are \\u000a, \\u000b, \\u000c, \\u000d, \\u0085, \\u2028, \\u2029.
| \\s       | ✓               | ✓               | Match a white space character. White space is defined as \[\\t\\n\\f\\r\\p{Z}\].
| \\S       | ✓               | ✓               | Match a non-white space character.
| \\t       | ✓               | ✓               | Match a HORIZONTAL TABULATION, \\u0009.
| \\uhhhh   | ✓               | ✓               | Match the character with the hex value hhhh.
| \\Uhhhhhhhh | ✓             | ✓               | Match the character with the hex value hhhhhhhh. Exactly eight hex digits must be provided, even though the largest Unicode code point is \\U0010ffff.
| \\v       | ✓               | ✓               | Match a new line character. The new line characters are \\u000a, \\u000b, \\u000c, \\u000d, \\u0085, \\u2028, \\u2029. Does not match the new line sequence CR LF.
| \\V       | ✓               | ✓               | Match a non-new line character.
| \\w       | ✓               | ✓               | Match a word character. Word characters are \[\\p{Alphabetic}\\p{Mark}\\p{Decimal_Number}\\p{Connector_Punctuation}\\u200c\\u200d\].
| \\W       | ✓               | ✓               | Match a non-word character.
| \\x{hhhh} | ✓               | ✓               | Match the character with hex value hhhh. From one to six hex digits may be supplied.
| \\xhh     | ✓               | ✓               | Match the character with two digit hex value hh.
| \\X       | ✓               |                 | Match a [Grapheme Cluster](http://www.unicode.org/reports/tr29/#Grapheme_Cluster_Boundaries).
| \\Z       | ✓               |                 | Match if the current position is at the end of input, but before the final line terminator, if one exists.
| \\z       | ✓               |                 | Match if the current position is at the end of input.
| \\*n*     | ✓               |                 | Back Reference. Match whatever the nth capturing group matched. n must be a number > 1 and < total number of capture groups in the pattern.
| \\0ooo    | ✓               | ✓               | Match an Octal character. 'ooo' is from one to three octal digits.  0377 is the largest allowed Octal character. The leading zero is required; it distinguishes Octal constants from back references.
| \[pattern\] | ✓             | ✓               | Match any one character from the set.
| .         | ✓               |                 | Match any character.
| ^         | ✓               |                 | Match at the beginning of a line.
| $         | ✓               |                 | Match at the end of a line. Line terminating characters are \\u000a, \\u000b, \\u000c, \\u000d, \\u0085, \\u2028, \\u2029 and the sequence \\u000d \\u000a.
| \\        | ✓               |                 | Quotes the following character. Characters that must be quoted to be treated as literals are \* ? + \[ ( ) { } ^ $ \| \\ .
| \\        |                 | ✓               | Quotes the following character. Characters that must be quoted to be treated as literals are \[ \] \\ Characters that may need to be quoted, depending on the context are - &

## Regular Expression Operators

| Operator      | Description
|:--------------|:---------------------------------------------------------------|
| `|`           | Alternation. A\|B matches either A or B.
| `*`           | Match 0 or more times. Match as many times as possible.
| `+`           | Match 1 or more times. Match as many times as possible.
| `?`           | Match zero or one times. Prefer one.
| `{n}`         | Match exactly n times
| `{n,}`        | Match at least n times. Match as many times as possible.
| `{n,m}`       | Match between n and m times. Match as many times as possible, but not more than m.
| `*?`          | Match 0 or more times. Match as few times as possible.
| `+?`          | Match 1 or more times. Match as few times as possible.
| `??`          | Match zero or one times. Prefer zero.
| `{n}?`        | Match exactly n times.
| `{n,}?`       | Match at least n times, but no more than required for an overall pattern match.
| `{n,m}?`      | Match between n and m times. Match as few times as possible, but not less than n.
| `*+`          | Match 0 or more times. Match as many times as possible when first encountered, do not retry with fewer even if overall match fails (Possessive Match).
| `++`          | Match 1 or more times. Possessive match.
| `?+`          | Match zero or one times. Possessive match.
| `{n}+`        | Match exactly n times.
| `{n,}+`       | Match at least n times. Possessive Match.
| `{n,m}+`      | Match between n and m times. Possessive Match.
| `( ...)`      | Capturing parentheses. Range of input that matched the parenthesized subexpression is available after the match.
| `(?: ...)`    | Non-capturing parentheses. Groups the included pattern, but does not provide capturing of matching text. Somewhat more efficient than capturing parentheses.
| `(?> ...)`    | Atomic-match parentheses. First match of the parenthesized subexpression is the only one tried; if it does not lead to an overall pattern match, back up the search for a match to a position before the "(?>".
| `(?# ...)`    | Free-format comment (?# comment ).
| `(?= ...)`    | Look-ahead assertion. True if the parenthesized pattern matches at the current input position, but does not advance the input position.
| `(?! ...)`    | Negative look-ahead assertion. True if the parenthesized pattern does not match at the current input position. Does not advance the input position.
| `(?<= ...)`   | Look-behind assertion. True if the parenthesized pattern matches text preceding the current input position, with the last character of the match being the input character just before the current position. Does not alter the input position. The length of possible strings matched by the look-behind pattern must not be unbounded (no \* or + operators.)
| `(?<! ...)`   | Negative Look-behind assertion. True if the parenthesized pattern does not match text preceding the current input position, with the last character of the match being the input character just before the current position. Does not alter the input position. The length of possible strings matched by the look-behind pattern must not be unbounded (no \* or + operators.)
| `(?<name>...)` | Named capture group. The <angle brackets> are literal - they appear in the pattern.
| `(?ismwx-ismwx:...)`  | Flag settings. Evaluate the parenthesized expression with the specified flags enabled or -disabled.
| `(?ismwx-ismwx)`      | Flag settings. Change the flag settings. Changes apply to the portion of the pattern following the setting. For example, (?i) changes to a case insensitive match.

## Set Expressions (Character Classes)

| Example       | Description
|:--------------|:---------------------------------------------------------------|
| `[abc]`                              | Match any of the characters a, b or c.
| `[^abc]`                             | Negation - match any character except a, b or c.
| `[A-M]`                              | Range - match any character from A to M. The characters to include are determined by Unicode code point ordering.
| `[\u0000-\U0010ffff]`                | Range - match all characters.
| `[\p{L}] [\p{Letter}] [\p{General_Category=Letter}]` | Characters with Unicode Category = Letter. All forms shown are equivalent.
| `[\P{Letter}]`                       | Negated property. (Upper case \P) Match everything except Letters.
| `[\p{numeric_value=9}]`              | Match all numbers with a numeric value of 9. Any Unicode Property may be used in set expressions.
| `[\p{Letter}&&\p{script=cyrillic}]`  | Logical AND or intersection. Match the set of all Cyrillic letters.
| `[\p{Letter}--\p{script=latin}]`     | Subtraction. Match all non-Latin letters.
| `[[a-z][A-Z][0-9]]` `[a-zA-Z0-9]`    | Implicit Logical OR or Union of Sets. The examples match ASCII letters and digits. The two forms are equivalent.
| `[:script=Greek:]`                   | Alternate POSIX-like syntax for properties. Equivalent to \\p{script=Greek}.

## Case Insensitive Matching

Case insensitive matching is specified by the UREGEX_CASE_INSENSITIVE flag
during pattern compilation, or by the (?i) flag within a pattern itself. Unicode
case insensitive matching is complicated by the fact that changing the case of a
string may change its length. See <http://www.unicode.org/faq/casemap_charprop.html>
for more information on Unicode casing operations.

Full case-insensitive matching handles situations where the number of characters
in equal string may differ. "fußball" compares equal "FUSSBALL", for example.

Simple case insensitive matching operates one character at a time on the strings
being compared. "fußball" does not compare equal to "FUSSBALL"

For ICU regular expression matching,

*   Anything from a regular expression pattern that looks like a literal string
    (even of one character) will be matched against the text using full case
    folding. The pattern string and the matched text may be of different
    lengths.
*   Any sequence that is composed by the matching engine from originally
    separate parts of the pattern will not match with the composition boundary
    within a case folding expansion of the text being matched.
*   Matching of \[set expressions\] uses simple matching. A \[set\] will match
    exactly one code point from the text.

Examples:

*   pattern "fussball" will match "fußball or "fussball".
*   pattern "fu(s)(s)ball" or "fus{2}ball" will match "fussball" or "FUSSBALL"
    but not "fußball.
*   pattern "ß" will find occurrences of "ss" or "ß".
*   pattern "s+" will not find "ß".

With these rules, a match or capturing sub-match can never begin or end in the
interior of an input text character that expanded when case folded.

## Flag Options

The following flags control various aspects of regular expression matching. The
flag values may be specified at the time that an expression is compiled into a
RegexPattern object, or they may be specified within the pattern itself using
the `(?ismx-ismx)` pattern options.

> :point_right: **Note**: The UREGEX_CANON_EQ option is not yet available.

| Flag (pattern) | Flag (API Constant) | Description
|:---------------|:--------------------|:-----------------|
|   | UREGEX_CANON_EQ         | If set, matching will take the canonical equivalence of characters into account. NOTE: this flag is not yet implemented.
| i | UREGEX_CASE_INSENSITIVE |  If set, matching will take place in a case-insensitive manner.
| x | UREGEX_COMMENTS         | If set, allow use of white space and #comments within patterns.
| s | UREGEX_DOTALL           | If set, a "." in a pattern will match a line terminator in the input text. By default, it will not. Note that a carriage-return / line-feed pair in text behave as a single line terminator, and will match a single "." in a RE pattern.  Line terminators are \\u000a, \\u000b, \\u000c, \\u000d, \\u0085, \\u2028, \\u2029 and the sequence \\u000d \\u000a.
| m | UREGEX_MULTILINE        | Control the behavior of "^" and "$" in a pattern. By default these will only match at the start and end, respectively, of the input text. If this flag is set, "^" and "$" will also match at the start and end of each line within the input text.
| w | UREGEX_UWORD            | Controls the behavior of \\b in a pattern. If set, word boundaries are found according to the definitions of word found in Unicode UAX 29, Text Boundaries. By default, word boundaries are identified by means of a simple classification of characters as either “word” or “non-word”, which approximates traditional regular expression behavior. The results obtained with the two options can be quite different in runs of spaces and other non-word characters.

## Using split()

ICU's split() function is similar in concept to Perl's – it will split a string
into fields, with a regular expression match defining the field delimiters and
the text between the delimiters being the field content itself.

Suppose you have a string of words separated by spaces:

        UnicodeString s = “dog cat giraffe”;

This code will extract the individual words from the string:

        UErrorCode status = U_ZERO_ERROR;
        RegexMatcher m(“\\s+”, 0, status);
        const int maxWords = 10;
        UnicodeString words[maxWords];
        int numWords = m.split(s, words, maxWords, status);

After the split():

| Variable        |  value       |
|:----------------|:-------------|
| `numWords`      | `3`
| `words[0]`      | `“dog”`
| `words[1]`      | `“cat”`
| `words[2]`      | `“giraffe”`
| `words[3 to 9]` | `“”`

The field delimiters, the spaces from the original string, do not appear in the
output strings.

Note that, in this example, `words` is a local, or stack array of actual
UnicodeString objects. No heap allocation is involved in initializing this array
of empty strings (C++ is not Java!). Local UnicodeString arrays like this are a
very good fit for use with split(); after extracting the fields, any values that
need to be kept in some more permanent way can be copied to their ultimate
destination.

If the number of fields in a string being split exceeds the capacity of the
destination array, the last destination string will contain all of the input
string data that could not be split, including any embedded field delimiters.
This is similar to split() in Perl.

If the pattern expression contains capturing parentheses, the captured data ($1,
$2, etc.) will also be saved in the destination array, interspersed with the
fields themselves.

If, in the “dog cat giraffe” example, the pattern had been `“(\s+)”` instead of
`“\s+”`, `split()` would have produced five output strings instead of three.
`Words[1]` and `words[3]` would have been the spaces.

## Find and Replace

Find and Replace operations are provided with the following functions.

| Function    | Description   |
|:------------|:--------------|
| `replaceFirst()` | Replace the first matching substring with the replacement text.  Performs the complete operation, including the `find()`.
| `replaceAll()` | Replace all matching substrings with the replacement text. Performs the complete operation, including all `find()`s.
| `appendReplacement()` | Incremental replace operation, intended to be used in a loop with `find()`.
| `appendTail()` | Final step in an incremental find & replace; appends any remaining text following the last replacement.

The replacement text for find-and-replace operations may contain references to
capture-group text from the find.

| Character | Descriptions  |
|:----------|:--------------|
| `$n`      | The text of capture group 'n' will be substituted for `$n`. n must be >= 0 and not greater than the number of capture groups. An unescaped $ in replacement text that is not followed by a capture group specification, either a number or name, is an error.
| `${name}` | The text of named capture group will be substituted. The name must appear in the pattern.
| `\`       | Treat the following character as a literal, suppressing any special meaning. Backslash escaping in substitution text is only required for '$' and '\\', but may be used on any other character without bad effects.

**Sample code showing the use of appendReplacement()**

        #include <stdio.h>
        #include "unicode/regex.h"

        int main() {
            UErrorCode status = U_ZERO_ERROR;
            RegexMatcher m(UnicodeString(" +"), 0, status);
            UnicodeString text("Here is some    text.");
            m.reset(text);

            UnicodeString result;
            UnicodeString replacement("_");
            int replacement_count = 0;

            while (m.find(status) && U_SUCCESS(status)) {
               m.appendReplacement(result, replacement, status);
               replacement_count++;
            }
            m.appendTail(result);

            char result_buf[100];
            result.extract(0, result.length(), result_buf, sizeof(result_buf));
            printf("The result of find & replace is \"%s\"\n", result_buf);
            printf("The number of replacements is %d\n", replacement_count);
        }

Running this sample produces the following:

        The result of find & replace is "Here_is_some_text."
        The number of replacements is 3

## Performance Tips

Some regular expression patterns can result in very slow match operations,
sometimes so slow that it will appear as though the match has gone into an
infinite loop. The problem is not unique to ICU - it affects any regular
expression implementation using a conventional nondeterministic finite automaton
(NFA) style matching engine. This section gives some suggestion on how to avoid
problems.

The performance problems tend to show up most commonly on failing matches - when
an input string does not match the regexp pattern. With a complex pattern
containing multiple \* or + (or similar) operators, the match engine will
tediously redistribute the input text between the different pattern terms, in a
doomed effort to find some combination that leads to a match (that doesn't
exist).

The running time for troublesome patterns is exponential with the length of the
input string. Every added character in the input doubles the (non)matching time.
It doesn't take a particularly long string for the projected running time to
exceed the age of the universe.

A simple pattern showing the problem is

  `(A+)+B`

matching against the string

  `AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC`

The expression can't match - there is no 'B' in the input - but the engine is
too dumb to realize that, and will try all possible permutations of rearranging
the input between the terms of the expression before failing.
Some suggestions:

*   Avoid, or examine carefully, any expressions with nested repeating
    quantifiers, like in the example above. They can often be recast in some
    other way. Any ambiguity in how input text could be distributed between the
    terms of the expression will cause problems.
*   Narrow every term in a pattern to match as small a set of characters as
    possible at each point. Fail as early as possible with bad input, rather
    than letting broad `.*` style terms eat intermediate input and relying on
    later terms in the expression to produce a failure.
*   Use possessive quantifiers when possible - `*+` instead of `*`, `++`
    instead of `+`
    These operators prevent backtracking; the initial match of a `*+` qualified
    pattern is either used in its entirety as part of the complete match, or it
    is not used at all.

*   Follow or surround `*` or `+` expressions with terms that the repeated
    expression can not match. The idea is to have only one possible way to match
    the input, with no possibility of redistributing the input between adjacent
    terms of the pattern.

*   Avoid overly long and complex regular expressions. Just because it's
    possible to do something completely in one large expression doesn't mean
    that you should. Long expressions are difficult to understand and can be
    almost impossible to debug when they go wrong. It is no sin to break a
    parsing problem into pieces and to have some code involved involved in the
    process.

*   Set a time limit. ICU includes the ability to limit the time spent on a
    regular expression match. This is a good idea when running untested
    expressions from users of your application, or as a fail safe for servers or
    other processes that cannot afford to be hung.

Examples from actual bug reports,

The pattern

        (?:[A-Za-z0-9]+[._]?){1,}[A-Za-z0-9]+\@(?:(?:[A-Za-z0-9]+[-]?){1,}[A-Za-z0-9]+\.){1,}
                      ^^^^^^^^^^^

and the text

        abcdefghijklmnopq

cause an infinite loop.

The problem is in the region marked with `^^^^^^^^^^`. The `"[._]?"` term can be ignored, because
it need not match anything. `{1,}` is the same as `+`. So we effectively have
`(?:[A-Za-z0-9]+)+`, which is trouble.

The initial part of the expression can be recast as

`[A-Za-z0-9]+([._][A-Za-z0-9]+)*`

which matches the same thing. The nested `+` and `*` qualifiers do not cause a
problem because the `[._]` term is not optional and contains no characters that
overlap with `[A-Za-z0-9]`, leaving no ambiguity in how input characters can be
distributed among terms in the match.

A further note: this expression was intended to parse email addresses, and has a
number of other flaws. For common tasks like this there are libraries of freely
available regular expressions that have been well debugged. It's worth making a
quick search before writing a new expression.

> :construction: **TODO**: add more examples.*

### Heap and Stack Usage

ICU keeps its match backtracking state on the heap. Because badly designed or
malicious patterns can result in matches that require large amounts of storage,
ICU sets a limit on heap usage by matches. The default is 8 MB; it can be
changed or removed via an API.

Because ICU does not use program recursion to maintain its backtracking state,
stack usage during matching operations is minimal, and does not increase with
complex patterns or large amounts of backtracking state. This is worth
mentioning only because excessive stack usage, resulting in blown off threads or
processes, can be a problem with some regular expression packages.

## Differences with Java Regular Expressions

*   ICU does not support UREGEX_CANON_EQ. See
    <https://unicode-org.atlassian.net/browse/ICU-9111>.
*   The behavior of \\cx (Control-X) differs from Java when x is outside the
    range A-Z. See <https://unicode-org.atlassian.net/browse/ICU-6068>.
*   Java allows quantifiers (\*, +, etc) on zero length tests. ICU does not.
    Occurrences of these in patterns are most likely unintended user errors, but
    it is an incompatibility with Java.
    <https://unicode-org.atlassian.net/browse/ICU-6080>
*   ICU recognizes all Unicode properties known to ICU, which is all of them.
    Java is restricted to just a few.
*   ICU case insensitive matching works with all Unicode characters, and, within
    string literals, does full Unicode matching (where matching strings may be
    different lengths.) Java does ASCII only by default, with Unicode aware case
    folding available as an option.
*   ICU has an extended syntax for set \[bracket\] expressions, including
    additional operators. Added for improved compatibility with the original ICU
    implementation, which was based on ICU UnicodeSet pattern syntax.
*   The property expression `\p{punct}` differs in what it matches. Java matches
    matches any of ```!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~```. From that list,
    ICU omits ```$+<=>^`|~``` &nbsp; &nbsp;
    ICU follows the recommendations from Unicode UTS-18,
    <http://www.unicode.org/reports/tr18/#Compatibility_Properties>. See also
    <https://unicode-org.atlassian.net/browse/ICU-20095>.
