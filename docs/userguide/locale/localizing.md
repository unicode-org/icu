---
layout: default
title: Localizing with ICU
nav_order: 3
parent: Locales and Resources
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Localizing with ICU
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

There are many different formats for software localization, i.e., for resource
bundles. The most important file format feature for translation of text elements
is to represent key-value pairs where the values are strings.

Each format was designed for a certain purpose. Many but not all formats are
recognized by translation tools. For localization it is best to use a source
format that is optimized for translation, and to convert from it to the
platform-specific formats at build time.

This overview concentrates on the formats that are relevant for working with
ICU. The examples below show only lists of strings, which is the lowest common
denominator for resource bundles.

## Recommendation

The most promising long-term approach is to author localizable data in XLIFF
format (see the [XLIFF](#xliff) (§) section below) and to convert it to native,
platform/tool-specific formats at build time.

Short-term, due to the lack of ICU tools for XLIFF, either custom tools must be
used to convert from some authoring/translation format to Java/ICU formats, or
one of the Java/ICU formats needs to be used for authoring and translation.

## Java and ICU4J

### .properties files

Java `PropertyResourceBundle` uses runtime-parsed .properties files. They contain
key-value pairs where both keys and values are Unicode strings. No other native
data types (e.g., integers or binaries) are supported. There is no way to
specify a charset, therefore .properties files must be in ISO 8859-1 with \u
escape sequences (see the Java `native2ascii` tool).

Defined at: http://java.sun.com/j2se/1.4/docs/api/java/util/PropertyResourceBundle.html

Example: (`example_de.properties`)

```properties
key1=Deutsche Sprache schwere Sprache
key2=Düsseldorf
```

### .java ListResourceBundle files

Java `ListResourceBundle` files provide implementation subclasses of the
`ListResourceBundle` abstract base class. **They are Java code!** Source files are
.java files that are compiled as usual with the javac compiler. Syntactic rules
of Java apply. As Java source code, they can contain arbitrary Java objects and
can be nested.

Although the Java compiler allows to specify a charset on the command line, this
is uncommon, and .java resource bundle files are therefore usually encoded in
ISO 8859-1 with \u escapes like .properties files.

Defined at: http://java.sun.com/j2se/1.4/docs/api/java/util/ListResourceBundle.html

Example: (`example_de.java`)

```java
public class example_de extends ListResourceBundle {
    public Object[][] getContents() {
        return contents;
    }
    static final Object[][] contents={
        { "key1", "Deutsche Sprache " +
            "schwere Sprache" },
        { "key2", "Düsseldorf" }
    };
}
```

ICU4J can also access the ICU4C resource bundles described in the next section,
using the API described in the [UResourceBundle](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/util/UResourceBundle.html) documentation.

## ICU4C

### .txt resource bundles

ICU4C natively uses a plain text source format with a nested structure that was
derived from Java `ListResourceBundle` .java files when the original ICU Java
class files were ported to C++. The ICU4C bundle format can of course contain
only data, not code, unlike .java files. Resource bundle source files are
compiled with the `genrb` tool into a binary runtime form (`.res` files) that is
portable among platforms with the same charset family (ASCII vs. EBCDIC) and
endianness.

Features:

1. Key-value pairs. Keys are strings of "invariant characters" - a portable subset of the ASCII graphic character repertoire. About "invariant characters" see the definition of the .txt file format (URL below) or [icu/source/common/unicode/utypes.h](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/utypes_8h.html)

2. Values can be Unicode strings, integers, binaries (BLOBs), integer array (vectors), and nested structures. Nested structures are either arrays (position-indexed vectors) of values or "tables" of key-value pairs.

3. Values inside nested structures can be all of the ones as on the top level, arbitrarily deeply nested via arrays and tables.

4. Long strings can be split across lines: Adjacent strings separated only by whitespace including line breaks) are automatically concatenated at build time.

5. At runtime, when a top-level item is not found, then ICU looks up the same key in the parent bundle as determined by the locale ID.

6. A value can also be an "alias", which is simply a reference to another bundle's item. This is to save space by storing large data pieces only once when they cannot be inherited along the locale ID hierarchy (e.g., collation data in ICU shared among zh_HK and zh_TW).

7. Source files can be in any charset. Unicode signature byte sequences are recognized automatically (UTF-8/16, SCSU, ...), otherwise the tool takes a charset name on the command line.

Defined at: [icu-docs/master/design/bnf_rb.txt](https://raw.githubusercontent.com/unicode-org/icu-docs/master/design/bnf_rb.txt)

To use with ICU4C, see the [Resource Bundle APIs](resources#resource-bundle-apis) section of this userguide.

Example: (`de.txt`)

```
de {
    key1 { "Deutsche Sprache "
            "schwere Sprache" }
    key2 { "Düsseldorf" }
}
```

### ICU4C XML resource bundles

The ICU4C XML resource bundle format was defined simply to express the same
capabilities of the .txt and binary ICU4C resource bundles in XML form. However,
we have decided to drop the format for lack of use and instead adopt standard
XLIFF format for localization. For more information on XLIFF format, see the
following section. For examples on using ICU tools to produce and read XLIFF
format see the XLIFF Usage section in the [resource management chapter](resources#using-xliff-for-localization).

## XLIFF

The XML Localization Interchange File Format (XLIFF) is an emerging industry
standard "for the interchange of localization information". Version 1.1 is
available (2003-Oct-31), and 1.2 is almost complete (2007-Jan-20).

This is the result of a quick review of XLIFF and may need to be improved.

Features:

1.  Multiple resource bundles per XLIFF file are supported.

2.  Multiple languages per XLIFF file are supported.

3.  XLIFF provides a rich set of ways to communicate intent, types of items,
    etc. all the way from content creation to all stages and phases of
    translation.

4.  Nesting of values appears to not be supported.

5.  XLIFF is independent of actual build-time or runtime resource bundle
    formats. .xlf files must be converted to native formats at build time.

Defined at: http://www.oasis-open.org/committees/xliff/

Example: (`example.xlf`)

```xml
<<?xml version="1.0" encoding="utf-8"?>
<xliff version = "1.1" xmlns='urn:oasis:names:tc:xliff:document:1.1'
xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
xsi:schemaLocation='urn:oasis:names:tc:xliff:document:1.1
http://www.oasis-open.org/committees/xliff/documents/xliff-core-1.1.xsd'>
    <file xml:space = "preserve" source-language = "en" target-language = "sh"
    datatype = "x-icu-resource-bundle" original = "root.txt"
    date = "2007-08-17T21:17:08Z">
        <header>
            <tool tool-id = "genrb-3.3-icu-3.8" tool-name = "genrb"/>
        </header>
        <body>
            <group id = "root" restype = "x-icu-table">
                <trans-unit id = "optionMessage" resname = "optionMessage">
                    <source>unrecognized command line option:</source>
                    <target>nepoznata opcija na komandnoj liniji:</target>
                </trans-unit>
                <trans-unit id = "usage" resname = "usage">
                    <source>usage: ufortune [-v] [-l locale]</source>
                    <target>upotreba: ufortune [-v] [-l lokal]</target>
                </trans-unit>
            </group>
        </body>
    </file>
</xliff>
```

For examples on using ICU tools to produce and read XLIFF format see the XLIFF
Usage (§) section in the [resource management chapter](resources#using-xliff-for-localization).

## DITA

The Darwin Information Typing Architecture (DITA) is "IBM's XML architecture for
topic-oriented information". It is a family of XML formats for several types of
publications including manuals and resource bundles. It is extensible. For
example, subformats can be defined by refining DTDs. One design feature is to
provide cross-document references for reuse of existing contents. For more
information see http://www.ibm.com/developerworks/xml/library/x-dita4/index.html

While it is certainly possible to define resource bundle formats via DTDs in the
DITA framework, there currently (2002-Nov-27) do not appear to be resource
bundle formats actually defined, or tools available specifically for them.

## Linux/gettext

The OpenI18N specification requires support for message handling functions
(mostly variants of `gettext()`) as defined in `libintl.h`. See Tables 3-5 and 3-6
and Annex C in http://www.openi18n.org/docs/html/LI18NUX-2000-amd4.htm

Resource bundles ("portable object files", extension .po) are plain text files
with key-value pairs for string values. The format and functions support a
simple selection of plural forms by associating integer values (via C language
expressions) with indexes of strings.

The `msgfmt` utility compiles .po files into "message object files" (extension
.mo). The charset is determined from the locale ID in `LC_CTYPE`. There are
additional supporting tools for .po files.

*Note: The OpenI18N specification also requires POSIX `gencat`/`catgets` support. See the [POSIX](#posixcatsgets) (§) section below.*

Defined at: Annex C of the Li18nux-2000 specification, see above.

Example: (`example.po`)

```
domain "example_domain"
msgid "key1"
msgstr "Deutsche Sprache schwere Sprache"
msgid "key2"
msgstr "Düsseldorf"
```

## POSIX/catgets

POSIX (The Open Group specification) defines message catalogs with the `catgets()`
C function and the gencat build-time tool. Message catalogs contain key-value
pairs where the keys are integers `1`..`NL_MSGMAX` (see `limits.h`), and the values
are strings. Strings can span multiple lines. The charset is determined from the
locale ID in `LC_CTYPE`.

Defined at:
https://pubs.opengroup.org/onlinepubs/009695399/utilities/gencat.html and
https://pubs.opengroup.org/onlinepubs/009695399/functions/catgets.html

Example: (`example.txt`)

```
1 Deutsche Sprache \
schwere Sprache
2 Düsseldorf
```

## Windows

Windows uses a number of file formats depending on the language environment --
MSVC 6, Visual Basic, or Visual Studio .NET. The most well-known source formats
are the [.rc Resource](https://docs.microsoft.com/windows/win32/menurc/about-resource-files)
and [.mc Message](https://docs.microsoft.com/en-us/windows/win32/eventlog/message-files)
file formats. They both get compiled into .res files that are linked into
special sections of executables. Source formats can be UTF-16, while compiled
strings are (almost) always UTF-16 from .rc files (except for predefined
ComboBox strings) and can optionally be UTF-16 from .mc files.

.rc files carry key-value pairs where the keys are usually numeric but can be
strings. Values can be strings, string tables, or one of many Windows
GUI-specific structured types that compile directly into binary formats that the
GUI system interprets at runtime. .rc files can include C #include files for
#defined numeric keys. .mc files contain string values preceded by per-message
headers similar to the Linux/gettext() format. There is a special format of
messages with positional arguments, with printf-style formatting per argument.
In both .rc and .mc formats, Windows LCID values are defined to be set on the
compiled resources.

Developers and translators usually overlook the fact that binary resources are
included, and include them into each translation. This despite Windows, like
Java and ICU, using locale ID fallback at runtime.

.rc and .mc files are tightly integrated with Microsoft C/C++, Visual Studio and
the Windows platform, but are not used on any other platforms.

A [sample Windows .rc file](#sample-windows-rc-file) (§) is at the end of this document.

## ICU tools

ICU 2.4 provides tools for conversion between resource bundle formats:

1.  ICU4C .txt -> ICU4C .res: Default operation of genrb (ICU 2.0 and before).

2.  ICU4C .txt -> ICU4C .xml: Option with genrb (ICU 2.4).

3.  ICU4C .txt -> Java ListResourceBundle .java format: Option with genrb (ICU
    2.2).
    Generates subclasses of ICUListResourceBundle to support non-string types.

4.  Java ListResourceBundle .java format -> ICU4C .txt: Use ICU4J 2.4's
    src/com/ibm/icu/dev/tools/localeconverter

5.  ICU4C .xml -> ICU4C .txt: There is a tool for this conversion, but it is not
    fully tested or documented. Please see the
    [XLIFF2ICUConverter](https://icu-project.org/download/xliff2icuconverter.html)
    tool.

There are currently no ICU tools for XLIFF.

### Converting de.txt to a ListResourceBundle

The following genrb invocation generates a ListResourceBundle from `de.txt` (see
the example file `de.txt` above):

`genrb -j -b TestName -p com.example de.txt`

The -j option causes .java output, -b is an arbitrary bundle name prefix, and -p
is an arbitrary package name. "Arbitrary" means "depends on your product" and
may be truly arbitrary if the generated .java files are not actually used in a
Java application. genrb auto-detects .txt files encoded in Unicode charsets like
UTF-8 or UTF-16 if they have a signature byte sequence ("BOM"). The .java output
file is in native2ascii format, i.e., it is encoded in US-ASCII with \u
escapes.

The output of the above genrb invocation is `TestName_de.java`:

```java
package com.example;
import java.util.ListResourceBundle;
import com.ibm.icu.impl.ICUListResourceBundle;
public class TestName_de extends ICUListResourceBundle {
    public TestName_de () {
        super.contents = data;
    }
    static final Object[][] data = new Object[][] {
        {
            "key1",
            "Deutsche Sprache schwere Sprache",
        },
        {
            "key2",
            "D\u00FCsseldorf",
        },
    };
}
```

### Converting a ListResourceBundle back to .txt

An ICUListResourceBundle .java file as generated in the previous example can be
converted to an ICU4C .txt file with the following steps:

1.  Compile the .java file, e.g. with `javac -d . TestName_de.java`. ICU4J needs
    to be on the classpath (or use the -classpath option). If the .java file is
    not in `native2ascii` format, then use the -encoding option (e.g. -encoding
    UTF-8). The -d option (specifying an output directory, in this example the
    current folder) is required. Without it, the Java compiler would not
    generate the com/example folder hierarchy that is required in the next step.

2.  You now have a .class file `com/example/TestName_de.class`.

3.  Invoke the ICU4J locale converter tool to generate ICU4C .txt format output for
    this .class file:
    
    `java -cp ;(folder to ICU4J)/icu4j.jar;(working folder for the previous steps); com.ibm.icu.dev.tool.localeconverter.ConvertICUListResourceBundle -icu -package com.example -bundle-name TestName de > de.txt`
    
    Note that the classpath must include the working folder for the previous
    steps (the folder that contains "com"). The package name (com.example),
    bundle name (TestName) and locale ID (de) must match the .java/.class files.
    Note also that the locale converter writes to the standard output; the
    command line above includes a redirection to de.txt.

The last step generates a new de.txt in `native2ascii` format:

```
de {
    key2{"D\u00FCsseldorf"}
    key1{"Deutsche Sprache schwere Sprache"}
}
```

## Further information

1.  TMX: "The purpose of TMX is to allow easier exchange of translation memory
    data between tools and/or translation vendors with little or no loss of
    critical data during the process."
    http://www.lisa.org/tmx/

2.  LISA: Localisation Industry Standards Association
    http://www.lisa.org/

## Sample Windows .rc file

This file (`winrc.rc`) was generated with MSVC 6, using the New Project wizard to
generate a simple "Hello World!" application, changing the LCIDs to German, then
adding the two example strings as above.

```
//Microsoft Developer Studio generated resource script.
//
#include "resource.h"
#define APSTUDIO_READONLY_SYMBOLS
/////////////////////////////////////////////////////////////////////////////
//
// Generated from the TEXTINCLUDE 2 resource.
//
#define APSTUDIO_HIDDEN_SYMBOLS
#include "windows.h"
#undef APSTUDIO_HIDDEN_SYMBOLS
#include "resource.h"
/////////////////////////////////////////////////////////////////////////////
#undef APSTUDIO_READONLY_SYMBOLS
/////////////////////////////////////////////////////////////////////////////
// German (Germany) resources
#if !defined(AFX_RESOURCE_DLL) || defined(AFX_TARG_DEU)
#ifdef _WIN32
LANGUAGE LANG_GERMAN, SUBLANG_GERMAN
#pragma code_page(1252)
#endif //_WIN32
/////////////////////////////////////////////////////////////////////////////
//
// Icon
//
// Icon with lowest ID value placed first to ensure application icon
// remains consistent on all systems.
IDI_WINRC ICON DISCARDABLE "winrc.ICO"
IDI_SMALL ICON DISCARDABLE "SMALL.ICO"
/////////////////////////////////////////////////////////////////////////////
//
// Menu
//
IDC_WINRC MENU DISCARDABLE
BEGIN
    POPUP "&File"
    BEGIN
        MENUITEM "E&xit", IDM_EXIT
    END
    POPUP "&Help"
    BEGIN
        MENUITEM "&About ...", IDM_ABOUT
    END
END
/////////////////////////////////////////////////////////////////////////////
//
// Accelerator
//
IDC_WINRC ACCELERATORS MOVEABLE PURE
BEGIN
    "?", IDM_ABOUT, ASCII, ALT
    "/", IDM_ABOUT, ASCII, ALT
END
/////////////////////////////////////////////////////////////////////////////
//
// Dialog
//
IDD_ABOUTBOX DIALOG DISCARDABLE 22, 17, 230, 75
STYLE DS_MODALFRAME | WS_CAPTION | WS_SYSMENU
CAPTION "About"
FONT 8, "System"
BEGIN
    ICON IDI_WINRC,IDC_MYICON,14,9,16,16
    LTEXT "winrc Version 1.0",IDC_STATIC,49,10,119,8,SS_NOPREFIX
    LTEXT "Copyright (C) 2002",IDC_STATIC,49,20,119,8
    DEFPUSHBUTTON "OK",IDOK,195,6,30,11,WS_GROUP
END
/////////////////////////////////////////////////////////////////////////////
//
// String Table
//
STRINGTABLE DISCARDABLE
BEGIN
IDS_APP_TITLE "winrc"
IDS_HELLO "Hello World!"
IDC_WINRC "WINRC"
IDS_SENTENCE "Deutsche Sprache schwere Sprache"
IDS_CITY "Düsseldorf"
END
#endif // German (Germany) resources
/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////
// English (U.S.) resources
#if !defined(AFX_RESOURCE_DLL) || defined(AFX_TARG_ENU)
#ifdef _WIN32
LANGUAGE LANG_ENGLISH, SUBLANG_ENGLISH_US
#pragma code_page(1252)
#endif //_WIN32
#ifdef APSTUDIO_INVOKED
/////////////////////////////////////////////////////////////////////////////
//
// TEXTINCLUDE
//
2 TEXTINCLUDE DISCARDABLE
BEGIN
    "#define APSTUDIO_HIDDEN_SYMBOLS\r\n"
    "#include ""windows.h""\r\n"
    "#undef APSTUDIO_HIDDEN_SYMBOLS\r\n"
    "#include ""resource.h""\r\n"
    "\0"
END
3 TEXTINCLUDE DISCARDABLE
BEGIN
    "\r\n"
    "\0"
END
1 TEXTINCLUDE DISCARDABLE
BEGIN
    "resource.h\0"
END
#endif // APSTUDIO_INVOKED
#endif // English (U.S.) resources
/////////////////////////////////////////////////////////////////////////////
#ifndef APSTUDIO_INVOKED
/////////////////////////////////////////////////////////////////////////////
//
// Generated from the TEXTINCLUDE 3 resource.
//
/////////////////////////////////////////////////////////////////////////////
#endif // not APSTUDIO_INVOKED
```