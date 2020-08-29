---
layout: default
title: How To Use ICU
nav_order: 2
parent: ICU
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# How To Use ICU
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

# Overview

ICU builds and installs as relatively standard libraries. For details about
building, installing and porting see the [ICU4C
readme](https://htmlpreview.github.io/?https://github.com/unicode-org/icu/blob/master/icu4c/readme.html) and the
[ICU4J readme](https://htmlpreview.github.io/?https://github.com/unicode-org/icu/blob/master/icu4j/readme.html).
In addition, ICU4C installs several scripts and makefile fragments that help
build other code using ICU.

For C++, note that there are [Recommended Build
Options](https://htmlpreview.github.io/?https://github.com/unicode-org/icu/blob/master/icu4c/readme.html#RecBuild)
(both for normal use and for ICU as system-level libraries) which are not
default simply for compatibility with older ICU-using code.

Starting with ICU 49, the ICU4C readme has a short section about
[User-Configurable
Settings](https://htmlpreview.github.io/?https://github.com/unicode-org/icu/blob/master/icu4c/readme.html#UserConfig).

## C++ Makefiles

The recommended way to use ICU in Makefiles is to use the
[pkg-config](http://pkg-config.freedesktop.org/) files which are installed by
ICU upon "`make install`". There are files for various libraries and components.
This is preferred over the deprecated icu-config script.

This table shows the package names used within pkg-config.

|**Package**|**Contents**|
|------|--------------------|
|icu-uc|Common (uc) and Data (dt/data) libraries|
|icu-i18n|Internationalization (in/i18n) library|icu-le [Layout Engine](layoutengine/index.md)|
|icu-lx|Paragraph Layout|
|icu-io|[Ustdio](io/ustdio.md)/[iostream](io/ustream.md) library (icuio)

For example, to compile a simple application, you could run the following
command. See the [pkg-config](http://pkg-config.freedesktop.org/) manpage for
more details.

    c++ -o test test.c `pkg-config --libs --cflags icu-uc icu-io`

ICU installs the pkg-config (.pc) files in `$(prefix)/lib/pkgconfig` (where
`$(prefix)` is the installation prefix for ICU). Note that you may need to add
`$(prefix)/lib/pkgconfig` to the `PKG_CONFIG_PATH` variable.

### ICU in a small project

For small projects, it may be convenient to take advantage of
ICU's `autoconf`'ed files. ICU `make install` writes
`$(prefix)/lib/icu/Makefile.inc` which defines most of the necessary *make*
variables such as `$(CXX)`, `$(CXXFLAGS)`, `$(ICULIBS)`, `$(INVOKE)`, `$(ICUPKG)`,
`$(datadir)`, etc.

By itself, `Makefile.inc` is incomplete. It assumes that it will be included into another
`Makefile` which will define `$(srcdir)`, `$(DYNAMICCXXFLAGS)` and similar values.

In this case, it is probably best to copy ICU's
`autoconf`'ed top-level `./Makefile` and/or library-target-style `i18n/Makefile` and/or
binary-target-style `tools/icupkg/Makefile`. Then modify them as needed.

### ICU in a medium-sized project

If you use your own `autoconf`/`CMake`/... setup, consider cherry-picking only the
definitions needed, for example paths to specific ICU data and tools.
This is often preferable to taking the entire `Makefile.inc` and
overriding (many) definitions that are different.

For selective ICU definitions, use the installed
`$(prefix)/bin/icu-config` script.
Its contents are synchronized with `$(prefix)/lib/icu/Makefile.inc`.
For example, use `icu-config --invoke=icupkg` to invoke the ICU .dat packaging tool.

### ICU in a large project

In this case, you probably have your own build system. Just use ICU's public header
files, `.so` files, etc. See the next section, "C++ With Your Own Build System".

## Notes on `icu-config`

> :point_right: **Note**: **icu-config is deprecated, and no longer recommended for
> production use. Please use pkg-config files or other options.**

As of ICU 63.1, [icu-config has been deprecated
(ICU-10464)](https://unicode-org.atlassian.net/browse/ICU-10464).
`icu-config` may be disabled by default in the future.
As of ICU 63.1, you may enable or disable 63.1 with a configure flag:
`--enable-icu-config` or `--disable-icu-config`

`icu-config` is installed (by ICU's `make install`) into `$(prefix)/bin/icu-config`.
It can be convenient for **trivial, single-file programs** that use ICU. For
example, you could compile and build a small program with this command line:

    icu-config --cxx --cxxflags --cppflags --ldflags -o sample sample.cpp

Detailed usage of `icu-config` script is described in its `man` page.

## C++ With Your Own Build System

If you are not using the standard build system, you will need to construct your
own system. Here are a couple of starting points:

*   At least for initial bring-up, use pre-built data files from the ICU
    download or from a normally-built ICU. Copy the icudt***XXx*.dat file from
    `icu/source/data/in/` or `icu/source/data/out/tmp/` in either of these two
    locations, into `icu/source/data/in/`** on your target ICU system. That way,
    you won't need to build ICU's data-generation tools.
*   Don't compile all files. Look in the `Makefile.in` files for `OBJECTS=`
    clauses which will indicate which source files should be compiled. (Some .c
    files are #included into others and cannot be compiled by themselves.)
*   ICU does not throw or handle exceptions. Consider turning them off via g++'s
    `-fno-exceptions` or equivalent.
*   Each ICU library needs to be compiled with -DU_COMMON_IMPLEMENTATION,
    -DU_I18N_IMPLEMENTATION etc. as appropriate. See unicode/utypes.h for the
    set of such macros. If you build one single DLL (shared library) for all of
    ICU, also use -DU_COMBINED_IMPLEMENTATION. If you build ICU as
    statically-linked libraries, use -DU_STATIC_IMPLEMENTATION.
*   Use the [icu-support mailing list](http://site.icu-project.org/contacts).
    Ask for help and guidance on your strategy.
*   Up until ICU 4.8, there are one or two header files (platform.h, icucfg.h)
    that are generated by autoconf/configure and thus differ by platform,
    sometimes even by target settings on a single platform (e.g., AIX 32-bit vs.
    64-bit, Mac OS X universal binaries PowerPC vs. x86). If you do not use
    autoconf, you probably need to configure-generate these header files for
    your target platforms and select among them, or merge the generated headers
    if they are similar, or simulate their generation by other means.
*   Starting with ICU 49, all source code files are fixed (not generated). In
    particular, there is one single platform.h file which determines
    platform-specific settings via `#if ...`

## C++ Namespace

ICU C++ APIs are normally defined in a versioned namespace, for example
"icu_50". There is a stable "icu" alias which should be used instead. (Entry
point versioning is only to allow for multiple ICU versions linked into one
program. [It is optional and should be off for system
libraries.](https://htmlpreview.github.io/?https://github.com/unicode-org/icu/blob/master/icu4c/readme.html#RecBuild))

By default, and only for backward compatibility, the ICU headers contain a line
`using namespace icu_50;` which makes all ICU APIs visible in/with the global
namespace (and potentially collide with non-ICU APIs there). One of the
[Recommended Build
Options](https://htmlpreview.github.io/?https://github.com/unicode-org/icu/blob/master/icu4c/readme.html#RecBuild)
is to turn this off.

To write forward declarations, use

    U_NAMESPACE_BEGIN
    class UnicodeSet;
    class UnicodeString;
    U_NAMESPACE_END

To qualify an ICU class name, use the "icu" alias:

    static myFunction(const icu::UnicodeString &s) {...}

Frequently used ICU classes can be made easier to use in .cpp files with

    using icu::UnicodeSet;
    using icu::UnicodeString;

## Other Notes

### Helper Install Utilities

ICU installs `$(prefix)/share/icu/$(VERSION)/install-sh` and
`$(prefix)/share/icu/$(VERSION)/mkinstalldirs`. These may be used by ICU tools and
samples. Their paths are given in the installed `Makefile.inc` (see above).

### Data Packaging Settings

The `pkgdata` tool (see [Packaging ICU4C](packaging/index.md) ) makes use of the
installed file `**$(prefix)/lib/icu/pkgdata.inc**` to set parameters for data
packaging operations that require use of platform compilers and linkers ( in
`static` or `dll` mode). `pkgdata` uses the icu-config script in order to locate
**pkgdata.inc**. If you are not building ICU using the supplied tools, you may
need to modify this file directly to allow `static` and `dll` modes to function.

### Building and Running Trivial C/C++ Programs with `icurun`

For building and running trivial (one-compilation-unit) programs with an
installed ICU4C, the shell script
[icurun](https://github.com/unicode-org/icu/blob/master/tools/scripts/icurun)
may be used. For detailed help, see the top of that script.
As an example, if ICU is installed to the prefix **/opt/local** and the current
directory contains two sample programs "test1.cpp" and "test2.c", they may be
compiled and run with any of the following commands. The "-i" option specifies
either the installed icu-config script, or the directory containing that script,
or the path to a 'bin' directory.

*   `icurun **-i /opt/local** test1.cpp`
*   `icurun **-i /opt/local/bin** test2.c`
*   `icurun **-i /opt/local/bin/icu-config** test1.cpp`

If "icu-config" is on the PATH, the -i option may be omitted:

*   `icurun test1.cpp`

Any additional arguments will be passed to the program.

*   `icurun test1.cpp *args...*`

*Please give feedback to the [icu-support mailing list](http://site.icu-project.org/contacts),
and refer to [Ticket #8481](https://unicode-org.atlassian.net/browse/ICU-8481).*
