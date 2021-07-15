---
layout: default
title: Rounding Modes
nav_order: 2
grand_parent: Formatting
parent: Formatting Numbers
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Rounding Modes
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

The following rounding modes are used with ICU's Decimal Formatter. Note that
ICU's use of the terms "Down" and "Up" here are somewhat at odds with other
definitions, but are equivalent to the same modes used in Java's JDK.

## Comparison of Rounding Modes

This chart shows the values -2.0 through 2.0 in increments of 0.1, and shows the
resulting ICU format when formatted with no decimal digits.

| #    | CEILING | FLOOR | DOWN | UP | HALFEVEN | HALFDOWN | HALFUP | #    |
|------|---------|-------|------|----|----------|----------|--------|------|
| -2.0 | -2      | -2    | -2   | -2 | -2       | -2       | -2     | -2.0 |
| -1.9 | -1      | -2    | -1   | -2 | -2       | -2       | -2     | -1.9 |
| -1.8 | -1      | -2    | -1   | -2 | -2       | -2       | -2     | -1.8 |
| -1.7 | -1      | -2    | -1   | -2 | -2       | -2       | -2     | -1.7 |
| -1.6 | -1      | -2    | -1   | -2 | -2       | -2       | -2     | -1.6 |
| -1.5 | -1      | -2    | -1   | -2 | -2       | -1       | -2     | -1.5 |
| -1.4 | -1      | -2    | -1   | -2 | -1       | -1       | -1     | -1.4 |
| -1.3 | -1      | -2    | -1   | -2 | -1       | -1       | -1     | -1.3 |
| -1.2 | -1      | -2    | -1   | -2 | -1       | -1       | -1     | -1.2 |
| -1.1 | -1      | -2    | -1   | -2 | -1       | -1       | -1     | -1.1 |
| -1.0 | -1      | -1    | -1   | -1 | -1       | -1       | -1     | -1.0 |
| -0.9 | -0      | -1    | -0   | -1 | -1       | -1       | -1     | -0.9 |
| -0.8 | -0      | -1    | -0   | -1 | -1       | -1       | -1     | -0.8 |
| -0.7 | -0      | -1    | -0   | -1 | -1       | -1       | -1     | -0.7 |
| -0.6 | -0      | -1    | -0   | -1 | -1       | -1       | -1     | -0.6 |
| -0.5 | -0      | -1    | -0   | -1 | -0       | -0       | -1     | -0.5 |
| -0.4 | -0      | -1    | -0   | -1 | -0       | -0       | -0     | -0.4 |
| -0.3 | -0      | -1    | -0   | -1 | -0       | -0       | -0     | -0.3 |
| -0.2 | -0      | -1    | -0   | -1 | -0       | -0       | -0     | -0.2 |
| -0.1 | -0      | -1    | -0   | -1 | -0       | -0       | -0     | -0.1 |
| 0.0  | 0       | 0     | 0    | 0  | 0        | 0        | 0      | 0.0  |
| 0.1  | 1       | 0     | 0    | 1  | 0        | 0        | 0      | 0.1  |
| 0.2  | 1       | 0     | 0    | 1  | 0        | 0        | 0      | 0.2  |
| 0.3  | 1       | 0     | 0    | 1  | 0        | 0        | 0      | 0.3  |
| 0.4  | 1       | 0     | 0    | 1  | 0        | 0        | 0      | 0.4  |
| 0.5  | 1       | 0     | 0    | 1  | 0        | 0        | 1      | 0.5  |
| 0.6  | 1       | 0     | 0    | 1  | 1        | 1        | 1      | 0.6  |
| 0.7  | 1       | 0     | 0    | 1  | 1        | 1        | 1      | 0.7  |
| 0.8  | 1       | 0     | 0    | 1  | 1        | 1        | 1      | 0.8  |
| 0.9  | 1       | 0     | 0    | 1  | 1        | 1        | 1      | 0.9  |
| 1.0  | 1       | 1     | 1    | 1  | 1        | 1        | 1      | 1.0  |
| 1.1  | 2       | 1     | 1    | 2  | 1        | 1        | 1      | 1.1  |
| 1.2  | 2       | 1     | 1    | 2  | 1        | 1        | 1      | 1.2  |
| 1.3  | 2       | 1     | 1    | 2  | 1        | 1        | 1      | 1.3  |
| 1.4  | 2       | 1     | 1    | 2  | 1        | 1        | 1      | 1.4  |
| 1.5  | 2       | 1     | 1    | 2  | 2        | 1        | 2      | 1.5  |
| 1.6  | 2       | 1     | 1    | 2  | 2        | 2        | 2      | 1.6  |
| 1.7  | 2       | 1     | 1    | 2  | 2        | 2        | 2      | 1.7  |
| 1.8  | 2       | 1     | 1    | 2  | 2        | 2        | 2      | 1.8  |
| 1.9  | 2       | 1     | 1    | 2  | 2        | 2        | 2      | 1.9  |
| 2.0  | 2       | 2     | 2    | 2  | 2        | 2        | 2      | 2.0  |
| #    | CEILING | FLOOR | DOWN | UP | HALFEVEN | HALFDOWN | HALFUP | #    |

### Half Even

This is ICU's default rounding mode. Values exactly on the 0.5 (half) mark
(shown dotted in the chart) are rounded to the nearest even digit. This is often
called Banker's Rounding because it is, on average, free of bias. It is the
default mode specified for IEEE 754 floating point operations.

Also known as ties-to-even, round-to-nearest, RN or RNE.

### Half Down

Values exactly on the 0.5 (half) mark are rounded down (next smaller absolute
value, towards zero).

### Half Up

Values exactly on the 0.5 (half) mark are rounded up (next larger absolute
value, away from zero).

### Down

All values are rounded towards the next smaller absolute value (rounded towards
zero, or RZ).

Also known as: truncation, because the insignificant decimal places are simply
removed.

### Up

All values are rounded towards the next greater absolute value (away from zero).

### Ceiling

All values are rounded towards positive infinity (+∞). Also known as RI for
Rounds to Infinity.

### Floor

All values are rounded towards negative infinity (-∞). Also known as RMI for
Rounds to Minus Infinity.

### Unnecessary

The mode "Unnecessary" doesn't perform any rounding, but instead returns an
error if the value cannot be represented exactly without rounding.

## **Other References/Comparison**

*   Decimal Context docs (used by ICU4C to implement rounding):
    <http://speleotrove.com/decimal/decifaq1.html#rounding>
*   Java 7 docs:
    <http://docs.oracle.com/javase/7/docs/api/java/math/RoundingMode.html>
*   IEEE 754 rounding rules:
    <http://en.wikipedia.org/wiki/IEEE_754-2008#Rounding_rules>
*   Wikipedia article on Rounding:
    <http://en.wikipedia.org/wiki/Rounding#Tie-breaking>
*   Live rounding mode chart: [Rounding Mode
    Chart](https://htmlpreview.github.io/?https://github.com/unicode-org/icu-demos/blob/master/roundmode/round.html)
    and [Source
    Code](https://github.com/unicode-org/icu-demos/tree/master/roundmode)
