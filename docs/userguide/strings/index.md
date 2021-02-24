---
layout: default
title: Chars and Strings
nav_order: 3
has_children: true
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Strings

## Overview

This section explains how to handle Unicode strings with ICU in C and C++.

Sample code is available in the ICU source code library at
[icu/source/samples/ustring/ustring.cpp](https://github.com/unicode-org/icu/blob/master/icu4c/source/samples/ustring/ustring.cpp)
.

## Text Access Overview

Strings are the most common and fundamental form of handling text in software.
Logically, and often physically, they contain contiguous arrays (vectors) of
basic units. Most of the ICU API functions work directly with simple strings,
and where possible, this is preferred.

Sometimes, text needs to be accessed via more powerful and complicated methods.
For example, text may be stored in discontiguous chunks in order to deal with
frequent modification (like typing) and large amounts, or it may not be stored
in the internal encoding, or it may have associated attributes like bold or
italic styles.

### Guidance

ICU provides multiple text access interfaces which were added over time. If
simple strings cannot be used, then consider the following:

1.  [UText](utext.md): Added in ICU4C 3.4 as a technology preview. Intended to
    be the strategic text access API for use with ICU. C API, high performance,
    writable, supports native indexes for efficient non-UTF-16 text storage. So
    far (3.4) only supported in BreakIterator. Some API changes are anticipated
    for ICU 3.6.

2.  Replaceable (Java & C++) and UReplaceable (C): Writable, designed for use
    with Transliterator.

3.  CharacterIterator (Java JDK & C++): Read-only, used in many APIs. Large
    differences between the JDK and C++ versions.

4.  UCharacterIterator (Java): Back-port of the C++ CharacterIterator to ICU4J
    for support of supplementary code points and post-increment iteration.

5.  UCharIterator (C): Read-only, C interface used mostly in incremental
    normalization and collation.

The following provides some historical perspective and comparison between the
interfaces.

### CharacterIterator

ICU has long provided the CharacterIterator interface for some services. It
allows for abstract text access, but has limitations:

1.  It has a per-character function call overhead.

2.  Originally, it was designed for UCS-2 operation and did not support direct
    handling of supplementary Unicode code points. Such support was later added.

3.  Its pre-increment iteration semantics are uncommon, and are inefficient when
    used with a variable-width encoding form (UTF-16). Functions for
    post-increment iteration were added later.

4.  The C++ version added iteration start/limit boundaries only because the C++
    UnicodeString copies string contents during substringing; the Java
    CharacterIterator does not have these extra boundaries – substringing is
    more efficient in Java.

5.  CharacterIterator is not available for use in C.

6.  CharacterIterator is a read-only interface.

7.  It uses UTF-16 indexes into the text, which is not efficient for other
    encoding forms.

8.  With the additions to the API over time, the number of methods that have to
    be overridden by subclasses has become rather large.

The core Java adopted an early version of CharacterIterator; later
functionality, like support for supplementary code points, was back-ported from
ICU4C to ICU4J to form the UCharacterIterator class.

The UCharIterator C interface was added to allow for incremental normalization
and collation in C. It is entirely code unit (UChar)-oriented, uses only
post-increment iteration and has a smaller number of overridable methods.

### Replaceable

The Replaceable (Java & C++) and UReplaceable (C) interfaces are designed for,
and used in, Transliterator. They are random-access interfaces, not iterators.

### UText

The [UText](utext.md) text access interface was designed as a possible
replacement for all previous interfaces listed above, with additional
functionality. It allows for high-performance operation through the use of
storage-native indexes (for efficient use of non-UTF-16 text) and through
accessing multiple characters per function call. Code point iteration is
available with functions as well as with C macros, for maximum performance.
UText is also writable, mostly patterned after Replaceable. For details see the
UText chaper.

## Strings in ICU

### Strings in Java

In Java, ICU uses the standard String and StringBuffer classes, `char[]`, etc.
See the Java documentation for details.

### Strings in C/C++

Strings in C and C++ are, at the lowest level, arrays of some particular base
type. In most cases, the base type is a char, which is an 8-bit byte in modern
compilers. Some APIs use a "wide character" type wchar_t that is typically 8,
16, or 32 bits wide and upwards compatible with char. C code passes `char *` or
wchar_t pointers to the first element of an array. C++ enables you to create a
class for encapsulating these kinds of character arrays in handy and safe
objects.

The interpretation of the byte or wchar_t values depends on the platform, the
compiler, the signed state of both char and wchar_t, and the width of wchar_t.
These characteristics are not specified in the language standards. When using
internationalized text, the encoding often uses multiple chars for most
characters and a wchar_t that is wide enough to hold exactly one character code
point value each. Some APIs, especially in the standard library (stdlib), assume
that wchar_t strings use a fixed-width encoding with exactly one character code
point per wchar_t.

### ICU: 16-bit Unicode strings

In order to take advantage of Unicode with its large character repertoire and
its well-defined properties, there must be types with consistent definitions and
semantics. The Unicode standard defines a default encoding based on 16-bit code
units. This is supported in ICU by the definition of the UChar to be an unsigned
16-bit integer type. This is the base type for character arrays for strings in
ICU.

> :point_right: **Note**: *Endianness is not an issue on this level because the interpretation of an
integer is fixed within any given platform.*

With the UTF-16 encoding form, a single Unicode code point is encoded with
either one or two 16-bit UChar code units (unambiguously). "Supplementary" code
points, which are encoded with pairs of code units, are rare in most texts. The
two code units are called "surrogates", and their unit value ranges are distinct
from each other and from single-unit value ranges. Code should be generally
optimized for the common, single-unit case.

16-bit Unicode strings in internal processing contain sequences of 16-bit code
units that may not always be well-formed UTF-16. ICU treats single, unpaired
surrogates as surrogate code points, i.e., they are returned in per-code point
iteration, they are included in the number of code points of a string, and they
are generally treated much like normal, unassigned code points in most APIs.
Surrogate code points have Unicode properties although they cannot be assigned
an actual character.

ICU string handling functions (including append, substring, etc.) do not
automatically protect against producing malformed UTF-16 strings. Most of the
time, indexes into strings are naturally at code point boundaries because they
result from other functions that always produce such indexes. If necessary, the
user can test for proper boundaries by checking the code unit values, or adjust
arbitrary indexes to code point boundaries by using the C macros
U16_SET_CP_START() and U16_SET_CP_LIMIT() (see utf.h) and the UnicodeString
functions getChar32Start() and getChar32Limit().

UTF-8 and UTF-32 are supported with converters (ucnv.h), macros (utf.h), and
convenience functions (ustring.h), but only a subset of APIs works with UTF-8
directly as string encoding form.

**See the [UTF-8](utf-8.md) subpage for details about working with
UTF-8.** Some of the following sections apply to UTF-8 APIs as well; for example
sections about handling lengths and overflows.

### Separate type for single code points

A Unicode code point is an integer with a value from 0 to 0x10FFFF. ICU 2.4 and
later defines the UChar32 type for single code point values as a 32 bits wide
signed integer (int32_t). This allows the use of easily testable negative values
as sentinels, to indicate errors, exceptions or "done" conditions. All negative
values and positive values greater than 0x10FFFF are illegal as Unicode code
points.

ICU 2.2 and earlier defined UChar32 depending on the platform: If the compiler's
wchar_t was 32 bits wide, then UChar32 was defined to be the same as wchar_t.
Otherwise, it was defined to be an unsigned 32-bit integer. This means that
UChar32 was either a signed or unsigned integer type depending on the compiler.
This was meant for better interoperability with existing libraries, but was of
little use because ICU does not process 32-bit strings — UChar32 is only used
for single code points. The platform dependence of UChar32 could cause problems
with C++ function overloading.

### Compiler-dependent definitions

The compiler's and the runtime character set's codepage encodings are not
specified by the C/C++ language standards and are usually not a Unicode encoding
form. They typically depend on the settings of the individual system, process,
or thread. Therefore, it is not possible to instantiate a Unicode character or
string variable directly with C/C++ character or string literals. The only safe
way is to use numeric values. It is not an issue for User Interface (UI) strings
that are translated. These UI strings are loaded from a resource bundle, which
is generated from a text file that can be in Unicode or in any other
ICU-provided codepage. The binary form of the genrb tool generates UTF-16
strings that are ready for direct use.

There is a useful exception to this for program-internal strings and test
strings. Within each "family" of character encodings, there is a set of
characters that have the same numeric code values. Such characters include Latin
letters, the basic digits, the space, and some punctuation. Most of the ASCII
graphic characters are invariant characters. The same set, with different but
again consistent numeric values, is invariant among almost all EBCDIC codepages.
For details, see
[icu4c/source/common/unicode/utypes.h](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/utypes_8h.html)
. With strings that contain only these invariant characters, it is possible to
use efficient ICU constructs to write a C/C++ string literal and use it to
initialize Unicode strings.

In some APIs, ICU uses `char *` strings. This is either for file system paths or
for strings that contain invariant characters only (such as locale identifiers).
These strings are in the platform-specific encoding of either ASCII or EBCDIC.
All other codepage differences do not matter for invariant characters and are
manipulated by the C stdlib functions like strcpy().

In some APIs where identifiers are used, ICU uses `char *` strings with invariant
characters. Such strings do not require the full Unicode repertoire and are
easier to handle in C and C++ with `char *` string literals and standard C
library functions. Their useful character repertoire is actually smaller than
the set of graphic ASCII characters; for details, see
[utypes.h](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/utypes_8h.html) . Examples of
`char *` identifier uses are converter names, locale IDs, and resource bundle
table keys.

There is another, less efficient way to have human-readable Unicode string
literals in C and C++ code. ICU provides a small number of functions that allow
any Unicode characters to be inserted into a string with escape sequences
similar to the one that is used in the C and C++ language. In addition to the
familiar \\n and \\xhh etc., ICU also provides the \\uhhhh syntax with four hex
digits and the \\Uhhhhhhhh syntax with eight hex digits for hexadecimal Unicode
code point values. This is very similar to the newer escape sequences used in
Java and defined in the latest C and C++ standards. Since ICU is not a compiler
extension, the "unescaping" is done at runtime and the backslash itself must be
escaped (duplicated) so that the compiler does not attempt to "unescape" the
sequence itself.

## Handling Lengths, Indexes, and Offsets in Strings

The length of a string and all indexes and offsets related to the string are
always counted in terms of UChar code units, not in terms of UChar32 code
points. (This is the same as in common C library functions that use `char *`
strings with multi-byte encodings.)

Often, a user thinks of a "character" as a complete unit in a language, like an
'Ä', while it may be represented with multiple Unicode code points including a
base character and combining marks. (See the Unicode standard for details.) This
often requires users to index and pass strings (UnicodeString or `UChar *`) with
multiple code units or code points. It cannot be done with single-integer
character types. Indexing of such "characters" is done with the BreakIterator
class (in C: ubrk_ functions).

Even with such "higher-level" indexing functions, the actual index values will
be expressed in terms of UChar code units. When more than one code unit is used
at a time, the index value changes by more than one at a time.

ICU uses signed 32-bit integers (int32_t) for lengths and offsets. Because of
internal computations, strings (and arrays in general) are limited to 1G base
units or 2G bytes, whichever is smaller.

## Using C Strings: NUL-Terminated vs. Length Parameters

Strings are either terminated with a NUL character (code point 0, U+0000) or
their length is specified. In the latter case, it is possible to have one or
more NUL characters inside the string.

**Input string** arguments are typically passed with two parameters: The (const)
`UChar *` pointer and an int32_t length argument. If the length is -1 then the
string must be NUL-terminated and the ICU function will call the u_strlen()
method or treat it equivalently. If the input string contains embedded NUL
characters, then the length must be specified.

**Output string** arguments are typically passed with a destination `UChar *`
pointer and an int32_t capacity argument and the function returns the length of
the output as an int32_t. There is also almost always a UErrorCode argument.
Essentially, a `UChar[]` array is passed in with its start and the number of
available UChars. The array is filled with the output and if space permits the
output will be NUL-terminated. The length of the output string is returned. In
all cases the length of the output string does not include the terminating NUL.
This is the same behavior found in most ICU and non-ICU string APIs, for example
u_strlen(). The output string may **contain** NUL characters as part of its
actual contents, depending on the input and the operation. Note that the
UErrorCode parameter is used to indicate both errors and warnings (non-errors).
The following describes some of the situations in which the UErrorCode will be
set to a non-zero value:

1.  If the output length is greater than the output array capacity, then the
    UErrorCode will be set to U_BUFFER_OVERFLOW_ERROR and the contents of the
    output array is undefined.

2.  If the output length is equal to the capacity, then the output has been
    completely written minus the terminating NUL. This is also indicated by
    setting the UErrorCode to U_STRING_NOT_TERMINATED_WARNING.
    Note that U_STRING_NOT_TERMINATED_WARNING does not indicate failure (it
    passes the U_SUCCESS() macro).
    Note also that it is more reliable to check the output length against the
    capacity, rather than checking for the warning code, because warning codes
    do not cause the early termination of a function and may subsequently be
    overwritten.

3.  If neither of these two conditions apply, the error code will indicate
    success and not a U_STRING_NOT_TERMINATED_WARNING. (If a
    U_STRING_NOT_TERMINATED_WARNING code had been set in the UErrorCode
    parameter before the function call, then it is reset to a U_ZERO_ERROR.)

**Preflighting:** The returned length is always the full output length even if
the output buffer is too small. It is possible to pass in a capacity of 0 (and
an output array pointer of NUL) for "pure preflighting" to determine the
necessary output buffer size. Add one to make the output string NUL-terminated.

Note that — whether the caller intends to "preflight" or not — if the output
length is equal to or greater than the capacity, then the UErrorCode is set to
U_STRING_NOT_TERMINATED_WARNING or U_BUFFER_OVERFLOW_ERROR respectively, as
described above.

However, "pure preflighting" is very expensive because the operation has to be
processed twice — once for calculating the output length, and a second time to
actually generate the output. It is much more efficient to always provide an
output buffer that is expected to be large enough for most cases, and to
reallocate and repeat the operation only when an overflow occurred. (Remember to
reset the UErrorCode to U_ZERO_ERROR before calling the function again.) In
C/C++, the initial output buffer can be a stack buffer. In case of a
reallocation, it may be possible and useful to cache and reuse the new, larger
buffer.

> :point_right: **Note**:*The exception to these rules are the ANSI-C-style functions like u_strcpy(),
which generally require NUL-terminated strings, forbid embedded NULs, and do not
take capacity arguments for buffer overflow checking.*

## Using Unicode Strings in C

In C, Unicode strings are similar to standard `char *` strings. Unicode strings
are arrays of UChar and most APIs take a `UChar *` pointer to the first element
and an input length and/or output capacity, see above. ICU has a number of
functions that provide the Unicode equivalent of the stdlib functions such as
strcpy(), strstr(), etc. Compared with their C standard counterparts, their
function names begin with u_. Otherwise, their semantics are equivalent. These
functions are defined in icu/source/common/unicode/ustring.h.

### Code Point Access

Sometimes, Unicode code points need to be accessed in C for iteration, movement
forward, or movement backward in a string. A string might also need to be
written from code points values. ICU provides a number of macros that are
defined in the icu/source/common/unicode/utf.h and utf8.h/utf16.h headers that
it includes (utf.h is in turn included with utypes.h).

Macros for 16-bit Unicode strings have a U16_ prefix. For example:

    U16_NEXT(s, i, length, c)
    U16_PREV(s, start, i, c)
    U16_APPEND(s, i, length, c, isError)

There are also macros with a U_ prefix for code point range checks (e.g., test
for non-character code point), and U8_ macros for 8-bit (UTF-8) strings. See the
header files and the API References for more details.

#### UTF Macros before ICU 2.4

In ICU 2.4, the utf\*.h macros have been revamped, improved, simplified, and
renamed. The old macros continue to be available. They are in utf_old.h,
together with an explanation of the change. utf.h, utf8.h and utf16.h contain
the new macros instead. The new macros are intended to be more consistent, more
useful, and less confusing. Some macros were simply renamed for consistency with
a new naming scheme.

The documentation of the old macros has been removed. If you need it, see a User
Guide version from ICU 4.2 or earlier (see the [download
page](http://site.icu-project.org/download)).

C Unicode String Literals

There is a pair of macros that together enable users to instantiate a Unicode
string in C — a `UChar []` array — from a C string literal:

    /*
    * In C, we need two macros: one to declare the UChar[] array, and
    * one to populate it; the second one is a noop on platforms where
    * wchar_t is compatible with UChar and ASCII-based.
    * The length of the string literal must be counted for both macros.
    */
    /* declare the invString array for the string */
    U_STRING_DECL(invString, "such characters are safe 123 %-.", 32);
    /* populate it with the characters */
    U_STRING_INIT(invString, "such characters are safe 123 %-.", 32);

With invariant characters, it is also possible to efficiently convert `char *`
strings to and from UChar \ strings:

    static const char *cs1="such characters are safe 123 %-.";
    static UChar us1[40];
    static char cs2[40];
    u_charsToUChars(cs1, us1, 33); /* include the terminating NUL */
    u_UCharsToChars(us1, cs2, 33);

## Testing for well-formed UTF-16 strings

It is sometimes useful to test if a 16-bit Unicode string is well-formed UTF-16,
that is, that it does not contain unpaired surrogate code units. For a boolean
test, call a function like u_strToUTF8() which sets an error code if the input
string is malformed. (Provide a zero-capacity destination buffer and treat the
buffer overflow error as "is well-formed".) If you need to know the position of
the unpaired surrogate, you can iterate through the string with U16_NEXT() and
U_IS_SURROGATE().

## Using Unicode Strings in C++

[UnicodeString](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classUnicodeString.html) is
a C++ string class that wraps a UChar array and associated bookkeeping. It
provides a rich set of string handling functions.

UnicodeString combines elements of both the Java String and StringBuffer
classes. Many UnicodeString functions are named and work similar to Java String
methods but modify the object (UnicodeString is "mutable").

UnicodeString provides functions for random access and use (insert/append/find
etc.) of both code units and code points. For each non-iterative string/code
point macro in utf.h there is at least one UnicodeString member function. The
names of most of these functions contain "32" to indicate the use of a UChar32.

Code point and code unit iteration is provided by the
[CharacterIterator](characteriterator.md) abstract class and its subclasses.
There are concrete iterator implementations for UnicodeString objects and plain
`UChar []` arrays.

Most UnicodeString constructors and functions do not have a UErrorCode
parameter. Instead, if the construction of a UnicodeString fails, for example
when it is constructed from a NULL `UChar *` pointer, then the UnicodeString
object becomes "bogus". This can be tested with the isBogus() function. A
UnicodeString can be put into the "bogus" state explicitly with the setToBogus()
function. This is different from an empty string (although a "bogus" string also
returns TRUE from isEmpty()) and may be used equivalently to NULL in `UChar *` C
APIs (or null references in Java, or NULL values in SQL). A string remains
"bogus" until a non-bogus string value is assigned to it. For complete details
of the behavior of "bogus" strings see the description of the setToBogus()
function.

Some APIs work with the
[Replaceable](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classReplaceable.html)
abstract class. It defines a simple interface for random access and text
modification and is useful for operations on text that may have associated
meta-data (e.g., styled text), especially in the Transliterator API.
UnicodeString implements Replaceable.

### C++ Unicode String Literals

Like in C, there are macros that enable users to instantiate a UnicodeString
from a C string literal. One macro requires the length of the string as in the C
macros, the other one implies a strlen().

    UnicodeString s1=UNICODE_STRING("such characters are safe 123 %-.", 32);
    UnicodeString s1=UNICODE_STRING_SIMPLE("such characters are safe 123 %-.");

It is possible to efficiently convert between invariant-character strings and
UnicodeStrings by using constructor, setTo() or extract() overloads that take
codepage data (`const char *`) and specifying an empty string ("") as the
codepage name.

## Using C++ Strings in C APIs

The internal buffer of UnicodeString objects is available for direct handling in
C (or C-style) APIs that take `UChar *` arguments. It is possible but usually not
necessary to copy the string contents with one of the extract functions. The
following describes several direct buffer access methods.

The UnicodeString function getBuffer() const returns a readonly const `UChar *`.
The length of the string is indicated by UnicodeString's length() function.
Generally, UnicodeString does not NUL-terminate the contents of its internal
buffer. However, it is possible to check for a NUL character if the length of
the string is less than the capacity of the buffer. The following code is an
example of how to check the capacity of the buffer:
`(s.length()<s.getCapacity() && buffer[s.length()]==0)`

An easier way to NUL-terminate the buffer and get a `const UChar *` pointer to it
is the getTerminatedBuffer() function. Unlike getBuffer() const,
getTerminatedBuffer() is not a const function because it may have to (reallocate
and) modify the buffer to append a terminating NUL. Therefore, use getBuffer()
const if you do not need a NUL-terminated buffer.

There is also a pair of functions that allow controlled write access to the
buffer of a UnicodeString: `UChar *getBuffer(int32_t minCapacity)` and
`releaseBuffer(int32_t newLength)`. `UChar *getBuffer(int32_t minCapacity)`
provides a writeable buffer of at least the requested capacity and returns a
pointer to it. The actual capacity of the buffer after the
`getBuffer(minCapacity)` call may be larger than the requested capacity and can be
determined with `getCapacity()`.

Once the buffer contents are modified, the buffer must be released with the
`releaseBuffer(int32_t newLength)` function, which sets the new length of the
UnicodeString (newLength=-1 can be passed to determine the length of
NUL-terminated contents like `u_strlen()`).

Between the `getBuffer(minCapacity)` and `releaseBuffer(newLength)` function calls,
the contents of the UnicodeString is unknown and the object behaves like it
contains an empty string. A nested `getBuffer(minCapacity)`, `getBuffer() const` or
`getTerminatedBuffer()` will fail (return NULL) and modifications of the string
via UnicodeString member functions will have no effect. Copying a string with an
"open buffer" yields an empty copy. The move constructor, move assignment
operator and Return Value Optimization (RVO) transfer the state, including the
open buffer.

See the UnicodeString API documentation for more information.

## Using C Strings in C++ APIs

There are efficient ways to wrap C-style strings in C++ UnicodeString objects
without copying the string contents. In order to use C strings in C++ APIs, the
`UChar *` pointer and length need to be wrapped into a UnicodeString. This can be
done efficiently in two ways: With a readonly alias and a writable alias. The
UnicodeString object that is constructed actually uses the `UChar *` pointer as
its internal buffer pointer instead of allocating a new buffer and copying the
string contents.

If the original string is a readonly `const UChar *`, then the UnicodeString must
be constructed with a read only alias. If the original string is a writable
(non-const) `UChar *` and is to be modified (e.g., if the `UChar *` buffer is an
output buffer) then the UnicodeString should be constructed with a writeable
alias. For more details see the section "Maximizing Performance with the
UnicodeString Storage Model" and search the unistr.h header file for "alias".

## Maximizing Performance with the UnicodeString Storage Model

UnicodeString uses four storage methods to maximize performance and minimize
memory consumption:

1.  Short strings are normally stored inside the UnicodeString object. The
    object has fields for the "bookkeeping" and a small UChar array. When the
    object is copied, the internal characters are copied into the destination
    object.
2.  Longer strings are normally stored in allocated memory. The allocated UChar
    array is preceded by a reference counter. When the string object is copied,
    the allocated buffer is shared by incrementing the reference counter. If any
    of the objects that share the same string buffer are modified, they receive
    their own copy of the buffer and decrement the reference counter of the
    previously co-used buffer.
3.  A UnicodeString can be constructed (or set with a setTo() function) so that
    it aliases a readonly buffer instead of copying the characters. In this
    case, the string object uses this aliased buffer for as long as the object
    is not modified and it will never attempt to modify or release the buffer.
    This model has copy-on-write semantics. For example, when the string object
    is modified, the buffer contents are first copied into writable memory
    (inside the object for short strings or the allocated buffer for longer
    strings). When a UnicodeString with a readonly setting is copied to another
    UnicodeString using the fastCopyFrom() function, then both string objects
    share the same readonly setting and point to the same storage. Copying a
    string with the normal assignment operator or copy constructor will copy the
    buffer. This prevents accidental misuse of readonly-aliased strings. (This
    is new in ICU 2.4; earlier, the assignment operator and copy constructor
    behaved like the new fastCopyFrom() does now.)
    **Important:**
    1.  The aliased buffer must remain valid for as long as any UnicodeString
        object aliases it. This includes unmodified fastCopyFrom()and
        `movedFrom()` copies of the object (including moves via the move
        constructor and move assignment operator), and when the compiler uses
        Return Value Optimization (RVO) where a function returns a UnicodeString
        by value.
    2.  Be prepared that return-by-value may either make a copy (which does not
        preserve aliasing), or moves the value or uses RVO (which do preserve
        aliasing).
    3.  It is an error to readonly-alias temporary buffers and then pass the
        resulting UnicodeString objects (or references/pointers to them) to APIs
        that store them for longer than the buffers are valid.
    4.  If it is necessary to make sure that a string is not a readonly alias,
        then use any modifying function without actually changing the contents
        (for example, s.setCharAt(0, s.charAt(0))).
    5.  In ICU 2.4 and later, a simple assignment or copy construction will also
        copy the buffer.
4.  A UnicodeString can be constructed (or set with a setTo() function) so that
    it aliases a writable buffer instead of copying the characters. The
    difference from the above is that the string object writes through to this
    aliased buffer for write operations. A new buffer is allocated and the
    contents are copied only when the capacity of the buffer is not sufficient.
    An efficient way to get the string contents into the original buffer is to
    use the `extract(..., UChar *dst, ...)` function.
    The `extract(..., UChar *dst, ...)` function copies the string contents only if the dst buffer is
    different from the buffer of the string object itself. If a string grows and
    shrinks during a sequence of operations, then it will not use the same
    buffer, even if the string would fit. When a UnicodeString with a writeable
    alias is assigned to another UnicodeString, the contents are always copied.
    The destination string will not point to the buffer that the source string
    aliases point to. However, a move constructor, move assignment operator, and
    Return Value Optimization (RVO) do preserve aliasing.

In general, UnicodeString objects have "copy-on-write" semantics. Several
objects may share the same string buffer, but a modification only affects the
object that is modified itself. This is achieved by copying the string contents
if it is not owned exclusively by this one object. Only after that is the object
modified.

Even though it is fairly efficient to copy UnicodeString objects, it is even
more efficient, if possible, to work with references or pointers. Functions that
output strings can be faster by appending their results to a UnicodeString that
is passed in by reference, compared with returning a UnicodeString object or
just setting the local results alone into a string reference.

> :point_right: **Note**: *UnicodeStrings can be copied in a thread-safe manner by just using their
standard copy constructors and assignment operators. fastCopyFrom() is also
thread-safe, but if the original string is a readonly alias, then the copy
shares the same aliased buffer.*

## Using UTF-8 strings with ICU

As mentioned in the overview of this chapter, ICU and most other
Unicode-supporting software uses 16-bit Unicode for internal processing.
However, there are circumstances where UTF-8 is used instead. This is usually
the case for software that does little or no processing of non-ASCII characters,
and/or for APIs that predate Unicode, use byte-based strings, and cannot be
changed or replaced for various reasons.

A common perception is that UTF-8 has an advantage because it was designed for
compatibility with byte-based, ASCII-based systems, although it was designed for
string storage (of Unicode characters in Unix file names) rather than for
processing performance.

While ICU mostly does not natively use UTF-8 strings, there are many ways to
work with UTF-8 strings and ICU. For more information see the newer
[UTF-8](utf-8.md) subpage.

## Using UTF-32 strings with ICU

It is even rarer to use UTF-32 for string processing than UTF-8. While 32-bit
Unicode is convenient because it is the only fixed-width UTF, there are few or
no legacy systems with 32-bit string processing that would benefit from a
compatible format, and the memory bandwidth requirements of UTF-32 diminish the
performance and handling advantage of the fixed-width format.

Over time, the wchar_t type of some C/C++ compilers became a 32-bit integer, and
some C libraries do use it for Unicode processing. However, application software
with good Unicode support tends to have little use for the rudimentary Unicode
and Internationalization support of the standard C/C++ libraries and often uses
custom types (like ICU's) and UTF-16 or UTF-8.

For those systems where 32-bit Unicode strings are used, ICU offers some
convenience functions.

1.  Conversion of whole strings: u_strFromUTF32() and u_strFromUTF32() in
    ustring.h.

2.  Access to code points is trivial and does not require any macros.

3.  Using a UTF-32 converter with all of the ICU conversion APIs in ucnv.h,
    including ones with an "Algorithmic" suffix.

4.  UnicodeString has `fromUTF32()` and `toUTF32()` methods.

5.  For conversion directly between UTF-32 and another charset use
    ucnv_convertEx(). However, since ICU converters work with byte streams in
    external charsets on the non-"Unicode" side, the UTF-32 string will be
    treated as a byte stream (UTF-32 Character Encoding *Scheme*) rather than a
    sequence of 32-bit code units (UTF-32 Character Encoding *Form*). The
    correct converter must be used: UTF-32BE or UTF-32LE according to the
    platform endianness (U_IS_BIG_ENDIAN). Treating the string like a byte
    stream also makes a difference in data types (`char *`), lengths and indexes
    (counting bytes), and NUL-termination handling (input NUL-termination not
    possible, output writes only a NUL byte, not a NUL 32-bit code unit). For
    the difference between internal encoding forms and external encoding schemes
    see the Unicode Standard.

6.  Some ICU APIs work with a CharacterIterator, a UText or a UCharIterator
    instead of directly with a C/C++ string parameter. There is currently no ICU
    instance of any of these interfaces that reads UTF-32, although an
    application could provide one.

## Changes in ICU 2.0

Beginning with ICU release 2.0, there are a few changes to the ICU string
facilities compared with earlier ICU releases.

Some of the NUL-termination behavior was inconsistent across the ICU API
functions. In particular, the following functions used to count the terminating
NUL character in their output length (counted one more before ICU 2.0 than now):
ucnv_toUChars, ucnv_fromUChars, uloc_getLanguage, uloc_getCountry,
uloc_getVariant, uloc_getName, uloc_getDisplayLanguage, uloc_getDisplayCountry,
uloc_getDisplayVariant, uloc_getDisplayName

Some functions used to set an overflow error code even when only the terminating
NUL did not fit into the output buffer. These functions now set UErrorCode to
U_STRING_NOT_TERMINATED_WARNING rather than to U_BUFFER_OVERFLOW_ERROR.

The aliasing UnicodeString constructors and most extract functions have existed
for several releases prior to ICU 2.0. There is now an additional extract
function with a UErrorCode parameter. Also, the getBuffer, releaseBuffer and
getCapacity functions are new to ICU 2.0.

For more information about these changes, please consult the old and new API
documentation.
