---
layout: default
title: Downloading ICU
nav_order: 100
has_children: true
---

<!--
© 2024 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Downloading ICU

If you want to use ICU (as opposed to developing it), it is recommended that you download an official packaged version of the ICU source code. These versions are tested more thoroughly than day-to-day development builds of the system, and they are packaged in zip and tar files for convenient download. Here are several recent releases of ICU that are available:

## Latest Release

***2024-10-24: ICU 76 is now available.***
It updates to [Unicode 16](https://www.unicode.org/versions/Unicode16.0.0/) ([blog](https://blog.unicode.org/2024/09/announcing-unicode-standard-version-160.html)), including new characters and scripts, emoji, collation & IDNA changes, and corresponding APIs and implementations. It also updates to [CLDR 46](https://cldr.unicode.org/downloads/cldr-46) ([beta blog](https://blog.unicode.org/2024/09/unicode-cldr-46-beta-available-for.html)) locale data with new locales, significant updates to existing locales, and various additions and corrections. For example, the CLDR and Unicode default sort orders are now very nearly the same.

Most of the java.time (Temporal) types can now be formatted directly. There are some new APIs to make ICU easier to use with modern C++ and Java patterns. The Java and C++ technology preview implementations of the CLDR MessageFormat 2.0 specification have been updated to match recent changes. See [ICU 76](76.md).

## Previous Releases

- 2024-04-17: **ICU 75** updates to [CLDR 45](https://cldr.unicode.org/index/downloads/cldr-45) ([beta blog](https://blog.unicode.org/2024/04/unicode-cldr-v45-beta-available-for.html)) locale data with new locales and various additions and corrections. C++ code now requires C++17 and is being made more robust. The CLDR MessageFormat 2.0 specification is now in [technology preview](https://github.com/unicode-org/message-format-wg?tab=readme-ov-file#messageformat-2-technical-preview), together with a corresponding update of the ICU4J (Java) tech preview and a new ICU4C (C++) tech preview. See [Downloading ICU &gt; ICU 75](https://icu.unicode.org/download/75).

- 2023-12-13: **ICU 74.2** released with date/time formatting bug fixes. See [Downloading ICU &gt; ICU 74](https://icu.unicode.org/download/74).

- 2023-10-31: **ICU 74** updates to [Unicode 15.1](http://blog.unicode.org/2023/09/announcing-unicode-standard-version-151.html), including new characters, emoji, security mechanisms, and corresponding APIs and implementations. It also updates to [CLDR 44](https://cldr.unicode.org/index/downloads/cldr-44) ([blog](https://blog.unicode.org/2023/10/unicode-cldr-v44-available.html)) locale data with new locales and various additions and corrections. See [Downloading ICU &gt; ICU 74](https://icu.unicode.org/download/74).

- 2023-06-15: **ICU 73.2**: Maintenance release with changes for GB18030 compliance, English AM/PM spaces, word segmentation around @ sign, etc. See [Downloading ICU &gt; ICU 73](https://icu.unicode.org/download/73).

- 2023-04-13: **ICU 73** updates to [CLDR 43](https://blog.unicode.org/2023/04/the-unicode-cldr-v43-released.html) locale data with various additions and corrections.
  ICU 73 improves Japanese and Korean short-text line breaking, reduces C++ memory use in date formatting, and promotes the Java person name formatter from tech preview to draft.
  For details, see [Downloading ICU &gt; ICU 73](https://icu.unicode.org/download/73).

| Release | ICU4C | ICU4J | Major Changes |
| ----- | ----- | ----- | ----- |
| [**ICU 72**](https://icu.unicode.org/download/72) | **72.1** | **72.1** | Unicode 15, CLDR 42, tech previews: person name formatting, MessageFormat 2 |
| [**ICU 71**](https://icu.unicode.org/download/71) | **71.1** | **71.1** | CLDR 41, phrase-based line breaking for Japanese. |
| [**ICU 70**](https://icu.unicode.org/download/70) | **70.1** | **70.1** | Unicode 14 & CLDR 40, emoji properties. |
| [**ICU 69**](https://icu.unicode.org/download/69) | **69.1** | **69.1** | CLDR 39, enhancements in measurement unit formatting and number formatting. |
| [**ICU 68**](https://icu.unicode.org/download/68) | **68.2** | **68.2** | CLDR 38, locale-dependent smart unit preferences (road distance, temperature, etc.), locale ID canonicalization conformant with CLDR. |
| **ICU 67** | [**67.1**](https://icu.unicode.org/download/67#h.ouaur1y940xf) | [**67.1**](https://icu.unicode.org/download/67#h.tfrvsxlm08j1) | Unicode 13 & CLDR 37. Bug fixes for date and number formatting, enhanced support for user preferences in the locale identifier. LocaleMatcher code and data improved. Number skeletons have a new “concise” form that can be used in MessageFormat strings. |
| **ICU 66** | [**66.1**](https://icu.unicode.org/download/66#h.udvu1uhvnd81) | [**66.1**](https://icu.unicode.org/download/66#h.4aoltvwuvna5) | Unicode 13 & CLDR 36.1. New, extra Q1 releases for low-risk integration of Unicode 13. |
| **ICU 65** | [**65.1**](https://icu.unicode.org/download/65#h.zcdcx3wezhki) | [**65.1**](https://icu.unicode.org/download/65#h.sve2yxt21gko) | CLDR 36 with some new measurement units. Java LocaleMatcher API is improved, and ported to C++. For building ICU data, there are new filtering options, and new tracing support for data loading in ICU4C. |
| **ICU 64** | [**64.2**](https://icu.unicode.org/download/64#h.vrjm9tyoew9f) | [**64.2**](https://icu.unicode.org/download/64#h.2p64shtntkxe) | Unicode 12.0, CLDR 35, data filtering/subsetting mechanism, improved formatting API, C++ LocaleBuilder. |
| **ICU 63** | [**63.2**](https://icu.unicode.org/download/63#h.u1gym8iosl1f) | [**63.2**](https://icu.unicode.org/download/63#h.ouec4bczpynw) | CLDR 34, API for number & currency *range* formatting, API for additional Unicode properties and for constructing custom properties; test data for upcoming Japanese era; C++ Locale enhancements; Java 7 as minimum runtime. |
| **ICU 62** | [**62.2**](https://icu.unicode.org/download/62#h.tesi0q3ajdsl) | [**62.2**](https://icu.unicode.org/download/62#h.aivenafc76s7) | Unicode 11.0, CLDR 33.1, number format skeleton pattern in MessageFormat and ICU4C DecimalFormat wrapping new NumberFormatter implementation. |
| **ICU 61** | [**61.2**](https://icu.unicode.org/download/61#h.bv7bijcse9ix) | [**61.2**](https://icu.unicode.org/download/61#h.w7fdpw54xbjz) | CLDR 33, new Java implementation for number and currency parsing, and many small API additions, improvements, and bug fixes. |
| **ICU 60** | [**60.3**](https://icu.unicode.org/download/60#h.s3yzinozhocu) | [**60.3**](https://icu.unicode.org/download/60#h.l452u1xfeng2) | Unicode 10.0, CLDR 32, and ICU4J has been tested with Java 9. New API for number formatting, NumberFormatter, which new users should link instead of NumberFormat/DecimalFormat. There are many more features and bug fixes. |
| [**ICU 59**](https://icu.unicode.org/download/59) | [**59.2**](https://icu.unicode.org/download/59#h.yksv2zydmofr) | [**59.2**](https://icu.unicode.org/download/59#h.4a4swub5xyga) | CLDR 31, Emoji 5.0 draft data, C++11 support, Java number formatting code rewrite. |
| [**ICU 58**](https://icu.unicode.org/download/58) | [**58.3**](https://icu.unicode.org/download/58#h.cii7gzh9hdsi) | [**58.3**](https://icu.unicode.org/download/58#h.yvn5vkyn1ufu) | Unicode 9.0, CLDR 30.0.2, Emoji 4.0 draft data, JUnit test integration, more locale data loading improvements. |
| [**ICU 57**](https://icu.unicode.org/download/57) | [**57.2**](https://icu.unicode.org/download/57#h.2orq2dh20i8c) | [**57.2**](https://icu.unicode.org/download/57#h.uclxjlbu3xgz) | CLDR 29, Unicode emoji properties, improved locale data loading. |
| [**ICU 56**](https://icu.unicode.org/download/56) | [**56.2**](https://icu.unicode.org/download/56#h.cyht33tccoc0) | [**56.2**](https://icu.unicode.org/download/56#h.jy58dax3hhcr) | Unicode 8.0, CLDR 28, ICU data size reduction, ICU4C DecimalFormat refactoring. |
| [**ICU 55**](https://icu.unicode.org/download/55) | [**55.2**](https://sites.google.com/unicode.org/icu/download/55?pli=1#h.o7eux855pigh) | [**55.2**](https://sites.google.com/unicode.org/icu/download/55?pli=1#h.ok82qb3y55ax) | CLDR 27.0.1, CSS line-break support in BreakIterator, easier-to-use ScientificNumberFormatter ("1.2 × 10³"), ICU4C DateFormat performance improvement, C wrapper for ListFormatter and FieldPositionIterator, regex named capture groups. ICU4J has been improved and tested for using ICU4C data and for running on Android. |
| [**ICU 54**](https://icu.unicode.org/download/54) | [**54.2**](https://icu.unicode.org/download/54#h.ub64y7kaqos7) | [**54.2**](https://icu.unicode.org/download/54#h.jnbcx7gqiidn) | CLDR 26, Unicode 7.0, many more units, Unihan in root collation, new RBNF PluralFormat syntax, dictionary-based break iterator for Burmese, tech preview of FilteredBreakIterator using ULI break data, time separator configurable from CLDR data, ... |
| [**ICU 53**](https://icu.unicode.org/download/53) | [**53.2**](https://icu.unicode.org/download/53#h.kbnahu7jfdg7) | [**53.2**](https://icu.unicode.org/download/53#h.bpcdfe5r3hxl) | CLDR 25, collation code rewrite, measure formatting for various units, relative date formatter and leniency controls in date format parsing |
| [**ICU 52**](https://icu.unicode.org/download/52) | [**52.2**](https://icu.unicode.org/download/52#h.clle0zr3jvqx) | [**52.2**](https://icu.unicode.org/download/52#h.ho3jsruxjpyp) | CLDR 24, Unicode 6.3, Plurals for fractional values, Islamic Umm al-Qura calendar, DateFormat parse leniency controls and Lao dictionary based word segmentation. |
| [**ICU 51**](https://icu.unicode.org/download/51) | [**51.3**](https://icu.unicode.org/download/51#h.ae67vrvucf1k) | [**51.3**](https://icu.unicode.org/download/51#h.p2kuuvx5ko5z) | CLDR 23, short weekday names, new time zone format patterns, display context for date format and locale display names |
| [ICU 50](https://icu.unicode.org/download/50) | [50.2](https://icu.unicode.org/download/50#h.au7pfptnrbk0) | [50.2](https://icu.unicode.org/download/50#h.h2lsiy5b0om3) | CLDR 22.1, Unicode 6.2, ordinal number support, dictionary-based break iterators for Chinese & Japanese… |
| [ICU 49](https://icu.unicode.org/download/49) | [49.1.2](https://icu.unicode.org/download/49#h.8kgchrymg08d) | [49.1](https://icu.unicode.org/download/49#h.plyzynidar2c) | CLDR 21.0.1, Unicode 6.1, simpler ICU4C build without generated platform.h, many small API additions |
|  |   |   | Note: We have changed the [ICU release version numbering](https://unicode-org.github.io/icu/userguide/icu/design.html#version-numbers-in-icu) , combining the former first two fields into one, thus the major release sequence is ICU 4.8, ICU 49, ICU 50, … |

For more details on building ICU once you've checked out the code, please see the ICU4C or ICU4J readme (linked from the nav bar).

Note: In 2012, starting with [ICU 49](https://icu.unicode.org/download/49), we changed the [ICU release version numbering](https://unicode-org.github.io/icu/userguide/icu/design.html#version-numbers-in-icu), combining the former first two fields into one, thus the major release sequence is ..., ICU 4.6, ICU 4.8, ICU **49**, ICU 50, ICU 51, …

For older ICU releases see the [Old ICU Releases](https://icu.unicode.org/download/old) page. 
