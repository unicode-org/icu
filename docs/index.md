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

The [ICU User Guide](./userguide/) provides information on i18n topics for which ICU has services, and
includes details that go beyond the C, C++, and Java API docs (and avoids some duplication between them).

This is the new home of the User Guide (since 2020 August).

## ICU Site

The official ICU Site is located at <https://icu.unicode.org/>.
It is the official landing page for the ICU project.

Some of the pages from the ICU Site have been migrated here.
The migrated sections and pages from the ICU Site are visible in the navigation bar of this site below the "ICU Site" section heading.

## Downloading ICU

The [Downloading ICU](download/) page has been migrated here.

<!--
### Release Candidate

***2026-09-25: [ICU 79 release candidate](download/79.md) is now available*** —
[releases/tag/release-79.1rc](https://github.com/unicode-org/icu/releases/tag/release-79.1rc)
- Unicode 18, CLDR 49, performance improvements, etc.
- Please use it for testing but not in production.
-->

### Latest Release

***2026-10-22: [ICU 79](79.md) is now available*** —
[releases/tag/release-79.1](https://github.com/unicode-org/icu/releases/tag/release-79.1) —
Maven: [com.ibm.icu / icu4j / version 79.1](https://mvnrepository.com/artifact/com.ibm.icu/icu4j/79.1)

ICU 79 updates to
[Unicode 18](https://www.unicode.org/versions/Unicode18.0.0/)
([blog](https://blog.unicode.org/2026/09/announcing-unicode-standard-version-180.html)),
including new characters and scripts, emoji, collation & IDNA changes, and corresponding APIs and implementations.

It also updates to
[CLDR 49](https://cldr.unicode.org/downloads/cldr-49)
([blog](https://blog.unicode.org/2026/10/unicode-cldr-49-released.html))
locale data with many additions and corrections.

Line break backwards-iteration performance has been significantly improved,
fixing a long-standing regression.

C++ and Java implementations of the 
[CLDR MessageFormat 2.0 specification](https://www.unicode.org/reports/tr35/tr35-messageFormat.html)
have been updated to CLDR 49.
Some of the API remains in technology preview.

ICU 79 and CLDR 49 are major releases, including a new version of Unicode and major locale data improvements.

## ICU team member pages

Other documentation pages here are written by and for team members.
