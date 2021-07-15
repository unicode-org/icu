---
layout: default
title: Converter
nav_order: 1
parent: Conversion
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Using Converters
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

When designing applications around Unicode characters, it is sometimes required
to convert between Unicode encodings or between Unicode and legacy text data.
The vast majority of modern Operating Systems support Unicode to some degree,
but sometimes the legacy text data from older systems need to be converted to
and from Unicode. This conversion process can be done with an ICU converter.

## ICU converters

ICU provides comprehensive character set conversion services, mapping tables,
and implementations for many encodings. Since ICU uses Unicode (UTF-16)
internally, all converters convert between UTF-16 (with the endianness according
to the current platform) and another encoding. This includes Unicode encodings.
In other words, internal text is 16-bit Unicode, while "external text" used as
source or target for a conversion is always treated as a byte stream.

ICU converters are available for a wide range of encoding schemes. Most of them
are based on mapping table data that is handled by few generic implementations.
Some encodings are implemented algorithmically in addition to (or instead of)
using mapping tables, especially Unicode encodings. The partly or entirely
table-based encoding schemes include: All ICU converters map only single Unicode
character code points to and from single codepage character code points. ICU
converters **do not** deal directly with combining characters, bidirectional
reordering, or Arabic shaping, for example. Such processes, if required, must be
handled separately. For example, while in Unicode, the ICU BiDi APIs can be used
for bidirectional reordering after a conversion to Unicode or before a
conversion from Unicode.

ICU converters are not designed to perform any encoding autodetection. This
means that the converters do not autodetect "endianness", the 6 Unicode encoding
signatures, or the Shift-JIS vs. EUC-JP, etc. There are two exceptions: The
UTF-16 and UTF-32 converters work according to Unicode's specification of their
Character Encoding Schemes, that is, they read the BOM to figure out the actual
"endianness".

