# Rounding Modes

The following rounding modes are used with ICU's Decimal Formatter. Note that
ICU's use of the terms "Down" and "Up" here are somewhat at odds with other
definitions, but are equivalent to the same modes used in Java's JDK.

## Comparison of Rounding Modes

This chart shows the values -2.0 through 2.0 in increments of 0.1, and shows the
resulting ICU format when formatted with no decimal digits.

#CEILINGFLOORDOWNUPHALFEVENHALFDOWNHALFUP#

[-2.0](#m20)-2-2-2-2-2-2-2[-2.0](#m20)

[-1.9](#m19)-1-2-1-2-2-2-2[-1.9](#m19)

[-1.8](#m18)-1-2-1-2-2-2-2[-1.8](#m18)

[-1.7](#m17)-1-2-1-2-2-2-2[-1.7](#m17)

[-1.6](#m16)-1-2-1-2-2-2-2[-1.6](#m16)

[-1.5](#m15)-1-2-1-2-2-1-2[-1.5](#m15)

[-1.4](#m14)-1-2-1-2-1-1-1[-1.4](#m14)

[-1.3](#m13)-1-2-1-2-1-1-1[-1.3](#m13)

[-1.2](#m12)-1-2-1-2-1-1-1[-1.2](#m12)

[-1.1](#m11)-1-2-1-2-1-1-1[-1.1](#m11)

[-1.0](#m10)-1-1-1-1-1-1-1[-1.0](#m10)

[-0.9](#m09)-0-1-0-1-1-1-1[-0.9](#m09)

[-0.8](#m08)-0-1-0-1-1-1-1[-0.8](#m08)

[-0.7](#m07)-0-1-0-1-1-1-1[-0.7](#m07)

[-0.6](#m06)-0-1-0-1-1-1-1[-0.6](#m06)

[-0.5](#m05)-0-1-0-1-0-0-1[-0.5](#m05)

[-0.4](#m04)-0-1-0-1-0-0-0[-0.4](#m04)

[-0.3](#m03)-0-1-0-1-0-0-0[-0.3](#m03)

[-0.2](#m02)-0-1-0-1-0-0-0[-0.2](#m02)

[-0.1](#m01)-0-1-0-1-0-0-0[-0.1](#m01)

[0.0](#p00)0000000[0.0](#p00)

[0.1](#p01)1001000[0.1](#p01)

[0.2](#p02)1001000[0.2](#p02)

[0.3](#p03)1001000[0.3](#p03)

[0.4](#p04)1001000[0.4](#p04)

[0.5](#p05)1001001[0.5](#p05)

[0.6](#p06)1001111[0.6](#p06)

[0.7](#p07)1001111[0.7](#p07)

[0.8](#p08)1001111[0.8](#p08)

[0.9](#p09)1001111[0.9](#p09)

[1.0](#p10)1111111[1.0](#p10)

[1.1](#p11)2112111[1.1](#p11)

[1.2](#p12)2112111[1.2](#p12)

[1.3](#p13)2112111[1.3](#p13)

[1.4](#p14)2112111[1.4](#p14)

[1.5](#p15)2112212[1.5](#p15)

[1.6](#p16)2112222[1.6](#p16)

[1.7](#p17)2112222[1.7](#p17)

[1.8](#p18)2112222[1.8](#p18)

[1.9](#p19)2112222[1.9](#p19)

[2.0](#p20)2222222[2.0](#p20)

#CEILINGFLOORDOWNUPHALFEVENHALFDOWNHALFUP#

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
    Chart](http://source.icu-project.org/repos/icu/icuapps/trunk/roundmode/round.html)
    and [Source
    Code](http://source.icu-project.org/repos/icu/icuapps/trunk/roundmode/)
