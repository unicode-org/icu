---
layout: default
title: Coding Guidelines
nav_order: 1
parent: Misc
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Coding Guidelines
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

This section provides the guidelines for developing C and C++ code, based on the
coding conventions used by ICU programmers in the creation of the ICU library.

## Details about ICU Error Codes

When calling an ICU API function and an error code pointer (C) or reference
(C++), a `UErrorCode` variable is often passed in. This variable is allocated by
the caller and must pass the test `U_SUCCESS()` before the function call.
Otherwise, the function will not work. Normally, an error code variable is
initialized by `U_ZERO_ERROR`.

`UErrorCode` is passed around and used this way, instead of using C++ exceptions
for the following reasons:

* It is useful in the same form for C also
* Some C++ compilers do not support exceptions

> :point_right: **Note**: *This error code mechanism, in fact, works similar to
> exceptions. If users call several ICU functions in a sequence, as soon as one
> sets a failure code, the functions in the following example will not work. This
> procedure prevents the API function from processing data that is not valid in
> the sequence of function calls and relieves the caller from checking the error
> code after each call. It is somewhat similar to how an exception terminates a
> function block or try block early.*

The following code shows the inside of an ICU function implementation:

```c++
U_CAPI const UBiDiLevel * U_EXPORT2
ubidi_getLevels(UBiDi *pBiDi, UErrorCode *pErrorCode) {
    int32_t start, length;

    if(U_FAILURE(*pErrorCode)) {
        return NULL;
    } else if(pBiDi==NULL || (length=pBiDi->length)<=0) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    ...
    return result;
}
```

Note: We have decided that we do not want to test for `pErrorCode==NULL`. Some
existing code does this, but new code should not.

Note: *Callers* (as opposed to implementers) of ICU APIs can simplify their code
by defining and using a subclass of `icu::ErrorCode`. ICU implementers can use the
`IcuTestErrorCode` class in intltest code.

It is not necessary to check for `U_FAILURE()` immediately before calling a
function that takes a `UErrorCode` parameter, because that function is supposed to
check for failure. Exception: If the failure comes from objection allocation or
creation, then you probably have a `NULL` object pointer and must not call any
method on that object, not even one with a `UErrorCode` parameter.

### Sample Function with Error Checking

```c++
    U_CAPI int32_t U_EXPORT2
    uplrules_select(const UPluralRules *uplrules,   // Do not check
                                                    // "this"/uplrules vs. NULL.
                    double number,
                    UChar *keyword, int32_t capacity,
                    UErrorCode *status)             // Do not check status!=NULL.
    {
        if (U_FAILURE(*status)) {                   // Do check for U_FAILURE()
                                                    // before setting *status
            return 0;                               // or calling UErrorCode-less
                                                    // select(number).
        }
        if (keyword == NULL ? capacity != 0 : capacity < 0) {
                                                    // Standard destination buffer
                                                    // checks.
            *status = U_ILLEGAL_ARGUMENT_ERROR;
            return 0;
        }
        UnicodeString result = ((PluralRules*)uplrules)->select(number);
        return result.extract(keyword, capacity, *status);
    }
```

### New API Functions

If the API function is non-const, then it should have a `UErrorCode` parameter.
(Not the other way around: Some const functions may need a `UErrorCode` as well.)

Default C++ assignment operators and copy constructors should not be used (they
should be declared private and not implemented). Instead, define an `assign(Class
&other, UErrorCode &errorCode)` function. Normal constructors are fine, and
should have a `UErrorCode` parameter.

### Warning Codes

Some `UErrorCode` values do not indicate a failure but an additional informational
return value. Their enum constants have the `_WARNING` suffix and they pass the
`U_SUCCESS()` test.

However, experience has shown that they are problematic: They can get lost
easily because subsequent function calls may set their own "warning" codes or
may reset a `UErrorCode` to `U_ZERO_ERROR`.

The source of the problem is that the `UErrorCode` mechanism is designed to mimic
C++/Java exceptions. It prevents ICU function execution after a failure code is
set, but like exceptions it does not work well for non-failure information
passing.

Therefore, we recommend to use warning codes very carefully:

* Try not to rely on any warning codes.
* Use real APIs to get the same information if possible.
  For example, when a string is completely written but cannot be
  NUL-terminated, then `U_STRING_NOT_TERMINATED_WARNING` indicates this, but so
  does the returned destination string length (which will have the same value
  as the destination capacity in this case). Checking the string length is
  safer than checking the warning code. (It is even safer to not rely on
  NUL-terminated strings but to use the length.)
* If warning codes must be used, then the best is to set the `UErrorCode` to
  `U_ZERO_ERROR` immediately before calling the function in question, and to
  check for the expected warning code immediately after the function returns.

Future versions of ICU will not introduce new warning codes, and will provide
real API replacements for all existing warning codes.

### Bogus Objects

Some objects, for example `UnicodeString` and `UnicodeSet`, can become "bogus". This
is used when methods that create or modify the object fail (mostly due to an
out-of-memory condition) but do not take a `UErrorCode` parameter and can
therefore not otherwise report the failure.

* A bogus object appears as empty.
* A bogus object cannot be modified except with assignment-like functions.
* The bogus state of one object does not transfer to another. For example,
  adding a bogus `UnicodeString` to a `UnicodeSet` does not make the set bogus.
  (It would be hard to make propagation consistent and test it well. Also,
  propagation among bogus states and error codes would be messy.)
* If a bogus object is passed into a function that does have a `UErrorCode`
  parameter, then the function should set the `U_ILLEGAL_ARGUMENT_ERROR` code.

## API Documentation

"API" means any public class, function, or constant.

### API status tag

Aside from documenting an API's functionality, parameters, return values etc. we
also mark every API with whether it is `@draft`, `@stable`, `@deprecated` or
`@internal`. (Where `@internal` is used when something is not actually supported
API but needs to be physically public anyway.) A new API is usually marked with
"`@draft ICU 4.8`". For details of how we mark APIs see the "ICU API
compatibility" section of the [ICU Architectural Design](../design.md) page. In
Java, also see existing @draft APIs for complete examples.

Functions that override a base class or interface definition take the API status
of the base class function. For C++, use the `@copydoc base::function()` tag to
copy both the description and the API status from the base function definition.
For Java methods the status tags must be added by hand; use the `{@inheritDoc}`
JavaDoc tag to pick up the rest of the base function documentation.
Documentation should not be manually replicated in overriding functions; it is
too hard to keep multiple copies synchronized.

The policy for the treatment of status tags in overriding functions was
introduced with ICU 64 for C++, and with ICU 59 for Java. Earlier code may
deviate.

### Coding Example

Coding examples help users to understand the usage of each API. Whenever
possible, it is encouraged to embed a code snippet illustrating the usage of an
API along with the functional specification.

#### Embedding Coding Examples in ICU4J - JCite

