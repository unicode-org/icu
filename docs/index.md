---
layout: default
title: ICU Documentation
nav_order: 1
description: ICU Documentation
---

<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU Documentation

[![ICU logo](https://github.com/unicode-org/icu-docs/raw/main/img/iculogo_64.png)](https://icu.unicode.org/)

## ICU User Guide

The [ICU User Guide](./userguide) provides information on i18n topics for which ICU has services, and
includes details that go beyond the C, C++, and Java API docs (and avoids some duplication between them).

This is the new home of the User Guide (since 2020 August).

## ICU Site

The official ICU Site is located at https://icu.unicode.org/.
It is the official landing page for the ICU project.

Some of the pages from the ICU Site have been migrated here.
The migrated sections and pages from the ICU Site are visible in the navigation bar of this site below the "ICU Site" section heading.

## Downloading ICU

The [Downloading ICU](download) page has been migrated here.

### Latest Release

***2025-10-30: [ICU 78](78.md) is now available*** —
[releases/tag/release-78.1](https://github.com/unicode-org/icu/releases/tag/release-78.1) —
Maven: [com.ibm.icu / icu4j / version 78.1](https://mvnrepository.com/artifact/com.ibm.icu/icu4j/78.1)

ICU 78 updates to
[Unicode 17](https://www.unicode.org/versions/Unicode17.0.0/)
([blog](https://blog.unicode.org/2025/09/unicode-170-release-announcement.html)),
including new characters and scripts, emoji, collation & IDNA changes, and corresponding APIs and implementations.

It also updates to
[CLDR 48](https://cldr.unicode.org/downloads/cldr-48)
([blog](https://blog.unicode.org/2025/10/unicode-cldr-48-available.html))
locale data with new locales, and various additions and corrections.

In Java, there is a new Segmenter API which is easier and safer to use than BreakIterator.\
In C++, there is a new set of APIs for Unicode string (UTF-8/16/32) code point iteration
that works seamlessly with modern C++ iterators and ranges.

The Java implementation of the
[CLDR MessageFormat 2.0 specification](https://www.unicode.org/reports/tr35/tr35-messageFormat.html)
has been updated to CLDR 48.
The core API has been upgraded to “draft”, while the Data Model API remains in technology preview.

The C++ implementation of MessageFormat 2.0 is at CLDR 47 level and remains in technology preview.

ICU 78 and CLDR 48 are major releases, including a new version of Unicode and major locale data improvements.

## ICU team member pages

Other documentation pages here are written by and for team members.
