---
layout: default
title: ICU4C
nav_order: 400
has_children: true
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU4C Readme
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


## Introduction

Today's software market is a global one in which it is desirable to develop and maintain one application (single source/single binary) that supports a wide variety of languages. The International Components for Unicode (ICU) libraries provide robust and full-featured Unicode services on a wide variety of platforms to help this design goal. The ICU libraries provide support for:

* The latest version of the Unicode standard
* Character set conversions with support for over 220 codepages
* Locale data for more than 300 locales
* Language sensitive text collation (sorting) and searching based on the Unicode Collation Algorithm (=ISO 14651)
* Regular expression matching and Unicode sets
* Transformations for normalization, upper/lowercase, script transliterations (50+ pairs)
* Resource bundles for storing and accessing localized information
* Date/Number/Message formatting and parsing of culture specific input/output formats
* Calendar specific date and time manipulation
* Text boundary analysis for finding characters, word and sentence boundaries

ICU has a sister project ICU4J that extends the internationalization capabilities of Java to a level similar to ICU. The ICU C/C++ project is also called ICU4C when a distinction is necessary.

## Getting started

This document describes how to build and install ICU on your machine. For other information about ICU please see the following table of links.
The ICU homepage also links to related information about writing internationalized software.

**Here are some useful links regarding ICU and internationalization in general.**


| ICU, ICU4C & ICU4J Homepage                        | <https://icu.unicode.org/>                                           |
| ICU FAQ - Frequently Asked Questions about ICU     | <https://unicode-org.github.io/icu/userguide/icu4c/faq>                  |
| ICU4J FAQ - Frequently Asked Questions about ICU4J | <https://unicode-org.github.io/icu/userguide/icu4j/faq>                  |
| ICU User's Guide                                   | <https://unicode-org.github.io/icu/>                                     |
| How To Use ICU                                     | <https://unicode-org.github.io/icu/userguide/icu/howtouseicu>            |
| Download ICU Releases                              | <https://icu.unicode.org/download>                                   |
| ICU4C API Documentation Online                     | <https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/>                                   |
| Online ICU Demos                                   | <https://icu4c-demos.unicode.org/icu-bin/icudemos>                           |
| Contacts and Bug Reports/Feature Requests          | <https://icu.unicode.org/contacts>                                   |

