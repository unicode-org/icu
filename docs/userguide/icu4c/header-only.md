---
layout: default
title: C++ Header-Only APIs
nav_order: 10
parent: ICU4C
---
<!--
© 2025 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Plug-ins
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

Starting with ICU 76, ICU4C has what we call C++ header-only APIs.
These are especially intended for users who rely on only binary stable DLL/library exports of C APIs
(C++ APIs cannot be binary stable).

Some header-only APIs provide functionality that is not otherwise available in C++; for example, the code point iteration and range APIs in
[`unicode/utfiterator.h`](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/utfiterator_8h.html)
and the string helpers in
[`unicode/utfstring.h`](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/utfstring_8h.html).

As before, regular C++ APIs can be hidden by callers defining `U_SHOW_CPLUSPLUS_API=0`.
The header-only APIs can be separately enabled via `U_SHOW_CPLUSPLUS_HEADER_API=1`.

([GitHub query for `U_SHOW_CPLUSPLUS_HEADER_API` in public header files](https://github.com/search?q=repo%3Aunicode-org%2Ficu+U_SHOW_CPLUSPLUS_HEADER_API+path%3Aunicode%2F*.h&type=code))

C++ header-only APIs are C++ definitions that are not exported by the ICU DLLs/libraries,
are thus inlined into the calling code.
They may call ICU C APIs,
but they do not call any ICU C++ APIs except other header-only ones.
(Therefore, these header-only C++ classes do not subclass UMemory or UObject.)

The header-only APIs are defined in a nested `header` namespace.
If entry point renaming is turned off (the main namespace is `icu` rather than `icu_76` etc.),
then the `U_HEADER_ONLY_NAMESPACE` is `icu::header`.

The following example iterates over the code point ranges in a `USet` (excluding the strings) using C++ header-only APIs on top of C-only functions.

```c++
using icu::header::USetRanges;
icu::LocalUSetPointer uset(uset_openPattern(u"[abcçカ🚴]", -1, &errorCode));
for (auto [start, end] : USetRanges(uset.getAlias())) {
    printf("uset.range U+%04lx..U+%04lx\n", (long)start, (long)end);
}
for (auto range : USetRanges(uset.getAlias())) {
    for (UChar32 c : range) {
        printf("uset.range.c U+%04lx\n", (long)c);
    }
}
```

(Implementation note: On most platforms, when compiling ICU library code,
the `U_HEADER_ONLY_NAMESPACE` is `icu_76::internal` / `icu::internal` etc.,
so that any such symbols that get exported differ from the ones that calling code sees.
On Windows, where DLL exports are explicit,
the namespace is always the same, but these header-only APIs are not marked for export.)