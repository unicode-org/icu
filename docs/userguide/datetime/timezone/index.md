---
layout: default
title: TimeZone Classes
nav_order: 3
parent: Date/Time
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU TimeZone Classes
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

A time zone is a system that is used for relating local times in different
geographical areas to one another. For example, in the United States, Pacific
Time is three hours earlier than Eastern Time; when it's 6 P.M. in San
Francisco, it's 9 P.M. in Brooklyn. To make things simple, instead of relating
time zones to one another, all time zones are related to a common reference
point.

For historical reasons, the reference point is Greenwich, England. Local time in
Greenwich is referred to as Greenwich Mean Time, or GMT. (This is similar, but
not precisely identical, to Universal Coordinated Time, or UTC. We use the two
terms interchangeably in ICU since ICU does not concern itself with either leap
seconds or historical behavior.) Using this system, Pacific Time is expressed as
GMT-8:00, or GMT-7:00 in the summer. The offset -8:00 indicates that Pacific
Time is obtained from GMT by adding -8:00, that is, by subtracting 8 hours.

The offset differs in the summer because of daylight savings time, or DST. At
this point it is useful to define three different flavors of local time:

* **Standard Time**:
  Standard Time is local time without a daylight savings time offset. For
  example, in California, standard time is GMT-8:00; that is, 8 hours before
  GMT.
* **Daylight Savings Time**:
  Daylight savings time is local time with a daylight savings time offset.
  This offset is typically one hour, but is sometimes less. In California,
  daylight savings time is GMT-7:00. Daylight savings time is observed in most
  non-equatorial areas.
* **Wall Time**:
  Wall time is what a local clock on the wall reads. In areas that observe
  daylight savings time for part of the year, wall time is either standard
  time or daylight savings time, depending on the date. In areas that do not
  observe daylight savings time, wall time is equivalent to standard time.

## Time Zones in ICU

ICU supports time zones through two classes:

* **TimeZone**:
  `TimeZone` is an abstract base class that defines the time zone API. This API
  supports conversion between GMT and local time.
* **SimpleTimeZone**:
  `SimpleTimeZone` is a concrete subclass of TimeZone that implements the
  standard time zones used today internationally.

Timezone classes are related to `UDate`, the `Calendar` classes, and the
`DateFormat` classes.

### Timezone Class in ICU

`TimeZone` is an abstract base class. It defines common protocol for a hierarchy
of classes. This protocol includes:

