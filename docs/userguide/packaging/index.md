---
layout: default
title: Packaging ICU4C
nav_order: 3
parent: ICU Data
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Packaging ICU4C
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

This chapter describes, for the advanced user, how to package ICU4C for
distribution, whether alone, as part of an application, or as part of the
operating system.

## Making ICU Smaller

The ICU project is intended to provide everything an application might need in
order to process Unicode. However, in doing so, the results may become quite
large on disk. A default build of ICU normally results in over 16 MB of data,
and a substantial amount of object code. This section describes some techniques
to reduce the size of ICU to only the items which are required for your
application.

### Link to ICU statically

If you add the `--enable-static` option to the ICU command line build (Makefile
or cygwin), ICU will also build a static library version which you can link to
only the exact functions your application needs. Users of your ICU must compile
with -DU_STATIC_IMPLEMENTATION. Also see [How To Use ICU](../howtouseicu.md).

### Reduce the number of libraries used

ICU consists of a number of different libraries. The library dependency chart in the [Design](../design#library-dependencies-c)
chapter can be used to understand and
determine the exact set of libraries needed.

### Disable ICU features

Certain features of ICU may be turned on and off through preprocessor defines.
These switches are located in the file "uconfig.h", and disable the code for
certain features from being built.

All of these switches are defined to '0' by default, unless overridden by the
build environment, or by modifying uconfig.h itself.

| Switch Name | Library | Effect if #defined to '1' |
|--------------------------------|------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| UCONFIG_ONLY_COLLATION | common & i18n | Turn off all other modules named here except collation and legacy conversion |
| UCONFIG_NO_LEGACY_CONVERSION | common | Turn off conversion apart from UTF, CESU-8, SCSU, BOCU-1, US-ASCII, and ISO-8859-1. Not possible to turn off legacy conversion on EBCDIC platforms. |
| UCONFIG_NO_BREAK_ITERATION | common | Turn off break iteration |
| UCONFIG_NO_COLLATION | i18n | Turn off collation and collation-based string search. |
| UCONFIG_NO_FORMATTING | i18n | Turn off all formatting (date, time, number, etc), and calendar/timezone services. |
| UCONFIG_NO_TRANSLITERATION | i18n | Turn off script-to-script transliteration |
| UCONFIG_NO_REGULAR_EXPRESSIONS | i18n | Turn off the regular expression functionality |

> :point_right: **NOTE**: *These switches do not necessarily disable data generation. For example, disabling formatting does not prevent formatting data from being built into the resource bundles. See the section on ICU data, for information on changing data packaging.*
*However, some ICU data builders will not function with these switches set, such
as UCONFIG_NO_FILE_IO or UCONFIG_NO_REGULAR_EXPRESSIONS. If using these
switches, it is best to use pre-built ICU data, such as is the default for ICU
source downloads, as opposed to data builds "from scratch" out of SVN.*

#### Using UCONFIG switches with Environment Variables

This method involves setting an environment variable when ICU is built. For
example, on a POSIX-like platform, settings may be chosen at the point
runConfigureICU is run:

```shell
env CPPFLAGS="-DUCONFIG_NO_COLLATION=1 -DUCONFIGU_NO_FORMATTING=1" \
  runConfigureICU SOLARISCC ...
```

> :point_right: **Note**: When end-user code is compiled,
> it must also have the same CPPFLAGS
> set, or else calling some functions may result in a link failure.

#### Using UCONFIG switches by changing uconfig.h

This method involves modifying the source file
icu/source/common/unicode/uconfig.h directly, before ICU is built. It has the
advantage that the configuration change is propagated to all clients who compile
against this build of ICU, however the altered file must be tracked when the
next version of ICU is installed.

Modify 'uconfig.h' to add the following lines before the first #ifndef
UCONFIG_... section

```c
#ifndef UCONFIG_NO_COLLATION
#define UCONFIG_NO_COLLATION 1
#enddif
#ifndef UCONFIG_NO_FORMATTING
#define UCONFIG_NO_FORMATTING 1
#endif
```

### Reduce ICU Data used

There are many ways in which ICU data may be reduced. If only certain locales or
converters will be used, others may be removed. Additionally, data may be
packaged as individual files or interchangeable archives (.dat files), allowing
data to be installed and removed without rebuilding ICU. For details, see the
[ICU Data](../icudata.md) chapter.

## ICU Versions

(This section assumes the reader is familiar with ICU version numbers (§) as
covered in the [Design](../design.md) chapter, and filename conventions for
libraries in the [ReadMe](../../../icu4c/readme.html#HowToPackage)
.)

### POSIX Library Names

The following table gives an example of the dynamically linked library and
symbolic links built by ICU for the common ('uc') library, version 5.4.3, for
Linux

| File | Links to | Purpose |
|------------------|------------------|------------------------------------------------------------------------------------|
| `libicuuc.so` | `libicuuc.so.54.3` | Required for link: Applications compiled with ' -licuuc' will follow this symlink. |
| `libicuuc.so.54` | `libicuuc.so.54.3` | Required for runtime: This name is what applications actually link against. |
| `libicuuc.so.54.3` | Actual library | Required for runtime and link. Contains the name `libicuuc.so.54`. |

> :point_right: **Note**: This discussion gives
> Linux as an example, but it is typical for most platforms,
> of which AIX and 390 (zOS) are exceptions.

An application compiled with '-licuuc' will follow the symlink from `libicuuc.so`
to `libicuuc.so.54.3`, and will actually read the file `libicuuc.so.54.3`. (fully
qualified). This library file has an embedded name (SONAME) of `libicuuc.so.54`,
that is, with only the major and minor number. The linker will write **this**
name into the client application, because Binary compatibility is for versions
that share the same major+minor number.

If ICU version 5.4.**7** is subsequently installed, the following files may be
updated.

| File | Links to | Purpose |
|------------------|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `libicuuc.so` | `libicuuc.so.54.7` | Required for link: Newly linked applications will follow this link, which should not cause any functional difference at link time. |
| libicuuc.so.54` | `libicuuc.so.54.7` | Required for runtime: Because it now links to version .7, existing applications linked to version 5.4.3 will follow this link and use the 5.4.7 code. |
| `libicuuc.so.54.7` | Actual library | Required for runtime and link. Contains the name `libicuuc.so.54`. |

If ICU version 5.6.3 or 3.2.9 were installed, they would not affect
already-linked applications, because the major+minor numbers are different - 56
and 32, respectively, as opposed to 54. They would, however, replace the link
`libicuuc.so`, which controls which version of ICU newly-linked applications
use.

In summary, what files should an application distribute in order to include a
functional runtime copy of ICU 5.4.3? The above application should distribute
`libicuuc.so.54.3` and the symbolic link `libicuuc.so.54`. (If symbolic links pose
difficulty, `libicuuc.so.54.3` may be renamed to `libicuuc.so.54`, and only
`libicuuc.so.54` distributed. This is less informative, but functional.)

### POSIX Library suffix

The --with-library-suffix option may be used with runConfigureICU or configure,
to distinguish on disk specially modified versions of ICU. For example, the
option --with-library-suffix=**myapp** will produce libraries with names such as
libicuuc**myapp**.so.54.3, thus preventing another ICU user from using myapp's
custom ICU libraries.

While two or more versions of ICU may be linked into the same application as
long as the major and minor numbers are different, changing the library suffix
is not sufficient to allow the same version of ICU to be linked. In other words,
linking ICU 5.4.3, 5.6.3, and 3.2.9 together is allowed, but 5.4.3 and 5.4.7 may
not be linked together, nor may 5.4.3 and 5.4.3-myapp be linked together.

### Windows library names

Assuming ICU version 5.4.3, Windows library names will follow this pattern:

| File | Purpose |
|---------------|--------------------------------------------------------------------------------------------|
| `icu`**uc**`.lib` | Release Link-time library. Needed for development. Contains `icuuc54.dll` name internally. |
| `icuuc54.dll` | Release runtime library. Needed for runtime. |
| `icuuc`**d**`.lib` | Debug link-time library  (The `d` suffix indicates debug) |
| `icuuc54`**d**`.dll` | Debug runtime library. |

Debug applications must be linked with debug libraries, and release applications
with release libraries.

When a new version of ICU is installed, the .lib files will be replaced so as to
keep new compiles in sync with the newly installed header files, and the latest
DLL. As well, if the new ICU version has the same major+minor version (such as
5.4.7), then DLLs will be replaced, as they are binary compatible. However, if
an ICU with a different major+minor version is installed, such as 5.5, then new
DLLs will be copied with names such as 'icuuc55.dll'.

## Packaging ICU4C as Part of the Operating System

The services which are now known as ICU were written to provide operating
system-level and application environment-level services. Several operating
systems include ICU as a standard or optional package.
See [ICU Binary Compatibility](../design#icu-binary-compatibility) for
more details.
