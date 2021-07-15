---
layout: default
title: Paragraph Layout
nav_order: 1
parent: Layout Engine
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Paragraph Layout
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

# Overview

This page is about the Paragraph Layout library that is available in ICU4C/C++.

For information about the deprecated Line Layout Engine, including its deprecation notice,
see: [Layout Engine](index.md).

### About the Paragraph Layout library

*   The ICU Line LayoutEngine works on small chunks - unidirectional runs. It does
    not layout text at the paragraph level.
*   The **ParagraphLayout** object will analyze the text into runs of text in
    the same font, script and direction, and will create a LayoutEngine object
    for each run. The LayoutEngine will transform the characters into glyph
    codes in visual order. Clients can use this to break a paragraph into lines,
    and to display the glyphs in each line.
*   Also see the
    [ParagraphLayout](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classicu_1_1ParagraphLayout.html)
    API Docs

### Building the Paragraph Layout library with HarfBuzz

While the ICU LayoutEngine is deprecated as of ICU 54, the ICU *Paragraph* Layout library is not.
The Paragraph Layout library must now be built using the HarfBuzz engine instead of the ICU LayoutEngine.

#### UNIX Makefile instructions / Cygwin / Msys / etc. (ICU 54+)

The following steps must be completed in order:

1.  Build and install a complete ICU with the **`--disable-layout` `--disable-layoutex`**
    switches passed to configure
3.  Build and install HarfBuzz - http://harfbuzz.org (HarfBuzz's use of ICU may
    be enabled or disabled at your choice)
4.  Build and install the [icu-le-hb](http://harfbuzz.org) library.
5.  Now, rerun "configure" on the exact **same** ICU workspace used above:
    *   with "icu-le-hb" AND the above-mentioned installed ICU available via
        pkg-config ( `pkg-config --modversion icu-le-hb` should return a version,
        such as "0.0.0" )
    *   with the --disable-layout **`--enable-layoutex`** switches passed to configure
6.  next, run `make install` JUST in the **`source/layoutex`** directory, to install
    libiculx and `icu-lx.pc`

The above steps will produce a libiculx library that depends on HarfBuzz.

If pkg-config visible installation is not suitable for step 4, you may also
manually set the following variables when building ICU in step 5:

*   set `ICULEHB_CFLAGS` to the appropriate include path for icu-le-hb ( such
    as **`-I/usr/local/include/icu-le-hb`** )
*   set `ICULEHB_LIBS` to link against icu-le-hb and dependents as needed
    (such as **`-L/usr/local/lib -licu-le-hb`** )