* A programmatic ID, for example, "America/Los_Angeles". This ID is used to
  call up a specific real-world time zone. It corresponds to the IDs defined
  in the [IANA Time Zone database](https://www.iana.org/time-zones) used by UNIX
  and other systems, and has the format continent/city or ocean/city.
* A raw offset. This is the difference, in milliseconds, between a time zone's
  standard time and GMT. Positive raw offsets are east of Greenwich.
* Factory methods and methods for handling the default time zone.
* Display name methods.
* An API to compute the difference between local wall time and GMT.

#### Factory Methods and the Default Timezone

The TimeZone factory method `createTimeZone()` creates and returns a `TimeZone`
object given a programmatic ID. The user does not know what the class of the
returned object is, other than that it is a subclass of `TimeZone`.

The `createAvailableIDs()` methods return lists of the programmatic IDs of all
zones known to the system. These IDs may then be passed to `createTimeZone()` to
create the actual time zone objects. ICU maintains a comprehensive list of
current international time zones, as derived from the Olson data.

`TimeZone` maintains a static time zone object known as the *default time zone*.
This is the time zone that is used implicitly when the user does not specify
one. ICU attempts to match this to the host OS time zone. The user may obtain a
clone of the default time zone by calling `createDefault()` and may change the
default time zone by calling `setDefault()` or `adoptDefault()`.

#### Display Name

When displaying the name of a time zone to the user, use the display name, not
the programmatic ID. The display name is returned by the `getDisplayName()`
method. A time zone may have three display names:

* Generic name, such as "Pacific Time".
* Standard name, such as "Pacific Standard Time".
* Daylight savings name, such as "Pacific Daylight Time".

Furthermore, each of these names may be LONG or SHORT. The SHORT form is
typically an abbreviation, e.g., "PST", "PDT".

In addition to being available directly from the `TimeZone` API, the display name
is used by the date format classes to format and parse time zones.

#### getOffset() API

`TimeZone` defines the API `getOffset()` by which the caller can determine the
difference between local time and GMT. This is a pure virtual API, so it is
implemented in the concrete subclasses of `TimeZone`.

## Updating the Time Zone Data

Time zone data changes often in response to governments around the world
changing their local rules and the areas where they apply. ICU derives its tz
data from the [IANA Time Zone Database](http://www.iana.org/time-zones).

The ICU project publishes updated timezone resource data in response to IANA
updates, and these can be used to patch existing ICU installations. Several
update strategies are possible, depending on the ICU version and configuration.

* ICU4J: Use the time zone update utility.
* ICU4C 54 and newer: Drop in the binary update files.
* ICU4C 36 and newer: the best update strategy will depend on how ICU data
  loading is configured for the specific ICU installation.
  * Data is loaded from a .dat package file: replace the time zone resources
    in the .dat file using the icupkg tool.
  * Data is loaded from a .dll or .so shared library: obtain the updated
    sources for the tz resources and rebuild the data library.
  * Data is loaded from individual files: drop in the updated binary .res
    files.

The [ICU Data](../../icudata.md) section of this user guide gives more
information on how ICU loads resources.

The ICU resource files required for time zone data updates are posted at
<https://github.com/unicode-org/icu-data/tree/main/tzdata/icunew>. The
required resource files for ICU version 44 and newer are

* zoneinfo64.res
* windowsZones.res
* timezoneTypes.res
* metaZones.res

### ICU4C TZ update of a .dat Package File

For ICU configurations that load data from a .dat package file, replace the time
zone resources in that file.

1. Download the new .res files from
   `https://github.com/unicode-org/icu-data/tree/main/tzdata/icunew/<IANA tz version>/44/<platform directory>`.
   * `<IANA tz version>` is a combination of year and letter, such as "2019c".
   * *"44"* is the directory for updates to ICU version 4.4 and newer.
   * `<platform directory>` is "le" for little endian processors, including
     all Intel processors.
   * `<platform directory>` is "be" for big endian processors, including IBM
     Power and Sparc.
   * `<platform directory>` is "ee" for IBM mainframes using EBCDIC character
     sets.
2. Check that the tool "icupkg" is available. If not already on your system,
   you can get it by [downloading](https://github.com/unicode-org/icu/releases)
   and building ICU, following the instructions in the ReadMe file included in
   the download. Alternatively, on many Linux systems, "apt-get install
   icu-devtools" will install the tool.
3. Locate the .dat file to be updated, and do the update. The commands below
   are for a .dat file named icudt55l.dat.

```shell
icupkg -a zoneinfo64.res icudt55l.dat
icupkg -a windowsZones.res icudt55l.dat
icupkg -a timezoneTypes.res icudt55l.dat
icupkg -a metaZones.res icudt55l.dat
```

In ICU versions older than 4.4 some of the time zone resources have slightly
different names. The update procedure is the same, but substitute the names
found in the desired download directory - 42, 40, 38 or 36.

### ICU4C TZ Update with Drop-in .res files (ICU 54 and newer)

With this approach, the four individual .res files are dropped in any convenient
location in the file system, and ICU is given an absolute path to the directory
containing them. For the time zone resources only, ICU will check this directory
first when loading data. This approach will work even when all other ICU data
loading is from a shared library or .dat file.

There are two ways to specify the directory:

* At ICU build time, by defining the C pre-processor variable
  `U_TIMEZONE_FILES_DIR` to the run time path to the directory containing the
  .res files.
* At run time, by setting the environment variable `ICU_TIMEZONE_FILES_DIR` to
  the absolute path of the directory containing the .res files.

If both are defined, the environment variable `ICU_TIMEZONE_FILES_DIR` take
precedence. If either is defined, the time zone directory will be checked first,
meaning that time zone resource files placed there will override time zone
resources that may exist in other ICU data locations.

To do the update, download the .res files appropriate for the platform, as
described for the .dat file update above, and copy them into the time zone res
file directory.

### ICU4C TZ update when ICU is configured for individual files

If the ICU-using application sets an ICU data path (or can be changed to set
one), then the time zone .res file can be placed there. Download the files as
described above and copy them to the specified directory. See the
[ICU Data](../../icudata.md) page of the user guide for more information about
the ICU data path.

### ICU4C TZ update when ICU data is built into a shared library

1. Set up the environment necessary to rebuild your specific configuration of
   ICU.
2. Download the .txt file sources for the updated resources from
   `https://github.com/unicode-org/icu-data/tree/main/tzdata/icunew/<IANA tz version>/44`
3. Copy the downloaded .txt files into the ICU sources for your installation,
   in the subdirectory source/data/misc/
4. Rebuid ICU.
5. Copy the freshly built ICU data shared library to the desired destination.

> :point_right: **Note**: The standard ICU download package contains pre-built
> ICU data. To rebuild ICU data from .txt files, you will need to replace the
> contents of `icu4c/source/data` with the contents of ICU4C data.zip. See
> [ICU Data Build Tool](../../icu_data/buildtool.md) for more details.

There are too many possible platform variations to be more specific about how to
rebuild ICU4C in these instructions. See the ReadMe file included with the ICU
sources for general information on building ICU.

### Update the time zone data for ICU4J

The [ICU4J Time Zone Update 
Utility](https://icu.unicode.org/download/icutzu) automates the process of
updating ICU4J jar files with the latest time zone data. Instructions for use
are [here](https://htmlpreview.github.io/?https://github.com/unicode-org/icu-data/blob/main/tzdata/tzu/readme.html).

The updater will work with ICU version 3.4.2 and newer.

## Sample Code

See the [Date and Time Zone Examples](examples.md) subpage.
