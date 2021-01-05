---
layout: default
title: ICU FAQ
nav_order: 6
parent: Misc
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU FAQs
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Introduction to ICU

#### What is ICU?

ICU is a cross-platform Unicode based globalization library. It includes support
for locale-sensitive string comparison, date/time/number/currency/message
formatting, text boundary detection, character set conversion and so on.

#### Where can I get ICU?

You can get ICU4C and ICU4J from <http://www.icu-project.org/download/>

**Why don't you build binaries for my platform?**

There are many versions of compilers on so many platforms that we cannot build
them all and guarantee compatibility between them all even on the same platform.
Due to these restrictions, we only distribute a limited number of binary
versions of ICU, but we will assist in building other versions from source.

**Why don't you provide project files for my MSVC version (MSVC 2008, etc)?**

You can use the Cygwin build environment to build ICU from source against the
MSVC compiler. See the ICU4C Readme.

#### How do I install the binary versions of ICU?

*   **Windows**:
    *   The DLLs you may need for your application are located in
        **bin\\icuXX##.dll**, where "XX" are two letters (such as "uc" for the
        "common" library, "in" for the "i18n" library, etc.) and ## is the major
        and the minor version number (such as **42** for **4.2** / **4.2**.0.1
        or **4.2**.4 ).
    *   Either place the DLLs in the same directory as your application's .EXE
        files, or set the PATH variable to point to the directory containing the
        ICU DLLs.
    *   For compiling applications, add the "include" direcotry (the parent of
        the "unicode" and "layout" directories) to the include search path.
    *   For linking applications, add the "lib" directory to the appropriate
        path.
*   **Other Platforms**:
    *   For other platforms, the .tgz file unpacks to a "/usr/local" type
        hierarchy. For system-wide installation, you can unpack all of the files
        into /usr/local/bin, /usr/local/include, etc.
    *   The configuration script **/usr/local/bin/icu-config** or the similar
        Makefile include fragment **/usr/local/lib/icu/current/Makefile.inc**
        can be used in building applications.

#### Can you help me build ICU4C for ...