**Important:** Please make sure you understand the [Copyright and License Information](https://github.com/unicode-org/icu/blob/main/icu4c/LICENSE).

## What Is New In The Current Release?

See the [ICU download page](https://icu.unicode.org/download/) to find the subpage for the current release, including any other changes, bug fixes, known issues, changes to supported platforms and build environments, and migration issues for existing applications migrating from previous ICU releases.

The subpage for the current release will also include an API Change Report, both for ICU4C and ICU4J, for a complete list of APIs added, removed, or changed in this release.

The list of API changes since the previous ICU4C release is available [here](https://htmlpreview.github.io/?https://raw.githubusercontent.com/unicode-org/icu/main/icu4c/APIChangeReport.html).

Changes in previous releases can also be found on the main [ICU download page](https://icu.unicode.org/download) in its version-specific subpages.

## How To Download the Source Code

There are two ways to download ICU releases:

*   **Official Release Snapshot:**
    If you want to use ICU (as opposed to developing it), you should download an official packaged version of the ICU source code. These versions are tested more thoroughly than day-to-day development builds of the system, and they are packaged in zip and tar files for convenient download. These packaged files can be found at [https://icu.unicode.org/download](https://icu.unicode.org/download).
    The packaged snapshots are named `icu-nnnn.zip` or `icu-nnnn.tgz`, where `nnnn` is the version number. The .zip file is used for Windows platforms, while the .tgz file is preferred on most other platforms.
    Please unzip this file.
    > :point_right: **Note**: There may be additional commits on the `maint-*` branch for a particular version that are not included in the prepackaged download files.
*   **GitHub Source Repository:**
    If you are interested in developing features, patches, or bug fixes for ICU, you should probably be working with the latest version of the ICU source code. You will need to clone and checkout the code from our GitHub repository to ensure that you have the most recent version of all of the files. See our [source repository](https://icu.unicode.org/repository) for details.

## ICU Source Code Organization

In the descriptions below, `<ICU>` is the full path name of the ICU4C directory (the top level directory from the distribution archives) in your file system. You can also view the [ICU Architectural Design](../icu/design.md) section of the User's Guide to see which libraries you need for your software product. You need at least the data (`[lib]icudt`) and the common (`[lib]icuuc`) libraries in order to use ICU.

**The following files describe the code drop.**

| File        | Description                                                    |
|-------------|----------------------------------------------------------------|
| LICENSE     | Contains the text of the ICU license                           |

**The following directories contain source code and data files.**

<table>

      <tr>
        <th scope="col">Directory</th>

        <th scope="col">Description</th>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/source/<b>common</b>/</td>

        <td>The core Unicode and support functionality, such as resource bundles,
        character properties, locales, codepage conversion, normalization,
        Unicode properties, Locale, and UnicodeString.</td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/source/<b>i18n</b>/</td>

        <td>Modules in i18n are generally the more data-driven, that is to say
        resource bundle driven, components. These deal with higher-level
        internationalization issues such as formatting, collation, text break
        analysis, and transliteration.</td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/source/<b>layoutex</b>/</td>

        <td>Contains the ICU paragraph layout engine.</td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/source/<b>io</b>/</td>

        <td>Contains the ICU I/O library.</td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/source/<b>data</b>/</td>

        <td>
          <p>This directory contains the source data in text format, which is
          compiled into binary form during the ICU build process. It contains
          several subdirectories, in which the data files are grouped by
          function. Note that the build process must be run again after any
          changes are made to this directory.</p>

          <p>If some of the following directories are missing, it's probably
          because you got an official download. If you need the data source files
          for customization, then please download the complete ICU source code from <a
          href="https://icu.unicode.org/repository">the ICU repository</a>.</p>

          <ul>
            <li><b>in/</b> A directory that contains a pre-built data library for
            ICU. A standard source code package will contain this file without
            several of the following directories. This is to simplify the build
            process for the majority of users and to reduce platform porting
            issues.</li>

            <li><b>brkitr/</b> Data files for character, word, sentence, title
            casing and line boundary analysis.</li>

            <li><b>coll/</b> Data for collation tailorings.</li>

            <li><b>locales/</b> These .txt files contain ICU language and
            culture-specific localization data. Two special bundles are
            <b>root</b>, which is the fallback data and parent of other bundles,
            and <b>index</b>, which contains a list of installed bundles.
            Some of the locale data is split out into the type-specific
            directories curr, lang, region, unit, and zone, described below.</li>

            <li><b>curr/</b> Locale data for currency symbols and names (including
            plural forms).</li>

            <li><b>lang/</b> Locale data for names of languages, scripts, and locale
            key names and values.</li>

            <li><b>region/</b> Locale data for names of regions.</li>

            <li><b>unit/</b> Locale data for measurement unit patterns and
            names.</li>

            <li><b>zone/</b> Locale data for time zone names.</li>

            <li><b>mappings/</b> Here are the code page converter tables. These
            .ucm files contain mappings to and from Unicode. These are compiled
            into .cnv files. <b>convrtrs.txt</b> is the alias mapping table from
            various converter name formats to ICU internal format and vice versa.
            It produces cnvalias.icu. The makefiles <b>ucmfiles.mk,
            ucmcore.mk,</b> and <b>ucmebcdic.mk</b> contain the list of
            converters to be built.</li>

            <li><b>translit/</b> This directory contains transliterator rules as
            resource bundles, and the special bundle <b>translit_index</b> which
            lists the system transliterator aliases.</li>

            <li><b>unidata/</b> This directory contains the Unicode data files.
            Please see <a href=
            "http://www.unicode.org/">http://www.unicode.org/</a> for more
            information.</li>

            <li><b>misc/</b> The misc directory contains other data files which
            did not fit into the above categories, including time zone
            information, region-specific data, and other data derived from CLDR
            supplemental data.</li>

            <li><b>out/</b> This directory contains the assembled memory mapped
            files.</li>

            <li><b>out/build/</b> This directory contains intermediate (compiled)
            files, such as .cnv, .res, etc.</li>
          </ul>

          <p>If you are creating a special ICU build, you can set the ICU_DATA
          environment variable to the out/ or the out/build/ directories, but
          this is generally discouraged because most people set it incorrectly.
          You can view the <a href=
          "https://unicode-org.github.io/icu/userguide/icu_data">ICU Data
          Management</a> section of the ICU User's Guide for details.</p>
        </td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/source/test/<b>intltest</b>/</td>

        <td>A test suite including all C++ APIs. For information about running
        the test suite, see the build instructions specific to your platform
        later in this document.</td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/source/test/<b>cintltst</b>/</td>

        <td>A test suite written in C, including all C APIs. For information
        about running the test suite, see the build instructions specific to your
        platform later in this document.</td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/source/test/<b>iotest</b>/</td>

        <td>A test suite written in C and C++ to test the icuio library. For
        information about running the test suite, see the build instructions
        specific to your platform later in this document.</td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/source/test/<b>testdata</b>/</td>

        <td>Source text files for data, which are read by the tests. It contains
        the subdirectories <b>out/build/</b> which is used for intermediate
        files, and <b>out/</b> which contains <b>testdata.dat.</b></td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/source/<b>tools</b>/</td>

        <td>Tools for generating the data files. Data files are generated by
        invoking <i>&lt;ICU&gt;</i>/source/data/build/makedata.bat on Win32 or
        <i>&lt;ICU&gt;</i>/source/make on UNIX.</td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/source/<b>samples</b>/</td>

        <td>Various sample programs that use ICU</td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/source/<b>extra</b>/</td>

        <td>Non-supported API additions. Currently, it contains the 'uconv' tool
        to perform codepage conversion on files.</td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/<b>packaging</b>/</td>

        <td>This directory contain scripts and tools for packaging the final
        ICU build for various release platforms.</td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/source/<b>config</b>/</td>

        <td>Contains helper makefiles for platform specific build commands. Used
        by 'configure'.</td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/source/<b>allinone</b>/</td>

        <td>Contains top-level ICU workspace and project files, for instance to
        build all of ICU under one MSVC project.</td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/<b>include</b>/</td>

        <td>Contains the headers needed for developing software that uses ICU on
        Windows.</td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/<b>lib</b>/</td>

        <td>Contains the import libraries for linking ICU into your Windows
        application.</td>
      </tr>

      <tr>
        <td><i>&lt;ICU&gt;</i>/<b>bin</b>/</td>

        <td>Contains the libraries and executables for using ICU on Windows.</td>
      </tr>
    </table>



## How To Build And Install ICU

See the page on [building ICU4C](./build).

## How To Package ICU

See the page on [packaging ICU4C](./packaging).

## Important Notes About Using ICU

### Using ICU in a Multithreaded Environment

Some versions of ICU require calling the `u_init()` function from `uclean.h` to ensure that ICU is initialized properly. In those ICU versions, `u_init()` must be called before ICU is used from multiple threads. There is no harm in calling `u_init()` in a single-threaded application, on a single-CPU machine, or in other cases where `u_init()` is not required.

In addition to ensuring thread safety, `u_init()` also attempts to load at least one ICU data file. Assuming that all data files are packaged together (or are in the same folder in files mode), a failure code from `u_init()` usually means that the data cannot be found. In this case, the data may not be installed properly, or the application may have failed to call `udata_setCommonData()` or `u_setDataDirectory()` which specify to ICU where it can find its data.

Since `u_init()` will load only one or two data files, it cannot guarantee that all of the data that an application needs is available. It cannot check for all data files because the set of files is customizable, and some ICU services work without loading any data at all. An application should always check for error codes when opening ICU service objects (using `ucnv_open()`, `ucol_open()`, C++ constructors, etc.).

#### ICU 3.4 and later

ICU 3.4 self-initializes properly for multi-threaded use. It achieves this without performance penalty by hardcoding the core Unicode properties data, at the cost of some flexibility. (For details see Jitterbug 4497.)

`u_init()` can be used to check for data loading. It tries to load the converter alias table (`cnvalias.icu`).

#### ICU 2.6..3.2

These ICU versions require a call to `u_init()` before multi-threaded use. The services that are directly affected are those that don't have a service object and need to be fast: normalization and character properties.

`u_init()` loads and initializes the data files for normalization and character properties (`unorm.icu` and `uprops.icu`) and can therefore also be used to check for data loading.

#### ICU 2.4 and earlier

ICU 2.4 and earlier versions were not prepared for multithreaded use on multi-CPU platforms where the CPUs implement weak memory coherency. These CPUs include: Power4, Power5, Alpha, Itanium. `u_init()` was not defined yet.

#### Using ICU in a Multithreaded Environment on HP-UX

When ICU is built with aCC on HP-UX, the [`-AA`](http://h21007.www2.hp.com/portal/site/dspp/menuitem.863c3e4cbcdc3f3515b49c108973a801?ciid=eb08b3f1eee02110b3f1eee02110275d6e10RCRD) compiler flag is used. It is required in order to use the latest `<iostream>` API in a thread safe manner. This compiler flag affects the version of the C++ library being used. Your applications will also need to be compiled with `-AA` in order to use ICU.

#### Using ICU in a Multithreaded Environment on Solaris

##### Linking on Solaris

In order to avoid synchronization and threading issues, developers are **suggested** to strictly follow the compiling and linking guidelines for multithreaded applications, specified in the following SUn Solaris document available from Oracle. Most notably, pay strict attention to the following statements from Sun:

> To use libthread, specify `-lthread` before `-lc` on the ld command line, or last on the cc command line.
>
> To use libpthread, specify `-lpthread` before `-lc` on the ld command line, or last on the cc command line.

Failure to do this may cause spurious lock conflicts, recursive mutex failure, and deadlock.

Source: "_Multithreaded Programming Guide, Compiling and Debugging_", Sun Microsystems, 2002
[https://docs.oracle.com/cd/E19683-01/806-6867/compile-74765/index.html](https://docs.oracle.com/cd/E19683-01/806-6867/compile-74765/index.html)

Note, a version of that chapter from a 2008 document update covering both Solaris 9 and Solaris 10 is available here:
[http://docs.oracle.com/cd/E19253-01/816-5137/compile-94179/index.html](http://docs.oracle.com/cd/E19253-01/816-5137/compile-94179/index.html)

### Windows Platform

If you are building on the Windows platform, it is important that you understand a few of the following build details.

#### DLL directories and the PATH setting

As delivered, the International Components for Unicode build as several DLLs, which are placed in the `<ICU>\bin64` directory. You must add this directory to the PATH environment variable in your system, or any executables you build will not be able to access International Components for Unicode libraries. Alternatively, you can copy the DLL files into a directory already in your PATH, but we do not recommend this. You can wind up with multiple copies of the DLL and wind up using the wrong one.

#### Changing your PATH

##### Windows 2000/XP and above

Use the System Icon in the Control Panel. Pick the "Advanced" tab. Select the "Environment Variables..." button. Select the variable `PATH` in the lower box, and select the lower "Edit..." button. In the "Variable Value" box, append the string `;<ICU>\bin64` to the end of the path string. If there is nothing there, just type in `<ICU>\bin64`. Click the Set button, then the OK button.

> :point_right: **Note**:  When packaging a Windows application for distribution and installation on user systems, copies of the ICU DLLs should be included with the application, and installed for exclusive use by the application. This is the only way to insure that your application is running with the same version of ICU, built with exactly the same options, that you developed and tested with. Refer to Microsoft's guidelines on the usage of DLLs, or search for the phrase "DLL hell" on [msdn.microsoft.com](http://msdn.microsoft.com/).

### UNIX Type Platform

If you are building on a UNIX platform, and if you are installing ICU in a non-standard location, you may need to add the location of your ICU libraries to your `LD_LIBRARY_PATH` or `LIBPATH` environment variable (or the equivalent runtime library path environment variable for your system). The ICU libraries may not link or load properly without doing this.

> :point_right: **Note**:  If you do not want to have to set this variable, you may instead use the `--enable-rpath` option at configuration time. This option will instruct the linker to always look for the libraries where they are installed. You will need to use the appropriate linker options when linking your own applications and libraries against ICU, too. Please refer to your system's linker manual for information about runtime paths. The use of rpath also means that when building a new version of ICU you should not have an older version installed in the same place as the new version's installation directory, as the older libraries will used during the build, instead of the new ones, likely leading to an incorrectly build ICU. This is the proper behavior of rpath.

## Platform Dependencies

### Porting To A New Platform

If you are using ICU's Makefiles to build ICU on a new platform, there are a few places where you will need to add or modify some files. If you need more help, you can always ask the [icu-support mailing list](https://icu.unicode.org/contacts). Once you have finished porting ICU to a new platform, it is recommended that you contribute your changes back to ICU via the icu-support mailing list. This will make it easier for everyone to benefit from your work.

#### Data For a New Platform

For some people, it may not be necessary for completely build ICU. Most of the makefiles and build targets are for tools that are used for building ICU's data, and an application's data (when an application uses ICU resource bundles for its data).

Data files can be built on a different platform when both platforms share the same endianness and the same charset family. This assertion does not include platform dependent DLLs/shared/static libraries. For details see the User Guide [ICU Data](../icu_data) chapter.

ICU 3.6 removes the requirement that ICU be completely built in the native operating environment. It adds the icupkg tool which can be run on any platform to turn binary ICU data files from any one of the three formats into any one of the other data formats. This allows a application to use ICU data built anywhere to be used for any other target platform.

**WARNING!** Building ICU without running the tests is not recommended. The tests verify that ICU is safe to use. It is recommended that you try to completely port and test ICU before using the libraries for your own application.

#### Adapting Makefiles For a New Platform

Try to follow the build steps from the [UNIX](#how-to-build-and-install-on-unix) build instructions. If the configure script fails, then you will need to modify some files. Here are the usual steps for porting to a new platform:

1.  Create an mh file in `<ICU>/source/config/`. You can use mh-linux or a similar mh file as your base configuration.
2.  Modify `<ICU>/source/aclocal.m4` to recognize your platform's mh file.
3.  Modify `<ICU>/source/configure.in` to properly set your **platform** C Macro define.
4.  Run [autoconf](http://www.gnu.org/software/autoconf/) in `<ICU>/source/` without any options. The autoconf tool is standard on most Linux systems.
5.  If you have any optimization options that you want to normally use, you can modify `<ICU>/source/runConfigureICU` to specify those options for your platform.
6.  Build and test ICU on your platform. It is very important that you run the tests. If you don't run the tests, there is no guarantee that you have properly ported ICU.

### Platform Dependent Implementations

The platform dependencies have been mostly isolated into the following files in the common library. This information can be useful if you are porting ICU to a new platform.

*   **unicode/platform.h.in** (autoconf'ed platforms)
    **unicode/p_XXXX_.h** (others: pwin32.h, ppalmos.h, ..): Platform-dependent typedefs and defines:
    *   Generic types like `UBool`, `int8_t`, `int16_t`, `int32_t`, `int64_t`, `uint64_t` etc.
    *   `U_EXPORT` and `U_IMPORT` for specifying dynamic library import and export
    *   String handling support for the `char16_t` and `wchar_t` types.
*   **unicode/putil.h, putil.c**: platform-dependent implementations of various functions that are platform dependent:
    *   `uprv_isNaN`, `uprv_isInfinite`, `uprv_getNaN` and `uprv_getInfinity` for handling special floating point values.
    *   `uprv_tzset`, `uprv_timezone`, `uprv_tzname` and `time` for getting platform specific time and time zone information.
    *   `u_getDataDirectory` for getting the default data directory.
    *   `uprv_getDefaultLocaleID` for getting the default locale setting.
    *   `uprv_getDefaultCodepage` for getting the default codepage encoding.
*   **umutex.h, umutex.c**: Code for doing synchronization in multithreaded applications. If you wish to use International Components for Unicode in a multithreaded application, you must provide a synchronization primitive that the classes can use to protect their global data against simultaneous modifications. We already supply working implementations for many platforms that ICU builds on.
*   **umapfile.h, umapfile.c**: functions for mapping or otherwise reading or loading files into memory. All access by ICU to data from files makes use of these functions.
*   Using platform specific `#ifdef` macros are highly discouraged outside of the scope of these files. When the source code gets updated in the future, these `#ifdef`'s can cause testing problems for your platform.

* * *

Copyright © 2016 and later: Unicode, Inc. and others. License & terms of use: [http://www.unicode.org/copyright.html](http://www.unicode.org/copyright.html)
Copyright © 1997-2016 International Business Machines Corporation and others. All Rights Reserved.
