---
layout: default
title: Universal Time Scale
nav_order: 5
parent: Date/Time
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Universal Time Scale
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

There are quite a few different conventions for binary datetime, depending on
the platform or protocol. Some of these have severe drawbacks. For example,
people using Unix time (seconds since Jan 1, 1970, usually in a 32-bit integer)
think that they are safe until near the year 2038. But cases can and do arise
where arithmetic manipulations causes serious problems. Consider the computation
of the average of two datetimes, for example: if one calculates them with
`averageTime = (time1 + time2)/2`, there will be overflow even with dates
beginning in 2004. Moreover, even if these problems don't occur, there is the
issue of conversion back and forth between different systems.

Binary datetimes differ in a number of ways: the data type, the unit, and the
epoch (origin). We'll refer to these as time scales. For example: (Sorted by
epoch and unit, descending. In Java, `int64_t`=`long` and `int32_t`=`int`.)

| Source                                         | Data Type                                                                          | Epoch       | Unit                                                    |
| ---------------------------------------------- | ---------------------------------------------------------------------------------- | ----------- | ------------------------------------------------------- |
| MacOS X (`CFDate/NSDate`)                      | `double` (1.0=1s but fractional seconds are used as well; imprecise for 0.1s etc.) | 2001-Jan-01 | seconds (and fractions thereof)                         |
| Unix `time_t`                                  | `int32_t` or `int64_t` (`signed int32_t` limited to 1970..2038)                    | 1970-Jan-01 | seconds                                                 |
| Java `Date`                                    | `int64_t`                                                                          | 1970-Jan-01 | milliseconds                                            |
| Joda `DateTime`                                | `int64_t`                                                                          | 1970-Jan-01 | milliseconds                                            |
| ICU4C `UDate`                                  | `double` (does not use fractional milliseconds)                                    | 1970-Jan-01 | milliseconds                                            |
| JavaScript `Date`                              | `double` (does not use fractional milliseconds; JavaScript Number stores a double) | 1970-Jan-01 | milliseconds                                            |
| Unix `struct timeval (as in gettimeofday)`     | `struct: time_t` (seconds); suseconds_t (microseconds)                             | 1970-Jan-01 | microseconds                                            |
| Gnome `g_get_real_time()`                      | `gint64`                                                                           | 1970-Jan-01 | microseconds                                            |
| Unix `struct timespec` (as in `clock_gettime`) | `struct: time_t` (seconds); long (nanoseconds)                                     | 1970-Jan-01 | nanoseconds                                             |
| MacOS (old)                                    | `uint32_t` (1904..2040)                                                            | 1904-Jan-01 | seconds                                                 |
| Excel                                          | ?                                                                                  | 1899-Dec-31 | days                                                    |
| DB2                                            | ?                                                                                  | 1899-Dec-31 | days                                                    |
| Windows `FILETIME`                             | `int64_t`                                                                          | 1601-Jan-01 | ticks (100 nanoseconds; finest granularity in industry) |
| .NET `DateTime`                                | `uint62` (only 0001-9999; only 62 bits; also 2-bit field for UTC/local)            | 0001-Jan-01 | ticks (100 nanoseconds; finest granularity in industry) |
| ICU Universal Time Scale                       | `int64_t`                                                                          | 0001-Jan-01 | same as .Net but allows 29000BC..29000AD                |

All of the epochs start at 00:00 am (the earliest possible time on the day in
question), and are usually assumed to be UTC.

The ranges, in years, for different data types are given in the following table.
The range for integer types includes the entire range expressible with positive
and negative values of the data type. The range for double is the range that
would be allowed without losing precision to the corresponding unit.

| Units                  | 64-bit integer          | Double         | 32-bit integer |
| ---------------------- | ----------------------- | -------------- | -------------- |
| 1 second               | 5.84542x10<sup>11</sup> | 285,420,920.94 | 136.10         |
| 1 millisecond          | 584,542,046.09          | 285,420.92     | 0.14           |
| 1 microsecond          | 584,542.05              | 285.42         | 0.00           |
| 100 nanoseconds (tick) | 58,454.20               | 28.54          | 0.00           |
| 1 nanosecond           | 584.5420461             | 0.2854         | 0.00           |