We can try ... make sure you read the latest "readme" and also the [ICU
Data](../icudata.md) section. You might also [searching the icu-support
archives](http://site.icu-project.org/contacts), and then posting a question
there. Additionally, sites such as
[StackOverflow](http://stackoverflow.com/search?q=icu) may have helpful tips for
your topic.

*   **Android NDK**
    *   Please try [searching the icu-support
        archives](http://site.icu-project.org/contacts) and also see
        [StackOverflow](http://stackoverflow.com/search?q=icu+android).
*   **iPhone**
    *   Please try [searching the icu-support
        archives](http://site.icu-project.org/contacts) and also see
        [StackOverflow](http://stackoverflow.com/search?q=icu+iphone).

#### What is the ICU binary compatibility policy?

Please see the section on
[binary compatibility](../design#icu-binary-compatibility)
in the [design chapter](../design.md).

#### How is ICU licensed?

The ICU license is intended to allow ICU to be included both in free software
projects and in proprietary or commercial products.

Since ICU 58, ICU is covered by the
[Unicode license](http://www.unicode.org/copyright.html) which is very similar to
the previous ICU license.

ICU 1.8.1–ICU 57 and ICU4J 1.3.1–ICU4J 57 are covered by the [ICU
license](https://github.com/unicode-org/icu/blob/release-57-1/icu4c/LICENSE),
a simple, permissive non-copyleft free software license, compatible with the GNU
GPL. The ICU license is identical to the version of the X license that was
formerly available at <http://www.x.org/Downloads_terms.html> . (This site no
longer exists, but can still be retrieved through internet archive services.)

#### Can I use ICU from other languages besides C/C++ and Java?

There are a number of wrappers available, please see the
[Related Projects](http://site.icu-project.org/related) page.

#### How do I upgrade to a new version of ICU? Should I be concerned about API changes, a new Unicode version or a new CLDR version)?

Our goal is for ICU upgrades to go smoothly. Here are some steps you can take to
prepare for an upgrade, or to make sure that your usage of ICU is
upgrade-friendly.

*   **API:** ensure that you are not using draft APIs which may have changed in
    a future release. See the section on
    [API compatibility](../design#icu-api-compatibility) in the
    [design chapter](../design.md).
*   **Unicode:** See the release notes for particular versions of Unicode to
    ensure that your code is not affected by property changes or other
    specification changes.
*   **CLDR:** If your application has test cases which depend on specific
    translations, these assumptions may become invalid if the translation of an
    item changes, new support is added, or if a country changes its currency.
    Try not to depend on specific translations, or be prepared to change test
    cases. Also, a newer version may support additional translations,
    currencies, types of calendars
*   **Building/Deploying your Application (ICU4C):** ICU4C usually builds with
    symbol renaming (See:
    [binary compatibility](../design#icu-binary-compatibility)
    in the [design chapter](../design.md)). Be sure that you build your
    application with the updated ICU header files, so that it will link against
    the current ICU. Also, don't hard-code the names of ICU libraries in your
    build scripts and projects. Where possible, link against just the
    'base name' such as `libicuuc.so` or `icuuc.lib` rather than a name
    containing the version number such as `libicuuc.so.**46**` or
    `icuuc**46**.dll`.

## Building and Testing ICU

#### How do I build ICU?

See the readme.html that is included with ICU.

#### How do I get 32- or 64-bit versions of the ICU libraries?

From ICU version 4.2 on, the configure script will build with the default bit
width of your platform. You can request 64 or 32 bits with the
**--with-library-bits=** option, (e.g. `runConfigureICU Linux
**--with-library-bits=64**` or `runConfigureICU MacOSX
**--with-library-bits=32**`).
(For the behavior of attempting 64 bits if possible, use
**--with-library-bits=64else32**).

#### How do I build an optimized, non debug ICU?

On Win32, choose the 'Release' configuration from the drop down menu. On other
platforms, use the runConfigureICU script, which uses the configure script. The
runConfigureICU script uses the safest level of optimization for the ICU
libraries. If your platform is not specified, set the following environment
variables before running configure or runConfigureICU: **CFLAGS=-O CXXFLAGS=-O**

#### Why am I getting so many test failures when I use "gmake check"?

Please view the readme that is included with ICU. It has all the details on how
to build and test ICU, and it usually answers most problems.

If you are using a compiler that hasn't been tested with ICU before, you may
have encountered an optimization bug with the compiler. On Unix platforms you
can specify **--disable-release** when you are using runConfigureICU (e.g.
`runConfigureICU --disable-release LinuxRedHat`). If this fixes your problem, it
is recommended that you report the optimization bug to the compiler
manufacturer.

If neither of these fix your problem, please send an e-mail to the [ICU4C
Support List](http://icu-project.org/contacts.html) .

#### How can I reduce the size of the ICU data library?

Use the [Data Customizer](https://unicode-org.atlassian.net/browse/ICU-12835)
or see
[Customizing ICU's Data Library](../icudata#customizing-icus-data-library)
in the [ICU Data Management](../icudata.md) chapter of this User's Guide.

#### Why am I seeing a small ( only a few K ) instead of a large ( several megabytes ) data shared library (icudt)?
#### Opening ICU services fails with U_MISSING_RESOURCE_ERROR and u_init() returns failure.

ICU libraries always must link with the ICU data library. However, so that ICU
can bootstrap itself, it first builds a 'stub' data library, in
**icu\\source\\stubdata**, so that the tools can function. You should only use
this in production if you are NOT using DLL-mode data access, in which case you
are accessing ICU data as individual files, as an archive (.dat) file, or some
other means. Normally, you should be using the larger library built from
**icu\\source\\data**. If you see this issue after ICU has completed building,
re-run 'make' in **icu\\source\\data**, or the '**makedata**' project in Visual
Studio.

#### Can I add or remove a converter from ICU?

Yes. Please see [Customizing ICU's Data Library](../icudata#customizing-icus-data-library)
in the [ICU Data Management](../icudata.md) of this User's Guide. You can also
get extra converters from <http://www.icu-project.org/charts/charset/> or use
the [ICU Data Customizer](https://unicode-org.atlassian.net/browse/ICU-12835)
tool.

#### Why don't the makefiles work?

You need GNU's make program version 3.8 or later, and you need to run the
runConfigureICU script, which is located in the `icu/source directory`. You may
be using a platform that ICU does not support. If the first two answers do not
apply to you, then you should send an e-mail to the
[ICU4C Support List](http://www.icu-project.org/contacts.html).

Here are some places you can find gmake:

1.  GNU: <http://www.gnu.org/software/make/>

2.  Sun® Source/Binaries: <http://www.sunfreeware.com>

3.  z/OS (OS/390) Source/Binaries:
    <http://www.ibm.com/servers/eserver/zseries/zos/unix/bpxa1ty1.html#opensrc>

4.  IBM i (OS/400) Source/Binaries:
    <http://www.ibm.com/servers/enable/site/porting/iseries/overview/gnu_utilities.html>

Due to differences in every platform's make program, we will not support other
versions of our make files.

#### What version of the C iostream is used in ICU4C?

ICU4C uses the latest available version of the iostream on the target platform.
Only the `io` library uses iostream.

#### I only want to use the C APIs, do I need a C++ compiler?

Large portions of ICU4C were always implemented in C++, and over time we are
moving more into that direction. We continue to support and add C APIs, in order
to provide binary-compatible APIs. For the implementation, C++ is much better:
It is generally easier to work with, which reduces bugs and maintenance. It is
closer to Java, which is important for porting between ICU4C and ICU4J. We use
[RAII](http://en.wikipedia.org/wiki/Resource_Acquisition_Is_Initialization)
(e.g., LocalPointer) to reduce opportunities for memory leaks, we use inline
functions and type-safe constants instead of #define, etc. However, we do not
use exceptions, and we do not use the Standard Template Library (STL), so
ICU4C's dependencies on the C++ library are minimal. See the new
[dependencies.txt](https://github.com/unicode-org/icu/blob/master/icu4c/source/test/depstest/dependencies.txt)
and search for "group: cplusplus".

As ICU does not use exceptions, the GCC option `-fno-exceptions` will reduce or
remove the dependencies on the standard C++ library. In
[GCC](http://gcc.gnu.org) 4.5 there is an option `-static-libstdc++` which will
remove C++ library dependencies. Visual Studio has the
[/MT option](http://msdn.microsoft.com/en-us/library/2kzt1wy3(v=VS.100).aspx),
and other compilers may have similar options. See the
[How To Use ICU](../howtouseicu.md) page for related information on this topic.

## Features of ICU

#### What computer languages does ICU support?

ICU4C (ICU) is written in C and C++, and ICU4J is written in Java™.

#### How are the APIs documented for deprecation?

Please read the [ICU API compatibility](../design#icu-api-compatibility)
section in the [ICU Design](../design.md) chapter.

#### What version of Unicode standard does ICU support?

ICU versions 65 supports Unicode version 12.

The Unicode versions for older versions of ICU are listed on the ICU download
page, <http://www.icu-project.org/download/>

#### Does ICU support UTF-16 surrogates and Unicode supplementary characters?

Yes.

#### Does Java support UTF-16 surrogates and Unicode supplementary characters?

Java 5 introduced support for Unicode supplementary characters. Java 1.4 and
earlier do not directly support them.

#### How does ICU relate to Java's java.text.\* package?

The International Components for Unicode are available both as a C/C++ library
and a Java class library. ICU provides internationalization utilities for
writing global applications in C, C++ or Java programming languages. ICU was
originally developed by the Unicode group at the IBM Globalization Center of
Competency in Cupertino, and ICU was contributed to Sun for inclusion into the
JDK 1.1. ICU4J includes enhanced versions of some of these contributed classes
plus additional classes that complement the classes in the JDK.

ICU4C started as a C++ port of the original Java Internationalization classes.
These classes are now partially implemented in C, with largely parallel C and
C++ APIs. ICU4C and ICU4J continue to leapfrog each other with features and bug
fixes. Over time, features from ICU4J get added to the JDK as well.

Both versions of ICU have a goal to implement the latest Unicode standard,
maintain a single portable source code base, and to make it easier for software
developers to create global applications.

## Using ICU

#### Can I use any of the features of ICU without Unicode strings?

No. In order to use the collation, text boundary analysis, formatting or other
ICU APIs, you must use Unicode strings. In order to get Unicode strings from
your native codepage, you can use the conversion API.

#### How do I declare a Unicode string in ICU?

Use the `U_STRING_DECL` and `U_STRING_INIT` macros or use the UnicodeString
class for C++. Strings are represented as `UChar \*` as the base string type.

Even though most platforms declare wide strings as `wchar_t \*` or `L""` as the
base string type, that declaration is not portable because the `sizeof(wchar_t)`
can be 1, 2 or 4, and the encoding may not even be Unicode. On the platforms
where `sizeof(wchar_t)` is 2 bytes, `UChar` is defined as `wchar_t`. In that
case you can  use ICU's strings with 3rd party legacy functions; however, we do
not suggest using Unicode strings without the `U_STRING_DECL` and
`U_STRING_INIT` macros or UnicodeString class because they are platform
independent implementations.

#### How is a Unicode string represented in ICU4C?

A Unicode string is currently represented as UTF-16. The endianess of UTF-16 is
platform dependent. You can guarantee the endianess of UTF-16 by using a
converter. UTF-16 strings can be converted to other Unicode forms by using a
converter or with the UTF conversion macros.

ICU does not use UCS-2. UCS-2 is a subset of UTF-16. UCS-2 does not support
surrogates, and UTF-16 does support surrogates. This means that UCS-2 only
supports UTF-16's Base Multilingual Plane (BMP). The notion of UCS-2 is
deprecated and dead. Unicode 2.0 in 1996 changed its default encoding to UTF-16.

If you need to do a quick and easy conversion between UTF-16 and UTF-8, UTF-32
or an encoding in `wchar_t`, you should take a look at unicode/ustring.h. In
that header file you will find `u_strToWCS`, `u_strFromWCS`, `u_strToUTF8`,
`u_strFromUTF8`, `u_strToUTF32` and `u_strFromUTF32` functions. These
functions are provided for your convenience instead of using the `ucnv_\*` API.

You can also take a look at the `UTF_\*`, `UTF8_\*`, `UTF16_\*` and `UTF32_\*`
macros, which are defined in
[unicode/utf.h](https://github.com/unicode-org/icu/blob/master/icu4c/source/common/unicode/utf.h),
[unicode/utf8.h](https://github.com/unicode-org/icu/blob/master/icu4c/source/common/unicode/utf8.h),
[unicode/utf16.h](https://github.com/unicode-org/icu/blob/master/icu4c/source/common/unicode/utf16.h)
and [unicode/utf32.h](https://github.com/unicode-org/icu/blob/master/icu4c/source/common/unicode/utf32.h).
These macros are helpful for programmers that need to manipulate and process
Unicode strings.

#### How do I index into a UTF-16 string?

Typically, indexes and offsets in strings count string units, not characters
(although in C and Java they have a char type).

For example, in old-fashioned MBCS strings, you would count indexes and offsets
by bytes, not by the variable-width character count. In UTF-16, you do the same,
just count 16-bit units (in ICU: UChar).

#### What is the performance difference between UTF-8 and UTF-16?

Most of the time, the memory throughput of the hard drive and RAM is the main
performance constraint. UTF-8 is 50% smaller than UTF-16 for US-ASCII, but UTF-8
is 50% larger than UTF-16 for East and South Asian scripts. There is no memory
difference for Latin extensions, Greek, Cyrillic, Hebrew, and Arabic.

For processing Unicode data, UTF-16 is much easier to handle. You get a choice
between either one or two units per character, not a choice among four lengths.
UTF-16 also does not have illegal 16-bit unit values, while you might want to
check for illegal bytes in UTF-8. Incomplete character sequences in UTF-16 are
less important and more benign. If you want to quickly convert small strings
between the different UTF encodings or get a UChar32 value, you can use the
macros provided in `utf.h` and its siblings `utf8.h` and `utf16.h`. For larger
or partial strings, please use the conversion API.

#### How do the converters work?

The converters act like a data stream. This means that the state of the last
character is saved in the converter after each call to the `ucnv_fromUnicode()`
and `ucnv_toUnicode()` functions. So if the source buffer ends with part of a
surrogate Unicode character pair, the next call to `ucnv_toUnicode()` will
write out the equivalent character to the destination buffer. Please see the
[Conversion](../conversion/index.md) chapter of the User's Guide for details.

#### What does a locale look like in ICU?

ICU locales are lightweight, and they are represented by just a string.
Lightweight means that there is just a string to represent a locale and nothing
more. Many platforms have numbers and other data structures to represent a
locale, but ICU has one simple platform independent string to represent a
locale.

ICU locales usually contain an ISO-639 language name (2-3 characters), an
ISO-3166 country name (2-3 characters), and a variant name which is user
specified. When a language or country is not represented by these standards, ICU
uses 3 characters to represent that part of the locale. All three parts are
separated by an underscore "_". For example, US English is "en_US", and German
in Germany with the Euro symbol is represented as "de_DE_EURO". Traditionally
the language part of the locale is lowercase, the country is uppercase and the
variant is uppercase. More details are available from the [Locale
Chapter](../locale/index.md) of this User's Guide.

#### How is ICU versioned?

Please read the [ICU Design](../design.md) chapter of the User's Guide.

#### What is the relationship between ICU locale data and system locale data?

There is no relationship. ICU is not dependent on the operating system for the
locale data.

This also means that `uloc_setDefault()` does not affect the operating system.
The function `uloc_setDefault()` only sets ICU's default locale. Normally the
default locale for ICU is whatever the operating system says is the default
locale.

#### How are errors handled in ICU?

Since not all compilers can handle exceptions, we return an error from functions
with a `UErrorCode` parameter. The `UErrorCode` parameter of a function will
return any errors that occurred while it was executing. It's usually a good idea
to check for errors after calling a function by using the `U_SUCCESS` and
`U_FAILURE` macros. `U_SUCCESS` returns true when the function did run properly,
and `U_FAILURE` returns true when the function did NOT run properly. You may
handle specific errors from a function by checking the exact value of error. The
possible values of `UErrorCode` are located in
[utypes.h](https://github.com/unicode-org/icu/blob/master/icu4c/source/common/unicode/utypes.h)
of the common project. Before any function is called with a `UErrorCode`, it
must be initialized to `U_ZERO_ERROR`.

Here is an example of `UErrorCode` being used.

```c++
UErrorCode err = U_ZERO_ERROR;
callMyFunction(&err);
if (U_FAILURE(err)) {
puts("callMyFunction() Failed!");
}
```

Please see the [ICU Design](../design.md) chapter for details.

#### With calendar classes, why are months 0-based?

"I have been using ICU for its calendar classes, and have found it to be
excellent. That said, I am wondering why the decision was made to keep months
0-based while almost all the other calendrical units (years, weeks of year,
weeks of month, date, days of year, days of week, days of week in month) are
1-based? This has been the source of several bugs whenever the mind is slightly
less than razor sharp." --Contributor

This was not our choice. We inherited it from the Java Calendar API,
unfortunately.

#### Is there a guideline for COBOL programs that want to use ICU?

There is a COBOL/ICU guideline available since ICU 2.2. For more details, please
refer to the [COBOL section](../usefrom/cobol.md) of this User's Guide.

#### Where can I get more information about using ICU?

Please send an e-mail to the [ICU4C Support
List](http://www.icu-project.org/contacts.html) .
