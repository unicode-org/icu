---
layout: default
title: Calendar Services
nav_order: 1
parent: Date/Time
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Calendar Classes
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

ICU has two main calendar classes used for parsing and formatting Calendar
information correctly:

1.  `Calendar`

    An abstract base class that defines the calendar API. This API supports
    UDate to fields conversion and field arithmetic.

2.  `GregorianCalendar`

    A concrete subclass of `Calendar` that implements the standard calendar used
    today internationally.

In addition to these, ICU has other `Calendar` subclasses to support
non-gregorian calendars including:

*   Japanese

*   Buddhist

*   Chinese

*   Persian

*   Indian

*   Islamic

*   Hebrew

*   Indian

*   Coptic

*   Ethiopic

The `Calendar` class is designed to support additional calendar systems in the future.

> :point_right: **Note**: *`Calendar` classes are related to `UDate`, the `TimeZone` classes, and the `DateFormat`
classes.*

### Calendar locale and keyword handling

When a calendar object is created, via either `Calendar::create()`, or
`ucal_open()`, or indirectly within a date formatter, ICU looks up the 'default'
calendar type for that locale. At present, all locales default to a Gregorian
calendar, except for the compatibility locales th_TH_TRADITIONAL and
ja_JP_TRADITIONAL. If the "calendar" keyword is supplied, this value will
override the default for that locale.

For instance, `Calendar::createInstance("fr_FR", status)` will create a Gregorian calendar,
but `Calendar::createInstance("fr_FR@calendar=buddhist")` will create a Buddhist calendar.

It is an error to use an invalid calendar type. It will produce a missing resource error.

> :point_right: **Note**: *As of ICU 2.8, the above description applies to ICU4J only. ICU4J will have
this behavior in 3.0*

## Usage

This section discusses how to use the `Calendar` class and the `GregorianCalendar` subclass.

### Calendar

`Calendar` is an abstract base class. It defines common protocols for a hierarchy
of classes. Concrete subclasses of `Calendar`, for example the `GregorianCalendar`
class, define specific operations that correspond to a real-world calendar
system. `Calendar` objects (instantiations of concrete subclasses of `Calendar`),
embody state that represents a specific context. They correspond to a real-world
locale. They also contain state that specifies a moment in time.

The API defined by `Calendar` encompasses multiple functions:

1.  Representation of a specific time as a `UDate`

2.  Representation of a specific time as a set of integer fields, such as `YEAR`,
    `MONTH`, `HOUR`, etc.

3.  Conversion from `UDate` to fields

4.  Conversion from fields to `UDate`

5.  Field arithmetic, including adding, rolling, and field difference

6.  Context management

7.  Factory methods

8.  Miscellaneous: field meta-information, time comparison

#### Representation and Conversion

The basic function of the `Calendar` class is to convert between a `UDate` value and
a set of integer fields. A `UDate` value is stored as UTC time in milliseconds,
which means it is calendar and time zone independent. `UDate` is the most compact
and portable way to store and transmit a date and time. `Integer` field values, on
the other hand, depend on the calendar system (that is, the concrete subclass of
`Calendar`) and the calendar object's context state.

> :point_right: **Note**: *`Integer` field values are needed when implementing a human interface that must
display or input a date and/or time.*

At any given time, a calendar object uses (when `DateFormat` is not sufficient)
either its internal `UDate` or its integer fields (depending on which has been set
most recently via `setTime()` or `set()`), to represent a specific date and time.
Whatever the current internal representation, when the caller requests a `UDate`
or an integer field it is computed if necessary. The caller need never trigger
the conversion explicitly. The caller must perform a conversion to set either
the `UDate` or the integer fields, and then retrieve the desired data. This also
applies in situations where the caller has some integer fields and wants to
obtain others.

#### Field Arithmetic

Arithmetic with `UDate` values is straightforward. Since the values are
millisecond scalar values, direct addition and subtraction is all that is
required. Arithmetic with integer fields is more complicated. For example, what
is the date June 4, 1999 plus 300 days? `Calendar` defines three basic methods (in
several variants) that perform field arithmetic: `add()`, `roll()`, and
`fieldDifference()`.

The `add()` method adds positive or negative values to a specified field. For
example, calling `add(Calendar::MONTH, 2)` on a `GregorianCalendar` object set to
March 15, 1999 sets the calendar to May 15, 1999. The `roll()` method is similar,
but does not modify fields that are larger. For example, calling
`roll(Calendar::HOUR, n)` changes the hour that a calendar is set to without
changing the day. Calling `roll(Calendar::MONTH, n)` changes the month without
changing the year.