The ICU mapping tables mostly come from an [IBM® codepage
repository](http://www.ibm.com/software/globalization/cdra). For non-IBM
codepages, there is typically an equivalent codepage registered with this
repository. However, the textual data format (.ucm files) is generic, and data
for other codepage mapping tables can also be added.

## Using the Default Codepage

ICU has code to determine the default codepage of the system or process. This
default codepage can be used to convert `char *` strings to and from Unicode.

Depending on system design, setup and APIs, it may not always be possible to
find a default codepage that fully works as expected. For example,

1.  On Windows there are three encodings in use at the same time. Unicode
    (UTF-16) is always used inside of Windows, while for `char *` encodings there
    are two classes, called "ANSI" and "OEM" codepages. ICU will use the ANSI
    codepage. Note that the OEM codepage is used by default for console window
    output.

2.  On some UNIX-type systems, non-standard names are used for encodings, or
    non-standard encodings are used altogether. Although ICU supports over 200
    encodings in its standard build and many more aliases for them, it will not
    be able to recognize such non-standard names.

3.  Some systems do not have a notion of a system or process codepage, and may
    not have APIs for that.

If you have means of detecting a default codepage name that are more appropriate
for your application, then you should set that name with `ucnv_setDefaultName()`
as the first ICU function call. This makes sure that the internally cached
default converter will be instantiated from your preferred name.

Starting in ICU 2.0, when a converter for the default codepage cannot be opened,
a fallback default codepage name and converter will be used. On most platforms,
this will be US-ASCII. For z/OS (OS/390), ibm-1047,swaplfnl is the default
fallback codepage. For AS/400 (iSeries), ibm-37 is the default fallback
codepage. This default fallback codepage is used when the operating system is
using a non-standard name for a default codepage, or the converter was not
packaged with ICU. The feature allows ICU to run in unusual computing
environments without completely failing.

## Usage Model

A "Converter" refers to the C structure "UConverter". Converters are cheap to
create. Any data that is shared between converters of the same kind (such as the
mappings, the name and the properties) are automatically cached and shared in
memory.

### Converter Names

Codepages with encoding schemes have been given many names by various vendors
and platforms over the years. Vendors have different ways specify which codepage
and encoding are being used. IBM uses a CCSID (Coded Character Set IDentifier).
Windows uses a CPID (CodePage IDentifier). Macintosh has a TextEncoding. Many
Unix vendors use [IANA](http://www.iana.org/assignments/character-sets)
character set names. Many of these names are aliases to converters within ICU.

In order to help identify which names are recognized by certain platforms, ICU
provides several converter alias functions. The complete description of these
functions can be found in the [ICU API Reference](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/ucnv_8h.html) .

| Function Names | Short Description |
| -------------- | ----------------- |
| `ucnv_countAvailable`, `ucnv_getAvailableName` | Get a list of available converter names that can be opened. |
| `ucnv_openAllNames` | Get a list of all known converter names. |
| `ucnv_getName` | Get the name of an open converter. |
| `ucnv_countAliases`, `ucnv_getAlias` | Get the list of aliases for the specified converter. |
| `ucnv_countStandards`, `ucnv_getStandard` | Get the list of known standards. |
| `ucnv_openStandardNames` | Get a filtered list of aliases for a converter that is known by the specified standard. |
| `ucnv_getStandardName` | Get the preferred alias name specified by a given standard. |
| `ucnv_getCanonicalName` | Get the converter name from the alias that is recognized by the specified standard. |
| `ucnv_getDefaultName` | Get the default converter name that is currently used by ICU and the operating system. |
| `ucnv_setDefaultName` | Use this function to override the default converter name. |

Even though IANA specifies a list of aliases, it usually does not specify the
mappings or the actual character set for the aliases. Sometimes vendors will map
similar glyph variants to different Unicode code points or sometimes they will
assign completely different glyphs for the same codepage code point. Because of
these ambiguities, you can sometimes get `U_AMBIGUOUS_ALIAS_WARNING` for the
returned `UErrorCode` when more than one converter uses the requested alias. This
is only a warning, and the results can still be used. This UErrorCode value is
just a reminder that you may not get what you expected. The above functions can
help you to determine which converter you actually wanted.

EBCDIC based converters do have the option to swap the newline and linefeed
character mappings. This can be useful when transferring EBCDIC documents
between z/OS (MVS, os/390 and the rest of the zSeries family) and another EBCDIC
machine like OS/400 on iSeries. The ",swaplnlf" or `UCNV_SWAP_LFNL_OPTION_STRING`
from ucnv.h can be appended to a converter alias in order to achieve this
behavior. You can view other available options in ucnv.h.

You can always skip many of these aliasing and mapping problems by just using
Unicode.

### Creating a Converter

There are four ways to create a converter:

1.  **By name**: Converters can be created using different types of names. No
    distinction is made when the converter is created, as to which name is being
    employed. There are many types of aliases possible. Among these are
    [IANA](http://www.iana.org/assignments/character-sets) ("shift_jis",
    "koi8-r", or "iso-8859-3"), host specific names ("cp1252" which is the name
    for a Microsoft® Windows™ or a similar IBM® codepage). Finally, ICU's own
    internal canonical names for a converter can be used. These include "UTF-8"
    or "ISO-8859-1" for built-in conversion types, and names such as
    "ibm-949_P110-2000" (Shift-JIS with '\\' <-> '¥' mapping) or
    "ibm-949_P11A-2000" (Shift-JIS with '\\' <-> '\\' mapping) for data-file
    based conversions.
    
    ```c
    UConverter *conv = ucnv_open("shift_jis", &myError);
    ```

    As a convenience, converter names can be passed in as Unicode. (for example,
    if a user passed in the string from a Unicode-based user interface).
    However, the actual names are restricted to an invariant ASCII/EBCDIC
    subset.

    ```c
    UChar *name = ...; UConverter *conv = ucnv_openU(name, &myError);
    ```

    Converter names are case-insensitive. In addition, beginning with ICU 3.6,
    leading zeroes are ignored in sequences of digits (if further digits
    follow), and all non-alphanumeric characters are ignored. Thus the strings
    "UTF-8", "utf_8", "u\*T@f08" and "Utf 8" are equivalent. (Before ICU 3.6,
    leading zeroes were not ignored, and only spaces, dashes and underscores
    were ignored.) The `ucnv_compareNames()` function provides such string
    comparisons.

    Unlike the names of resources or other types of ICU data, converter names
    can **not** be qualified with a path that indicates the directory or common
    data file containing the corresponding converter data. The requested
    converter's data must be present either in the main ICU data library or as a
    separate file located in the ICU data directory. However, you can always
    create a package of converters with pkgdata and open a converter from the
    package with `ucnv_openPackage()`

    ```c
    UConverter *conv = ucnv_openPackage("./myPackage.dat", "customConverter", &myError);
    ```

2.  **By number**: The design of the ICU is to accommodate codepages provided by
    different vendors. For example, the IBM CDRA (Character Data Representation
    Architecture which is an IBM architecture that defines a set of identifiers)
    has an ID type called the CCSID (Coded Character Set Identifier). The ICU
    API for opening a codepage by number must be given a vendor along with the
    number. Currently, only IBM (`UCNV_IBM`) is supported. For example, the US
    EBCDIC codepage (IBM #37) can be opened with the following code:

    ```c
    ucnv_openCCSID(37, UCNV_IBM, &myErr);
    ```

3.  **By iteration**: An application might not know ahead of time which codepage
    to use, and thus might need to query ICU to determine the entire list of
    installed converters. The ICU returns a list of its canonical (internal)
    names. From each names, the standard IANA name can be determined, and also a
    list of aliases which point to that name can be determined. For example, ICU
    might return among the canonical names "ibm-367". That name itself may or
    may not provide the application or its users with the information needed.
    (367 is actually the decimal form of a number that is calculated by
    appending certain hex digits together.) However, the IANA name can be
    requested from this canonical name, which should return something like
    "us-ascii". The alias list for ibm-367 can be iterated over as well, which
    returns additional names like "ascii", "646", "ansi_x3.4-1968" etc. If this
    is not sufficient information, once a converter is opened, it can be queried
    for its type, min and max char size, etc. This information is not available
    without actually opening the converter (a fairly lightweight process.)

    ```c
    /* Returns count of the number of available names */
    int count = ucnv_countAvailable();
    /* get the canonical name of the 36th available converter */
    const char *convName1 = ucnv_getAvailableName(36);
    /* get the 3rd alias for a given codepage. */
    const char *asciiAlias = ucnv_getAlias("ibm-367", 3, &myError);
    /* Get the IANA name of the converter */
    const char *ascii = ucnv_getStandardName("ibm-367", "IANA");
    /* Get the one of the non preferred IANA name of the converter. */
    UEnumeration *asciiEnum =
    ucnv_openStandardNames("ibm-367", "IANA", &myError);
    uenum_next(asciiEnum, &myError); /* skip preferred IANA alias */
    /* get one of the non-preferred IANA aliases */
    const char *ascii2 = uenum_next(asciiEnum, &myError);
    uenum_close(asciiEnum);
    ```

4.  **By using the default converter**: The default converter can be opened by
    passing a NULL as the name of the converter.

    ```c
    ucnv_open(NULL, &myErr);
    ```

> :point_right: **Note**: ICU chooses this converter based on the best information available to it.
> The purpose of this converter is to interface with the OS using a codepage (i.e. `char *`).
> Do not use it as a way of determining the best overall converter to use.
> Usually any Unicode encoding form is the best way to store and send text data,
> so that important data does not get lost in the conversion.
> Also, if the OS supports Unicode-based API's (such as Win32),
> it is better to use only those Unicode API's.
> As an example, the new Windows 2000 locales (such as Hindi) do not
> define the default codepage to something that supports Hindi.
> The default converter is used in expressions such as: `UnicodeString text("abc");`
> to convert 'abc', and in the `u_uastrcpy()` C functions.
> Code operating at the [OS level](../design.md) MAY choose to
> change the default converter with `ucnv_setDefaultName()`.
> However, be aware that this change has inconsistent results if it is done after
> ICU components are initialized.

### Closing a Converter

Closing a converter frees memory occupied by that instance of the converter.
However it does not release the larger shared data tables the converter might
use. OS-level code may call `ucnv_flushCache()` to explicitly free memory occupied
by [unused tables](../design.md).

```c
ucnv_close(conv)
```

### Converter Life Cycle

Note that a Converter is created with a certain type (for instance, ISO-8859-3)
which does not change over the life of that [object](../design.md). Converters
should be allocated one per thread. They are cheap to create, as the shared data
doesn't need to be reallocated.

This is the typical life cycle of a converter, as shown step-by-step:

1.  First, open up the converter with a specified name (or alias name).
    ```c
    UConverter *conv = ucnv_open("shift_jis", &status);
    ```

2.  Target here is the `char s[]` to write into, and targetSize is how big the
    target buffer is. Source is the UChars that are being converted.
    ```c
    int32_t len = ucnv_fromUChars(conv, target, targetSize, source, u_strlen(source), &status);
    ```

3.  Clean up the converter.
    ```c
    ucnv_close(conv);
    ```

### Sharing Converters Between Threads

A converter cannot be shared between threads at the same time. However, if it is
reset it can be used for unrelated chunks of data. For example, use the same
converter for converting data from Unicode to ISO-8859-3, and then reset it. Use
the same converter for converting data from ISO-8859-3 back into Unicode.

### Converting Large Quantities of Data

If it is necessary to convert a large quantity of data in smaller buffers, use
the same converter to convert each buffer. This will make sure any state is
preserved from one chunk to the next. Doing this conversion is known as
streaming or buffering, and is mentioned [Buffered or Streamed](#3-buffered-or-streamed)
section (§) later in this chapter.

### Cloning a Converter

Cloning a converter returns a clone of the converter object along with any
internal state that the converter might be storing. Cloning routines must be
used with extreme care when using converters for stateful or multibyte
encodings. If the converter object is carrying an internal state, and the
newly-created clone is used to convert a new chunk of text, the converter
produces incorrect results. Also note that the caller owns the cloned object and
has to call `ucnv_close()` to dispose of the object. Calling `ucnv_reset()` before
cloning will reset the converter to its original state.

```c
UConverter* newCnv = ucnv_safeClone(oldCnv, 0, &bufferSize, &err)
```

## Converter Behavior

### Conversion

1.  The converters always consume the source buffer as far as possible, and
    advance the source pointer.

2.  The converters write to the target all converted output as far as possible,
    and then write any remaining output to the internal services buffer. When
    the conversion routines are called again, the internal buffer is flushed out
    and written to the target buffer before proceeding with any further
    conversion.

3.  In conversions to Unicode from Multi-byte encodings or conversions from
    Unicode involving surrogates, if (a) only a partial byte sequence is
    retrieved from the source buffer, (b) the "flush" parameter is set to "TRUE"
    and (c) the end of source is reached, then the callback is called with
    `U_TRUNCATED_CHAR_FOUND`.

### Reset

Converters can be reset explicitly or implicitly. Explicit reset is done by
calling:

1.  `ucnv_reset()`: Resets the converter to initial state in both directions.

2.  `ucnv_resetToUnicode()`: Resets the converter to initial state to Unicode
    direction.

3.  `ucnv_resetFromUnicode()`: Resets the converter to initial state from Unicode
    direction.

The converters are reset implicitly when the conversion functions are called
with the "flush" parameter set to "TRUE" and the source is consumed.

### Error

#### Conversion from Unicode

Not all characters can be converted from Unicode to other codepages. In most
cases, Unicode is a superset of the characters supported by any given codepage.

The default behavior of ICU in this case is to substitute the illegal or
unmappable sequence, with the appropriate substitution sequence for that
codepage. For example, ISO-8859-1, along with most ASCII-based codepages, has
the character 0x1A (Control-Z) as the substitution sequence. When converting
from Unicode to ISO-8859-1, any characters which cannot be converted would be
replaced by 0x1A's.

SubChar1 is sometimes used as substitution character in MBCS conversions. For
more information on SubChar1 please see the [Conversion Data](data.md) chapter.

In stateful converters like ISO-2022-JP, if a substitution character has to be
written to the target, then an escape/shift sequence to change the state to
single byte mode followed by a substitution character is written to the target.

The substitution character can be changed by calling the `ucnv_setSubstChars()`
function with the desired codepage byte sequence. However, this has some
limitations: It only allows setting a single character (although the character
can consist of multiple bytes), and it may not work properly for some stateful
converters (like HZ or ISO 2022 variants) when setting a multi-byte substitution
character. (It will work for EBCDIC_STATEFUL ones.) Moreover, for setting a
particular character, the caller needs to know the correct byte sequence for
that character in the converter's codepage. (For example, a space (U+0020) is
encoded as 0x20 in ASCII-based codepages, 0x40 in EBCDIC-based ones, 0x00 0x20
or 0x20 0x00 in UTF-16 depending on the stream's endianness, etc.)

The `ucnv_setSubstString()` function (new in ICU 3.6) lifts these limitations. It
takes a Unicode string and verifies that it can be converted to the codepage
without error and that it is not too long (32 bytes as of ICU 3.6). The string
can contain zero, one or more characters. An empty string has the effect of
using the skip callback. See the Error Callbacks below. Stateful converters are
fully supported. The same Unicode string will give equivalent results with all
converters that support its conversion.

Internally, `ucnv_setSubstString()` stores the byte sequence from the test
conversion if the converter is stateless, or the Unicode string itself if the
converter is stateful. If the Unicode string is stored, then it is converted on
the fly during substitution, handling all state transitions.

The function `ucnv_getSubstChars()` can be used to retrieve the substitution byte
sequence if it is the default one, set by `ucnv_setSubstChars()`, or if
`ucnv_setSubstString()` stored the byte sequence for a stateless converter. The
Unicode string set for a stateful converter cannot be retrieved.

#### Conversion to Unicode

In conversion to Unicode, errors are normally due to ill-formed byte sequences:
Unused byte values, or lead bytes not followed by trail bytes according to the
encoding scheme. Well-formed but unmappable sequences are unusual but possible.

The ICU default behavior is to emit an `U+FFFD REPLACEMENT CHARACTER` per
offending sequence.

If the conversion table .ucm file contains a `<subchar1>` entry (such as in the
ibm-943 table), a U+001A C0 control ("SUB") is emitted for single-byte
illegal/unmappable input rather than `U+FFFD REPLACEMENT CHARACTER`. For details
on this behavior look for "001A" in the [Conversion Data](data.md) chapter.

* This behavior originates from mainframes with dedicated single-byte-to-single-byte
  and double-to-double conversions.
* Emitting U+001A for single-byte errors can be avoided by (a) removing the
  `<subchar1>` mapping or (b) using a similar conversion table that does not
  have this mapping (e.g., windows-932 instead of ibm-943) or (c) writing a
  custom callback function.

### Error Codes

Here are some of the `UErrorCode`s which have significant meaning for conversion:

#### U_INDEX_OUTOFBOUNDS_ERROR

In `getNextUChar()` - all source data
has been consumed without producing a Unicode character

#### U_INVALID_CHAR_FOUND
No mapping was found from the source to the target encoding. For example, U+0398
(Capital Theta) has no mapping into ISO-8859-1, and so U_INVALID_CHAR_FOUND
will result.

#### U_TRUNCATED_CHAR_FOUND

All of the source data was read, and a
character sequence was incomplete. For example, only half of a double-byte
sequence may have been encountered. When converting FROM Unicode, this error
would occur when a conversion ends with a low surrogate (U+D800) at the end of
the source, with no corresponding high surrogate.

#### U_ILLEGAL_CHAR_FOUND

A character sequence was found in the source which is disallowed in the source
encoding scheme. For example, many MBCS encodings have only certain byte
sequences which are allowed as lead bytes. When converting from Unicode, if a
low surrogate is NOT followed immediately by a high surrogate, or a high
surrogate without its preceding low surrogate, an illegal sequence results.
Note: Most, but not all, converters forbid surrogate code points or unpaired
surrogate code units. (Lead surrogate without trail, or trail without lead.)
Some converters permit surrogate code points/unpaired surrogates because their
charset specification permits it. For example, LMBCS, SCSU and
BOCU-1.

#### U_INVALID_TABLE_FORMAT

An error occurred trying to read the backing data
for the converter. The data could be corrupt, or the wrong
version.

#### U_BUFFER_OVERFLOW_ERROR

More output (target) characters were produced
than fit in the target buffer. If in `to/fromUnicode()`, then process the target
buffer and call the function again to retrieve the overflowed characters.

### Error Callbacks

What actually happens is that an "error callback function" is called at the
point where the conversion failure occurred. The function can deal with the
failed characters as it sees fit. Possible options at the callback's disposal
include ignoring the bad sequence, converting it to a different sequence, and
returning an error to the caller. The callback can also consume any data past
where the error occurred, whether or not that data would have caused an error.
Only one callback is installed at a time, per direction (to or from unicode).

A number of canned functions are provided by ICU, and an application can write
new ones. The "callbacks" are either From Unicode (to codepage), or To Unicode
(from codepage). Here is a list of the canned callbacks in ICU:

1.  UCNV_**FROM_U**_CALLBACK_SUBSTITUTE: This callback is installed by default.
    It will write the codepage's substitute sequence or a user-set substitute
    sequence, or convert a user-set substitute UnicodeString to the codepage.
    See "Error / Conversion from Unicode" above.

2.  UCNV_**TO_U**_CALLBACK_SUBSTITUTE: This callback is installed by default. It
    will write U+FFFD or sometimes U+001A. See "Error / Conversion to Unicode"
    above.

3.  UCNV_FROM_U_CALLBACK_SKIP, UCNV_TO_U_CALLBACK_SKIP: Simply ignores any
    invalid characters in the input, no error is returned.

4.  UCNV_FROM_U_CALLBACK_STOP, UCNV_TO_U_CALLBACK_STOP: Stop at the error.
    Return the error to the caller. (When using the 'BUFFER' mode of conversion,
    the source and target pointers returned can be examined to determine where
    the error occurred. `ucnv_getInvalidUChars()` and `ucnv_getInvalidChars()`
    return the actual text which failed).

5.  UCNV_FROM_U_CALLBACK_ESCAPE, UCNV_TO_U_CALLBACK_ESCAPE: This callback is
    especially useful for debugging. Missing codepage characters are replaced by
    strings such as '%U094D' with the Unicode value, and missing Unicode chars
    are replaced with text of the form '%X0A' where the codepage had the
    unconvertible byte hex 0A.

    When a callback is set, a "context" pointer is also provided. How this
    pointer is created depends on the specific callback. There is usually a
    `createContext()` function for that specific callback, where the caller can
    set certain options for the callback. Consult the documentation for the
    specific callback you are using. For ICU's canned callbacks, this pointer
    may be set to NULL. The functions for setting a different callback also
    return the old callback, and the old context pointer. These may be stored so
    that the old callback is re-installed when an operation is finished.
    
    Additionally the following options can be passed as the context parameter to
    UCNV_FROM_U_CALLBACK_ESCAPE callback function to produce different outputs.

    | UCNV_ESCAPE_ICU     | %U12345 |
    | ------------------- | ------- |
    | UCNV_ESCAPE_JAVA    | \\u1234 |
    | UCNV_ESCAPE_C       | \\udbc9\\udd36 for Plane 1 and \\u1234 for Plane 0 codepoints |
    | UCNV_ESCAPE_XML_DEC | \&#4460; number expressed in Decimal |
    | UCNV_ESCAPE_XML_HEX | \&#x1234; number expressed in Hexadecimal |

Here are some examples of how to use callbacks.

```c
UConverter              *u;
void                    *oldContext, *newContext;
UConverterFromUCallback oldAction, newAction;
u = ucnv_open("shift_jis", &myError);

... /* do some conversion with u from unicode.. */

ucnv_setFromUCallBack(
    u, MY_FROMU_CALLBACK, newContext, &oldAction, &oldContext, &myError);

... /* do some other conversion from unicode */

/* Now, set the callback back */
ucnv_setFromUCallBack(
    u, oldAction, oldContext, &newAction, &newContext, &myError);

```

### Custom Callbacks

Writing a callback is somewhat involved, and will be covered more completely in
a future version of this document. One might look at the source to the provided
callbacks as a starting point, and address any further questions to the mailing
list.

Basically, callback, unlike other ICU functions which expect to be called with
`U_ZERO_ERROR` as the input, is called in an exceptional error condition. The
callback is a kind of 'last ditch effort' to rectify the error which occurred,
before it is returned back to the caller. This is why the implementation of STOP
is very simple:

```c
void UCNV_FROM_U_CALLBACK_STOP(...) { }
```

The error code such as `U_INVALID_CHAR_FOUND` is returned to the user. If the
callback determines that no error should be returned to the user, then the
callback must set the error code to `U_ZERO_ERROR`. Note that this is a departure
from most ICU functions, which are supposed to check the error code and return
immediately if it is set.

> :point_right: **Note**: See the functions `ucnv_cb_write...()` for
> functions which a callback may use to perform its task.

#### Ignore Default_Ignorable_Code_Point

Unicode has a number of characters that are not by themselves meaningful but
assist with line breaking (e.g., U+00AD Soft Hyphen & U+200B Zero Width Space),
bi-directional text layout (U+200E Left-To-Right Mark), collation and other
algorithms (U+034F Combining Grapheme Joiner), or indicate a preference for a
particular glyph variant (U+FE0F Variation Selector 16). These characters are
"invisible" by default, that is, they should normally not be shown with a glyph
of their own, except in special circumstances. Examples include showing a hyphen
for when a Soft Hyphen was used for a line break, or modifying the glyph of a
character preceding a Variation Selector.

Unicode has a character property to identify such characters, as well as
currently-unassigned code points that are intended to be used for similar
purposes: Default_Ignorable_Code_Point, or "DI" for short:
http://www.unicode.org/cldr/utility/list-unicodeset.jsp?a=[:DI:]

Most charsets do not have most or any of these characters.

**ICU 54 and above by default skip default-ignorable code points if they are
unmappable**. (Ticket #[10551](https://unicode-org.atlassian.net/browse/ICU-10551))

**Older versions of ICU** replaced unmappable default-ignorable code points like
any other unmappable code points, by a question mark or whatever substitution
character is defined for the charset.

For best results, a custom from-Unicode callback can be used to ignore
Default_Ignorable_Code_Point characters that cannot be converted, so that they
are removed from the charset output rather than replaced by a visible character.

This is a code snippet for use in a custom from-Unicode callback:

```c
#include "unicode/uchar.h"
// ...
(from-Unicode callback)
    switch(reason) {
    case UCNV_UNASSIGNED:
        if(u_hasBinaryProperty(codePoint, UCHAR_DEFAULT_IGNORABLE_CODE_POINT)) {
            // Ignore/drop default ignorable code points that cannot be converted,
            // rather than treating them like errors/writing a substitution character etc.
            // For example, U+200B Zero Width Space,
            // U+200E Left-To-Right Mark, U+FE0F Variation Selector 16.
            *pErrorCode = U_ZERO_ERROR;
            return;
        } else {
            // ...
```

## Modes of Conversion

When a converter is instantiated, it can be used to convert both in the Unicode
to Codepage direction, and also in the Codepage to Unicode direction. There are
three ways to use the converters, as well as a convenience function which does
not require the instantiation of a converter.

1.  **Single-String**: Simplest type of conversion to or from Unicode. The data
    is entirely contained within a single string.

2.  **Character**: Converting from the codepage to a single Unicode codepoint,
    one at a time.

3.  **Buffer**: Convert data which may not fit entirely within a single buffer.
    Usually the most efficient and flexible.

4.  **Convenience**: Convert a single buffer from one codepage to another
    through Unicode, without requiring the instantiation of a converter.

### 1. Single-String

Data must be contained entirely within a single string or buffer.

```c
conv = ucnv_open("shift_jis", &status);

/* Convert from Unicode to Shift JIS */
len = ucnv_fromUChars(conv, target, targetLen, source, sourceLen, &status);
ucnv_close(conv);

conv = ucnv_open("iso-8859-3", &status);
/* Convert from ISO-8859-3 to Unicode */
len = ucnv_toUChars(conv, target, targetSize, source, sourceLen, &status);
ucnv_close(conv);
```

### 2. Character

In this type, the input data is in the specified codepage. With each function
call, only the next Unicode codepoint is converted at a time. This might be the
most efficient way to scan for a certain character, or other processing of a
single character at a time, because converters are stateful. This works even for
multibyte charsets, and for stateful ones such as iso-2022-jp.

```c
conv = ucnv_open("Big-5", &status);
UChar32 target;
while(source < sourceLimit) {
    target = ucnv_getNextUChar(conv, &source, sourceLimit, &status);
    ASSERT(status);
    processChar(target);
}
```

### 3. Buffered or Streamed

This is used in situations where a large document may be read in off of disk and
processed. Also, many codepages take multiple bytes to encode a character, or
have state. These factors make it impossible to convert arbitrary chunks of data
without maintaining state across chunks. Even conversion from Unicode may
encounter a leading surrogate at the end of one buffer, which needs to be paired
with the trailing surrogate in the next buffer.

A basic API principle of the ICU to/from Unicode functions is that they will
ALWAYS attempt to consume all of the input (source) data, unless the output
buffer is full or some other error occurs. In other words, there is no need to
ever test whether all of the source data has been consumed.

The basic loop that is used with the ICU buffer conversion routines is the same
in the to and from Unicode directions. In the following pseudocode, either
'source' (for fromUnicode) or 'target' (for toUnicode) are UTF-16 UChars.

```c
UErrorCode err = U_ZERO_ERROR;

while (... /*input data available*/ ) {
    ... /* read input data into buffer */
    
    source = ... /* beginning of read data */;
    sourceLimit = source + readLength; // end + 1

    UBool flush = (further input data still available) // (i.e. feof())

    /* loop until all source has been processed */
    do {
        /* set up target pointers */
        target = ... /* beginning of output buffer */;
        targetLimit = target + sizeOfOutput;

        err = U_ZERO_ERROR; /* so that the to/from does not fail */

        ucnv_to/fromUnicode(converter, &target, targetLimit,
                    &source, sourceLimit, NULL, flush, &err);

        ... /* write (target-beginningOfOutputBuffer) items
               starting at beginning of output buffer */
    } while (err == U_BUFFER_OVERFLOW_ERROR);
    if(U_FAILURE(error)) {
        ... /* process error */
        break; /* out of the 'while' loop that reads source data */
    }
}
/* loop to read input data */
if(U_FAILURE(error)) {
    ... /* process error further */
}
```

The above code optimizes for processing entire chunks of input data. An
efficient size for the output buffer can be calculated as follows. (in bytes):

```c
ucnv_getMinCharSize() * inputBufferSize * sizeof(UChar)
ucnv_getMaxCharSize() * inputBufferSize
```

There are two loops used, an outer and an inner. The outer loop fetches input
data to keep the source buffer full, and the inner loop 'writes' out data to
keep the output buffer empty.

Note that while this efficiently handles data on the input side, there are some
cases where the size of the output buffer is fixed. For instance, in network
applications it is sometimes desirable to fill every output packet completely
(not including the last packet in the sequence). The above loop does not ensure
that every output buffer is completely full. For example, if a 4 UChar input
buffer was used, and a 3 byte output buffer with `fromUnicode()`, the loop would
typically write 3 bytes, then 1, then 3, and so on. If, instead of efficient use
of the input data, the goal is filling output buffers, a slightly different loop
can be used.

In such a scenario, the inner write does not occur unless a buffer overflow
occurs OR 'flush' is true. So, the 'write' and resetting of the target and
targetLimit pointers would only happen
`if (err == U_BUFFER_OVERFLOW_ERROR || flush == TRUE)`

The flush parameter on each conversion call should be set to FALSE, until the
conversion call is called for the last time for the buffer. This is because the
conversion is stateful. On the last conversion call, the flush parameter should
be set to TRUE. More details are mentioned in the API reference in
[ucnv.h](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/ucnv_8h.html) .

### 4. Pre-flighting

Preflighting is the process of asking the conversion API for the size of target
buffer required. (For a more general discussion, see the Preflighting section
(§) in the [Strings](../strings/index.md) chapter.)

This is accomplished by calling the `ucnv_fromUChars` and `ucnv_toUChars` functions.

```c
UChar uchar2;
char input_char_buffer = "This is some text";

targetsize = ucnv_toUChars(myConverter, NULL, targetcapacity,
                           input_char_buffer, sizeof(input_char_buffer), &err);

if(err==U_BUFFER_OVERFLOW_ERROR) {
    err=U_ZERO_ERROR;
    uchar2=(UChar*)malloc((targetsize) * sizeof(UChar));
    targetsize = ucnv_toUChars(myConverter, uchar2, targetsize,
                               input_char_buffer, sizeof(input_char_buffer), &err);
    if(U_FAILURE(err)) {
        printf("ucnv_toUChars() FAILED %s\n", myErrorName(err));
    }
    else {
        printf("ucnv_toUChars() o.k.\n");
    }
}
```

> :point_right: **Note**: *This is inefficient since the conversion is performed
> **twice**, once for finding the size of target and once for writing to the target*.

### 5. Convenience

ICU provides some convenience functions for conversions:

```c
ucnv_toUChars(myConverter, target_uchars, targetsize,
              input_char_buffer, sizeof(input_char_buffer), &err);
ucnv_fromUChars(cnv, cTarget, (cTargetLimit-cTarget),
                uSource, (uSourceLimit-uSource), &errorCode);

char target[100];
UnicodeString str("ABCDEF", "iso-8859-1");
int32_t targetsize = str.extract(0, str.length(), target, sizeof(target), "SJIS");
target[targetsize] = 0; /* NULL termination */
```

## Conversion Examples

See the [ICU Conversion Examples](https://github.com/unicode-org/icu/blob/master/icu4c/source/samples/ucnv/convsamp.cpp) for more information.
