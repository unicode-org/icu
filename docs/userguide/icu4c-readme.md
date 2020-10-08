---
layout: default
title: ICU4C Readme
nav_order: 8
parent: ICU
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


**Note:** This is a draft readme.


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


| ICU, ICU4C & ICU4J Homepage                | <http://icu-project.org/>                                      |
| FAQ - Frequently Asked Questions about ICU | <https://unicode-org.github.io/icu/userguide/icufaq/>          |
| ICU User's Guide                           | <https://unicode-org.github.io/icu/>                           |
| How To Use ICU                             | <https://unicode-org.github.io/icu/userguide/howtouseicu.html> |
| Download ICU Releases                      | <http://site.icu-project.org/download>                         |
| ICU4C API Documentation Online             | <http://icu-project.org/apiref/icu4c/>                         |
| Online ICU Demos                           | <http://demo.icu-project.org/icu-bin/icudemos>                 |
| Contacts and Bug Reports/Feature Requests  | <http://site.icu-project.org/contacts>                         |

**Important:** Please make sure you understand the [Copyright and License Information](http://source.icu-project.org/repos/icu/trunk/icu4c/LICENSE).

## What Is New In This Release?

> :construction: TODO: move this section elsewhere

See the [ICU 67 download page](http://site.icu-project.org/download/67) for more information on this release, including any other changes, bug fixes, known issues, changes to supported platforms and build environments, and migration issues for existing applications migrating from previous ICU releases.

See the [API Change Report](APIChangeReport.html) for a complete list of APIs added, removed, or changed in this release.

<a name="RecentPreviousChanges" id="RecentPreviousChanges"></a>For changes in previous releases, see the main [ICU download page](http://site.icu-project.org/download) with its version-specific subpages.

## How To Download the Source Code

There are two ways to download ICU releases:

*   **Official Release Snapshot:**  
    If you want to use ICU (as opposed to developing it), you should download an official packaged version of the ICU source code. These versions are tested more thoroughly than day-to-day development builds of the system, and they are packaged in zip and tar files for convenient download. These packaged files can be found at [http://site.icu-project.org/download](http://site.icu-project.org/download).  
    The packaged snapshots are named `icu-nnnn.zip` or `icu-nnnn.tgz`, where nnnn is the version number. The .zip file is used for Windows platforms, while the .tgz file is preferred on most other platforms.  
    Please unzip this file.
*   **GitHub Source Repository:**  
    If you are interested in developing features, patches, or bug fixes for ICU, you should probably be working with the latest version of the ICU source code. You will need to clone and checkout the code from our GitHub repository to ensure that you have the most recent version of all of the files. See our [source repository](http://site.icu-project.org/repository) for details.

## ICU Source Code Organization

In the descriptions below, `<ICU>` is the full path name of the ICU directory (the top level directory from the distribution archives) in your file system. You can also view the [ICU Architectural Design](design.md) section of the User's Guide to see which libraries you need for your software product. You need at least the data (`[lib]icudt`) and the common (`[lib]icuuc`) libraries in order to use ICU.

**The following files describe the code drop.**

| File        | Description                                                    |
|-------------|----------------------------------------------------------------|
| readme.html | Describes the International Components for Unicode (this file) |
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
          href="http://site.icu-project.org/repository">the ICU repository</a>.</p>

          <ul>
            <li><b>in/</b> A directory that contains a pre-built data library for
            ICU. A standard source code package will contain this file without
            several of the following directories. This is to simplify the build
            process for the majority of users and to reduce platform porting
            issues.</li>

            <li><b>brkitr/</b> Data files for character, word, sentence, title
            casing and line boundary analysis.</li>

            <li><b>coll/</b> Data for collation tailorings. The makefile
            <b>colfiles.mk</b> contains the list of resource bundle files.</li>

            <li><b>locales/</b> These .txt files contain ICU language and
            culture-specific localization data. Two special bundles are
            <b>root</b>, which is the fallback data and parent of other bundles,
            and <b>index</b>, which contains a list of installed bundles. The
            makefile <b>resfiles.mk</b> contains the list of resource bundle
            files. Some of the locale data is split out into the type-specific
            directories curr, lang, region, unit, and zone, described below.</li>

            <li><b>curr/</b> Locale data for currency symbols and names (including
            plural forms), with its own makefile <b>resfiles.mk</b>.</li>

            <li><b>lang/</b> Locale data for names of languages, scripts, and locale
            key names and values, with its own makefile <b>resfiles.mk</b>.</li>

            <li><b>region/</b> Locale data for names of regions, with its own
            makefile <b>resfiles.mk</b>.</li>

            <li><b>unit/</b> Locale data for measurement unit patterns and names, 
            with its own makefile <b>resfiles.mk</b>.</li>

            <li><b>zone/</b> Locale data for time zone names, with its own
            makefile <b>resfiles.mk</b>.</li>

            <li><b>mappings/</b> Here are the code page converter tables. These
            .ucm files contain mappings to and from Unicode. These are compiled
            into .cnv files. <b>convrtrs.txt</b> is the alias mapping table from
            various converter name formats to ICU internal format and vice versa.
            It produces cnvalias.icu. The makefiles <b>ucmfiles.mk,
            ucmcore.mk,</b> and <b>ucmebcdic.mk</b> contain the list of
            converters to be built.</li>

            <li><b>translit/</b> This directory contains transliterator rules as
            resource bundles, a makefile <b>trnsfiles.mk</b> containing the list
            of installed system translitaration files, and as well the special
            bundle <b>translit_index</b> which lists the system transliterator
            aliases.</li>

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
          "https://unicode-org.github.io/icu/userguide/icudata">ICU Data
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

### Recommended Build Options

Depending on the platform and the type of installation, we recommend a small number of modifications and build options. Note that C99 compatibility is now required.

*   **Namespace (ICU 61 and later):** Since ICU 61, call sites need to qualify ICU types explicitly, for example `icu::UnicodeString`, or do `using icu::UnicodeString;` where appropriate. If your code relies on the "using namespace icu;" that used to be in unicode/uversion.h, then you need to update your code.  
    You could temporarily (until you have more time to update your code) revert to the default "using" via `-DU_USING_ICU_NAMESPACE=1` or by modifying unicode/uversion.h:

```
    Index: icu4c/source/common/unicode/uversion.h
    ===================================================================
    --- icu4c/source/common/unicode/uversion.h      (revision 40704)
    +++ icu4c/source/common/unicode/uversion.h      (working copy)
    @@ -127,7 +127,7 @@
                     defined(U_LAYOUTEX_IMPLEMENTATION) || defined(U_TOOLUTIL_IMPLEMENTATION)
     #           define U_USING_ICU_NAMESPACE 0
     #       else
    -#           define U_USING_ICU_NAMESPACE 0
    +#           define U_USING_ICU_NAMESPACE 1
     #       endif
     #   endif
     #   if U_USING_ICU_NAMESPACE
``` 

*   **Namespace (ICU 60 and earlier):** By default, unicode/uversion.h has "using namespace icu;" which defeats much of the purpose of the namespace. (This is for historical reasons: Originally, ICU4C did not use namespaces, and some compilers did not support them. The default "using" statement preserves source code compatibility.)  
    You should turn this off via `-DU_USING_ICU_NAMESPACE=0` or by modifying unicode/uversion.h:

```
    Index: source/common/unicode/uversion.h
    ===================================================================
    --- source/common/unicode/uversion.h    (revision 26606)
    +++ source/common/unicode/uversion.h    (working copy)
    @@ -180,7 +180,8 @@
     #   define U_NAMESPACE_QUALIFIER U_ICU_NAMESPACE::

     #   ifndef U_USING_ICU_NAMESPACE
    -#       define U_USING_ICU_NAMESPACE 1
    +        // Set to 0 to force namespace declarations in ICU usage.
    +#       define U_USING_ICU_NAMESPACE 0
     #   endif
     #   if U_USING_ICU_NAMESPACE
             U_NAMESPACE_USE
```

    ICU call sites then either qualify ICU types explicitly, for example `icu::UnicodeString`, or do `using icu::UnicodeString;` where appropriate.
*   **Hardcode the default charset to UTF-8:** On platforms where the default charset is always UTF-8, like MacOS X and some Linux distributions, we recommend hardcoding ICU's default charset to UTF-8. This means that some implementation code becomes simpler and faster, and statically linked ICU libraries become smaller. (See the [U_CHARSET_IS_UTF8](http://icu-project.org/apiref/icu4c/platform_8h.html#a0a33e1edf3cd23d9e9c972b63c9f7943) API documentation for more details.)  
    You can `-DU_CHARSET_IS_UTF8=1` or modify unicode/utypes.h (in ICU 4.8 and below) or modify unicode/platform.h (in ICU 49 and higher):

```
    Index: source/common/unicode/utypes.h
    ===================================================================
    --- source/common/unicode/utypes.h      (revision 26606)
    +++ source/common/unicode/utypes.h      (working copy)
    @@ -160,7 +160,7 @@
      * @see UCONFIG_NO_CONVERSION
      */
     #ifndef U_CHARSET_IS_UTF8
    -#   define U_CHARSET_IS_UTF8 0
    +#   define U_CHARSET_IS_UTF8 1
     #endif

     /*===========================================================================*/
```

*   **UnicodeString constructors:** The UnicodeString class has several single-argument constructors that are not marked "explicit" for historical reasons. This can lead to inadvertent construction of a `UnicodeString` with a single character by using an integer, and it can lead to inadvertent dependency on the conversion framework by using a C string literal.  
    Beginning with ICU 49, you should do the following:
    *   Consider marking the from-`UChar` and from-`UChar32` constructors explicit via `-DUNISTR_FROM_CHAR_EXPLICIT=explicit` or similar.
    *   Consider marking the from-`const char*` and from-`const UChar*` constructors explicit via `-DUNISTR_FROM_STRING_EXPLICIT=explicit` or similar.Note: The ICU test suites cannot be compiled with these settings.
*   **utf.h, utf8.h, utf16.h, utf_old.h:** By default, utypes.h (and thus almost every public ICU header) includes all of these header files. Often, none of them are needed, or only one or two of them. All of utf_old.h is deprecated or obsolete.  
    Beginning with ICU 49, you should define `U_NO_DEFAULT_INCLUDE_UTF_HEADERS` to 1 (via -D or uconfig.h, as above) and include those header files explicitly that you actually need.  
    Note: The ICU test suites cannot be compiled with this setting.
*   **utf_old.h:** All of utf_old.h is deprecated or obsolete.  
    Beginning with ICU 60, you should define `U_HIDE_OBSOLETE_UTF_OLD_H` to 1 (via -D or uconfig.h, as above). Use of any of these macros should be replaced as noted in the comments for the obsolete macro.  
    Note: The ICU test suites _can_ be compiled with this setting.
*   **.dat file:** By default, the ICU data is built into a shared library (DLL). This is convenient because it requires no install-time or runtime configuration, but the library is platform-specific and cannot be modified. A .dat package file makes the opposite trade-off: Platform-portable (except for endianness and charset family, which can be changed with the icupkg tool) and modifiable (also with the icupkg tool). If a path is set, then single data files (e.g., .res files) can be copied to that location to provide new locale data or conversion tables etc.  
    The only drawback with a .dat package file is that the application needs to provide ICU with the file system path to the package file (e.g., by calling `u_setDataDirectory()`) or with a pointer to the data (`udata_setCommonData()`) before other ICU API calls. This is usually easy if ICU is used from an application where `main()` takes care of such initialization. It may be hard if ICU is shipped with another shared library (such as the Xerces-C++ XML parser) which does not control `main()`.  
    See the [User Guide ICU Data](https://unicode-org.github.io/icu/userguide/icudata) chapter for more details.
    If possible, we recommend building the .dat package. Specify `--with-data-packaging=archive` on the configure command line, as in  
    `runConfigureICU Linux --with-data-packaging=archive`  
    (Read the configure script's output for further instructions. On Windows, the Visual Studio build generates both the .dat package and the data DLL.)  
    Be sure to install and use the tiny stubdata library rather than the large data DLL.
*   **Static libraries:** It may make sense to build the ICU code into static libraries (.a) rather than shared libraries (.so/.dll). Static linking reduces the overall size of the binary by removing code that is never called.  
    Example configure command line:  
    `runConfigureICU Linux --enable-static --disable-shared`
*   **Out-of-source build:** It is usually desirable to keep the ICU source file tree clean and have build output files written to a different location. This is called an "out-of-source build". Simply invoke the configure script from the target location:
```
    ~/icu$ git clone export https://github.com/unicode-org/icu.git
    ~/icu$ mkdir icu4c-build
    ~/icu$ cd icu4c-build
    ~/icu/icu4c-build$ ../icu/icu4c/source/runConfigureICU Linux
    ~/icu/icu4c-build$ make check</pre>
```
    (Note: this example shows a relative path to `runConfigureICU`. If you experience difficulty, try using an absolute path to `runConfigureICU` instead.)

#### ICU as a System-Level Library

If ICU is installed as a system-level library, there are further opportunities and restrictions to consider. For details, see the _Using ICU as an Operating System Level Library_ section of the [User Guide ICU Architectural Design](https://unicode-org.github.io/icu/userguide/design) chapter.

*   **Data path:** For a system-level library, it is best to load ICU data from the .dat package file because the file system path to the .dat package file can be hardcoded. ICU will automatically set the path to the final install location using U_ICU_DATA_DEFAULT_DIR. Alternatively, you can set `-DICU_DATA_DIR=/path/to/icu/data` when building the ICU code. (Used by source/common/putil.c.)  
    Consider also setting `-DICU_NO_USER_DATA_OVERRIDE` if you do not want the "ICU_DATA" environment variable to be used. (An application can still override the data path via `u_setDataDirectory()` or `udata_setCommonData()`.
*   **Hide draft API:** API marked with `@draft` is new and not yet stable. Applications must not rely on unstable APIs from a system-level library. Define `U_HIDE_DRAFT_API`, `U_HIDE_INTERNAL_API` and `U_HIDE_SYSTEM_API` by modifying unicode/utypes.h before installing it.
*   **Only C APIs:** Applications must not rely on C++ APIs from a system-level library because binary C++ compatibility across library and compiler versions is very hard to achieve. Most ICU C++ APIs are in header files that contain a comment with `\brief C++ API`. Consider not installing these header files, or define `U_SHOW_CPLUSPLUS_API` to be `0` by modifying unicode/utypes.h before installing it.
*   **Disable renaming:** By default, ICU library entry point names have an ICU version suffix. Turn this off for a system-level installation, to enable upgrading ICU without breaking applications. For example:  
    `runConfigureICU Linux --disable-renaming`  
    The public header files from this configuration must be installed for applications to include and get the correct entry point names.

### [User-Configurable Settings](#UserConfig)

ICU4C can be customized via a number of user-configurable settings. Many of them are controlled by preprocessor macros which are defined in the `source/common/unicode/uconfig.h` header file. Some turn off parts of ICU, for example conversion or collation, trading off a smaller library for reduced functionality. Other settings are recommended (see previous section) but their default values are set for better source code compatibility.

In order to change such user-configurable settings, you can either modify the `uconfig.h` header file by adding a specific `#define ...` for one or more of the macros before they are first tested, or set the compiler's preprocessor flags (`CPPFLAGS`) to include an equivalent `-D` macro definition.

### [How To Build And Install On Windows](#HowToBuildWindows)

Building International Components for Unicode requires:

*   Microsoft Windows
*   Microsoft Visual C++ (part of [Visual Studio](https://www.visualstudio.com/)) (from either Visual Studio 2015 or Visual Studio 2017)
*   _**Optional:**_ A version of the [Windows 10 SDK](https://developer.microsoft.com/windows/downloads) (if you want to build the UWP projects)

> :point_right: **Note**: [Cygwin](#HowToBuildCygwin) is required if using a version of MSVC other than the one compatible with the supplied project files or if other compilers are used to build ICU. (e.g. GCC)

The steps are:

1.  Unzip the `icu-XXXX.zip` file into any convenient location. 
    *   You can use the built-in zip functionality of Windows Explorer to do this. Right-click on the .zip file and choose the "Extract All" option from the context menu. This will open a new window where you can choose the output location to put the files.
    *   Alternatively, you can use a 3<sup>rd</sup> party GUI tool like 7-Zip or WinZip to do this as well.
2.  Be sure that the ICU binary directory, (ex: `<ICU>\bin\`), is included in the **PATH** environment variable. The tests will not work without the location of the ICU DLL files in the path. Note that the binary directory name can depend on what architecture you select when you compile ICU. For x86 or 32-bit builds, the binary directory is `bin`. Whereas for x64 or 64-bit builds the binary directory is `bin64`.
3.  Open the `<ICU>\source\allinone\allinone.sln` solution file in 'Visual Studio 2017'. (This solution includes all the International Components for Unicode libraries, necessary ICU building tools, and the test suite projects). Please see the [command line note below](#HowToBuildWindowsCommandLine) if you want to build from the command line instead.
4.  If you are building using 'Visual Studio 2015' instead, or if you are building the UWP projects and you have a different version of the Windows 10 SDK installed you will first need to modify the two `Build.Windows.*.props` files in the `allinone` directory before you can open the "allinone" solution file. Please see the notes below about [building with other versions of Visual Studio](#HowToUseOtherVSVersions) and the notes on [re-targeting the Windows 10 SDK for the UWP projects](#HowToRetargetTheWin10SDK) for details. Alternatively, you can [skip building the UWP projects](#HowToSkipBuildingUWP) entirely as well.
5.  Set the active platform to "Win32" or "x64" (See [Windows platform note](#HowToBuildWindowsPlatform) below) and configuration to "Debug" or "Release" (See [Windows configuration note](#HowToBuildWindowsConfig) below).
6.  Choose the "Build" menu and select "Rebuild Solution". If you want to build the Debug and Release at the same time, see the [batch configuration note](#HowToBuildWindowsBatch) below.
7.  Run the tests. They can be run from the command line or from within Visual Studio.

    #### Running the Tests from the Windows Command Line (cmd)

    *   The general syntax is:  

        <div class="indent"><tt>_&lt;ICU&gt;_\source\allinone\icucheck.bat _Platform_ _Configuration_</tt></div>

    *   So, for example for x86 (32-bit) and Debug, use the following:  
        <samp>_<ICU>_\source\allinone\icucheck.bat **x86** **Debug**</samp> 
        For x86 (32-bit) and Release: <samp>_<ICU>_\source\allinone\icucheck.bat **x86** **Release**</samp>
        For x64 (64-bit) and Debug: <samp>_<ICU>_\source\allinone\icucheck.bat **x64** **Debug**</samp> 
        For x64 (64-bit) and Release: <samp>_<ICU>_\source\allinone\icucheck.bat **x64** **Release**</samp>

    #### Running the Tests from within Visual Studio

    1.  Run the C++ test suite, `intltest`. To do this: set the active startup project to "intltest", and press Ctrl+F5 to run it. Make sure that it passes without any errors.
    2.  Run the C test suite, `cintltst`. To do this: set the active startup project to "cintltst", and press Ctrl+F5 to run it. Make sure that it passes without any errors.
    3.  Run the I/O test suite, `iotest`. To do this: set the active startup project to "iotest", and press Ctrl+F5 to run it. Make sure that it passes without any errors.
8.  You are now able to develop applications with ICU by using the libraries and tools in <tt>_<ICU>_\bin\</tt>. The headers are in <tt>_<ICU>_\include\</tt> and the link libraries are in <tt>_<ICU>_\lib\</tt>. To install the ICU runtime on a machine, or ship it with your application, copy the needed components from <tt>_<ICU>_\bin\</tt> to a location on the system PATH or to your application directory.

<a name="HowToUseOtherVSVersions" id="HowToUseOtherVSVersions">**Building with other versions of Visual Studio Note:**</a> The particular version of the MSVC compiler tool-set (and thus the corresponding version of Visual Studio) that is used to compile ICU is determined by the "<tt>PlatformToolset</tt>" property. This property is stored in two different shared files that are used to set common configuration settings amongst the various ICU "<tt>*.vcxproj</tt>" project files. For the non-UWP projects, this setting is in the shared file called "<tt>Build.Windows.ProjectConfiguration.props</tt>" located in the "allinone" directory. For the UWP projects, this setting is in the shared file called "<tt>Build.Windows.UWP.ProjectConfiguration.props</tt>", also located in the "allinone" directory.  
The value of <tt>v140</tt> corresponds to the Visual Studio 2015 compiler tool set, whereas the value of <tt>v141</tt> corresponds to the Visual Studio 2017 compiler tool set.  
In order to build the non-UWP projects with Visual Studio 2015 you will need to modify the file called "<tt>Build.Windows.ProjectConfiguration.props</tt>" to change the value of the <tt>PlatformToolset</tt> property. Note however that Visual Studio 2017 is required for building the UWP projects.

Please consider: Using older versions of the MSVC compiler is generally not recommended due to the improved support for the C++11 standard in newer versions of the compiler.

<a name="HowToRetargetTheWin10SDK" id="HowToRetargetTheWin10SDK">**Re-targeting the Windows 10 SDK for the UWP projects Note:**</a> If the version of the Windows 10 SDK that you have installed does not match the version used by the UWP projects, then you will need to "retarget" them to use the version of the SDK that you have installed instead. There are two ways to do this:

*   In Visual Studio you can right-click on the UWP projects in the 'Solution Explorer' and select the option 'Retarget Projects' from the context menu. This will open up a window where you can select the SDK version to target from a drop-down list of the various SDKs that are installed on the machine.
*   Alternatively, you can manually edit the shared file called "<tt>Build.Windows.UWP.ProjectConfiguration.props</tt>" which is located in the "allinone" directory. You will need to change the of the "<tt>WindowsTargetPlatformVersion</tt>" property to the version of the SDK that you would like to use instead.

<a name="HowToBuildWindowsCommandLine" id="HowToBuildWindowsCommandLine">**Using MSBUILD At The Command Line Note:**</a> You can build ICU from the command line instead of using the Visual Studio GUI. Assuming that you have properly installed Visual Studio to support command line building, you should have a shortcut for the "Developer Command Prompt" listed in the Start Menu. (For Visual Studio 2017 you will need to install the "Desktop development with C++" option).

*   Open the "Developer Command Prompt" shortcut from the Start Menu. (This will open up a new command line window).
*   From within the "Developer Command Prompt" change directory (<tt>cd</tt>) to the ICU source directory.
*   You can then use either '<tt>msbuild</tt>' directly, or you can use the '<tt>devenv.com</tt>' command to build ICU.
*   Using <tt>MSBUILD</tt>:

*   To build the 32-bit Debug version, use the following command line:  
    `'msbuild source\allinone\allinone.sln /p:Configuration=Debug /p:Platform=Win32'`.
*   To build the 64-bit Release version, use the following command line:  
    `'msbuild source\allinone\allinone.sln /p:Configuration=Release /p:Platform=x64'`.

*   Using <tt>devenv.com</tt>:

*   To build the 32-bit Debug version, use the following command line:  
    `'devenv.com source\allinone\allinone.sln /build "Debug|Win32"'`.
*   To build the 64-bit Release version, use the following command line:  
    `'devenv.com source\allinone\allinone.sln /build "Release|x64"'`.

<a name="HowToSkipBuildingUWP" id="HowToSkipBuildingUWP">**Skipping the UWP Projects on the Command Line Note:**</a> You can skip (or omit) building the UWP projects on the command line by passing the argument '`SkipUWP=true`' to either MSBUILD or devenv.

*   For example, using <tt>MSBUILD</tt>:

*   To skip building the UWP projects with a 32-bit Debug build, use the following command line:  
    `'msbuild source\allinone\allinone.sln /p:Configuration=Debug /p:Platform=Win32 /p:SkipUWP=true'`.
*   To skip building the UWP projects with a 64-bit Release version, use the following command line:  
    `'msbuild source\allinone\allinone.sln /p:Configuration=Release /p:Platform=x64 /p:SkipUWP=true'`.

You can also use Cygwin with the MSVC compiler to build ICU, and you can refer to the [How To Build And Install On Windows with Cygwin](#HowToBuildCygwin) section for more details.

<a name="HowToBuildWindowsPlatform" id="HowToBuildWindowsPlatform">**Setting Active Platform Note:**</a> Even though you are able to select "x64" as the active platform, if your operating system is not a 64 bit version of Windows, the build will fail. To set the active platform, two different possibilities are:

*   Choose "Build" menu, select "Configuration Manager...", and select "Win32" or "x64" for the Active Platform Solution.
*   Another way is to select the desired build configuration from "Solution Platforms" dropdown menu from the standard toolbar. It will say "Win32" or "x64" in the dropdown list.

<a name="HowToBuildWindowsConfig" id="HowToBuildWindowsConfig">**Setting Active Configuration Note:**</a> To set the active configuration, two different possibilities are:

*   Choose "Build" menu, select "Configuration Manager...", and select "Release" or "Debug" for the Active Configuration Solution.
*   Another way is to select the desired build configuration from "Solution Configurations" dropdown menu from the standard toolbar. It will say "Release" or "Debug" in the dropdown list.

<a name="HowToBuildWindowsBatch" id="HowToBuildWindowsBatch">**Batch Configuration Note:**</a> If you want to build the Win32 and x64 platforms and Debug and Release configurations at the same time, choose "Build" menu, and select "Batch Build...". Click the "Select All" button, and then click the "Rebuild" button.

### [How To Build And Install On Windows with Cygwin](#HowToBuildCygwin)

Building International Components for Unicode with this configuration requires:

*   Microsoft Windows
*   Microsoft Visual C++ (from Visual Studio 2015 or newer, when gcc isn't used).
*   Cygwin with the following installed:
    *   bash
    *   GNU make
    *   ar
    *   ranlib
    *   man (if you plan to look at the man pages)

There are two ways you can build ICU with Cygwin. You can build with gcc or Microsoft Visual C++. If you use gcc, the resulting libraries and tools will depend on the Cygwin environment. If you use Microsoft Visual C++, the resulting libraries and tools do not depend on Cygwin and can be more easily distributed to other Windows computers (the generated man pages and shell scripts still need Cygwin). To build with gcc, please follow the "[How To Build And Install On UNIX](#HowToBuildUNIX)" instructions, while you are inside a Cygwin bash shell. To build with Microsoft Visual C++, please use the following instructions:

1.  Start the Windows "Command Prompt" window. This is different from the gcc build, which requires the Cygwin Bash command prompt. The Microsoft Visual C++ compiler will not work with a bash command prompt.
2.  If the computer isn't set up to use Visual C++ from the command line, you need to run vcvars32.bat.  
    For example:  
    "<tt>C:\Program Files (x86)\Microsoft Visual Studio 14.0\VC\bin\vcvars32.bat</tt>" can be used for 32-bit builds **or**  
    "<tt>C:\Program Files (x86)\Microsoft Visual Studio 14\VC\bin\x86_amd64\vcvarsx86_amd64.bat</tt>" can be used for 64-bit builds on Windows x64.
3.  Unzip the icu-XXXX.zip file into any convenient location. Using command line zip, type "unzip -a icu-XXXX.zip -d drive:\directory", or just use WinZip.
4.  Change directory to "icu/source", which is where you unzipped ICU.
5.  Run "<tt>bash [./runConfigureICU](source/runConfigureICU) Cygwin/MSVC</tt>" (See [Windows configuration note](#HowToWindowsConfigureICU) and non-functional configure options below).
6.  Type <tt>"make"</tt> to compile the libraries and all the data files. This make command should be GNU make.
7.  Optionally, type <tt>"make check"</tt> to run the test suite, which checks for ICU's functionality integrity (See [testing note](#HowToTestWithoutGmake) below).
8.  Type <tt>"make install"</tt> to install ICU. If you used the --prefix= option on configure or runConfigureICU, ICU will be installed to the directory you specified. (See [installation note](#HowToInstallICU) below).

<a name="HowToWindowsConfigureICU" id="HowToWindowsConfigureICU">**Configuring ICU on Windows NOTE:**</a>

Ensure that the order of the PATH is MSVC, Cygwin, and then other PATHs. The configure script needs certain tools in Cygwin (e.g. grep).

Also, you may need to run <tt>"dos2unix.exe"</tt> on all of the scripts (e.g. configure) in the top source directory of ICU. To avoid this issue, you can download the ICU source for Unix platforms (icu-xxx.tgz).

In addition to the Unix [configuration note](#HowToConfigureICU) the following configure options currently do not work on Windows with Microsoft's compiler. Some options can work by manually editing <tt>icu/source/common/unicode/pwin32.h</tt>, but manually editing the files is not recommended.

*   <tt>--disable-renaming</tt>
*   <tt>--enable-tracing</tt>
*   <tt>--enable-rpath</tt>
*   <tt>--enable-static</tt> (Requires that U_STATIC_IMPLEMENTATION be defined in user code that links against ICU's static libraries.)
*   <tt>--with-data-packaging=files</tt> (The pkgdata tool currently does not work in this mode. Manual packaging is required to use this mode.)

### [How To Build And Install On UNIX](#HowToBuildUNIX)

Building International Components for Unicode on UNIX requires:

*   A C++ compiler installed on the target machine (for example: gcc, CC, xlC_r, aCC, cxx, etc...).
*   An ANSI C compiler installed on the target machine (for example: cc).
*   A recent version of GNU make (3.80+).
*   For a list of z/OS tools please view the [z/OS build section](#HowToBuildZOS) of this document for further details.

Here are the steps to build ICU:

1.  Decompress the icu-_X_._Y_.tgz (or icu-_X_._Y_.tar.gz) file. For example, <samp>gunzip -d < icu-_X_._Y_.tgz | tar xvf -</samp>
2.  Change directory to `icu/source`. <samp>cd icu/source</samp>
3.  Some files may have the wrong permissions.<samp>chmod +x runConfigureICU configure install-sh</samp>
4.  Run the <span style="font-family: monospace;">[runConfigureICU](source/runConfigureICU)</span> script for your platform. (See [configuration note](#HowToConfigureICU) below).
5.  Now build: <samp>gmake</samp> (or just `make` if GNU make is the default make on your platform) to compile the libraries and all the data files. The proper name of the GNU make command is printed at the end of the configuration run, as in <tt>"You must use gmake to compile ICU"</tt>.  
    Note that the compilation command output may be simplified on your platform. If this is the case, you will see just: <tt>gcc ... stubdata.c</tt> rather than <tt>gcc -DU_NO_DEFAULT_INCLUDE_UTF_HEADERS=1 -D_REENTRANT -I../common -DU_ATTRIBUTE_DEPRECATED= -O2 -Wall -std=c99 -pedantic -Wshadow -Wpointer-arith -Wmissing-prototypes -Wwrite-strings -c -DPIC -fPIC -o stubdata.o stubdata.c</tt>  
    If you need to see the whole compilation line, use <span style="font-family: monospace;">"gmake VERBOSE=1"</span>. The full compilation line will print if an error occurs.
6.  Optionally,<samp>gmake check</samp> will run the test suite, which checks for ICU's functionality integrity (See [testing note](#HowToTestWithoutGmake) below).
7.  To install, <samp>gmake install</samp> to install ICU. If you used the --prefix= option on configure or runConfigureICU, ICU will be installed to the directory you specified. (See [installation note](#HowToInstallICU) below).

<a name="HowToConfigureICU" id="HowToConfigureICU">**Configuring ICU NOTE:**</a> Type <tt>"./runConfigureICU --help"</tt> for help on how to run it and a list of supported platforms. You may also want to type <tt>"./configure --help"</tt> to print the available configure options that you may want to give runConfigureICU. If you are not using the runConfigureICU script, or your platform is not supported by the script, you may need to set your CC, CXX, CFLAGS and CXXFLAGS environment variables, and type <tt>"./configure"</tt>. HP-UX users, please see this [note regarding HP-UX multithreaded build issues](#ImportantNotesHPUX) with newer compilers. Solaris users, please see this [note regarding Solaris multithreaded build issues](#ImportantNotesSolaris).

ICU is built with strict compiler warnings enabled by default. If this causes excessive numbers of warnings on your platform, use the --disable-strict option to configure to reduce the warning level.

<a name="HowToTestWithoutGmake" id="HowToTestWithoutGmake">**Running The Tests From The Command Line NOTE:**</a> You may have to set certain variables if you with to run test programs individually, that is apart from "gmake check". The environment variable **ICU_DATA** can be set to the full pathname of the data directory to indicate where the locale data files and conversion mapping tables are when you are not using the shared library (e.g. by using the .dat archive or the individual data files). The trailing "/" is required after the directory name (e.g. "$Root/source/data/out/" will work, but the value "$Root/source/data/out" is not acceptable). You do not need to set **ICU_DATA** if the complete shared data library is in your library path.

<a name="HowToInstallICU" id="HowToInstallICU">**Installing ICU NOTE:**</a> Some platforms use package management tools to control the installation and uninstallation of files on the system, as well as the integrity of the system configuration. You may want to check if ICU can be packaged for your package management tools by looking into the "packaging" directory. (Please note that if you are using a snapshot of ICU from Git, it is probable that the packaging scripts or related files are not up to date with the contents of ICU at this time, so use them with caution).

### [How To Build And Install On z/OS (OS/390)](#HowToBuildZOS)

You can install ICU on z/OS or OS/390 (the previous name of z/OS), but IBM tests only the z/OS installation. You install ICU in a z/OS UNIX system services file system such as HFS or zFS. On this platform, it is important that you understand a few details:

*   The makedep and GNU make tools are required for building ICU. If it is not already installed on your system, it is available at the [z/OS UNIX - Tools and Toys](http://www-03.ibm.com/servers/eserver/zseries/zos/unix/bpxa1toy.html) site. The PATH environment variable should be updated to contain the location of this executable prior to build. Failure to add these tools to your PATH will cause ICU build failures or cause pkgdata to fail to run.
*   Since USS does not support using the mmap() function over NFS, it is recommended that you build ICU on a local filesystem. Once ICU has been built, you should not have this problem while using ICU when the data library has been built as a shared library, which is this is the default setting.
*   Encoding considerations: The source code assumes that it is compiled with codepage ibm-1047 (to be exact, the UNIX System Services variant of it). The pax command converts all of the source code files from ASCII to codepage ibm-1047 (USS) EBCDIC. However, some files are binary files and must not be converted, or must be converted back to their original state. You can use the [unpax-icu.sh](as_is/os390/unpax-icu.sh) script to do this for you automatically. It will unpackage the tar file and convert all the necessary files for you automatically.
*   z/OS supports both native S/390 hexadecimal floating point and (with OS/390 2.6 and later) IEEE 754 binary floating point. This is a compile time option. Applications built with IEEE should use ICU DLLs that are built with IEEE (and vice versa). The environment variable IEEE390=0 will cause the z/OS version of ICU to be built without IEEE floating point support and use the native hexadecimal floating point. By default ICU is built with IEEE 754 support. Native floating point support is sufficient for codepage conversion, resource bundle and UnicodeString operations, but the Format APIs require IEEE binary floating point.
*   z/OS introduced the concept of Extra Performance Linkage (XPLINK) to bring performance improvement opportunities to call-intensive C and C++ applications such as ICU. XPLINK is enabled on a DLL-by-DLL basis, so if you are considering using XPLINK in your application that uses ICU, you should consider building the XPLINK-enabled version of ICU. You need to set ICU's environment variable `OS390_XPLINK=1` prior to invoking the make process to produce binaries that are enabled for XPLINK. The XPLINK option, which is available for z/OS 1.2 and later, requires the PTF PQ69418 to build XPLINK enabled binaries.
*   ICU requires XPLINK for the icuio library. If you want to use the rest of ICU without XPLINK, then you must use the --disable-icuio configure option.
*   The latest versions of z/OS use [XPLINK version (C128) of the C++ standard library](https://www.ibm.com/support/knowledgecenter/SSLTBW_2.2.0/com.ibm.zos.v2r2.cbcux01/oebind6.htm) by default. You may see [an error](https://www.ibm.com/support/knowledgecenter/SSLTBW_2.2.0/com.ibm.zos.v2r2.cbcux01/oebind5.htm) when running with XPLINK disabled. To avoid this error, set the following environment variable or similar:

    <pre><samp>export _CXX_PSYSIX="CEE.SCEELIB(C128N)":"CBC.SCLBSID(IOSTREAM,COMPLEX)"</samp></pre>

*   When building ICU data, the heap size may need to be increased with the following environment variable:

    <pre><samp>export _CEE_RUNOPTS="HEAPPOOLS(ON),HEAP(4M,1M,ANY,FREE,0K,4080)"</samp></pre>

*   The rest of the instructions for building and testing ICU on z/OS with UNIX System Services are the same as the [How To Build And Install On UNIX](#HowToBuildUNIX) section.

#### z/OS (Batch/PDS) support outside the UNIX system services environment

By default, ICU builds its libraries into the UNIX file system (HFS). In addition, there is a z/OS specific environment variable (OS390BATCH) to build some libraries into the z/OS native file system. This is useful, for example, when your application is externalized via Job Control Language (JCL).

The OS390BATCH environment variable enables non-UNIX support including the batch environment. When OS390BATCH is set, the libicui18n_XX_.dll, libicuuc_XX_.dll, and libicudt_XX_e.dll binaries are built into data sets (the native file system). Turning on OS390BATCH does not turn off the normal z/OS UNIX build. This means that the z/OS UNIX (HFS) DLLs will always be created.

Two additional environment variables indicate the names of the z/OS data sets to use. The LOADMOD environment variable identifies the name of the data set that contains the dynamic link libraries (DLLs) and the LOADEXP environment variable identifies the name of the data set that contains the side decks, which are normally the files with the .x suffix in the UNIX file system.

A data set is roughly equivalent to a UNIX or Windows file. For most kinds of data sets the operating system maintains record boundaries. UNIX and Windows files are byte streams. Two kinds of data sets are PDS and PDSE. Each data set of these two types contains a directory. It is like a UNIX directory. Each "file" is called a "member". Each member name is limited to eight bytes, normally EBCDIC.

Here is an example of some environment variables that you can set prior to building ICU:

<pre><samp>OS390BATCH=1
LOADMOD=_USER_.ICU.LOAD
LOADEXP=_USER_.ICU.EXP</samp>
</pre>

The PDS member names for the DLL file names are as follows:

<pre><samp>IXMI_XX_IN --> libicui18n_XX_.dll
IXMI_XX_UC --> libicuuc_XX_.dll
IXMI_XX_DA --> libicudt_XX_e.dll</samp>
</pre>

You should point the LOADMOD environment variable at a partitioned data set extended (PDSE) and point the LOADEXP environment variable at a partitioned data set (PDS). The PDSE can be allocated with the following attributes:

<pre><samp>Data Set Name . . . : _USER_.ICU.LOAD
Management class. . : _**None**_
Storage class . . . : _BASE_
Volume serial . . . : _TSO007_
Device type . . . . : _3390_
Data class. . . . . : _LOAD_
Organization  . . . : PO
Record format . . . : U
Record length . . . : 0
Block size  . . . . : _32760_
1st extent cylinders: 1
Secondary cylinders : 5
Data set name type  : LIBRARY</samp>
</pre>

The PDS can be allocated with the following attributes:

<pre><samp>Data Set Name . . . : _USER_.ICU.EXP
Management class. . : _**None**_
Storage class . . . : _BASE_
Volume serial . . . : _TSO007_
Device type . . . . : _3390_
Data class. . . . . : _**None**_
Organization  . . . : PO
Record format . . . : FB
Record length . . . : 80
Block size  . . . . : _3200_
1st extent cylinders: 3
Secondary cylinders : 3
Data set name type  : PDS</samp>
</pre>

### [How To Build And Install On The IBM i Family (IBM i, i5/OS OS/400)](#HowToBuildOS400)

Before you start building ICU, ICU requires the following:

*   QSHELL interpreter installed (install base option 30, operating system)
*   ILE C/C++ Compiler installed on the system
*   The latest IBM tools for Developers for IBM i — [https://www-356.ibm.com/partnerworld/wps/servlet/ContentHandler/pw_com_porting_tools_index](https://www-356.ibm.com/partnerworld/wps/servlet/ContentHandler/pw_com_porting_tools_index)

The following describes how to setup and build ICU. For background information, you should look at the [UNIX build instructions](#HowToBuildUNIX).

1.  Copy the ICU source .tgz to the IBM i environment, as binary. Also, copy the [unpax-icu.sh](as_is/os400/unpax-icu.sh) script into the same directory, as a text file.
2.  Create target library. This library will be the target for the resulting modules, programs and service programs. You will specify this library on the OUTPUTDIR environment variable.

    <pre><samp>CRTLIB LIB(_libraryname_)
    ADDENVVAR ENVVAR(OUTPUTDIR) VALUE('_libraryname_') REPLACE(*YES)</samp> </pre>

3.  Set up the following environment variables and job characteristics in your build process

    <pre><samp>ADDENVVAR ENVVAR(MAKE) VALUE('gmake') REPLACE(*YES)
    CHGJOB CCSID(37)</samp></pre>

4.  Fire up the QSH _(all subsequent commands are run inside the qsh session.)_

    <pre><samp>qsh</samp></pre>

5.  Set up the PATH:

    <pre><samp>export PATH=/QIBM/ProdData/DeveloperTools/qsh/bin:$PATH:/QOpenSys/usr/bin</samp></pre>

6.  Unpack the ICU source code archive:

    <pre><samp>gzip -d icu-_X_._Y_.tgz</samp></pre>

7.  Run unpax-icu.sh on the tar file generated from the previous step.

    <pre><samp>unpax-icu.sh icu.tar</samp></pre>

8.  Build the program ICULD which ICU will use for linkage.

    <pre><samp>cd icu/as_is/os400
    qsh bldiculd.sh
    cd ../../..</samp></pre>

9.  Change into the 'source' directory, and configure ICU. (See [configuration note](#HowToConfigureICU) for details). Note that --with-data-packaging=archive and setting the --prefix are recommended, building in default (dll) mode is currently not supported.

    <pre><samp>cd icu/source
    ./runConfigureICU IBMi --prefix=_/path/to/somewhere_ --with-data-packaging=archive</samp></pre>

10.  Build ICU. _(Note: Do not use the -j option)_

    <pre><samp>gmake</samp></pre>

11.  Test ICU.

    <pre><samp>gmake check</samp></pre>

    (The <tt>QIBM_MULTI_THREADED=Y</tt> flag will be automatically applied to intltest - you can look at the [iSeries Information Center](https://www.ibm.com/support/knowledgecenter/ssw_ibm_i_73/rzahw/rzahwceeco.htm) for more details regarding the running of multiple threads on IBM i.)

### [How To Cross Compile ICU](#HowToCrossCompileICU)

This section will explain how to build ICU on one platform, but to produce binaries intended to run on another. This is commonly known as a cross compile.

Normally, in the course of a build, ICU needs to run the tools that it builds in order to generate and package data and test-data.In a cross compilation setting, ICU is built on a different system from that which it eventually runs on. An example might be, if you are building for a small/headless system (such as an embedded device), or a system where you can't easily run the ICU command line tools (any non-UNIX-like system).

To reduce confusion, we will here refer to the "A" and the "B" system.System "A" is the actual system we will be running on- the only requirements on it is are it is able to build ICU from the command line targetting itself (with configure or runConfigureICU), and secondly, that it also contain the correct toolchain for compiling and linking for the resultant platform, referred to as the "B" system.

The autoconf docs use the term "build" for A, and "host" for B. More details at: [http://www.gnu.org/software/autoconf/manual/html_node/Specifying-Names.html](http://www.gnu.org/software/autoconf/manual/html_node/Specifying-Names.html#Specifying-Names)

Three initially-empty directories will be used in this example:

<table summary="Three directories used in this example" class="docTable">

<tbody>

<tr>

<th align="left">/icu</th>

<td>a copy of the ICU source</td>

</tr>

<tr>

<th align="left">/buildA</th>

<td>an empty directory, it will contain ICU built for A  
(MacOSX in this case)</td>

</tr>

<tr>

<th align="left">/buildB</th>

<td>an empty directory, it will contain ICU built for B  
(HaikuOS in this case)</td>

</tr>

</tbody>

</table>

1.  Check out or unpack the ICU source code into the /icu directory.You will have the directories /icu/source, etc.
2.  Build ICU in /buildA normally (using runConfigureICU or configure):

    <pre class="samp">cd /buildA
    sh /icu/source/runConfigureICU **MacOSX**
    gnumake
    </pre>

3.  Set PATH or other variables as needed, such as CPPFLAGS.
4.  Build ICU in /buildB  

    "`--with-cross-build`" takes an absolute path.

    <pre class="samp">cd /buildB
    sh /icu/source/configure --host=**i586-pc-haiku** --with-cross-build=**/buildA**
    gnumake</pre>

5.  Tests and testdata can be built with "gnumake tests".

## [How To Package ICU](#HowToPackage)

There are many ways that a person can package ICU with their software products. Usually only the libraries need to be considered for packaging.

On UNIX, you should use "<tt>gmake install</tt>" to make it easier to develop and package ICU. The bin, lib and include directories are needed to develop applications that use ICU. These directories will be created relative to the "<tt>--prefix=</tt>_dir_" configure option (See the [UNIX build instructions](#HowToBuildUNIX)). When ICU is built on Windows, a similar directory structure is built.

When changes have been made to the standard ICU distribution, it is recommended that at least one of the following guidelines be followed for special packaging.

1.  Add a suffix name to the library names. This can be done with the --with-library-suffix configure option.
2.  The installation script should install the ICU libraries into the application's directory.

Following these guidelines prevents other applications that use a standard ICU distribution from conflicting with any libraries that you need. On operating systems that do not have a standard C++ ABI (name mangling) for compilers, it is recommended to do this special packaging anyway. More details on customizing ICU are available in the [User's Guide](https://unicode-org.github.io/icu/userguide/). The [ICU Source Code Organization](#SourceCode) section of this readme.html gives a more complete description of the libraries.

<table class="docTable" summary="ICU has several libraries for you to use."><caption>Here is an example of libraries that are frequently packaged.</caption>

<tbody>

<tr>

<th scope="col">Library Name</th>

<th scope="col">Windows Filename</th>

<th scope="col">Linux Filename</th>

<th scope="col">Comment</th>

</tr>

<tr>

<td>Data Library</td>

<td>icudt_XY_l.dll</td>

<td>libicudata.so._XY_._Z_</td>

<td>Data required by the Common and I18n libraries. There are many ways to package and [customize this data](https://unicode-org.github.io/icu/userguide/icudata), but by default this is all you need.</td>

</tr>

<tr>

<td>Common Library</td>

<td>icuuc_XY_.dll</td>

<td>libicuuc.so._XY_._Z_</td>

<td>Base library required by all other ICU libraries.</td>

</tr>

<tr>

<td>Internationalization (i18n) Library</td>

<td>icuin_XY_.dll</td>

<td>libicui18n.so._XY_._Z_</td>

<td>A library that contains many locale based internationalization (i18n) functions.</td>

</tr>

<tr>

<td>Layout Extensions Engine</td>

<td>iculx_XY_.dll</td>

<td>libiculx.so._XY_._Z_</td>

<td>An optional engine for doing paragraph layout that uses parts of ICU. HarfBuzz is required.</td>

</tr>

<tr>

<td>ICU I/O (Unicode stdio) Library</td>

<td>icuio_XY_.dll</td>

<td>libicuio.so._XY_._Z_</td>

<td>An optional library that provides a stdio like API with Unicode support.</td>

</tr>

<tr>

<td>Tool Utility Library</td>

<td>icutu_XY_.dll</td>

<td>libicutu.so._XY_._Z_</td>

<td>An internal library that contains internal APIs that are only used by ICU's tools. If you do not use ICU's tools, you do not need this library.</td>

</tr>

</tbody>

</table>

Normally only the above ICU libraries need to be considered for packaging. The versionless symbolic links to these libraries are only needed for easier development. The _X_, _Y_ and _Z_ parts of the name are the version numbers of ICU. For example, ICU 2.0.2 would have the name libicuuc.so.20.2 for the common library. The exact format of the library names can vary between platforms due to how each platform can handles library versioning.

## [Important Notes About Using ICU](#ImportantNotes)

### [Using ICU in a Multithreaded Environment](#ImportantNotesMultithreaded)

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

#### [Using ICU in a Multithreaded Environment on HP-UX](#ImportantNotesHPUX)

When ICU is built with aCC on HP-UX, the [-AA](http://h21007.www2.hp.com/portal/site/dspp/menuitem.863c3e4cbcdc3f3515b49c108973a801?ciid=eb08b3f1eee02110b3f1eee02110275d6e10RCRD) compiler flag is used. It is required in order to use the latest <iostream> API in a thread safe manner. This compiler flag affects the version of the C++ library being used. Your applications will also need to be compiled with -AA in order to use ICU.

#### [Using ICU in a Multithreaded Environment on Solaris](#ImportantNotesSolaris)

##### Linking on Solaris

In order to avoid synchronization and threading issues, developers are **suggested** to strictly follow the compiling and linking guidelines for multithreaded applications, specified in the following SUn Solaris document available from Oracle. Most notably, pay strict attention to the following statements from Sun:

> To use libthread, specify -lthread before -lc on the ld command line, or last on the cc command line.
> 
> To use libpthread, specify -lpthread before -lc on the ld command line, or last on the cc command line.

Failure to do this may cause spurious lock conflicts, recursive mutex failure, and deadlock.

Source: "_Multithreaded Programming Guide, Compiling and Debugging_", Sun Microsystems, 2002  
[https://docs.oracle.com/cd/E19683-01/806-6867/compile-74765/index.html](https://docs.oracle.com/cd/E19683-01/806-6867/compile-74765/index.html)

Note, a version of that chapter from a 2008 document update covering both Solaris 9 and Solaris 10 is available here:  
[http://docs.oracle.com/cd/E19253-01/816-5137/compile-94179/index.html](http://docs.oracle.com/cd/E19253-01/816-5137/compile-94179/index.html)

### [Windows Platform](#ImportantNotesWindows)

If you are building on the Windows platform, it is important that you understand a few of the following build details.

#### DLL directories and the PATH setting

As delivered, the International Components for Unicode build as several DLLs, which are placed in the "_<ICU>_\bin64" directory. You must add this directory to the PATH environment variable in your system, or any executables you build will not be able to access International Components for Unicode libraries. Alternatively, you can copy the DLL files into a directory already in your PATH, but we do not recommend this. You can wind up with multiple copies of the DLL and wind up using the wrong one.

#### <a name="ImportantNotesWindowsPath" id="ImportantNotesWindowsPath">Changing your PATH</a>

**Windows 2000/XP and above**: Use the System Icon in the Control Panel. Pick the "Advanced" tab. Select the "Environment Variables..." button. Select the variable PATH in the lower box, and select the lower "Edit..." button. In the "Variable Value" box, append the string ";_<ICU>_\bin64" to the end of the path string. If there is nothing there, just type in "_<ICU>_\bin64". Click the Set button, then the OK button.

Note: When packaging a Windows application for distribution and installation on user systems, copies of the ICU DLLs should be included with the application, and installed for exclusive use by the application. This is the only way to insure that your application is running with the same version of ICU, built with exactly the same options, that you developed and tested with. Refer to Microsoft's guidelines on the usage of DLLs, or search for the phrase "DLL hell" on [msdn.microsoft.com](http://msdn.microsoft.com/).

### [UNIX Type Platform](#ImportantNotesUNIX)

If you are building on a UNIX platform, and if you are installing ICU in a non-standard location, you may need to add the location of your ICU libraries to your **LD_LIBRARY_PATH** or **LIBPATH** environment variable (or the equivalent runtime library path environment variable for your system). The ICU libraries may not link or load properly without doing this.

Note that if you do not want to have to set this variable, you may instead use the --enable-rpath option at configuration time. This option will instruct the linker to always look for the libraries where they are installed. You will need to use the appropriate linker options when linking your own applications and libraries against ICU, too. Please refer to your system's linker manual for information about runtime paths. The use of rpath also means that when building a new version of ICU you should not have an older version installed in the same place as the new version's installation directory, as the older libraries will used during the build, instead of the new ones, likely leading to an incorrectly build ICU. This is the proper behavior of rpath.

## [Platform Dependencies](#PlatformDependencies)

### [Porting To A New Platform](#PlatformDependenciesNew)

If you are using ICU's Makefiles to build ICU on a new platform, there are a few places where you will need to add or modify some files. If you need more help, you can always ask the [icu-support mailing list](http://site.icu-project.org/contacts). Once you have finished porting ICU to a new platform, it is recommended that you contribute your changes back to ICU via the icu-support mailing list. This will make it easier for everyone to benefit from your work.

#### Data For a New Platform

For some people, it may not be necessary for completely build ICU. Most of the makefiles and build targets are for tools that are used for building ICU's data, and an application's data (when an application uses ICU resource bundles for its data).

Data files can be built on a different platform when both platforms share the same endianness and the same charset family. This assertion does not include platform dependent DLLs/shared/static libraries. For details see the User Guide [ICU Data](https://unicode-org.github.io/icu/userguide/icudata) chapter.

ICU 3.6 removes the requirement that ICU be completely built in the native operating environment. It adds the icupkg tool which can be run on any platform to turn binary ICU data files from any one of the three formats into any one of the other data formats. This allows a application to use ICU data built anywhere to be used for any other target platform.

**WARNING!** Building ICU without running the tests is not recommended. The tests verify that ICU is safe to use. It is recommended that you try to completely port and test ICU before using the libraries for your own application.

#### Adapting Makefiles For a New Platform

Try to follow the build steps from the [UNIX](#HowToBuildUNIX) build instructions. If the configure script fails, then you will need to modify some files. Here are the usual steps for porting to a new platform:  

1.  Create an mh file in icu/source/config/. You can use mh-linux or a similar mh file as your base configuration.
2.  Modify icu/source/aclocal.m4 to recognize your platform's mh file.
3.  Modify icu/source/configure.in to properly set your **platform** C Macro define.
4.  Run [autoconf](http://www.gnu.org/software/autoconf/) in icu/source/ without any options. The autoconf tool is standard on most Linux systems.
5.  If you have any optimization options that you want to normally use, you can modify icu/source/runConfigureICU to specify those options for your platform.
6.  Build and test ICU on your platform. It is very important that you run the tests. If you don't run the tests, there is no guarentee that you have properly ported ICU.

### [Platform Dependent Implementations](#PlatformDependenciesImpl)

The platform dependencies have been mostly isolated into the following files in the common library. This information can be useful if you are porting ICU to a new platform.

*   **unicode/platform.h.in** (autoconf'ed platforms)  
    **unicode/p_XXXX_.h** (others: pwin32.h, ppalmos.h, ..): Platform-dependent typedefs and defines:  

    *   Generic types like UBool, int8_t, int16_t, int32_t, int64_t, uint64_t etc.
    *   U_EXPORT and U_IMPORT for specifying dynamic library import and export
    *   String handling support for the char16_t and wchar_t types.  

*   **unicode/putil.h, putil.c**: platform-dependent implementations of various functions that are platform dependent:  

    *   uprv_isNaN, uprv_isInfinite, uprv_getNaN and uprv_getInfinity for handling special floating point values.
    *   uprv_tzset, uprv_timezone, uprv_tzname and time for getting platform specific time and time zone information.
    *   u_getDataDirectory for getting the default data directory.
    *   uprv_getDefaultLocaleID for getting the default locale setting.
    *   uprv_getDefaultCodepage for getting the default codepage encoding.  

*   **umutex.h, umutex.c**: Code for doing synchronization in multithreaded applications. If you wish to use International Components for Unicode in a multithreaded application, you must provide a synchronization primitive that the classes can use to protect their global data against simultaneous modifications. We already supply working implementations for many platforms that ICU builds on.  

*   **umapfile.h, umapfile.c**: functions for mapping or otherwise reading or loading files into memory. All access by ICU to data from files makes use of these functions.  

*   Using platform specific #ifdef macros are highly discouraged outside of the scope of these files. When the source code gets updated in the future, these #ifdef's can cause testing problems for your platform.

* * *

Copyright © 2016 and later: Unicode, Inc. and others. License & terms of use: [http://www.unicode.org/copyright.html](http://www.unicode.org/copyright.html)  
Copyright © 1997-2016 International Business Machines Corporation and others. All Rights Reserved.