The `fieldDifference()` method is the inverse of the `add()` method. It computes the
difference between a calendar's currently set time and a specified `UDate` in
terms of a specified field. Repeated calls to `fieldDifference()` compute the
difference between two `UDate` objects in terms of whatever fields the caller specifies
(for example, years, months, days, and hours). If the `add()` method is called
with the results of `fieldDifference(when, n)`, then the calendar is moved toward
field by field.

This is demonstrated in the following example:

```c++
Calendar cal = Calendar.getInstance();
cal.set(2000, Calendar.MARCH, 15);
Date date = new Date(2000-1900, Calendar.JULY, 4);
int yearDiff = cal.fieldDifference(date, Calendar.YEAR); // yearDiff <= 0
int monthDiff = cal.fieldDifference(date, Calendar.MONTH); // monthDiff ;<= 3
// At this point cal has been advanced 3 months to June 15, 2000.
int dayDiff = cal.fieldDifference(date, Calendar.DAY_OF_MONTH); // dayDiff ;<=19
// At this point cal has been advanced 19 days to July 4, 2000.
```

#### Context Management

A `Calendar` object performs its computations within a specific context. The
context affects the results of conversions and arithmetic computations. When a
`Calendar` object is created, it establishes its context using either default
values or values specified by the caller:

1.  Locale-specific week data, including the first day of the week and the
    minimal days in the first week. Initially, this is retrieved from the locale
    resource data for the specified locale, or if none is specified, for the
    default locale.

2.  A `TimeZone` object. Initially, this is set to the specified zone object, or
    if none is specified, the default `TimeZone`.

The context of a `Calendar` object can be queried after the calendar is created
using calls such as `getMinimalDaysInFirstWeek()`, `getFirstDayOfWeek()`, and
`getTimeZone()`. The context can be changed using calls such as
`setMinimalDaysInFirstWeek()`, `setFirstDayOfWeek()`, and `setTimeZone()`.

#### Factory Methods

Like other format classes, the best way to create a calendar object is by using
one of the factory methods. These are static methods on the `Calendar` class that
create and return an instance of a concrete subclass. Factory methods should be
used to enable the code to obtain the correct calendar for a locale without
having to know specific details. The factory methods on `Calendar` are named
`createInstance()`.

***`MONTH` field***
> :point_right: **Note**: *Calendar numbers months starting from zero, so calling `cal.set(1998, 3, 5)`
sets cal to April 15, 1998, not March 15, 1998. This follows the Java
convention. To avoid mistakes, use the constants defined in the `Calendar` class
for the months and days of the week. For example, `cal.set(1998, Calendar::APRIL, 15)`.*

#### Ambiguous Wall Clock Time Resolution

When the time offset from UTC has changed, it produces an ambiguous time slot
around the transition. For example, many US locations observe daylight saving
time. On the date of transition to daylight saving time in US, wall clock time
jumps from 12:59 AM (standard) to 2:00 AM (daylight). Therefore, wall clock
times from 1:00 AM to 1:59 AM do not exist on the date. When the input wall time
falls into this missing time slot, the ICU Calendar resolves the time using the
UTC offset before the transition by default. In this example, 1:30 AM is
interpreted as 1:30 AM standard time (non-exist), so the final result will be
2:30 AM daylight time.
On the date of transition back to standard time, wall clock time is moved back
one hour at 2:00 AM. So wall clock times from 1:00 AM to 1:59 AM occur twice. In
this case, the ICU Calendar resolves the time using the UTC offset after the
transition by default. For example, 1:30 AM on the date is resolved as 1:30 AM
standard time.
Ambiguous wall clock time resolution behaviors can be customized by Calendar
APIs `setRepeatedWallTimeOption()` and `setSkippedWallTimeOption()`. These APIs are
available in ICU 49 or later versions.

### `GregorianCalendar`

The `GregorianCalendar` class implements two calendar systems, the Gregorian
calendar and the Julian calendar. These calendar systems are closely related,
differing mainly in their definition of the leap year. The Julian calendar has
leap years every four years; the Gregorian calendar refines this by excluding
century years that are not divisible by 400. `GregorianCalendar` defines two eras,
BC (B.C.E.) and AD (C.E.).

Historically, most western countries used the Julian calendar until the 16th to
20th century, depending on the country. They then switched to the Gregorian
calendar. The `GregorianCalendar` class mirrors this behavior by defining a
cut-over date. Before this date, the Julian calendar algorithms are used. After
it, the Gregorian calendar algorithms are used. By default, the cut-over date is
set to October 4, 1582 C.E., which reflects the time when countries first began
adopting the Gregorian calendar. The `GregorianCalendar` class does not attempt
historical accuracy beyond this behavior, and does not vary its cut-over date by
locale. However, users can modify the cut-over date by using the
`setGregorianChange()` method.