ICU implements a universal time scale that is similar to the 
[.NET framework's System.DateTime](https://docs.microsoft.com/dotnet/api/system.datetime?view=netframework-4.8).
The universal time scale is a 64-bit integer that holds ticks since midnight,
January 1<sup>st</sup>, 0001. Negative values are supported. This has enough
range to guarantee that calculations involving dates around the present are safe.

The universal time scale always measures time according to the proleptic
Gregorian calendar. That is, the Gregorian calendar's leap year rules are used
for all times, even before 1582 when it was introduced. (This is different from
the default ICU calendar which switches from the Julian to the Gregorian
calendar in 1582. See `GregorianCalendar::setGregorianChange()` and
`ucal_setGregorianChange()`).

ICU provides conversion functions to and from all other major time scales,
allowing datetimes in any time scale to be converted to the universal time
scale, safely manipulated, and converted back to any other datetime time scale.

## Background

So how did we decide what to use for the universal time scale? Java time has
plenty of range, but cannot represent a .NET `System.DateTime` value without
severe loss of precision. ICU4C time addresses this by using a `double` that is
otherwise equivalent to the Java time. However, there are disadvantages with
doubles. They provide for much more graceful degradation in arithmetic
operations. But they only have 53 bits of accuracy, which means that they will
lose precision when converting back and forth to ticks. What would really be
nice would be a `long double` (80 bits -- 64 bit mantissa), but that is not
supported on most systems.

The Unix extended time uses a structure with two components: time in seconds and
a fractional field (microseconds). However, this is clumsy, slow, and prone to
error (you always have to keep track of overflow and underflow in the fractional
field). `BigDecimal` would allow for arbitrary precision and arbitrary range, but
we did not want to use this as the normal type, because it is slow and does not
have a fixed size.

Because of these issues, we concluded that the .NET `System.DateTime` is the best
timescale to use. However, we use the full range allowed by the data type,
allowing for datetimes back to 29,000 BC and up to 29,000 AD. (`System.DateTime`
uses only 62 bits and only supports dates from 0001 AD to 9999 AD). This time
scale is very fine grained, does not lose precision, and covers a range that
will meet almost all requirements. It will not handle the range that Java times
do, but frankly, being able to handle dates before 29,000 BC or after 29,000 AD
is of very limited interest.

## Constants

ICU provides routines to convert from other timescales to the universal time
scale, to convert from the universal time scale to other timescales, and to get
information about a particular timescale. In all of these routines, the
timescales are referenced using an integer constant, according to the following
table:

| Source                 | ICU4C                         | ICU4J                    |
| ---------------------- | ----------------------------- | ------------------------ |
| Java                   | `UDTS_JAVA_TIME`              | `JAVA_TIME`              |
| Unix                   | `UDTS_UNIX_TIME`              | `UNIX_TIME`              |
| ICU4C                  | `UDTS_ICU4C_TIME`             | `ICU4C_TIME`             |
| Windows FILETIME       | `UDTS_WINDOWS_FILE_TIME`      | `WINDOWS_FILE_TIME`      |
| .NET DateTime          | `UDTS_DOTNET_DATE_TIME`       | `DOTNET_DATE_TIME`       |
| Macintosh (old)        | `UDTS_MAC_OLD_TIME`           | `MAC_OLD_TIME`           |
| Macintosh              | `UDTS_MAC_TIME`               | `MAC_TIME`               |
| Excel                  | `UDTS_EXCEL_TIME`             | `EXCEL_TIME`             |
| DB2                    | `UDTS_DB2_TIME`               | `DB2_TIME`               |
| Unix with microseconds | `UDTS_UNIX_MICROSECONDS_TIME` | `UNIX_MICROSECONDS_TIME` |

The routine that gets a particular piece of information about a timescale takes
an integer constant that identifies the particular piece of information,
according to the following table:

| Value                | ICU4C                      | ICU4J                |
| -------------------- | -------------------------- | -------------------- |
| Precision            | `UTSV_UNITS_VALUE`         | `UNITS_VALUE`        |
| Epoch offset         | `UTSV_EPOCH_OFFSET_VALUE`  | `EPOCH_OFFSET_VALUE` |
| Minimum "from" value | `UTSV_FROM_MIN_VALUE`      | `FROM_MIN_VALUE`     |
| Maximum "from" value | `UTSV_FROM_MAX_VALUE`      | `FROM_MAX_VALUE`     |
| Minimum "to" value   | `UTSV_TO_MIN_VALUE`        | `TO_MIN_VALUE`       |
| Maximum "to" value   | `UTSV_TO_MAX_VALUE`        | `TO_MAX_VALUE`       |

Here is what the values mean:

* Precision -- the precision of the timescale, in ticks.
* Epoch offset -- the distance from the universal timescale's epoch to the timescale's epoch, in the timescale's precision.
* Minimum "from" value -- the minimum timescale value that can safely be converted to the universal timescale.
* Maximum "from" value -- the maximum timescale value that can safely be converted to the universal timescale.
* Minimum "to" value -- the minimum universal timescale value that can safely be converted to the timescale.
* Maximum "to" value -- the maximum universal timescale value that can safely be converted to the timescale.

## Converting

You can convert from other timescale values to the universal timescale using the
"from" methods. In ICU4C, you use `utmscale_fromInt64`:

```c
UErrorCode err = U_ZERO_ERROR;
int64_t unixTime = ...;
int64_t universalTime;

universalTime = utmscale_fromInt64(unixTime, UDTS_UNIX_TIME, &err);
```

In ICU4J, you use `UniversalTimeScale.from`:

```java
long javaTime = ...;
long universalTime;

universalTime = UniversalTimeScale.from(javaTime, UniversalTimeScale.JAVA_TIME);
```

You can convert values in the universal timescale to other timescales using the
"to" methods. In ICU4C, you use `utmscale_toInt64`:

```c
UErrorCode err = U_ZERO_ERROR;
int64_t universalTime = ...;
int64_t unixTime;

unixTime = utmscale_toInt64(universalTime, UDTS_UNIX_TIME, &err);
```

In ICU4J, you use `UniversalTimeScale.to`:

```java
long universalTime = ...;
long javaTime;

javaTime = UniversalTimeScale.to(universalTime, UniversalTimeScale.JAVA_TIME);
```

That's all there is to it!

If the conversion is out of range, the ICU4C routines
will set the error code to `U_ILLEGAL_ARGUMENT_ERROR`, and the ICU4J methods will
throw `IllegalArgumentException`. In ICU4J, you can avoid out of range conversions
by using the `BigDecimal` methods:

```java
long fileTime = ...;
double icu4cTime = ...;
BigDecimal utICU4C, utFile, utUnix, unixTime, macTime;

utFile   = UniversalTimeScale.bigDecimalFrom(fileTime, UniversalTime.WINDOWS_FILE_TIME);

utICU4C  = UniversalTimeScale.bigDecimalFrom(icu4cTime, UniversalTimeScale.ICU4C_TIME);

unixTime = UniversalTimeScale.toBigDecimal(utFile, UniversalTime.UNIX_TIME);
macTime  = UniversalTimeScale.toBigDecimal(utICU4C, UniversalTime.MAC_TIME);

utUnix   = UniversalTimeScale.bigDecimalFrom(unixTime, UniversalTime.UNIX_TIME);
```

> :point_right: **Note**: Because the Universal Time Scale has a finer resolution
> than some other time scales, time values that can be represented exactly in the 
> Universal Time Scale will be rounded when converting to these time scales, and
> resolution will be lost. If you convert these values back to the Universal Time
> Scale, you will not get the same time value that you started with. If the time
> scale to which you are converting uses a double to represent the time value, you
> may loose precision even though the double supports a range that is larger than
> the range supported by the Universal Time Scale.

## Formatting and Parsing

Currently, ICU does not support direct formatting or parsing of Universal Time
Scale values. If you want to format a Universal Time Scale value, you will need
to convert it to an ICU time scale value first. Use `UTDS_ICU4C_TIME` with ICU4C,
and `UniversalTimeScale.JAVA_TIME` with ICU4J.

When you parse a datetime string, the result will be an ICU time scale value.
You can convert this value to a Universal Time Scale value using `UDTS_ICU4C_TIME`
with ICU4C, and `UniversalTime.JAVA_TIME` for ICU4J.

See the previous section, *Converting*, for details of how to do the conversion.

## Getting Timescale Information

To get information about a particular timescale in ICU4C, use
`utmscale_getTimeScaleValue`:

```c
UErrorCode err = U_ZERO_ERROR;
int64_t unixEpochOffset = utmscale_getTimeScaleValue(
    UDTS_UNIX_TIME,
    UTSV_EPOCH_OFFSET_VALUE,
    &err);
```

In ICU4J, use `UniversalTimeScale.getTimeScaleValue`:

```java
long javaEpochOffset = UniversalTimeScale.getTimeScaleValue(
    UniversalTimeScale.JAVA_TIME,
    UniversalTimeScale.EPOCH_OFFSET_VALUE);
```

If the integer constants for selecting the timescale or the timescale value are
out of range, the ICU4C routines will set the error code to
`U_ILLEGAL_ARGUMENT_ERROR`, and the ICU4J methods will throw
`IllegalArgumentException`.
