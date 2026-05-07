---
layout: default
title: Copy Shared Tests
parent: Release & Milestone Tasks
grand_parent: Contributors
nav_order: 40
---

<!--
© 2021 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Copy Shared Tests

Certain tests are structured so that they can be shared between CLDR and ICU.
That allows the tests to be run in CLDR earlier in the development process. The
tests include:

*   LocaleMatcherTest
*   TransformTest (to be cleaned up)

Copy all of these from CLDR to ICU, and sanity check the differences, then check
in.