Code that is written correctly instantiates calendar objects using the Calendar
factory methods, and therefore holds a `Calendar*` pointer. Such code cannot
directly access the GregorianCalendar-specific methods not present in `Calendar`.
The correct way to handle this is to perform a dynamic cast, after testing the
type of the object using `getDynamicClassID()`. For example:

```c++
void setCutover(Calendar *cal, UDate myCutover) {
    if (cal->getDynamicClassID() == GregorianCalendar::getStaticClassID()) {
        GregorianCalendar *gc = (GregorianCalendar*)cal;
        gc->setGregorianChange(myCutover, status);
    }
}
```

> :point_right: **Note**: *This is a general technique that should be used throughout ICU in conjunction
with the factory methods.*

### Disambiguation

When computing a `UDate` from fields, some special circumstances can arise. There
might be insufficient information to compute the `UDate` (such as only year and
month but no day in the month), there might be inconsistent information (such as
"Tuesday, July 15, 1996" -— July 15, 1996, is actually a Monday), or the input
time might be ambiguous because of time zone transition.

1.  **Insufficient Information**
    ICU Calendar uses the default field values to specify missing fields. The
    default for a field is the same as that of the start of the epoch (that is,
    `YEAR = 1970`, `MONTH = JANUARY`, `DAY_OF_MONTH = 1`).

2.  **Inconsistent Information**
    If fields conflict, the calendar gives preference to fields set more
    recently. For example, when determining the day, the calendar looks for one
    of the following combinations of fields:
    `MONTH + DAY_OF_MONTH`
    `MONTH + WEEK_OF_MONTH + DAY_OF_WEEK`
    `MONTH + DAY_OF_WEEK_IN_MONTH + DAY_OF_WEEK`
    `DAY_OF_YEAR`
    `DAY_OF_WEEK + WEEK_OF_YEAR`
    For the time of day, the calendar looks for one of the following
    combinations of fields:
    `HOUR_OF_DAY`
    `AM_PM + HOUR`

3.  **Ambiguous Wall Clock Time**
    When time offset from UTC has changed, it produces ambiguous time slot
    around the transition. For example, many US locations observe daylight
    saving time. On the date switching to daylight saving time in US, wall clock
    time jumps from 1:00 AM (standard) to 2:00 AM (daylight). Therefore, wall
    clock time from 1:00 AM to 1:59 AM do not exist on the date. When the input
    wall time fall into this missing time slot, the ICU Calendar resolves the
    time using the UTC offset before the transition by default. In this example,
    1:30 AM is interpreted as 1:30 AM standard time (non-exist), so the final
    result will be 2:30 AM daylight time.
    On the date switching back to standard time, wall clock time is moved back
    one hour at 2:00 AM. So wall clock time from 1:00 AM to 1:59 AM occur twice.
    In this case, the ICU Calendar resolves the time using the UTC offset after
    the transition by default. For example, 1:30 AM on the date is resolved as
    1:30 AM standard time.

***Options for Ambiguous Time Resolution***
> :point_right: **Note**: *Ambiguous wall clock time resolution behaviors can be customized by Calendar APIs `setRepeatedTimeOption()` and `setSkippedTimeOption()`. These methods are available in ICU 49 or later versions.*

***`WEEK_OF_YEAR` field***
> :point_right: **Note**: *Values calculated for the `WEEK_OF_YEAR` field range from 1 to 53. Week 1 for a year is the first week that contains at least `getMinimalDaysInFirstWeek()` days from that year. It depends on the values of `getMinimalDaysInFirstWeek()`, `getFirstDayOfWeek()`, and the day of the week of January 1. Weeks between week 1 of one year and week 1 of the following year are numbered sequentially from 2 to 52 or 53 (if needed).
For example, January 1, 1998 was a Thursday. If `getFirstDayOfWeek()` is `MONDAY`
and `getMinimalDaysInFirstWeek()` is `4` (these are the values reflecting ISO 8601
and many national standards), then week 1 of 1998 starts on December 29, 1997,
and ends on January 4, 1998. However, if `getFirstDayOfWeek()` is `SUNDAY`, then
week 1 of 1998 starts on January 4, 1998, and ends on January 10, 1998. The
first three days of 1998 are then part of week 53 of 1997.*

## Programming Examples

Programming for calendar [examples in C++, C, and Java](examples.md) .
