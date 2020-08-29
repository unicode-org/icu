---
layout: default
title: RuleBasedNumberFormat
nav_order: 5
grand_parent: Formatting
parent: Formatting Numbers
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# RuleBasedNumberFormat
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

# Overview

[RuleBasedNumberFormat](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classRuleBasedNumberFormat.html)
can format and parse numbers in spelled-out format, e.g. "one hundred and
thirty-four". For example:

```
"one hundred and thirty-four" // 134 using en_US spellout
"one hundred and thirty-fourth" // 134 using en_US ordinal
"hundertvierunddreissig" // 134 using de_DE spellout
"MCMLVIII" // custom, 1958 in roman numerals
```

RuleBasedNumberFormat is based on rules describing how to format a number. The
rule syntax is designed primarily for formatting and parsing numbers as
spelled-out text, though other kinds of formatting are possible. As a
convenience, custom API is provided to allow selection from three predefined
rule definitions, when available: SPELLOUT, ORDINAL, and DURATION. Users can
request formatters either by providing a locale and one of these predefined rule
selectors, or by specifying the rule definitions directly.

> :point_right: **Note**: ICU provides number spellout rules for several locales, but not for all of the
locales that ICU supports, and not all of the predefined rule types. Also, as of
release 2.6, some of the provided rules are known to be incomplete.

## Instantiation

Unlike the other standard number formats, there is no corresponding factory
method on NumberFormat. Instead, RuleBasedNumberFormat objects are instantiated
via constructors. Constructors come in two flavors, ones that take rule text,
and ones that take one of the predefined selectors. Constructors that do not
take a Locale parameter use the current default locale.

The following constructors are available:

1.  **RuleBasedNumberFormat(int)**
    Returns a format using predefined rules of the selected type from the
    current locale.

2.  **RuleBasedNumberFormat(Locale, int)**
    As above, but specifies locale.

3.  **RuleBasedNumberFormat(String)**
    Returns a format using the provided rules, and symbols (if required) from
    the current locale.

4.  **RuleBasedNumberFormat(String, Locale)**
    As above, but specifies locale.

## Usage

RuleBasedNumberFormat can be used like other NumberFormats. For example, in
Java:

```java
double num = 2718.28;
NumberFormat formatter = 
    new RuleBasedNumberFormat(RuleBasedNumberFormat.SPELLOUT);
String result = formatter.format(num);
System.out.println(result);

// output (in en_US locale):
// two thousand seven hundred and eighteen point two eight
```

## Rule Sets

Rule descriptions can provide multiple named rule sets, for example, the rules
for en_US spellout provides a '%simplified' rule set that displays text without
commas or the word 'and'. Rule sets can be queried and set on a
RuleBasedNumberFormat. This lets you customize a RuleBasedNumberFormat for use
through its inherited NumberFormat API. For example, in Java:

You can also format a number specifying the ruleset directly, using an
additional overload of format provided by RuleBasedNumberFormat. For example, in
Java:

> :point_right: **Note**: There is no standardization of rule set names, so you must either query the
names, as in the first example above, or know the names that are defined in the
rules for that formatter.

## Rules

The following example provides a quick look at the RuleBasedNumberFormat rule
syntax.

These rules format a number using standard decimal place-value notation, but
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

Rulesets are invoked by first applying negative and fractional rules, and then
using a recursive process. It starts by finding the rule whose range includes
the current value and applying that rule. If the rule so directs, it emits text,
including text obtained by recursing on new values as directed by the rule. As
you can see, the rules are designed to accomodate recursive processing of
numbers, and so are best suited for formatting numbers in ways that are
inherently recursive.

A full explanation of this example can be found in the [RuleBasedNumberFormat
examples](rbnf-examples.md). A complete description of the rule syntax can be
found in the [RuleBasedNumberFormat API
Documentation](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classRuleBasedNumberFormat.html).
