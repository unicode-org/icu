---
layout: default
title: ustdio
nav_order: 1
parent: IO
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# C: ustdio

This API provides a `<stdio.h>`-like API wrapper around ICU's other [formatting
and parsing](../format_parse/index.md) APIs. It is meant to ease the transition of adding
Unicode support to a preexisting applications using stdio. The following is a
small list of noticeable differences between stdio and ICU I/O's ustdio
implementation.

*   Locale specific formatting and parsing is only done with file IO.
*   `u_fstropen` can be used to simulate file IO with strings. This is similar
    to the iostream API, and it allows locale specific formatting and parsing to
    be used.
*   This API provides uniform formatting and parsing behavior between platforms
    (unlike the standard stdio implementations found on various platforms).
*   This API is better suited for text data handling than binary data handling
    when compared to the typical stdio implementation.
*   You can specify a [Transliterator](../transforms/index.md) while using the
    file IO.
*   You can specify a file's [codepage](../conversion/converters.md) separately
    from the codepage.