Since ICU4J 49M2, the ICU4J ant build target "doc" utilizes an external tool
called [JCite](https://arrenbrecht.ch/jcite/). The tool allows us to cite a
fragment of existing source code into JavaDoc comment using a tag. To embed a
code snippet with the tag. For example,
`{@.jcite com.ibm.icu.samples.util.timezone.BasicTimeZoneExample:---getNextTransitionExample}`
will be replaced a fragment of code marked by comment lines
`// ---getNextTransisionExample` in `BasicTimeZoneExample.java` in package
`com.ibm.icu.samples.util.timezone`. When embedding code snippet using JCite, we
recommend to follow next guidelines

* A sample code should be placed in `<icu4j_root>/samples/src` directory,
  although you can cite any source fragment from source files in
  `<icu4j_root>/demos/src`, `<icu4j_root\>/main/core/*/src`,
  `<icu4j_root>/main/test/*/src`.
* A sample code should use package name -
  `com.ibm.icu.samples.<subpackage>.<facility>`. `<subpackage>` is corresponding
  to the target ICU API class's package, that is, one of lang/math/text/util.
  `<facility>` is a name of facility, which is usually the base class of the
  service. For example, use package `com.ibm.icu.samples.text.dateformat` for
  samples related to ICU's date format service,
  `com.ibm.icu.samples.util.timezone` for samples related to time zone service.
* A sample code should be self-contained as much as possible (use only JDK and
  ICU public APIs if possible). This allows readers to cut & paste a code
  snippet to try it out easily.
* The citing comment should start with three consecutive hyphen followed by
  lower camel case token - for example, "`// ---compareToExample`"
* Keep in mind that the JCite tag `{@.jcite ...}` is not resolved without JCite.
  It is encouraged to avoid placing code snippet within a sentence. Instead,
  you should place a code snippet using JCite in an independent paragraph.

#### Embedding Coding Examples in ICU4C

Also since ICU4C 49M2, ICU4C docs (using the [\\snippet command](http://www.doxygen.nl/manual/commands.html#cmdsnippet)
which is new in Doxygen 1.7.5) can cite a fragment of existing sample or test code.

Example in `ucnv.h`:

```c++
 /**
  * \snippet samples/ucnv/convsamp.cpp ucnv_open
  */
 ucnv_open( ... ) ...
```

This cites code in `icu4c/source/samples/ucnv/convsamp.cpp` as follows:

```c++
  //! [ucnv_open]
  conv = ucnv_open("koi8-r", &status);
  //! [ucnv_open]
```

Notice the tag "`ucnv_open`" which must be the same in all three places (in
the header file, and twice in the cited file).

## C and C++ Coding Conventions Overview

The ICU group uses the following coding guidelines to create software using the
ICU C++ classes and methods as well as the ICU C methods.

### C/C++ Hiding Un-@stable APIs

In C/C++, we enclose `@draft` and such APIs with `#ifndef U_HIDE_DRAFT_API` or
similar as appropriate. When a draft API becomes stable, we need to remove the
surrounding `#ifndef`.

Note: The `@system` tag is *in addition to* the
`@draft`/`@stable`/`@deprecated`/`@obsolete` status tag.

Copy/paste the appropriate `#ifndef..#endif` pair from the following:

```c++
#ifndef U_HIDE_DRAFT_API
#endif  // U_HIDE_DRAFT_API

#ifndef U_HIDE_DEPRECATED_API
#endif  // U_HIDE_DEPRECATED_API

#ifndef U_HIDE_OBSOLETE_API
#endif  // U_HIDE_OBSOLETE_API

#ifndef U_HIDE_SYSTEM_API
#endif  // U_HIDE_SYSTEM_API

#ifndef U_HIDE_INTERNAL_API
#endif  // U_HIDE_INTERNAL_API
```

We `#ifndef` `@draft`/`@deprecated`/... APIs as much as possible, including C
functions, many C++ class methods (see exceptions below), enum constants (see
exceptions below), whole enums, whole classes, etc.

We do not `#ifndef` APIs where that would be problematic:

* struct/class members where that would modify the object layout (non-static
  struct/class fields, virtual methods)
* enum constants where that would modify the numeric values of following
  constants
* C++ class boilerplate (e.g., default/copy constructors) because otherwise
  the compiler would create public ones
* private class members
* definitions in internal/test/tools header files (that would be pointless;
  they should probably not have API tags in the first place)
* forward or friend declarations
* definitions that are needed for other definitions that would not be
  `#ifndef`'ed (e.g., for public macros or private methods)
* platform macros (mostly in `platform.h`/`umachine.h` & similar) and
  user-configurable settings (mostly in `uconfig.h`)

More handy copy-paste text:

```c++
    // Do not enclose the protected default constructor with #ifndef U_HIDE_INTERNAL_API
    // or else the compiler will create a public default constructor.

    // Do not enclose protected default/copy constructors with #ifndef U_HIDE_INTERNAL_API
    // or else the compiler will create public ones.
```

### C and C++ Type and Format Convention Guidelines

The following C and C++ type and format conventions are used to maximize
portability across platforms and to provide consistency in the code:

#### Constants (#define, enum items, const)

Use uppercase letters for constants. For example, use `UBREAKITERATOR_DONE`,
`UBIDI_DEFAULT_LTR`, `ULESS`.

For new enum types (as opposed to new values added to existing types), do not
define enum types in C++ style. Instead, define C-style enums with U... type
prefix and `U_`/`UMODULE_` constants. Define such enum types outside the ICU
namespace and outside any C++ class. Define them in C header files if there are
appropriate ones.

#### Variables and Functions

Use mixed-case letters that start with a lowercase letter for variables and
functions. For example, use `getLength()`.

#### Types (class, struct, enum, union)

Use mixed-case that start with an uppercase letter for types. For example, use
class `DateFormatSymbols`.

#### Function Style

Use the `getProperty()` and `setProperty()` style for functions where a lowercase
letter begins the first word and the second word is capitalized without a space
between it and the first word. For example, `UnicodeString`
`getSymbol(ENumberFormatSymbol symbol)`,
`void setSymbol(ENumberFormatSymbol symbol, UnicodeString value)` and
`getLength()`, `getSomethingAt(index/offset)`.

#### Common Parameter Names

In order to keep function parameter names consistent, the following are
recommendations for names or suffixes (usual "Camel case" applies):

* "start": the index (of the first of several code units) in a string or array
* "limit": the index (of the **first code unit after** a specified range) in a
  string or array (the number of units are (limit-start))
* name the length (for the number of code units in a (range of a) string or
  array) either "length" or "somePrefixLength"
* name the capacity (for the number of code units available in an output
  buffer) either "capacity" or "somePrefixCapacity"

#### Order of Source/Destination Arguments

Many ICU function signatures list source arguments before destination arguments,
as is common in C++ and Java APIs. This is the preferred order for new APIs.
(Example: `ucol_getSortKey(const UCollator *coll, const UChar *source,
int32_t sourceLength, uint8_t *result, int32_t resultLength)`)

Some ICU function signatures list destination arguments before source arguments,
as is common in C standard library functions. This should be limited to
functions that closely resemble such C standard library functions or closely
related ICU functions. (Example: `u_strcpy(UChar *dst, const UChar *src)`)

#### Order of Include File Includes

Include system header files (like `<stdio.h>`) before ICU headers followed by
application-specific ones. This assures that ICU headers can use existing
definitions from system headers if both happen to define the same symbols. In
ICU files, all used headers should be explicitly included, even if some of them
already include others.

Within a group of headers, place them in alphabetical order.

#### Style for ICU Includes

All ICU headers should be included using ""-style includes (like
`"unicode/utypes.h"` or `"cmemory.h"`) in source files for the ICU library, tools,
and tests.

#### Pointer Conversions

Do not cast pointers to integers or integers to pointers. Also, do not cast
between data pointers and function pointers. This will not work on some
compilers, especially with different sizes of such types. Exceptions are only
possible in platform-specific code where the behavior is known.

Please use C++-style casts, at least for pointers, for example `const_cast`.

* For conversion between related types, for example from a base class to a
  subclass (when you *know* that the object is of that type), use
  `static_cast`. (When you are not sure if the object has the subclass type,
  then use a `dynamic_cast`; see a later section about that.)
* Also use `static_cast`, not `reinterpret_cast`, for conversion from `void *`
  to a specific pointer type. (This is accepted and recommended because there
  is an implicit conversion available for the opposite conversion.) See
  [ICU-9434](https://unicode-org.atlassian.net/browse/ICU-9434) for details.
* For conversion between unrelated types, for example between `char *` and
  `uint8_t *`, or between `Collator *` and `UCollator *`, use a
  `reinterpret_cast`.

#### Returning a Number of Items

To return a number of items, use `countItems()`, **not** `getItemCount()`, even if
there is no need to actually count using that member function.

#### Ranges of Indexes

Specify a range of indexes by having start and limit parameters with names or
suffix conventions that represent the index. A range should contain indexes from
start to limit-1 such as an interval that is left-closed and right-open. Using
mathematical notation, this is represented as: \[start..limit\[.

#### Functions with Buffers

Set the default value to -1 for functions that take a buffer (pointer) and a
length argument with a default value so that the function determines the length
of the input itself (for text, calling `u_strlen()`). Any other negative or
undefined value constitutes an error.

#### Primitive Types

Primitive types are defined by the `unicode/utypes.h` file or a header file that
includes other header files. The most common types are `uint8_t`, `uint16_t`,
`uint32_t`, `int8_t`, `int16_t`, `int32_t`, `char16_t`,
`UChar` (same as `char16_t`), `UChar32` (signed, 32-bit), and `UErrorCode`.

The language built-in type `bool` and constants `true` and `false` may be used
internally, for local variables and parameters of internal functions. The ICU
type `UBool` must be used in public APIs and in the definition of any persistent
data structures. `UBool` is guaranteed to be one byte in size and signed; `bool` is
not.

Traditionally, ICU4C has defined its own `FALSE`=0 / `TRUE`=1 macros for use with `UBool`.
Starting with ICU 68 (2020q4), we no longer define these in public header files
(unless `U_DEFINE_FALSE_AND_TRUE`=1),
in order to avoid name collisions with code outside ICU defining enum constants and similar
with these names.

Instead, the versions of the C and C++ standards we require now do define type `bool`
and values `false` & `true`, and we and our users can use these values.

As of ICU 68, we are not changing ICU4C API from `UBool` to `bool`.
Doing so in C API, or in structs that cross the library boundary,
would break binary compatibility.
Doing so only in other places in C++ could be confusingly inconsistent.
We may revisit this.

Note that the details of type `bool` (e.g., `sizeof`) depend on the compiler and
may differ between C and C++.

#### File Names (.h, .c, .cpp, data files if possible, etc.)

Limit file names to 31 lowercase ASCII characters. (Older versions of MacOS have
that length limit.)

Exception: The layout engine uses mixed-case file names.

(We have abandoned the 8.3 naming standard although we do not change the names
of old header files.)

#### Language Extensions and Standards

Proprietary features, language extensions, or library functions, must not be
used because they will not work on all C or C++ compilers.
In Microsoft Visual C++, go to Project Settings(alt-f7)->All Configurations->
C/C++->Customize and check Disable Language Extensions.

Exception: some Microsoft headers will not compile without language extensions
being enabled, which in turn requires some ICU files be built with language
extensions.

#### Tabs and Indentation

Save files with spaces instead of tab characters (\\x09). The indentation size
is 4.

#### Documentation

Use Java doc-style in-file documentation created with
[doxygen](http://www.doxygen.org/) .

#### Multiple Statements

Place multiple statements in multiple lines. `if()` or loop heads must not be
followed by their bodies on the same line.

#### Placements of `{}` Curly Braces

Place curly braces `{}` in reasonable and consistent locations. Each of us
subscribes to different philosophies. It is recommended to use the style of a
file, instead of mixing different styles. It is requested, however, to not have
`if()` and loop bodies without curly braces.

#### `if() {...}` and Loop Bodies

Use curly braces for `if()` and else as well as loop bodies, etc., even if there
is only one statement.

#### Function Declarations

Have one line that has the return type and place all the import declarations,
extern declarations, export declarations, the function name, and function
signature at the beginning of the next line.

Function declarations need to be in the form `U_CAPI` return-type `U_EXPORT2` to
satisfy all the compilers' requirements.

For example, use the following
convention:

```c++
U_CAPI int32_t U_EXPORT2
u_formatMessage(...);
```

> :point_right: **Note**: The `U_CAPI`/`U_DEPRECATED` and `U_EXPORT2` qualifiers
> are required for both the declaration and the definiton of *exported C and
> static C++ functions*. Use `U_CAPI` (or `U_DEPRECATED`) before and `U_EXPORT2`
> after the return type of *exported C and static C++ functions*.
> 
> Internal functions that are visible outside a compilation unit need a `U_CFUNC`
> before the return type.
> 
> *Non-static C++ class member functions* do *not* get `U_CAPI`/`U_EXPORT2`
> because they are exported and declared together with their class exports.

> :point_right: **Note**: Before ICU 68 (2020q4) we used to use alternate qualifiers
> like `U_DRAFT`, `U_STABLE` etc. rather than `U_CAPI`,
> but keeping these in sync with API doc tags `@draft` and guard switches like `U_HIDE_DRAFT_API`
> was tedious and error-prone and added no value.
> Since ICU 68 (ICU-9961) we only use `U_CAPI` and `U_DEPRECATED`.

#### Use Anonymous Namesapces or Static For File Scope

Use anonymous namespaces or `static` for variables, functions, and constants that
are not exported explicitly by a header file. Some platforms are confused if
non-static symbols are not explicitly declared extern. These platforms will not
be able to build ICU nor link to it.

#### Using C Callbacks From C++ Code

z/OS and Windows COM wrappers around ICU need `__cdecl` for callback functions.
The reason is that C++ can have a different function calling convention from C.
These callback functions also usually need to be private. So the following code

```c++
UBool
isAcceptable(void * /* context */,
             const char * /* type */, const char * /* name */,
             const UDataInfo *pInfo)
{
    // Do something here.
}
```

should be changed to look like the following by adding `U_CDECL_BEGIN`, `static`,
`U_CALLCONV` and `U_CDECL_END`.

```c++
U_CDECL_BEGIN
static UBool U_CALLCONV
isAcceptable(void * /* context */,
             const char * /* type */, const char * /* name */,
             const UDataInfo *pInfo)
{
    // Do something here.
}
U_CDECL_END
```

#### Same Module and Functionality in C and in C++

Determine if two headers are needed. If the same functionality is provided with
both a C and a C++ API, then there can be two headers, one for each language,
even if one uses the other. For example, there can be `umsg.h` for C and `msgfmt.h`
for C++.

Not all functionality has or needs both kinds of API. More and more
functionality is available only via C APIs to avoid duplication of API,
documentation, and maintenance. C APIs are perfectly usable from C++ code,
especially with `UnicodeString` methods that alias or expose C-style string
buffers.

#### Platform Dependencies

Use the platform dependencies that are within the header files that `utypes.h`
files include. They are `platform.h` (which is generated by the configuration
script from `platform.h.in`) and its more specific cousins like `pwin32.h` for
Windows, which define basic types, and `putil.h`, which defines platform
utilities.
**Important:** Outside of these files, and a small number of implementation
files that depend on platform differences (like `umutex.c`), **no** ICU source
code may have **any** `#ifdef` **OperatingSystemName** instructions.

#### Short, Unnested Mutex Blocks

Do not use function calls within a mutex block for mutual-exclusion (mutex)
blocks. This can prevent deadlocks from occurring later. There should be as
little code inside a mutex block as possible to minimize the performance
degradation from blocked threads.
Also, it is not guaranteed that mutex blocks are re-entrant; therefore, they
must not be nested.

#### Names of Internal Functions

Internal functions that are not declared static (regardless of inlining) must
follow the naming conventions for exported functions because many compilers and
linkers do not distinguish between library exports and intra-library visible
functions.

#### Which Language for the Implementation

Write implementation code in C++. Use objects very carefully, as always:
Implicit constructors, assignments etc. can make simple-looking code
surprisingly slow.

For every C API, make sure that there is at least one call from a pure C file in
the cintltst test suite.

Background: We used to prefer C or C-style C++ for implementation code because
we used to have users ask for pure C. However, there was never a large, usable
subset of ICU that was usable without any C++ dependencies, and C++ can(!) make
for much shorter, simpler, less error-prone and easier-to-maintain code, for
example via use of "smart pointers" (`unicode/localpointer.h` and `cmemory.h`).

We still try to expose most functionality via *C APIs* because of the
difficulties of binary compatible C++ APIs exported from DLLs/shared libraries.

#### No Compiler Warnings

ICU must compile without compiler warnings unless such warnings are verified to
be harmless or bogus. Often times a warning on one compiler indicates a breaking
error on another.

#### Enum Values

When casting an integer value to an enum type, the enum type *should* have a
constant with this integer value, or at least it *must* have a constant whose
value is at least as large as the integer value being cast, with the same
signedness. For example, do not cast a -1 to an enum type that only has
non-negative constants. Some compilers choose the internal representation very
tightly for the defined enum constants, which may result in the equivalent of a
`uint8_t` representation for an enum type with only small, non-negative constants.
Casting a -1 to such a type may result in an actual value of 255. (This has
happened!)

When casting an enum value to an integer type, make sure that the enum value's
numeric value is within range of the integer type.

#### Do not check for `this!=NULL`, do not check for `NULL` references

In public APIs, assume `this!=0` and assume that references are not 0. In C code,
`"this"` is the "service object" pointer, such as `set` in
`uset_add(USet* set, UChar32 c)` — don't check for `set!=NULL`.

We do usually check all other (non-this) pointers for `NULL`, in those cases when
`NULL` is not valid. (Many functions allow a `NULL` string or buffer pointer if the
length or capacity is 0.)

Rationale: `"this"` is not really an argument, and checking it costs a little bit
of code size and runtime. Other libraries also commonly do not check for valid
`"this"`, and resulting failures are fairly obvious.

### Memory Usage

#### Dynamically Allocated Memory

ICU4C APIs are designed to allow separate heaps for its libraries vs. the
application. This is achieved by providing factory methods and matching
destructors for all allocated objects. The C++ API uses a common base class with
overridden `new`/`delete` operators and/or forms an equivalent pair with `createXyz()`
factory methods and the `delete` operator. The C API provides pairs of `open`/`close`
functions for each service. See the C++ and C guideline sections below for
details.

Exception: Most C++ API functions that return a `StringEnumeration` (by pointer
which the caller must delete) are named `getXyz()` rather than `createXyz()`
because `"get"` is much more natural. (These are not factory methods in the sense
of `NumberFormat::createScientificInstance()`.) For example,
`static StringEnumeration *Collator::``get``Keywords(UErrorCode &)`. We should document
clearly in the API comments that the caller must delete the returned
`StringEnumeration`.

#### Declaring Static Data

All unmodifiable data should be declared `const`. This includes the pointers and
the data itself. Also if you do not need a pointer to a string, declare the
string as an array. This reduces the time to load the library and all its
pointers. This should be done so that the same library data can be shared across
processes automatically. Here is an example:

```c++
#define MY_MACRO_DEFINED_STR "macro string"
const char *myCString = "myCString";
int16_t myNumbers[] = {1, 2, 3};
```

This should be changed to the following:

```c++
static const char MY_MACRO_DEFINED_STR[] = "macro string";
static const char myCString[] = "myCString";
static const int16_t myNumbers[] = {1, 2, 3};
```

#### No Static Initialization

The most common reason to have static initialization is to declare a
`static const UnicodeString`, for example (see `utypes.h` about invariant characters):

```c++
static const UnicodeString myStr("myStr", "");
```

The most portable and most efficient way to declare ASCII text as a Unicode
string is to do the following instead:

```c++
static const UChar myStr[] = { 0x6D, 0x79, 0x53, 0x74, 0x72, 0}; /* "myStr" */
```

We do not use character literals
for Unicode characters and strings because the execution character set of C/C++
compilers is almost never Unicode and may not be ASCII-compatible (especially on
EBCDIC platforms). Depending on the API where the string is to be used, a
terminating NUL (0) may or may not be required. The length of the string (number
of `UChar`s in the array) can be determined with `sizeof(myStr)/U_SIZEOF_UCHAR`,
(subtract 1 for the NUL if present). Always remember to put in a comment at the
end of the declaration what the Unicode string says.

Static initialization of C++ objects **must not be used** in ICU libraries
because of the following reasons:

1. It leads to intractable order-of-initialization dependencies.
2. It makes it difficult or impossible to release all of the libraries
   resources. See `u_cleanup()`.
3. It takes time to initialize the library.
4. Dependency checking is not completely done in C or C++. For instance, if an
   ICU user creates an ICU object or calls an ICU function statically that
   depends on static data, it is not guaranteed that the statically declared
   data is initialized.
5. Certain users like to manage their own memory. They can not manage ICU's
   memory properly because of item #2.
6. It is easier to debug code that does not use static initialization.
7. Memory allocated at static initialization time is not guaranteed to be
   deallocated with a C++ destructor when the library is unloaded. This is a
   problem when ICU is unloaded and reloaded into memory and when you are using
   a heap debugging tool. It would also not work with the `u_cleanup()` function.
8. Some platforms cannot handle static initialization or static destruction
   properly. Several compilers have this random bug (even in the year 2001).

ICU users can use the `U_STRING_DECL` and `U_STRING_INIT` macros for C strings. Note
that on some platforms this will incur a small initialization cost (simple
conversion). Also, ICU users need to make sure that they properly and
consistently declare the strings with both macros. See `ustring.h` for details.

### C++ Coding Guidelines

This section describes the C++ specific guidelines or conventions to use.

#### Portable Subset of C++

ICU uses only a portable subset of C++ for maximum portability. Also, it does
not use features of C++ that are not implemented well in all compilers or are
cumbersome. In particular, ICU does not use exceptions, or the Standard Template
Library (STL).

We have started to use templates in ICU 4.2 (e.g., `StringByteSink`) and ICU 4.4
(`LocalPointer` and some internal uses). We try to limit templates to where they
provide a lot of benefit (robust code, avoid duplication) without much or any
code bloat.

We continue to not use the Standard Template Library (STL) in ICU library code
because its design causes a lot of code bloat. More importantly:

* Exceptions: STL classes and algorithms throw exceptions. ICU does not throw
  exceptions, and ICU code is not exception-safe.
* Memory management: STL uses default new/delete, or Allocator parameters
  which create different types; they throw out-of-memory exceptions. ICU
  memory allocation is customizable and must not throw exceptions.
* Non-polymorphic: For APIs, STL classes are also problematic because
  different template specializations create different types. For example, some
  systems use custom string classes (different allocators, different
  strategies for buffer sharing vs. copying), and ICU should be able to
  interface with most of them.

We have started to use compiler-provided Run-Time Type Information (RTTI) in ICU
4.6. It is now required for building ICU, and encouraged for using ICU where
RTTI is needed. For example, use `dynamic_cast<DecimalFormat*>` on a
`NumberFormat` pointer that is usually but not always a `DecimalFormat` instance.
Do not use `dynamic_cast<>` on a reference, because that throws a `bad_cast`
exception on failure.

ICU uses a limited form of multiple inheritance equivalent to Java's interface
mechanism: All but one base classes must be interface/mixin classes, i.e., they
must contain only pure virtual member functions. For details see the
'boilerplate' discussion below. This restriction to at most one base class with
non-virtual members eliminates problems with the use and implementation of
multiple inheritance in C++. ICU does not use virtual base classes.

> :point_right: **Note**: Every additional base class, *even an interface/mixin
class*, adds another vtable pointer to each subclass object, that is, it
*increases the object/instance size by 8 bytes* on most platforms.

#### Classes and Members

C++ classes and their members do not need a 'U' or any other prefix.

#### Global Operators

Global operators (operators that are not class members) can be problematic for
library entry point versioning, may confuse users and cannot be easily ported to
Java (ICU4J). They should be avoided if possible.

~~The issue with library entry point versioning is that on platforms that do not
support namespaces, users must rename all classes and global functions via
urename.h. This renaming process is not possible with operators.~~ Starting with
ICU 49, we require C++ namespace support. However, a global operator can be used
in ICU4C (when necessary) if its function signature contains an ICU C++ class
that is versioned. This will result in a mangled linker name that does contain
the ICU version number via the versioned name of the class parameter. For
example, ICU4C 2.8 added an operator + for `UnicodeString`, with two `UnicodeString`
reference parameters.

#### Virtual Destructors

In classes with virtual methods, destructors must be explicitly declared, and
must be defined (implemented) outside the class definition in a .cpp file.

More precisely:

1. All classes with any virtual members or any bases with any virtual members
   should have an explicitly declared virtual destructor.
2. Constructors and destructors should be declared and/or defined prior to
   *any* other methods, public or private, within the class definition.
3. All virtual destructors should be defined out-of-line, and in a .cpp file
   rather than a header file.

This is so that the destructors serve as "key functions" so that the compiler
emits the vtable in only and exactly the desired files. It can help make
binaries smaller that use statically-linked ICU libraries, because the compiler
and linker can prove more easily that some code is not used.

The Itanium C++ ABI (which is used on all x86 Linux) says: "The virtual table
for a class is emitted in the same object containing the definition of its key
function, i.e. the first non-pure virtual function that is not inline at the
point of class definition. If there is no key function, it is emitted everywhere
used."

(This was first done in ICU 49; see [ticket #8454](https://unicode-org.atlassian.net/browse/ICU-8454.)

#### Namespaces

Beginning with ICU version 2.0, ICU uses namespaces. The actual namespace is
`icu_M_N` with M being the major ICU release number and N being the minor ICU
release number. For convenience, the namespace `icu` is an alias to the current
release-specific one. (The actual namespace name is `icu` itself if renaming is
turned off.)

Starting with ICU 49, we require C++ namespace support.

Class declarations, even forward declarations, must be scoped to the ICU
namespace. For example:

```c++
U_NAMESPACE_BEGIN

class Locale;

U_NAMESPACE_END

// outside U_NAMESPACE_BEGIN..U_NAMESPACE_END
extern void fn(icu::UnicodeString&);

// outside U_NAMESPACE_BEGIN..U_NAMESPACE_END
// automatically set by utypes.h
// but recommended to be not set automatically
U_NAMESPACE_USE
Locale loc("fi");
```

`U_NAMESPACE_USE` (expands to using namespace icu_M_N; when available) is
automatically done when `utypes.h` is included, so that all ICU classes are
immediately usable. However, we recommend that you turn this off via
`CXXFLAGS="-DU_USING_ICU_NAMESPACE=0"`.

#### Declare Class APIs

Class APIs need to be declared like either of the following:

#### Inline-Implemented Member Functions

Class member functions are usually declared but not inline-implemented in the
class declaration. A long function implementation in the class declaration makes
it hard to read the class declaration.

It is ok to inline-implement *trivial* functions in the class declaration.
Pretty much everyone agrees that inline implementations are ok if they fit on
the same line as the function signature, even if that means bending the
single-statement-per-line rule slightly:

```c++
T *orphan() { T *p=ptr; ptr=NULL; return p; }
```

Most people also agree that very short multi-line implementations are ok inline
in the class declaration. Something like the following is probably the maximum:

```c++
Value *getValue(int index) {
    if(index>=0 && index<fLimit) {
        return fArray[index];
    }
    return NULL;
}
```

If the inline implementation is longer than that, then just declare the function
inline and put the actual inline implementations after the class declaration in
the same file. (See `unicode/unistr.h` for many examples.)

If it's significantly longer than that, then it's probably not a good candidate
for inlining anyway.

#### C++ class layout and 'boilerplate'

There are different sets of requirements for different kinds of C++ classes. In
general, all instantiable classes (i.e., all classes except for interface/mixin
classes and ones with only static member functions) inherit the `UMemory` base
class. `UMemory` provides `new`/`delete` operators, which allows to keep the ICU
heap separate from the application heap, or to customize ICU's memory allocation
consistently.

> :point_right: **Note**: Public ICU APIs must return or orphan only C++ objects
that are to be released with `delete`. They must not return allocated simple
types (including pointers, and arrays of simple types or pointers) that would
have to be released with a `free()` function call using the ICU library's heap.
Simple types and pointers must be returned using fill-in parameters (instead of
allocation), or cached and owned by the returning API.

**Public ICU C++ classes** must inherit either the `UMemory` or the `UObject`
base class for proper memory management, and implement the following common set
of 'boilerplate' functions:

* default constructor
* copy constructor
* assignment operator
* operator==
* operator!=

> :point_right: **Note**: Each of the above either must be implemented, verified
that the default implementation according to the C++ standard will work
(typically not if any pointers are used), or declared private without
implementation.

* If public subclassing is intended, then the public class must inherit
  `UObject` and should implement
  * `clone()`
* **RTTI:**
  * If a class is a subclass of a parent (e.g., `Format`) with ICU's "poor
    man's RTTI" (Run-Time Type Information) mechanism (via
    `getDynamicClassID()` and `getStaticClassID()`) then add that to the new
    subclass as well (copy implementations from existing C++ APIs).
  * If a class is a new, immediate subclass of `UObject` (e.g.,
    `Normalizer2`), creating a whole new class hierarchy, then declare a
    *private* `getDynamicClassID()` and define it to return `NULL` (to
    override the pure virtual version in `UObject`); copy the relevant lines
    from `normalizer2.h` and `normalizer2.cpp`
    (`UOBJECT_DEFINE_NO_RTTI_IMPLEMENTATION(className)`). Do not add any
    "poor man's RTTI" at all to subclasses of this class.

**Interface/mixin classes** are equivalent to Java interfaces. They are as much
multiple inheritance as ICU uses — they do not decrease performance, and they do
not cause problems associated with multiple base classes having data members.
Interface/mixin classes contain only pure virtual member functions, and must
contain an empty virtual destructor. See for example the `UnicodeMatcher` class.
Interface/mixin classes must not inherit any non-interface/mixin class,
especially not `UMemory` or `UObject`. Instead, implementation classes must inherit
one of these two (or a subclass of them) in addition to the interface/mixin
classes they implement. See for example the `UnicodeSet` class.

**Static classes** contain only static member functions and are therefore never
instantiated. They must not inherit `UMemory` or `UObject`. Instead, they must
declare a private default constructor (without any implementation) to prevent
instantiation. See for example the `LESwaps` layout engine class.

**C++ classes internal to ICU** need not (but may) implement the boilerplate
functions as mentioned above. They must inherit at least `UMemory` if they are
instantiable.

#### Make Sure The Compiler Uses C++

The `__cplusplus` macro being defined ensures that the compiler uses C++. Starting
with ICU 49, we use this standard predefined macro.

Up until ICU 4.8 we used to define and use `XP_CPLUSPLUS` but that was redundant
and did not add any value because it was defined if-and-only-if `__cplusplus` was
defined.

#### Adoption of Objects

Some constructors and factory functions take pointers to objects that they
adopt. The newly created object contains a pointer to the adoptee and takes over
ownership and lifecycle control. If an error occurs while creating the new
object (and thus in the code that adopts an object), then the semantics used
within ICU must be *adopt-on-call* (as opposed to, for example,
adopt-on-success):

* **General**: A constructor or factory function that adopts an object does so
  in all cases, even if an error occurs and a `UErrorCode` is set. This means
  that either the adoptee is deleted immediately or its pointer is stored in
  the new object. The former case is most common when the constructor or
  factory function is called and the `UErrorCode` already indicates a failure.
  In the latter case, the new object must take care of deleting the adoptee
  once it is deleted itself regardless of whether or not the constructor was
  successful.

* **Constructors**: The code that creates the object with the new operator
  must check the resulting pointer returned by new and delete any adoptees if
  it is 0 because the constructor was not called. (Typically, a `UErrorCode`
  must be set to `U_MEMORY_ALLOCATION_ERROR`.)

  **Pitfall**: If you allocate/construct via "`ClassName *p = new ClassName(adoptee);`"
  and the memory allocation failed (`p==NULL`), then the
  constructor has not been called, the adoptee has not been adopted, and you
  are still responsible for deleting it!

* **Factory functions (createInstance())**: The factory function must set a
  `U_MEMORY_ALLOCATION_ERROR` and delete any adoptees if it cannot allocate the
  new object. If the construction of the object fails otherwise, then the
  factory function must delete it and the factory function must delete its
  adoptees. As a result, a factory function always returns either a valid
  object and a successful `UErrorCode`, or a 0 pointer and a failure `UErrorCode`.
  A factory function returns a pointer to an object that must be deleted by
  the user/owner.

Example: (This is a best-practice example. It does not reflect current `Calendar`
code.)

```c++
Calendar*
Calendar::createInstance(TimeZone* zone, UErrorCode& errorCode) {
    LocalPointer<TimeZone> adoptedZone(zone);
    if(U_FAILURE(errorCode)) {
        // The adoptedZone destructor deletes the zone.
        return NULL;
    }
    // since the Locale isn't specified, use the default locale
    LocalPointer<Calendar> c(new GregorianCalendar(zone, Locale::getDefault(), errorCode));
    if(c.isNull()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        // The adoptedZone destructor deletes the zone. return NULL;
    } else if(U_FAILURE(errorCode)) {
        // The c destructor deletes the Calendar.
        return NULL;
    } // c adopted the zone. adoptedZone.orphan();
    return c.orphan();
}
```

#### Memory Allocation

All ICU C++ class objects directly or indirectly inherit `UMemory` (see
'boilerplate' discussion above) which provides `new`/`delete` operators, which in
turn call the internal functions in `cmemory.c`. Creating and releasing ICU C++
objects with `new`/`delete` automatically uses the ICU allocation functions.

> :point_right: **Note**: Remember that (in absence of explicit :: scoping) C++
determines which `new`/`delete` operator to use from which type is allocated or
deleted, not from the context of where the statement is. Since non-class data
types (like `int`) cannot define their own `new`/`delete` operators, C++ always
uses the global ones for them by default.

When global `new`/`delete` operators are to be used in the application (never inside
ICU!), then they should be properly scoped as e.g. `::new`, and the application
must ensure that matching `new`/`delete` operators are used. In some cases where
such scoping is missing in non-ICU code, it may be simpler to compile ICU
without its own `new`/`delete` operators. See `source/common/unicode/uobject.h` for
details.

In ICU library code, allocation of non-class data types — simple integer types
**as well as pointers** — must use the functions in `cmemory.h`/`.c` (`uprv_malloc()`,
`uprv_free()`, `uprv_realloc()`). Such memory objects must be released inside ICU,
never by the user; this is achieved either by providing a "close" function for a
service or by avoiding to pass ownership of these objects to the user (and
instead filling user-provided buffers or returning constant pointers without
passing ownership).

The `cmemory.h`/`.c` functions can be overridden at ICU compile time for custom
memory management. By default, `UMemory`'s `new`/`delete` operators are
implemented by calling these common functions. Overriding the `cmemory.h`/`.c`
functions changes the memory management for both C and C++.

C++ objects that were either allocated with new or returned from a `createXYZ()`
factory method must be deleted by the user/owner.

#### Memory Allocation Failures

All memory allocations and object creations should be checked for success. In
the event of a failure (a `NULL` returned), a `U_MEMORY_ALLOCATION_ERROR` status
should be returned by the ICU function in question. If the allocation failure
leaves the ICU service in an invalid state, such that subsequent ICU operations
could also fail, the situation should be flagged so that the subsequent
operations will fail cleanly. Under no circumstances should a memory allocation
failure result in a crash in ICU code, or cause incorrect results rather than a
clean error return from an ICU function.

Some functions, such as the C++ assignment operator, are unable to return an ICU
error status to their caller. In the event of an allocation failure, these
functions should mark the object as being in an invalid or bogus state so that
subsequent attempts to use the object will fail. Deletion of an invalid object
should always succeed.

#### Memory Management

C++ memory management is error-prone, and memory leaks are hard to avoid, but
the following helps a lot.

First, if you can stack-allocate an object (for example, a `UnicodeString` or
`UnicodeSet`), do so. It is the easiest way to manage object lifetime.

Inside functions, avoid raw pointers to owned objects. Instead, use
[LocalPointer](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/localpointer_8h.html)`<UnicodeString>`
or `LocalUResouceBundlePointer` etc., which is ICU's "smart pointer"
implementation. This is the "[Resource Acquisition Is Initialization(RAII)](http://en.wikipedia.org/wiki/Resource_Acquisition_Is_Initialization)"
idiom. The "smart pointer" auto-deletes the object when it goes out of scope,
which means that you can just return from the function when an error occurs and
all auto-managed objects are deleted. You do not need to remember to write an
increasing number of "`delete xyz;`" at every function exit point.

*In fact, you should almost never need to write "delete" in any function.*

* Except in a destructor where you delete all of the objects which the class
  instance owns.
* Also, in static "cleanup" functions you still need to delete cached objects.

When you pass on ownership of an object, for example to return the pointer of a
newly built object, or when you call a function which adopts your object, use
`LocalPointer`'s `.orphan()`.

* Careful: When you return an object or pass it into an adopting factory
  method, you can use `.orphan()` directly.
* However, when you pass it into an adopting constructor, you need to pass in
  the `.getAlias()`, and only if the *allocation* of the new owner succeeded
  (you got a non-NULL pointer for that) do you `.orphan()` your `LocalPointer`.
* See the `Calendar::createInstance()` example above.
* See the `AlphabeticIndex` implementation for live examples. Search for other
  uses of `LocalPointer`/`LocalArray`.

Every object must always be deletable/destructable. That is, at a minimum, all
pointers to owned memory must always be either NULL or point to owned objects.

Internally:

[cmemory.h](https://github.com/unicode-org/icu/blob/master/icu4c/source/common/cmemory.h)
defines the `LocalMemory` class for chunks of memory of primitive types which
will be `uprv_free()`'ed.

[cmemory.h](https://github.com/unicode-org/icu/blob/master/icu4c/source/common/cmemory.h)
also defines `MaybeStackArray` and `MaybeStackHeaderAndArray` which automate
management of arrays.

Use `CharString`
([charstr.h](https://github.com/unicode-org/icu/blob/master/icu4c/source/common/charstr.h))
for `char *` strings that you build and modify.

#### Global Inline Functions

Global functions (non-class member functions) that are declared inline must be
made static inline. Some compilers will export symbols that are declared inline
but not static.

#### No Declarations in the for() Loop Head

Iterations through `for()` loops must not use declarations in the first part of
the loop. There have been two revisions for the scoping of these declarations
and some compilers do not comply to the latest scoping. Declarations of loop
variables should be outside these loops.

#### Common or I18N

Decide whether or not the module is part of the common or the i18n API
collection. Use the appropriate macros. For example, use
`U_COMMON_IMPLEMENTATION`, `U_I18N_IMPLEMENTATION`, `U_COMMON_API`, `U_I18N_API`.
See `utypes.h`.

#### Constructor Failure

If there is a reasonable chance that a constructor fails (For example, if the
constructor relies on loading data), then either it must use and set a
`UErrorCode` or the class needs to support an `isBogus()`/`setToBogus()` mechanism
like `UnicodeString` and `UnicodeSet`, and the constructor needs to set the object
to bogus if it fails.

#### `UVector`, `UVector32`, or `UVector64`

Use `UVector` to store arrays of `void *`; use `UVector32` to store arrays of
`int32_t`; use `UVector64` to store arrays of `int64_t`. Historically, `UVector`
has stored either `int32_t` or `void *`, but now storing `int32_t` in a `UVector`
is deprecated in favor of `UVector32`.

### C Coding Guidelines

This section describes the C-specific guidelines or conventions to use.

#### Declare and define C APIs with both `U_CAPI` and `U_EXPORT2`

All C APIs need to be **both declared and defined** using the `U_CAPI` and
`U_EXPORT2` qualifiers.

```c++
U_CAPI int32_t U_EXPORT2
u_formatMessage(...);
```

> :point_right: **Note**: Use `U_CAPI` before and `U_EXPORT2` after the return
type of exported C functions. Internal functions that are visible outside a
compilation unit need a `U_CFUNC` before the return type.

#### Subdivide the Name Space

Use prefixes to avoid name collisions. Some of those prefixes contain a 3- (or
sometimes 4-) letter module identifier. Very general names like
`u_charDirection()` do not have a module identifier in their prefix.

* For POSIX replacements, the (all lowercase) POSIX function names start with
  "u_": `u_strlen()`.
* For other API functions, a 'u' is appended to the beginning with the module
  identifier (if appropriate), and an underscore '_', followed by the
  **mixed-case** function name. For example, use `u_charDirection()`,
  `ubidi_setPara()`.
* For types (struct, enum, union), a "U" is appended to the beginning, often
  "`U<module identifier>`" directly to the typename, without an underscore. For
  example, use `UComparisonResult`.
* For #defined constants and macros, a "U_" is appended to the beginning,
  often "`U<module identifier>_`" with an underscore to the uppercase macro
  name. For example, use `U_ZERO_ERROR`, `U_SUCCESS()`. For example, `UNORM_NFC`

#### Functions for Constructors and Destructors

Functions that roughly compare to constructors and destructors are called
`umod_open()` and `umod_close()`. See the following example:

```c++
CAPI UBiDi * U_EXPORT2
ubidi_open();

CAPI UBiDi * U_EXPORT2
ubidi_openSized(UTextOffset maxLength, UTextOffset maxRunCount);

CAPI void U_EXPORT2
ubidi_close(UBiDi *pBiDi);
```

Each successful call to a `umod_open()` returns a pointer to an object that must
be released by the user/owner by calling the matching `umod_close()`.

#### C "Service Object" Types and LocalPointer Equivalents

For every C "service object" type (equivalent to C++ class), we want to have a
[LocalPointer](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/localpointer_8h.html)
equivalent, so that C++ code calling the C API can use the specific "smart
pointer" to implement the "[Resource Acquisition Is Initialization
(RAII)](http://en.wikipedia.org/wiki/Resource_Acquisition_Is_Initialization)"
idiom.

For example, in `ubidi.h` we define the `UBiDi` "service object" type and also
have the following "smart pointer" definition which will call `ubidi_close()` on
destruction:

```c++
// Use config switches like this only after including unicode/utypes.h
// or another ICU header.
#if U_SHOW_CPLUSPLUS_API

U_NAMESPACE_BEGIN

/**
 * class LocalUBiDiPointer
 * "Smart pointer" class, closes a UBiDi via ubidi_close().
 * For most methods see the LocalPointerBase base class.
 *
 * @see LocalPointerBase
 * @see LocalPointer
 * @stable ICU 4.4
 */
U_DEFINE_LOCAL_OPEN_POINTER(LocalUBiDiPointer, UBiDi, ubidi_close);

U_NAMESPACE_END

#endif
```

#### Inline Implementation Functions

Some, but not all, C compilers allow ICU users to declare functions inline
(which is a C++ language feature) with various keywords. This has advantages for
implementations because inline functions are much safer and more easily debugged
than macros.

ICU *used to* use a portable `U_INLINE` declaration macro that can be used for
inline functions in C. However, this was an unnecessary platform dependency.

We have changed all code that used `U_INLINE` to C++ (.cpp) using "inline", and
removed the `U_INLINE` definition.

If you find yourself constrained by .c, change it to .cpp.

All functions that are declared inline, or are small enough that an optimizing
compiler might inline them even without the inline declaration, should be
defined (implemented) – not just declared – before they are first used. This is
to enable as much inlining as possible, and also to prevent compiler warnings
for functions that are declared inline but whose definition is not available
when they are called.

#### C Equivalents for Classes with Multiple Constructors

In cases like `BreakIterator` and `NumberFormat`, instead of having several
different 'open' APIs for each kind of instances, use an enum selector.

#### Source File Names

Source file names for C begin with a 'u'.

#### Memory APIs Inside ICU

For memory allocation in C implementation files for ICU, use the functions and
macros in `cmemory.h`. When allocated memory is returned from a C API function,
there must be a corresponding function (like a `ucnv_close()`) that deallocates
that memory.

All memory allocations in ICU should be checked for success. In the event of a
failure (a `NULL` returned from `uprv_malloc()`), a `U_MEMORY_ALLOCATION_ERROR` status
should be returned by the ICU function in question. If the allocation failure
leaves the ICU service in an invalid state, such that subsequent ICU operations
could also fail, the situation should be flagged so that the subsequent
operations will fail cleanly. Under no circumstances should a memory allocation
failure result in a crash in ICU code, or cause incorrect results rather than a
clean error return from an ICU function.

#### // Comments

C++ style // comments may be used in plain C files and in headers that will be
included in C files.

## Source Code Strings with Unicode Characters

### `char *` strings in ICU

| Declared type | encoding | example | Used with |
| --- | --- | --- | --- |
| `char *` | varies with platform | `"Hello"` | Most ICU API functions taking `char *` parameters. Unless otherwise noted, characters are restricted to the "Invariant" set, described below |
| `char *` | UTF-8 |  `u8"¡Hola!"` | Only functions that are explicitly documented as expecting UTF-8. No restrictions on the characters used. |
| `UChar *` | UTF-16 | `u"¡Hola!"` | All ICU functions with `UChar *` parameters |
| `UChar32` | Code Point value | `U'😁'` | UChar32 single code point constant. |
| `wchar_t` | unknown | `L"Hello"` | Not used with ICU. Unknown encoding, unknown size, not portable. |

ICU source files are UTF-8 encoded, allowing any Unicode character to appear in
Unicode string or character literals, without the need for escaping. But, for
clarity, use escapes when plain text would be confusing, e.g. for invisible
characters.

For convenience, ICU4C tends to use `char *` strings in places where only
"invariant characters" (a portable subset of the 7-bit ASCII repertoire) are
used. This allows locale IDs, charset names, resource bundle item keys and
similar items to be easily specified as string literals in the source code. The
same types of strings are also stored as "invariant character" `char *` strings
in the ICU data files.

ICU has hard coded mapping tables in `source/common/putil.c` to convert invariant
characters to and from Unicode without using a full ICU converter. These tables
must match the encoding of string literals in the ICU code as well as in the ICU
data files.

> :point_right: **Note**: Important: ICU assumes that at least the invariant
characters always have the same codes as is common on platforms with the same
charset family (ASCII vs. EBCDIC). **ICU has not been tested on platforms where
this is not the case.**

Some usage of `char *` strings in ICU assumes the system charset instead of
invariant characters. Such strings are only handled with the default converter
(See the following section). The system charset is usually a superset of the
invariant characters.

The following are the ASCII and EBCDIC byte values for all of the invariant
characters (see also `unicode/utypes.h`):

| Character(s) | ASCII | EBCDIC |
| --- | --- | --- |
| a..i | 61..69 | 81..89 |
| j..r | 6A..72 | 91..99 |
| s..z | 73..7A | A2..A9 |
| A..I | 41..49 | C1..C9 |
| J..R | 4A..52 | D1..D9 |
| S..Z | 53..5A | E2..E9 |
| 0..9 | 30..39 | F0..F9 |
| (space) | 20 | 40 |
| " | 22 | 7F |
| % | 25 | 6C |
| & | 26 | 50 |
| ' | 27 | 7D |
| ( | 28 | 4D |
| ) | 29 | 5D |
| \* | 2A | 5C |
| + | 2B | 4E |
| , | 2C | 6B |
| - | 2D | 60 |
| . | 2E | 4B |
| / | 2F | 61 |
| : | 3A | 7A |
| ; | 3B | 5E |
| < | 3C | 4C |
| = | 3D | 7E |
| > | 3E | 6E |
| ? | 3F | 6F |
| _ | 5F | 6D |

### Rules Strings with Unicode Characters

In order to include characters in source code strings that are not part of the
invariant subset of ASCII, one has to use character escapes. In addition, rules
strings for collation, etc. need to follow service-specific syntax, which means
that spaces and ASCII punctuation must be quoted using the following rules:

* Single quotes delineate literal text: `a'>'b` => `a>b`
* Two single quotes, either between or outside of single quoted text, indicate
  a literal single quote:
  * `a''b` => `a'b`
  * `a'>''<'b` => `a>'<b`
* A backslash precedes a single literal character:
* Several standard mechanisms are handled by `u_unescape()` and its variants.

> :point_right: **Note**: All of these quoting mechanisms are supported by the
`RuleBasedTransliterator`. The single quote mechanisms (not backslash, not
`u_unescape()`) are supported by the format classes. In its infancy,
`ResourceBundle` supported the `\uXXXX` mechanism and nothing else.
This quoting method is the current policy. However, there are modules within
the ICU services that are being updated and this quoting method might not have
been applied to all of the modules.

## Java Coding Conventions Overview

The ICU group uses the following coding guidelines to create software using the
ICU Java classes and methods.

### Code style

The standard order for modifier keywords on APIs is:

* `public static final synchronized strictfp`
* `public abstract`

Do not use wild card import, such as "`import java.util.*`". The sort order of
import statements is `java` / `javax` / `org` / `com`. Within each top level package
category, sub packages and classes are sorted by alphabetical order. We
recommend ICU developers to use the Eclipse IDE feature \[Source\] - \[Organize
Imports\] (Ctrl+Shift+O) to organize import statements.

All if/else/for/while/do loops use braces, even if the controlled statement is a
single line. This is for clarity and to avoid mistakes due to bad nesting of
control statements, especially during maintenance.

Tabs should not be present in source files.

Indentation is 4 spaces.

Make sure the code is formatted cleanly with regular indentation. Follow Java
style code conventions, e.g., don't put multiple statements on a single line,
use mixed-case identifiers for classes and methods and upper case for constants,
and so on.

Java source formatting rules described above is coming with the Eclipse project
file. It is recommended to run \[Source\] - \[Format\] (Ctrl+Shift+F) on Eclipse
IDE to clean up source files if necessary.

Use UTF-8 encoding (without BOM) for java source files.

Javadoc should be complete and correct when code is checked in, to avoid playing
catch-up later during the throes of the release. Please javadoc all methods, not
just external APIs, since this helps with maintenance.

### Code organization

Avoid putting more than one top-level class in a single file. Either use
separate files or nested classes.

Always define at least one constructor in a public API class. The Java compiler
automatically generates no-arg constructor when a class has no explicit
constructors. We cannot provide proper API documentations for such default
constructors.

Do not mix test, tool, and runtime code in the same file. If you need some
access to private or package methods or data, provide public accessors for them
and mark them `@internal`. Test code should be placed in `com.ibm.icu.dev.test`
package, and tools (e.g., code that generates data, source code, or computes
constants) in `com.ibm.icu.dev.tool` package. Occasionally for very simple cases
you can leave a few lines of tool code in the main source and comment it out,
but maintenance is easier if you just comment the location of the tools in the
source and put the actual code elsewhere.

Avoid creating new interfaces unless you know you need to mix the interface into
two or more classes that have separate inheritance. Interfaces are impossible to
modify later in a backwards-compatible way. Abstract classes, on the other hand,
can add new methods with default behavior. Use interfaces only if it is required
by the architecture, not just for expediency.

Current releases of ICU4J (since ICU 63) are restricted to use Java SE 7 APIs
and language features.

### ICU Packages

Public APIs should be placed in `com.ibm.icu.text`, `com.ibm.icu.util`, and
`com.ibm.icu.lang`. For historical reasons and for easier migration from JDK
classes, there are also APIs in `com.ibm.icu.math` but new APIs should not be
added there.

APIs used only during development, testing, or tools work should be placed in
`com.ibm.icu.dev`.

A class or method which is used by public APIs (listed above) but which is not
itself public can be placed in different places:

1. If it is only used by one class, make it private in that class.
2. If it is only used by one class and its subclasses, make it protected in
   that class. In general, also tag it `@internal` unless you are working on a
   class that supports user-subclassing (rare).
3. If it is used by multiple classes in one package, make it package private
   (also known as default access) and mark it `@internal`.
4. If it is used by multiple packages, make it public and place the class in
   `the com.ibm.icu.impl` package.

### Error Handling and Exceptions

Errors should be indicated by throwing exceptions, not by returning “bogus”
values.

If an input parameter is in error, then a new
`IllegalArgumentException("description")` should be thrown.

Exceptions should be caught only when something must be done, for example
special cleanup or rethrowing a different exception. If the error “should never
occur”, then throw a `new RuntimeException("description")` (rare). In this case,
a comment should be added with a justification.

Use exception chaining: When an exception is caught and a new one created and
thrown (usually with additional information), the original exception should be
chained to the new one.

A catch expression should not catch Throwable. Catch expressions should specify
the most specific subclass of Throwable that applies. If there are two concrete
subclasses, both should be specified in separate catch statements.

### Binary Data Files

ICU4J uses the same binary data files as ICU4C, in the big-endian/ASCII form.
The `ICUBinary` class should be used to read them.

Some data sources (for example, compressed Jar files) do not allow the use of
several `InputStream` and related APIs:

* Memory mapping is efficient, but not available for all data sources.
* Do not depend on `InputStream.available()`: It does not provide reliable
  information for some data sources. Instead, the length of the data needs to
  be determined from the data itself.
* Do not call `mark()` and `reset()` methods on `InputStream` without wrapping the
  `InputStream` object in a new `BufferedInputStream` object. These methods are
  not implemented by the `ZipInputStream` class, and their use may result in an
  `IOException`.

### Compiler Warnings

There should be no compiler warnings when building ICU4J. It is recommended to
develop using Eclipse, and to fix any problems that are shown in the Eclipse
Problems panel (below the main window).

When a warning is not avoidable, you should add `@SuppressWarnings` annotations
with minimum scope.

### Miscellaneous

Objects should not be cast to a class in the `sun.*` packages because this would
cause a `SecurityException` when run under a `SecurityManager`. The exception needs
to be caught and default action taken, instead of propagating the exception.

## Adding .c, .cpp and .h files to ICU

In order to add compilable files to ICU, add them to the source code control
system in the appropriate folder and also to the build environment.

To add these files, use the following steps:

1. Choose one of the ICU libraries:
   * The common library provides mostly low-level utilities and basic APIs that
     often do not make use of Locales. Examples are APIs that deal with character
     properties, the Locale APIs themselves, and ResourceBundle APIs.
   * The i18n library provides Locale-dependent and -using APIs, such as for
     collation and formatting, that are most useful for internationalized user
     input and output.
2. Put the source code files into the folder `icu/source/library-name`, then add
   them to the build system:
   * For most platforms, add the expected .o files to
     `icu/source/library-name/Makefile.in`, to the OBJECTS variable. Add the
     **public** header files to the HEADERS variable.
   * For Microsoft Visual C++ 6.0, add all the source code files to
     `icu/source/library-name/library-name.dsp`. If you don't have Visual C++, add
     the filenames to the project file manually.
3. Add test code to `icu/source/test/cintltest` for C APIs and to
   `icu/source/test/intltest` for C++ APIs.
4. Make sure that the API functions are called by the test code (100% API
   coverage) and that at least 85% of the implementation code is exercised by
   the tests (>=85% code coverage).
5. Create test code for C using the `log_err()`, `log_info()`, and `log_verbose()`
   APIs from `cintltst.h` (which uses `ctest.h`) and check it into the appropriate
   folder.
6. In order to get your C test code called, add its top level function and a
   descriptive test module path to the test system by calling `addTest()`. The
   function that makes the call to `addTest()` ultimately must be called by
   `addAllTests()` in `calltest.c`. Groups of tests typically have a common
   `addGroup()` function that calls `addTest()` for the test functions in its
   group, according to the common part of the test module path.
7. Add that test code to the build system also. Modify `Makefile.in` and the
   appropriate `.dsp` file (For example, the file for the library code).

## C Test Suite Notes

The cintltst Test Suite contains all the tests for the International Components
for Unicode C API. These tests may be automatically run by typing "cintltst" or
"cintltst -all" at the command line. This depends on the C Test Services:
`cintltst` or `cintltst -all`.

### C Test Services

The purpose of the test services is to enable the writing of tests entirely in
C. The services have been designed to make creating tests or converting old ones
as simple as possible with a minimum of services overhead. A sample test file,
"demo.c", is included at the end of this document. For more information
regarding C test services, please see the `icu4c/source/tools/ctestfw` directory.

### Writing Test Functions

The following shows the possible format of test functions:

```c++
void some_test()
{
}
```

Output from the test is accomplished with three printf-like functions:

```c++
void log_err ( const char *fmt, ... );
void log_info ( const char *fmt, ... );
void log_verbose ( const char *fmt, ... );
```

* `log_info()` writes to the console for informational messages.
* `log_verbose()` writes to the console ONLY if the VERBOSE flag is turned
  on (or the `-v` option to the command line). This option is useful for
  debugging. By default, the VERBOSE flag is turned OFF.
* `log_error()` can be called when a test failure is detected. The error is
  then logged and error count is incremented by one.

To use the tests, link them into a hierarchical structure. The root of the
structure will be allocated by default.

```c++
TestNode *root = NULL; /* empty */
addTest( &root, &some_test, "/test");
```

Provide `addTest()` with the function pointer for the function that performs the
test as well as the absolute 'path' to the test. Paths may be up to 127 chars in
length and may be used to group tests.

The calls to `addTest` must be placed in a function or a hierarchy of functions
(perhaps mirroring the paths). See the existing cintltst for more details.

### Running the Tests

A subtree may be extracted from another tree of tests for the programmatic
running of subtests.

```c++
TestNode* sub;
sub = getTest(root, "/mytests");
```

And a tree of tests may be run simply by:

```c++
runTests( root ); /* or 'sub' */
```

Similarly, `showTests()` lists out the tests. However, it is easier to use the
command prompt with the Usage specified below.

### Globals

The command line parser resets the error count and prints a summary of the
failed tests. But if `runTest` is called directly, for instance, it needs to be
managed manually. `ERROR_COUNT` contains the number of times `log_err` was
called. `runTests` resets the count to zero before running the tests.
`VERBOSITY` must be 1 to display `log_verbose()` data. Otherwise, `VERBOSITY`
must be set to 0 (default).

### Building cintltst

To compile this test suite using Microsoft Visual C++ (MSVC), follow the
instructions in `icu4c/source/readme.html#HowToInstall` for building the `allC`
workspace. This builds the libraries as well as the `cintltst` executable.

### Executing cintltst

To run the test suite from the command line, change the directories to
`icu4c/source/test/cintltst/Debug` for the debug build (or
`icu4c/source/test/cintltst/Release` for the release build) and then type `cintltst`.

### cintltst Usage

Type `cintltst -h` to view its command line parameters.

```text
### Syntax:
### Usage: [ -l ] [ -v ] [ -verbose] [-a] [ -all] [-n]
 [-no_err_msg] [ -h] [ /path/to/test ]
### -l To get a list of test names
### -all To run all the test
### -a To run all the test(same as -all)
### -verbose To turn ON verbosity
### -v To turn ON verbosity(same as -verbose)
### -h To print this message
### -n To turn OFF printing error messages
### -no_err_msg (same as -n)
### -[/subtest] To run a subtest
### For example to run just the utility tests type: cintltest /tsutil)
### To run just the locale test type: cintltst /tsutil/loctst
###

/******************** sample ctestfw test ********************
********* Simply link this with libctestfw or ctestfw.dll ****
************************* demo.c *****************************/

#include "stdlib.h"
#include "ctest.h"
#include "stdio.h"
#include "string.h"

/**
* Some sample dummy tests.
* the statics simply show how often the test is called.
*/
void mytest()
{
    static i = 0;
    log_info("I am a test[%d]\n", i++);
}

void mytest_err()
{
    static i = 0;
    log_err("I am a test containing an error[%d]\n", i++);
    log_err("I am a test containing an error[%d]\n", i++);
}

void mytest_verbose()
{
    /* will only show if verbose is on (-v) */
    log_verbose("I am a verbose test, blabbing about nothing at
all.\n");
}

/**
* Add your tests from this function
*/

void add_tests( TestNode** root )
{
    addTest(root, &mytest, "/apple/bravo" );
    addTest(root, &mytest, "/a/b/c/d/mytest");
    addTest(root, &mytest_err, "/d/e/f/h/junk");
    addTest(root, &mytest, "/a/b/c/d/another");
    addTest(root, &mytest, "/a/b/c/etest");
    addTest(root, &mytest_err, "/a/b/c");
    addTest(root, &mytest, "/bertrand/andre/damiba");
    addTest(root, &mytest_err, "/bertrand/andre/OJSimpson");
    addTest(root, &mytest, "/bertrand/andre/juice/oj");
    addTest(root, &mytest, "/bertrand/andre/juice/prune");
    addTest(root, &mytest_verbose, "/verbose");

}

int main(int argc, const char *argv[])
{
    TestNode *root = NULL;

    add_tests(&root); /* address of root ptr- will be filled in */

    /* Run the tests. An int is returned suitable for the OS status code.
    (0 for success, neg for parameter errors, positive for the # of
    failed tests) */
    return processArgs( root, argc, argv );
}
```

## C++ IntlTest Test Suite Documentation

The IntlTest suite contains all of the tests for the C++ API of International
Components for Unicode. These tests may be automatically run by typing `intltest`
at the command line. Since the verbose option prints out a considerable amount
of information, it is recommended that the output be redirected to a file:
`intltest -v > testOutput`.

### Building IntlTest

To compile this test suite using MSVC, follow the instructions for building the
`alCPP` (All C++ interfaces) workspace. This builds the libraries as well as the
`intltest` executable.

### Executing IntelTest

To run the test suite from the command line, change the directories to
`icu4c/source/test/intltest/Debug`, then type: `intltest -v >testOutput`. For the
release build, the executable will reside in the
`icu4c/source/test/intltest/Release` directory.

### IntelTest Usage

Type just `intltest -h` to see the usage:

```text
### Syntax:
### IntlTest [-option1 -option2 ...] [testname1 testname2 ...]
### where options are: verbose (v), all (a), noerrormsg (n),
### exhaustive (e) and leaks (l).
### (Specify either -all (shortcut -a) or a test name).
### -all will run all of the tests.
###
### To get a list of the test names type: intltest LIST
### To run just the utility tests type: intltest utility
###
### Test names can be nested using slashes ("testA/subtest1")
### For example to list the utility tests type: intltest utility/LIST
### To run just the Locale test type: intltest utility/LocaleTest
###
### A parameter can be specified for a test by appending '@' and the value
### to the testname.
```

## C: Testing with Fake Time

The "Fake Time" capability allows ICU4C to be tested as if the hardware clock is
set to a specific time. This section documents how to use this facility.
Note that this facility requires the POSIX `'gettimeofday'` function to be
operable.

This facility affects all ICU 'current time' calculations, including date,
calendar, time zone formats, and relative formats. It doesn't affect any calls
directly to the underlying operating system.

1. Build ICU with the **`U_DEBUG_FAKETIME`** preprocessor macro set. This can
   be accomplished with the following line in a file
   **icu/source/icudefs.local** :

   ```shell
   CPPFLAGS+=-DU_DEBUG_FAKETIME
   ```

2. Determine the `UDate` value (the time value in milliseconds ± Midnight, Jan 1,
   1970 GMT) which you want to use as the target. For this sample we will use
   the value `28800000`, which is Midnight, Pacific Standard Time 1/1/1970.
3. Set the environment variable `U_FAKETIME_START=28800000`
4. Now, the first time ICU checks the current time, it will start at midnight
   1/1/1970 (pacific time) and roll forward. So, at the end of 10 seconds of
   program runtime, the clock will appear to be at 12:00:10.
5. You can test this by running the utility '`icuinfo -m`' which will print out
   the 'Milliseconds since Epoch'.
6. You can also test this by running the cintltest test
   `/tsformat/ccaltst/TestCalendar` in verbose mode which will print out the
   current time:

   ```shell
   $ make check ICUINFO_OPTS=-m U_FAKETIME_START=28800000 CINTLTST_OPTS=-v
   /tsformat/ccaltst/TestCalendar
   U_DEBUG_FAKETIME was set at compile time, so the ICU clock will start at a
   preset value
   env variable U_FAKETIME_START=28800000 (28800000) for an offset of
   -1281957858861 ms from the current time 1281986658861
   PASS: The current date and time fetched is Thursday, January 1, 1970 12:00:00
   ```

## C: Threading Tests

Threading tests for ICU4C functions should be placed in under utility /
`MultithreadTest`, in the file `intltest/tsmthred.h` and `.cpp`. See the existing
tests in this file for examples.

Tests from this location are automatically run under the [Thread
Sanitizer](https://github.com/google/sanitizers/wiki/ThreadSanitizerCppManual)
(TSAN) in the ICU continuous build system. TSAN will reliably detect race
conditions that could possibly occur, however improbable that occurrence might
be normally.

Data races are one of the most common and hardest to debug types of bugs in
concurrent systems. A data race occurs when two threads access the same variable
concurrently and at least one of the accesses is write. The C++11 standard
officially bans data races as undefined behavior.

## Binary Data Formats

ICU services rely heavily on data to perform their functions. Such data is
available in various more or less structured text file formats, which make it
easy to update and maintain. For high runtime performance, most data items are
pre-built into binary formats, i.e., they are parsed and processed once and then
stored in a format that is used directly during processing.

Most of the data items are pre-built into binary files that are then installed
on a user's machine. Some data can also be built at runtime but is not
persistent. In the latter case, a primary object should be built once and then
cloned to avoid the multiple parsing, processing, and building of the same data.

Binary data formats for ICU must be portable across platforms that share the
same endianness and the same charset family (ASCII vs. EBCDIC). It would be
possible to handle data from other platform types, but that would require
load-time or even runtime conversion.

### Data Types

Binary data items are memory-mapped, i.e., they are used as readonly, constant
data. Their structures must be portable according to the criteria above and
should be efficiently usable at runtime without building additional runtime data
structures.

Most native C/C++ data types cannot be used as part of binary data formats
because their sizes are not fixed across compilers. For example, an int could be
16/32/64 or even any other number of bits wide. Only types with absolutely known
widths and semantics must be used.

Use for example:

* `uint8_t`, `uint16_t`, `int32_t` etc.
* `UBool`: same as `int8_t`
* `UChar`: for 16-bit Unicode strings
* `UChar32`: for Unicode code points
* `char`: for "invariant characters", see `utypes.h`

> :point_right: **Note**: ICU assumes that `char` is an 8-bit byte but makes no
assumption about its signedness.

**Do not use** for example:

* `short`, `int`, `long`, `unsigned int` etc.: undefined widths
* `float`, `double`: undefined formats
* `bool`: undefined width and signedness
* `enum`: undefined width and signedness
* `wchar_t`: undefined width, signedness and encoding/charset

Each field in a binary/mappable data format must be aligned naturally. This
means that a field with a primitive type of size n bytes must be at an n-aligned
offset from the start of the data block. `UChar` must be 2-aligned, `int32_t` must
be 4-aligned, etc.

It is possible to use struct types, but one must make sure that each field is
naturally aligned, without possible implicit field padding by the compiler —
assuming a reasonable compiler.

```c++
// bad because i will be preceded by compiler-dependent padding
// for proper alignment
struct BadExample {
    UBool flag;
    int32_t i;
};

// ok with explicitly added padding or generally conscious
// sequence of types
struct OKExample {
    UBool flag;
    uint8_t pad[3];
    int32_t i;
};
```

Within the binary data, a `struct` type field must be aligned according to its
widest member field. The struct `OKExample` must be 4-aligned because it contains
an `int32_t` field. Make padding explicit via additional fields, rather than
letting the compiler choose optional padding.

Another potential problem with `struct` types, especially in C++, is that some
compilers provide RTTI for all classes and structs, which inserts a `_vtable`
pointer before the first declared field. When using `struct` types with
binary/mappable data in C++, assert in some place in the code that `offsetof` the
first field is 0. For an example see the genpname tool.

### Versioning

ICU data files have a `UDataHeader` structure preceding the actual data. Among
other fields, it contains a `formatVersion` field with four parts (one `uint8_t`
each). It is best to use only the first (major) or first and second
(major/minor) fields in the runtime code to determine binary compatibility,
i.e., reject a data item only if its `formatVersion` contains an unrecognized
major (or major/minor) version number. The following parts of the version should
be used to indicate variations in the format that are backward compatible, or
carry other information.

For example, the current `uprops.icu` file's `formatVersion` (see the genprops tool
and `uchar.c`/`uprops.c`) is set to indicate backward-incompatible changes with the
major version number, backward-compatible additions with the minor version
number, and shift width constants for the `UTrie` data structure in the third and
fourth version numbers (these could change independently of the `uprops.icu`
format).

## C/C++ Debugging Hints and Tips

### Makefile-based platforms

* use `Makefile.local` files (override of `Makefile`), or `icudefs.local` (at the
  top level, override of `icudefs.mk`) to avoid the need to modify
  change-controlled source files with debugging information.
  * Example: **`CPPFLAGS+=-DUDATA_DEBUG`** in common to enable data
    debugging
  * Example: **`CINTLTST_OPTS=/tscoll`** in the cintltst directory provides
    arguments to the cintltest test upon make check, to only run collation
    tests.
    * intltest: `INTLTEST_OPTS`
    * cintltst: `CINTLTST_OPTS`
    * iotest: `IOTEST_OPTS`
    * icuinfo: `ICUINFO_OPTS`
    * (letest does not have an OPTS variable as of ICU 4.6.)

### Windows/Microsoft Visual Studio

The following addition to autoexp.dat will cause **`UnicodeString`**s to be
visible as strings in the debugger without expanding sub-items:

```text
;; Copyright (C) 2010 IBM Corporation and Others. All Rights Reserved.
;; ICU Additions
;; Add to {VISUAL STUDIO} \Common7\Packages\Debugger\autoexp.dat
;;   in the [autoexpand] section just before the final [hresult] section.
;;
;; Need to change 'icu_##' to the current major+minor (so icu_46 for 4.6.1 etc)

icu_46::UnicodeString {
    preview        (
              #if($e.fFlags & 2)   ; stackbuffer
               (
                  #(
                "U= '",
                [$e.fUnion.fStackBuffer, su],
                "', len=",
                [$e.fShortLength, u]
                ;[$e.fFields.fArray, su]
               )
              )
              #else
               (
                  #(
                "U* '",
                [$e.fUnion.fFields.fArray, su],
                "', len=",
                [$e.fShortLength, u]
                ;[$e.fFields.fArray, su]
               )
              )
            )

    stringview    (
              #if($e.fFlags & 2)   ; stackbuffer
               (
                  #(
                "U= '",
                [$e.fUnion.fStackBuffer, su],
                "', len=",
                [$e.fShortLength, u]
                ;[$e.fFields.fArray, su]
               )
              )
              #else
               (
                  #(
                "U* '",
                [$e.fUnion.fFields.fArray, su],
                "', len=",
                [$e.fShortLength, u]
                ;[$e.fFields.fArray, su]
               )
              )
            )

}
;;;
;;; End ICU Additions
;;;
```
