---
layout: default
title: Cobol
nav_order: 1
parent: Use From...
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# How To Use ICU4C From COBOL
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

This document describes how to use ICU functions within a COBOL program. It is
assumed that the programmer understands the concepts behind ICU, and is able to
identify which ICU APIs are appropriate for his/her purpose. The programmer must
also understand the meaning of the arguments passed to these APIs and of the
returned value, if any. This is all explained in the ICU documentation, although
in C/C++ style. This document’s objective is to facilitate the adaptation of
these explanations to COBOL syntax.

It must be understood that the packaging of ICU data and executable code into
libraries is platform dependent. Consequently, the calling conventions between
COBOL programs and the C/C++ functions in ICU may vary from platform to
platform. In a lesser way, the C/C++ types of arguments and return values may
have different equivalents in COBOL, depending on the platform and even the
specific COBOL compiler used.

This document is supplemented with three [sample
programs](https://sourceforge.net/projects/icu/files/OldFiles/samples/ICU-COBOL.zip)
illustrating using ICU APIs for code page conversion, collation and
normalization. Description of the sample programs appears in the appendix at the
end of this document.

## ICU API invocation in COBOL

1.  Invocation of ICU APIs is done with the COBOL “CALL” statement.

2.  Variables, pointers and constants appearing in ICU \*.H files (for C/C++)
    must be defined in the WORKING-STORAGE section for COBOL.

3.  Arguments to a C/C++ API translate into arguments to a COBOL CALL statement,
    passed by value or by reference as will be detailed below.

4.  For a C/C++ API with a non-void return value, the RETURNING clause will be
    used for the CALL statement.

5.  Character string arguments to C/C++ must be null-terminated. In COBOL, this
    means using the `Z"xxx"` format for literals, and adding `X"00"` at the end of
    the content of variables.

6.  Special consideration must be given when a pointer is the value returned by
    an API, since COBOL implements a more limited concept of pointers than
    C/C++. How to handle this case will be explained below.

### COBOL and C/C++ Data Types

The following table (extracted from IBM VisualAge COBOL documentation) shows the
correspondence between the data types available in COBOL and C/C++.

> :point_right: **Note**: Parts of identifier names in Cobol are separated by `-`, not by `_` as in C.

| C/C++ data types          	| COBOL data types                                                                                  	|
|---------------------------	|---------------------------------------------------------------------------------------------------	|
| wchar_t                   	| "DISPLAY-1 (PICTURE N, G) wchar_t is the processing code whereas DISPLAY-1 is the file code."     	|
| char                      	| PIC X.                                                                                            	|
| signed char               	| No appropriate COBOL equivalent.                                                                  	|
| unsigned char             	| No appropriate COBOL equivalent.                                                                  	|
| short signed int          	| PIC S9-S9(4) COMP-5. Can beCOMP, COMP-4, or BINARY if you use the TRUNC(BIN) compiler option.     	|
| short unsigned int        	| PIC 9-9(4) COMP-5. Can be COMP, COMP-4, or BINARY if you use the TRUNC(BIN) compiler option.      	|
| long int                  	| PIC 9(5)-9(9) COMP-5. Can be COMP, COMP-4, or BINARY if you use the TRUNC(BIN) compiler option.   	|
| long long int             	| PIC 9(10)-9(18) COMP-5. Can be COMP, COMP-4, or BINARY if you use the TRUNC(BIN) compiler option. 	|
| float                     	| COMP-1.                                                                                           	|
| double                    	| COMP-2.                                                                                           	|
| enumeration               	| Equivalent to level 88, but not identical.                                                        	|
| char(n)                   	| PICTURE X(n).                                                                                     	|
| array pointer (*) to type 	| No appropriate COBOL equivalent.                                                                  	|
| pointer(*) to function    	| PROCEDURE-POINTER.                                                                                	|

A number of C definitions specific to ICU (and many other compilers on POSIX
platforms) that are not presented in the table above can also be translated into
COBOL definitions.

| C/C++ data types                         | COBOL data types                                                                            |
|------------------------------------------|---------------------------------------------------------------------------------------------|
| int8_t                                   | PIC X. Not really equivalent.                                                               |
| uint8_t                                  | PIC X. Not really equivalent.                                                               |
| int16_t                                  | PIC S9(4) BINARY. Can beCOMP, COMP-4, or BINARY if you use the TRUNC(BIN) compiler option.  |
| uint16_t                                 | PIC 9(4) BINARY. Can beCOMP, COMP-4, or BINARY if you use the TRUNC(BIN) compiler option.   |
| int32_t                                  | PIC S9(9) COMP-5. Can be COMP, COMP-4, or BINARY if you use the TRUNC(BIN) compiler option. |
| uint32_t                                 | PIC 9(9) COMP-5. Can be COMP, COMP-4, or BINARY if you use the TRUNC(BIN) compiler option.  |
| Uchar                                    | PIC 9(4) BINARY. Can beCOMP, COMP-4, or BINARY if you use the TRUNC(BIN) compiler option.   |
| Uchar32                                  | PIC 9(9) COMP-5. Can be COMP, COMP-4, or BINARY if you use the TRUNC(BIN) compiler option.  |
| UNormalizationMode                       | PIC S9(9) COMP-5. Can be COMP, COMP-4, or BINARY if you use the TRUNC(BIN) compiler option. |
| UerrorCode                               | PIC S9(9) COMP-5. Can be COMP, COMP-4, or BINARY if you use the TRUNC(BIN) compiler option. |
| pointer(*) to object (e.g. Uconverter *) | PIC S9(9) COMP-5. Can be COMP, COMP-4, or BINARY if you use the TRUNC(BIN) compiler option. |
| Windows Handle                           | PIC S9(9) COMP-5. Can be COMP, COMP-4, or BINARY if you use the TRUNC(BIN) compiler option. |

### Enumerations (first possibility)

C Enumeration types do not translate very well into COBOL. There are two
possible ways to simulate these enumerations.

#### C example

```c
    typedef enum {
        /** No decomposition/composition. @draft ICU 1.8 */
        UNORM_NONE = 1,
        /** Canonical decomposition. @draft ICU 1.8 */
        UNORM_NFD = 2,
        . . .
    } UNormalizationMode;
```

#### COBOL example

```cobol
    WORKING-STORAGE section.
    *--------------- Ported from unorm.h ------------
    * enum UNormalizationMode {
    77 UNORM-NONE PIC
    S9(9) Binary value 1.
    77 UNORM-NFD PIC
    S9(9) Binary value 2.
        …
```

### Enumerations (second possibility)

#### C example

```c
    /*==== utypes.h ========*/
    typedef enum UErrorCode {
        U_USING_FALLBACK_WARNING = -128, /* (not an error) */
        U_USING_DEFAULT_WARNING = -127, /* (not an error) */
        . . .
    } UErrorCode;
```

#### COBOL example

```cobol
    *==== utypes.h ========
    01 UerrorCode PIC S9(9) Binary value 0.
    * A resource bundle lookup returned a fallback
    * (not an error)
        88 U-USING-FALLBACK-WARNING value -128.
    * (not an error)
        88 U-USING-DEFAULT-WARNING value -127.
        . . .
```

## Call statement, calling by value or by reference

In general, arguments defined in C as pointers (`\*`) must be listed in the
COBOL Call statement with the using by reference clause. Arguments which are not
pointers must be transferred with the using by value clause. The exception to
this requirement is when an argument is a pointer which has been assigned to a
COBOL variable (e.g. as a value returned by an ICU API), then it must be passed
by value. For instance, a pointer to a Converter passed as argument to
conversion APIs.

### Conversion Declaration Examples

#### C (API definition in \*.h file)

```c
    /*--------------------- UCNV.H ---------------------------*/
    U_CAPI int32_t U_EXPORT2
    ucnv_toUChars(UConverter * cnv,
        UChar * dest,
        int32_t destCapacity,
        const char * src,
        int32_t srcLength,
        UErrorCode * pErrorCode);
```

#### COBOL

```cobol
    PROCEDURE DIVISION.
        Call API-Pointer using
            by value Converter-toU-Pointer
            by reference Unicode-Input-Buffer
            by value destCapacity
            by reference Input-Buffer
            by value srcLength
            by reference UErrorCode
            Returning Text-Length.
```

## Call statement, Returning clause

### Returned value is Pointer or Binary

#### C (API definition in \*.h file)

```c
    U_CAPI UConverter * U_EXPORT2
    ucnv_open(const char * converterName,
        UErrorCode * err);
```

#### COBOL

```cobol
    WORKING-STORAGE section.
        01 Converter-Pointer PIC S9(9) BINARY.
    PROCEDURE DIVISION
        Move Z"iso-8859-8" to converterNameSource.
    . . .
        Call API-Pointer using
            by reference converterNameSource
            by reference UErrorCode
            Returning Converter-Pointer.
```

### Returned value is a Pointer to string

If the returned value in C is a string pointer (`char \*`), then in COBOL we
must use a pointer to string defined in the Linkage section.

#### C ( API definition in \*.h file)

```c
    U_CAPI const char * U_EXPORT2
    ucnv_getAvailableName(int32_t n);
```

#### COBOL

```cobol
    DATA DIVISION.
    WORKING-STORAGE section.
        01 Converter-Name-Link-Pointer Usage is Pointer.
    LINKAGE section.
        01 Converter-Name-Link.
            03 Converter-Name-String pic X(80).
    PROCEDURE DIVISION using Converter-Name-Link.
        Call API-Pointer using by value Converters-Index
            Returning Converter-Name-Link-Pointer.
        SET Address of Converter-Name-Link
            to Converter-Name-Link-Pointer.
    . . .
        Move Converter-Name-String to Debug-Value.
```

## How to invoke ICU APIs

Inter-language communication is often problematic. This is certainly the case
when calling C/C++ functions from COBOL, because of the very different roots of
the two languages. How to invoke the ICU APIs from a COBOL program is likely to
depend on the operating system and even on the specific compilers in use. The
section below deals with COBOL to C calls on a Windows platform. Similar
sections should be added for other platforms.

### Windows platforms

The following instructions were tested on a Windows 2000 platform, with the IBM
VisualAge COBOL compiler and the Microsoft Visual C/C++ compiler.

For Windows, ICU APIs are normally packaged as DLLs (Dynamic Load Libraries).
For technical reasons, COBOL calls to C/C++ functions need to be done via
dynamic loading of the DLLs at execution time (load on call).

The COBOL program must be compiled with the following compiler options:

    \* options CBL PGMNAME(MIXED) CALLINT(SYSTEM) NODYNAM

In order to call an ICU API, two preparation steps are needed:

1.  Load in memory the DLL which contains the API

2.  Get the address of the API

For performance, it is better to perform these steps once before the first call
and to save the returned values for future use (the sample programs get the
address of APIs for each call, for the sake of logging; production programs
should get the address once and reuse it
as many times as needed).

When no more APIs from a DLL are needed, the DLL should be unloaded in order to
free the associated memory.

### Load DLL Into Memory

This is done as follows:

    Call "LoadLibraryA" using by reference DLL-Name
        Returning DLL-Handle.
    IF DLL-Handle = ZEROS
        Perform error handling. . .

Return value: DLL Handle, defined as `PIC S9(9) BINARY`

Input Value: DLL Name (null-terminated string)

Errors may happen if the DLL name is not correct, or the string is not
null-terminated, or the DLL file is not available (in the current directory or
in a directory included in the PATH system variable).

#### Get API address

This is done as follows:

    Call "GetProcAddress" using by value DLL-Handle
        by reference API-Name
        Returning API-Pointer.
    IF API-Pointer = NULL
        Perform error handling...

Return value: API address, defined as PROCEDURE-POINTER
Input Value: DLL Handle (returned by call to LoadLibraryA)
Procedure Name (null-terminated string)

Errors may happen if the API name is not correct (remember that API names are
case-sensitive), or the string is not null-terminated, or the API is not
included in the specified DLL. If the API pointer is not null, the call to the
API is done with following according to the arguments and return value of the
API.

    Call API-Pointer using . . . returning . . .

After calling an API, the returned error code should be checked when relevant.
Code to check for error conditions is illustrated in the sample programs.

#### Unload DLL from Memory

This is done as follows:

    Call "FreeLibrary" using DLL-Handle.

Return value: none
Input Value: DLL Handle (returned by call to LoadLibraryA)

## Sample Programs

Three sample programs are supplied with this document. The sample programs were
developed on and for a Windows 2000 platform. Some adaptations may be necessary
for other platforms

Before running the sample programs, you must perform the following steps:

1.  Install the version of ICU appropriate for your platform

2.  Build ICU libraries if needed (see the ICU Readme file)

3.  Make the libraries accessible (for instance on Windows systems, add the
    directory containing the libraries to the PATH system variable)

4.  Compile the sample programs with appropriate compiler options

5.  Copy the test files to a work directory

Each program is supplied with input test files and with a model log file. If the
log file that you create by running a sample program is equivalent to the model
log file, your setup is probably correct.

The three sample programs focus each on a certain ICU area of functionality:

1.  Conversion

2.  Collation

3.  Normalization

### Conversion sample program

    * The sample program includes the following steps:
    * - Display the names of the converters from a list of all
    * converters contained in the alias file.
    * - Display the current default converter name.
    * - Set new default converter name.
    *
    * - Read a string from Input file "ICU_Conv_Input_8.txt"
    * (File in UTF-8 Format)
    * - Convert this string from UTF-8 to code page iso-8859-8
    * - Write the result to output file "ICU_Conv_Output.txt"
    *
    * - Read a line from Input file "ICU_Conv_Input.txt"
    * (File in ANSI Format, code page 862)
    * - Convert this string from code page ibm-862 to UTF-16
    * - Convert the resulting string from UTF-16 to code page windows-1255
    * - Write the result to output file "ICU_ Conv_Output.txt"
    * - Write debugging information to Display and
    * log file "ICU_Conv_Log.txt" (File in ANSI Format)
    * - Repeat for all lines in Input file
    **
    * The following ICU APIs are used:
    * ucnv_countAvailable
    * ucnv_getAvailableName
    * ucnv_getDefaultName
    * ucnv_setDefaultName
    * ucnv_convert
    * ucnv_open
    * ucnv_toUChars
    * ucnv_fromUChars
    * ucnv_close

The ucnv_xxx APIs are documented in file "UCNV.H".

### Collation sample program

    * The sample program includes the following steps:
    * - Read a string array from Input file "ICU_Coll_Input.txt"
    * (file in ANSI format)
    * - Convert string array from code page into UTF-16 format
    * - Compare the string array into the canonical composed
    * - Perform bubble sort of string array, according
    * to Unicode string equivalence comparisons
    * - Convert string array from Unicode into code page format
    * - Write the result to output file "ICU_Coll_Output.txt"
    * (file in ANSI format)
    * - Write debugging information to Display and
    * log file "ICU_Coll_Log.txt" (file in ANSI format)
    **
    * The following ICU APIs are used:
    * ucol_open
    * ucol_strcoll
    * ucol_close
    * ucnv_open
    * ucnv_toUChars
    * ucnv_fromUChars
    * ucnv_close

The ucol_xxx APIs are documented in file "UCOL.H".
The ucnv_xxx APIs are documented in file "UCNV.H".

### Normalization sample program

    * The sample includes the following steps:
    * - Read a string from input file "ICU_NORM_Input.txt"
    * (file in ANSI format)
    * - Convert the string from code page into UTF-16 format
    * - Perform quick check on the string, to determine if the
    * string is in NFD (Canonical decomposition)
    * normalization format.
    * - Normalize the string into canonical composed form
    * (FCD and decomposed)
    * - Perform quick check on the result string, to determine
    * if the string is in NFD normalization form
    * - Convert the string from Unicode into the code page format
    * - Write the result to output file "ICU_NORM_Output.txt"
    * (file in ANSI format)
    * - Write debugging information to Display and
    * log file "ICU_NORM_Log.txt" (file in ANSI format)
    **
    * The following ICU APIs are used:
    * ucnv_open
    * ucnv_toUChars
    * unorm_normalize
    * unorm_quickCheck
    * ucnv_fromUChars
    * ucnv_close

The unorm_xxx APIs are documented in file "UNORM.H".

The ucnv_xxx APIs are documented in file "UCNV.H".
