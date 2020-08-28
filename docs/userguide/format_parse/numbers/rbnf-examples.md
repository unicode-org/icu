---
layout: default
title: RuleBasedNumberFormat Examples
nav_order: 6
grand_parent: Formatting
parent: Formatting Numbers
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# `RuleBasedNumberFormat` Examples
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Annotated `RuleBasedNumberFormat` Example

The following example provides a quick idea of how the rules work. The
[`RuleBasedNumberFormat` API
documentation](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classRuleBasedNumberFormat.html)
describes the rule syntax in more detail.

This ruleset formats a number using standard decimal place-value notation, but
using words instead of digits, e.g. 123.4 formats as 'one two three point four':

```
"-x: minus >>;\n"
+ "x.x: << point >>;\n"
+ "zero; one; two; three; four; five; six;\n"
+ "    seven; eight; nine;\n"
+ "10: << >>;\n"
+ "100: << >>>;\n"
+ "1000: <<, >>>;\n"
+ "1,000,000: <<, >>>;\n"
+ "1,000,000,000: <<, >>>;\n"
+ "1,000,000,000,000: <<, >>>;\n"
+ "1,000,000,000,000,000: =#,##0=;\n";
```

In this example, the rules consist of one (unnamed) ruleset. It lists nineteen
rules, each terminated by a semicolon. It starts with two special rules for
handling negative numbers and non-integers. (This is true of most rulesets.)
Following are rules for increasing integer ranges, up to 10e15. The portion of
the rule before a colon, if any, provides information about the range and some
additional information about how to apply the rule. Most rule bodies (following
the colon) consist of recursion instructions and/or plain text substitutions.
The rules in this example work as follows:

1.  **-x: minus >>;**
    If the number is negative, output the string 'minus ' and recurse using the
    absolute value.

2.  **x.x: << point >>;**
    If the number is not an integer, recurse using the integral part, emit the
    string ' point ', and process the ruleset in 'fractional mode' for the
    fractional part. Generally, this emits single digits.

3.  **zero; one; ... nine;**
    Each of these ten rules applies to a range. By default, the first range
    starts at zero, and succeeding ranges start at the previous start + 1. These
    ranges all default, so each of these ten rules has a 'range' of a single
    integer, 0 to 9. When the current value is in one of these ranges, the rules
    emit the corresponding text (e.g. 'one', 'two', and so on).

4.  **10: << >>;**
    This starts a new range at 10 (not default) and sets the limit of the range
    for the previous rule. Divide the number by the divisor (which defaults to
    the highest power of 10 lower or equal to range start value, e.g. 10),
    recurse using the integral part, emit the string ' ' (space), then recurse
    using the remainder.

5.  **100: << >>>;**
    This starts a new range at 100 (again, limiting the previous rule's range).
    It is similar to the previous rule, except for the use of '>>>'. '>>' means
    to recurse by matching the value against all the ranges to find the rule,
    '>>>' means to recurse using the previous rule. We must force the previous
    rule in order to get the rule for 'ten' invoked in order to emit '0' when
    processing numbers like 105.

6.  **1000: <<, >>>; 1,000,000: ...**
    These start new ranges at intervals of 1000. They are all similar to the
    rule for 100 except they output ', ' (comma space) to delimit thousands.
    Note that the range value can include commas for readability.

7.  **1,000... =#,##0=;**
    This last rule in the ruleset applies to all values at or over 10e15. The
    pattern '==' means to use the current unmodified value, and text within in
    the pattern (this works for '<<' and similar patterns as well) describes the
    ruleset or decimal format to use. If this text starts with '0' or '#', it is
    presumed to be a decimal format pattern. So this rule means to format the
    unmodified number using a decimal format constructed with the pattern
    '#,##0'.

Rulesets are invoked by first applying negative and fractional rules, then by
finding the rule whose range includes the current value and applying that rule,
recursing as directed by the rule. Again, a complete description of the rule
syntax can be found in the [API
Documentation](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classRuleBasedNumberFormat.html).

More rule examples can be found in the `RuleBasedNumberFormat` [demo
source](https://github.com/unicode-org/icu/blob/master/icu4j/demos/src/com/ibm/icu/dev/demo/rbnf/RbnfSampleRuleSets.java).
