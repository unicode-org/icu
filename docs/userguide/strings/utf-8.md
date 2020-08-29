---
layout: default
title: UTF-8
nav_order: 1
parent: Chars and Strings
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# UTF-8

*Note: This page is only relevant for C/C++. In Java, all strings are encoded in
UTF-16, except for conversion from bytes to strings (via InputStreamReader or
similar) and from strings to bytes (OutputStreamWriter etc.).*

While most of ICU works with UTF-16 strings and uses data structures optimized
for UTF-16, there are APIs that facilitate working with UTF-8, or are optimized
for UTF-8, or work with Unicode code points (21-bit integer values) regardless
of string encoding. Some data structures are designed to work equally well with
UTF-16 and UTF-8.

For UTF-8 strings, ICU normally uses `(const) char *` pointers and `int32_t`
lengths, normally with semantics parallel to UTF-16 handling. (Input length=-1
means NUL-terminated, output is NUL-terminated if there is space, output
overflow is handled with preflighting; for details see the parent [Strings
page](index.md).) Some newer APIs take an `icu::StringPiece` argument and write
to an `icu::ByteSink` or to a string class object like `std::string`.

## Conversion Between UTF-8 and UTF-16

The simplest way to use UTF-8 strings in UTF-16 APIs is via the C++
`icu::UnicodeString` methods `fromUTF8(const StringPiece &utf8)` and
`toUTF8String(StringClass &result)`. There is also `toUTF8(ByteSink &sink)`.

In C, `unicode/ustring.h` has functions like `u_strFromUTF8WithSub()` and
`u_strToUTF8WithSub()`. (Also `u_strFromUTF8()`, `u_strToUTF8()` and
`u_strFromUTF8Lenient()`.)

The conversion functions in `unicode/ucnv.h` are intended for very flexible
handling of conversion to/from external byte streams (with customizable error
handling and support for split buffers at arbitrary boundaries) which is
normally unnecessary for internal strings.

Note: `icu::``UnicodeString` has constructors, `setTo()` and `extract()` methods
which take either a converter object or a charset name. These can be used for
UTF-8, but are not as efficient or convenient as the
`fromUTF8()`/`toUTF8()`/`toUTF8String()` methods mentioned above. (Among
conversion methods, APIs with a charset name are more convenient but internally
open and close a converter; ones with a converter object parameter avoid this.)

## UTF-8 as Default Charset

ICU has many functions that take or return `char *` strings that are assumed to
be in the default charset which should match the system encoding. Since this
could be one of many charsets, and the charset can be different for different
processes on the same system, ICU uses its conversion framework for converting
to and from UTF-16.

If it is known that the default charset is always UTF-8 on the target platform,
then you should `#define`` U_CHARSET_IS_UTF8 1` in or before `unicode/utypes.h`.
(For example, modify the default value there or pass `-D``U_CHARSET_IS_UTF8=1`
as a compiler flag.) This will change most of the implementation code to use
dedicated (simpler, faster) UTF-8 code paths and avoid dependencies on the
conversion framework. (Avoiding such dependencies helps with statically linked
libraries and may allow the use of `UCONFIG_NO_LEGACY_CONVERSION` or even
`UCONFIG_NO_CONVERSION` \[see `unicode/uconfig.h`\].)

## Low-Level UTF-8 String Operations

`unicode/utf8.h` defines macros for UTF-8 with semantics parallel to the UTF-16
macros in `unicode/utf16.h`. The macros handle many cases inline, but call
internal functions for complicated parts of the UTF-8 encoding form. For
example, the following code snippet counts white space characters in a string:

```c
#include "unicode/utypes.h"
#include "unicode/stringpiece.h"
#include "unicode/utf8.h"
#include "unicode/uchar.h"

int32_t countWhiteSpace(StringPiece sp) {
    const char *s=sp.data();
    int32_t length=sp.length();
    int32_t count=0;
    for(int32_t i=0; i<length;) {
        UChar32 c;
        U8_NEXT(s, i, length, c);
        if(u_isUWhiteSpace(c)) {
            ++count;
        }
    }
    return count;
}
```

## Dedicated UTF-8 APIs

ICU has some APIs dedicated for UTF-8. They tend to have been added for "worker
functions" like comparing strings, to avoid the string conversion overhead,
rather than for "builder functions" like factory methods and attribute setters.

For example, `icu::Collator::compareUTF8()` compares two UTF-8 strings
incrementally, without converting all of the two strings to UTF-16 if there is
an early base letter difference.

`ucnv_convertEx()` can convert between UTF-8 and another charset, if one of the
two `UConverter`s is a UTF-8 converter. The conversion *from UTF-8 to* most
other charsets uses a dedicated, optimized code path, avoiding the pivot through
UTF-16. (Conversion *from* other charsets *to UTF-8* could be optimized as well,
but that has not been implemented yet as of ICU 4.4.)

Other examples: (This list may or may not be complete.)

*   ucasemap_utf8ToLower(), ucasemap_utf8ToUpper(), ucasemap_utf8ToTitle(),
    ucasemap_utf8FoldCase()
*   ucnvsel_selectForUTF8()
*   icu::UnicodeSet::spanUTF8(), spanBackUTF8() and uset_spanUTF8(),
    uset_spanBackUTF8() (These are highly optimized for UTF-8 processing.)
*   ures_getUTF8String(), ures_getUTF8StringByIndex(), ures_getUTF8StringByKey()
*   uspoof_checkUTF8(), uspoof_areConfusableUTF8(), uspoof_getSkeletonUTF8()

## Abstract Text APIs

ICU offers several interfaces for text access, designed for different use cases.
(Some interfaces are simply newer and more modern than others.) Some ICU
services work with some of these interfaces, and for some of these interfaces
ICU offers UTF-8 implementations out of the box.

`UText` can be used with `BreakIterator` APIs (character/word/sentence/...
segmentation). `utext_openUTF8()` creates a read-only `UText` for a UTF-8
string.

*   *Note: In ICU 4.4 and before, BreakIterator only works with UTF-8 (or any
    other charset with non-1:1 index conversion to UTF-16) if no dictionary is
    supported. This excludes Thai word break. See [ticket #5532](https://unicode-org.atlassian.net/browse/ICU-5532).*
*   *As a workaround for Thai word breaking, you can convert the string to
    UTF-16 and convert indexes to UTF-8 string indexes via
    `u_strToUTF8(dest=NULL, destCapacity=0, *destLength gets UTF-8 index).`*
*   *ICU 4.4 has a technology preview for UText in the regular expression API,
    but some of the UText regex API and semantics are likely to change for ICU
    4.6. (Especially indexing semantics.)*

A `UCharIterator` can be used with several collation APIs (although there is
also the newer `icu::Collator::compareUTF8()`) and with `u_strCompareIter()`.
`uiter_setUTF8()` creates a UCharIterator for a UTF-8 string.

It is also possible to create a `CharacterIterator` subclass for UTF-8 strings,
but `CharacterIterator` has a lot of virtual methods and it requires UTF-16
string index semantics.
