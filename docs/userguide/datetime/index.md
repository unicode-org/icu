---
layout: default
title: Date/Time
nav_order: 6
has_children: true
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Date/Time Services
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview of ICU System Time Zones

A time zone represents an offset applied to Greenwich Mean Time (GMT) to obtain
local time. The offset might vary throughout the year, if daylight savings time
(DST) is used, or might be the same all year long. Typically, regions closer to
the equator do not use DST. If DST is in use, then specific rules define the
point at which the offset changes and the amount by which it changes. Thus, a
time zone is described by the following information:

*   An identifying string, or ID. This consists only of invariant characters
    (see the file `utypes.h`). It typically has the format continent / city. The
    city chosen is not the only city in which the zone applies, but rather a
    representative city for the region. Some IDs consist of three or four
    uppercase letters; these are legacy zone names that are aliases to standard
    zone names.

*   An offset from GMT, either positive or negative. Offsets range from
    approximately minus half a day to plus half a day.

If DST is observed, then three additional pieces of information are needed:

1.  The precise date and time during the year when DST begins. In the first half
    of the year it's in the northern hemisphere, and in the second half of the
    year it's in the southern hemisphere.

2.  The precise date and time during the year when DST ends. In the first half
    of the year it's in the southern hemisphere, and in the second half of the
    year it's in the northern hemisphere.

3.  The amount by which the GMT offset changes when DST is in effect. This is
    almost always one hour.

### System and User Time Zones

ICU supports local time zones through the classes `TimeZone` and `SimpleTimeZone` in
the C++ API. In the C API, time zones are designated by their ID strings.

Users can construct their own time zone objects by specifying the above
information to the C++ API. However, it is more typical for users to use a
pre-existing system time zone since these represent all current international
time zones in use. This document lists the system time zones, both in order of
GMT offset and in alphabetical order of ID.

Since this list changes one or more times a year, *this document only represents
a snapshot*. For the most current list of ICU system zones, use the method
`TimeZone::getAvailableIDs()`.

*The zones are listed in binary sort order (that is, 'A' through 'Z' come before
'a' through 'z'). This is the same order in which the zones are stored
internally, and the same order in which they are returned by
`TimeZone::getAvailableIDs()`. The reason for this is that ICU locates zones using
a binary search, and the binary search relies on this sort order.*
*You might notice that zones such as Etc/GMT+1 appear to have the wrong sign for
their GMT offset. In fact, their sign is inverted since the the Etc zones follow
the POSIX sign conventions. This is the way the original Olson data is set up,
and ICU reproduces the Olson data faithfully. See the Olson files for more
details.*

### References

The ICU system time zones are derived from the tz database (also known as the
“Olson” database) at [ftp://elsie.nci.nih.gov/pub](ftp://elsie.nci.nih.gov/pub).
This is the data used across much of the industry, including by UNIX systems,
and is usually updated several times each year. ICU (since version 2.8) and base
Java (since Java 1.4) contain code and tz data supporting both current and
historic time zone usage.

## How ICU Represents Dates/Times

ICU represents dates and times using `UDate`s. A `UDate` is a scalar value that
indicates a specific point in time, independent of calendar system and local
time zone. It is stored as the number of milliseconds from a reference point
known as the epoch. The epoch is midnight Universal Time Coordinated (UTC)
January 1, 1970 A.D. Negative `UDate` values indicate times before the epoch.

*These classes have the same architecture as the Java classes.*

Most people only need to use the `DateFormat` classes for parsing and formatting
dates and times. However, for those who need to convert dates and times or
perform numeric calculations, the services described in this section can be very
useful.

To translate a `UDate` to a useful form, a calendar system and local time zone
must be specified. These are specified in the form of objects of the `Calendar`
and `TimeZone` classes. Once these two objects are specified, they can be used to
convert the `UDate` to and from its corresponding calendar fields. The different
fields are defined in the `Calendar` class and include the year, month, day, hour,
minute, second, and so on.

Specific `Calendar` objects correspond to calendar systems (such as Gregorian) and
conventions (such as the first day of the week) in use in different parts of the
world. To obtain a `Calendar` object for France, for example, call
`Calendar::createInstance(Locale::getFrance(), status)`.

The `TimeZone` class defines the conversion between universal coordinated time
(UTC), and local time, according to real-world rules. Different `TimeZone`
objects correspond to different real-world time zones. For example, call
`TimeZone::createTimeZone("America/Los_Angeles")` to obtain an object that
implements the U.S. Pacific time zone, both Pacific Standard Time (PST) and
Pacific Daylight Time (PDT).

As previously mentioned, the `Calendar` and `TimeZone` objects must be specified
correctly together. One way of doing so is to create each independently, then
use the `Calendar::setTimeZone()` method to associate the time zone with the
calendar. Another is to use the `Calendar::createInstance()` method that takes a
`TimeZone` object. For example, call `Calendar::createInstance(
TimeZone::createInstance( "America/Los_Angeles"), Locale:getUS(), status)` to
obtain a `Calendar` appropriate for use in the U.S. Pacific time zone.

ICU has four classes pertaining to calendars and timezones:

*   [`Calendar`](calendar/index.md)
    
    `Calendar` is an abstract base class that represents a calendar system.
    `Calendar` objects map `UDate` values to and from the individual fields used in
    a particular calendar system. `Calendar` also performs field computations such
    as advancing a date by two months.

*   [`Gregorian Calendar`](calendar/index.md)
    
    `GregorianCalendar` is a concrete subclass of `Calendar` that implements the
    rules of the Julian calendar and the Gregorian calendar, which is the common
    calendar in use internationally today.

*   [`TimeZone`](timezone/index.md)
    
    `TimeZone` is an abstract base class that represents a time zone. `TimeZone`
    objects map between universal coordinated time (UTC) and local time.

*   [`SimpleTimeZone`](timezone/index.md)
    
    `SimpleTimeZone` is a concrete subclass of `TimeZone` that implements standard
    time and daylight savings time according to real-world rules. Individual
    `SimpleTimeZone` objects correspond to real-world time zones.
