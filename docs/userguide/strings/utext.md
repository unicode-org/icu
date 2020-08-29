---
layout: default
title: UText
nav_order: 4
parent: Chars and Strings
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# UText

## Overview

UText is a text abstraction facility for ICU

The intent is to make it possible to extend ICU to work with text data that is
in formats above and beyond those that are native to ICU.

UText directly supports text in these formats:

1.  UTF-8 (`char*`) strings
2.  UTF-16 (`UChar*` or `UnicodeString`) strings
3.  `Replaceable`

The ICU services that can accept UText based input are:

1.  Regular Expressions
2.  Break Iteration

Examples of text formats that UText could be extended to support:

1.  UTF-32 format.
2.  Text that is stored in discontiguous chunks in memory, or in application-specific representations.
3.  Text that is in a non-Unicode code page

If ICU does not directly support a desired text format, it is possible for
application developers themselves to extend UText, and in that way gain the
ability to use their text with ICU.

## Using UText

There are three fairly distinct classes of use of UText. These are:

1.  **Simple wrapping of existing text.** Application text data exists in a
    format that is already supported by UText (such as UTF-8). The application
    opens a UText on the data, and then passes the UText to an ICU service for
    analysis/processing. Most use of UText from applications will follow this
    simple pattern. Only a very few UText APIs and only a few lines of code are
    required.

2.  **Accessing the underlying text.** UText provides APIs for iterating over
    the text in various ways, and for fetching individual code points from the
    text. These functions will probably be used primarily from within ICU, in
    the implementation of services that can accept input in the form of a UText.
    While applications are certainly free to use these text access functions if
    necessary, there may often be no need.

3.  **UText support for new text storage formats.** If an application has text
    data stored in a format that is not directly supported by ICU, extending
    UText to support that format will provide the ability to conveniently use
    those ICU services that support UText.

    Extending UText to a new format is accomplished by implementing a well
    defined set of *Text Provider Functions* for that format.

## UText compared with CharacterIterator

CharacterIterator is an abstract base class that defines a protocol for
accessing characters in a text-storage object. This class has methods for
iterating forward and backward over Unicode characters to return either the
individual Unicode characters or their corresponding index values.

UText and CharacterIterator both provide an abstraction for accessing text while
hiding details of the actual storage format. UText is the more flexible of the
two, however, with these advantages:

1.  UText can conveniently operate on text stored in formats other than UTF-16.
2.  UText includes functions for modifying or editing the text.
3.  UText is more efficient. When iterating over a range of text using the
    CharacterIterator API, a function call is required for every character. With
    UText, iterating to the next character is usually done with small amount of
    inline code.

At this time, more ICU services support CharacterIterator than UText. ICU
services that can operate on text represented by a CharacterIterator are

1.  Normalizer
2.  Break Iteration
3.  String Search
4.  Collation Element Iteration

## Example: Counting the Words in a UTF-8 String

Here is a function that uses UText and an ICU break iterator to count the number
of words in a nul-terminated UTF-8 string. The use of UText only adds two lines
of code over what a similar function operating on normal UTF-16 strings would
require.

```c
#include "unicode/utypes.h"
#include "unicode/ubrk.h"
#include "unicode/utext.h"

int countWords(const char *utf8String) {
    UText          *ut        = NULL;
    UBreakIterator *bi        = NULL;
    int             wordCount = 0;
    UErrorCode      status    = U_ZERO_ERROR;

    ut = utext_openUTF8(ut, utf8String, -1, &status);
    bi = ubrk_open(UBRK_WORD, "en_us", NULL, 0, &status);

    ubrk_setUText(bi, ut, &status);
    while (ubrk_next(bi) != UBRK_DONE) {
        if (ubrk_getRuleStatus(bi) != UBRK_WORD_NONE) {
            /* Count only words and numbers, not spaces or punctuation */
            wordCount++;
        }
    }
    utext_close(ut);
    ubrk_close(bi);
    assert(U_SUCCESS(status));
    return wordCount;
}
```

## UText API Functions

The UText API is declared in the ICU header file
[utext.h](https://github.com/unicode-org/icu/blob/master/icu4c/source/common/unicode/utext.h)

### Opening and Closing.

Normal usage of UText by an application consists of opening a UText to wrap some
existing text, then passing the UText to ICU functions for processing. For this
kind of usage, all that is needed is the appropriate UText open and close
functions.

| Function | Description |
|--------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `uext_openUChars` | Open a UText over a standard ICU (`UChar *`) string. The string consists of a UTF-16 array in memory, either nul terminated or with an explicit length. |
| `utext_openUnicodeString` | Open a UText over an instance of an ICU C++ `UnicodeString`. |
| `Utext_openConstUnicodeString` | Open a UText over a read-only `UnicodeString`. Disallows UText APIs that modify the text. |
| `utext_openReplaceable` | Open a UText over an instance of an ICU C++ `Replaceable`. |
| `utext_openUTF8` | Open a UText over a UTF-8 encoded C string. May be either Nul terminated or have an explicit length. |
| `utext_close` | Close an open UText. Frees any allocated memory; required to prevent memory leaks. |

Here are some suggestions and techniques for efficient use of UText.

#### Minimizing Heap Usage

Utext's open functions include features to allow applications to minimize the
number of heap memory allocations that will be needed. Specifically,

1.  UText structs may declared as local variables, that is, they may be stack
    allocated rather than heap allocated.
2.  Existing UText structs may be reused to refer to new text, avoiding the need
    to allocate and initialize a new UText instance.

Minimizing heap allocations is important in code that has critical performance
requirements, and is doubly important for code that must scale well in
multithreaded, multiprocessor environments.

#### Stack Allocation

Here is code for stack-allocating a UText:

```c
UText mytext = UTEXT_INITIALIZER;
utext_openUChars(&myText, ...
```

The first parameter to all `utext_open` functions is a pointer to a UText. If it
is non-null, the supplied UText will be used; if it is null, a new UText will be
heap allocated.

Stack allocated UText objects *must *be initialized with `UTEXT_INITIALIZER`. An
uninitialized instance will fail to open.

#### Heap Allocation

Here is code for creating a heap allocated UText:

```c
UText *mytext = utext_openUChars(NULL, ...
```

This is slightly smaller and more convenient to write than the stack allocated
code, and there is no reason not to use heap allocated UText objects in the vast
majority of code that does not have extreme performance constraints.

#### Reuse

To reuse an existing UText, simply pass it as the first parameter to any of the
UText open functions. There is no need to close the UText first, and it may
actually be more efficient not to close it first.

Here is an example of a function that iterates over an array of UTF-8 strings,
wrapping each in a UText and passing it off to another function. On the first
time through the loop the utext open function will heap allocate a UText. On
each subsequent iterations the existing UText will be reused.

```c
#include "unicode/utypes.h"
#include "unicode/utext.h"

void f(char **strings, int numStrings) {
    UText *ut = NULL;
    UErrorCode status;

    int i;
    for (i=0; i<numStrings; i++) {
        status = U_ZERO_ERROR;
        ut = utext_openUTF8(ut, strings[i], -1, &status);
        assert(U_SUCCESS(status));
        do_something(ut);
    }
    utext_close(ut);
}
```

#### close

Closing a UText with `utext_close()` frees any storage associated with it, including the UText itself
for those that are heap allocated. Stack allocated UTexts should also be closed
because in some cases there may be additional heap allocated storage associated
with them, depending on the type of the underlying text storage.

## Accessing the Text

For accessing the underlying text, UText provides functions both for iterating
over the characters, and for direct random access by index. Here are the
conventions that apply for all of the access functions:

1.  access to individual characters is always by code points, that is, 32 bit
    Unicode values are always returned. UTF-16 surrogate values from a surrogate
    pair, like bytes from a UTF-8 sequence, are not separately visible.
2.  Indexing always uses the index values from the original underlying text
    storage, in whatever form it has. If the underlying storage is UTF-8, the
    indexes will be UTF-8 byte indexes, not UTF-16 offsets.
3.  Indexes always refer to the first position of a character. This is
    equivalent to saying that indexes always lie at the boundary between
    characters. If an index supplied to a UText function refers to the 2<sup>nd</sup>
    through the N<sup>th</sup> positions of a multi byte or multi-code-unit character, the
    index will be normalized back to the first or lowest index.
4.  An input index that is greater than the length of the text will be set to
    refer to the end of the string, and will not generate out of bounds error.
    This is similar to the indexing behavior in the UnicodeString class.
5.  Iteration uses post-increment and pre-decrement conventions. That is,
    `utext_next32()` fetches the code point at the current index, then leaves the
    index pointing at the next character.

Here are the functions for accessing the actual text data represented by a
UText. The primary use of these functions will be in the implementation of ICU
services that accept input in the form of a UText, although application code may
also use them if the need arises.

For more detailed descriptions of each, see the API reference.

| Function | Description |
|-------------------------|------------------------------------------------------------------------------------------------------------|
| `utext_nativeLength` | Get the length of the text string in terms of the underlying native storage – bytes for UTF-8, for example |
| `utext_isLengthExpensive` | Indicate whether determining the length of the string would require scanning the string. |
| `utext_char32At` | Get the code point at the specified index. |
| `utext_current32` | Get the code point at the current iteration position. Does not advance the position. |
| `utext_next32` | Get the next code point, iterating forwards. |
| `utext_previous32` | Get the previous code point, iterating backwards. |
| `utext_next32From` | Begin a forwards iteration at a specified index. |
| `utext_previous32From` | Begin a reverse iteration at a specified index. |
| `utext_getNativeIndex` | Get the current iteration index. |
| `utext_setNativeIndex` | Set the iteration index. |
| `utext_moveIndex32` | Move the current index forwards or backwards by the specified number of code points. |
| `utext_extract` | Retrieve a range of text, placing it into a UTF-16 buffer. |
| `UTEXT_NEXT32` | inline (high performance) version of `utext_next32` |
| `UTEXT_PREVIOUS32` | inline (high performance) version of `utext_previous32` |

## Modifying the Text

UText provides API for modifying or editing the text.

| Function | Description |
|---------------------|----------------------------------------------------------------------------------------------------|
| `utext_replace` | Replace a range of the original text with a replacement string. |
| `utext_copy` | Copy or Move a range of the text to a new position. |
| `utext_isWritable` | Test whether a UText supports writing operations. |
| `utext_hasMetaData` | Test whether the text includes metadata. See the class `Replaceable` for more information on meta data.. |

Certain conventions must be followed when modifying text using these functions:

1.  Not all types of UText can support modifying the data. Code working with
    UText instances of unknown origin should check `utext_isWritable()` first, and
    be prepared to deal with failures.
2.  There must be only one UText open onto the underlying string that is being
    modified. (Strings that are not being modified can be the target of any
    number of UTexts at the same time) The existence of a second UText that
    refers to a string that is being modified is not a situation that is
    detected by the implementation. The application code must be structured to
    avoid the situation.

#### Cloning

UText instances may be cloned. The clone function,

```c
UText * utext_clone(UText *dest,
    const UText *src,
    UBool deep,
    UBool readOnly,
    UErrorCode *status)
```

behaves very much like a UText open functions, with the source of the text being
another UText rather than some other form of a string.

A *shallow* clone creates a new UText that maintains its own iteration state,
but does not clone the underlying text itself.

A *deep* clone copies the underlying text in addition to the UText state. This
would be appropriate if you wished to modify the text without the changes being
reflected back to the original source string. Not all text providers support
deep clone, so checking for error status returns from `utext_clone()` is
importatnt.

#### Thread Safety

UText follows the usual ICU conventions for thread safety: concurrent calls to
functions accessing the same non-const UText is not supported. If concurrent
access to the text is required, the UText can be cloned, allowing each thread
access via a separate UText. So long as the underlying text is not being
modified, a shallow clone is sufficient.

## Text Providers

A *text provider* is a set of functions that let UText support a specific text
storage format.

ICU includes several UText text provider implementations, and applications can
provide additional ones if needed.

To implement a new UText text provider, it is necessary to have an understanding
of how UText is designed.

Underneath the covers, UText is a struct that includes:

1.  A pointer to a *Text Chunk*, which is a UTF-16 buffer containing a section
    (or all) of the text being referenced.
    
    For text sources whose native format
    is UTF-16, the chunk description can refer directly to the original text
    data. For non-UTF-16 sources, the chunk will refer to a side buffer
    containing some range of the text that has been converted to UTF-16 format.
2.  The iteration position, as a UTF-16 offset within the chunk.

If a text access function (one of those described above, in the previous
section) can do its thing based on the information maintained in the UText
struct, it will. If not, it will call out to one of the provider functions
(below) to do the work, or to update the UText.

The best way to really understand what is required of a UText provider is to
study the implementations that are included with ICU, and to borrow as much as
possible.

Here is the list of text provider functions.

| Function | Description |
|----------------------------|----------------------------------------------------------------------------------------------------|
| `UTextAccess` | Set up the Text Chunk associated with this UText so that it includes a requested index position. |
| `UTextNativeLength` | Return the full length of the text. |
| `UTextClone` | Clone the UText. |
| `UTextExtract` | Extract a range of text into a caller-supplied buffer |
| `UTextReplace` | Replace a range of text with a caller-supplied replacement. May expand or shrink the overall text. |
| `UTextCopy` | Move or copy a range of text to a new position. |
| `UTextMapOffsetToNative` | Within the current text chunk, translate a UTF-16 buffer offset to an absolute native index. |
| `UTextMapNativeIndexToUTF16` | Translate an absolute native index to a UTF-16 buffer offset within the current text. |
| `UTextClose` | Provider specific close. Free storage as required. |

Not every provider type requires all of the functions. If the text type is
read-only, no implementation for Replace or Copy is required. If the text is in
UTF-16 format, no implementation of the native to UTF-16 index conversions is
required.

To fully understand what is required to support a new string type with UText, it
will be necessary to study both the provider function declarations from
[utext.h](https://github.com/unicode-org/icu/blob/master/icu4c/source/common/unicode/utext.h)
and the existing text provider implementations in
[utext.cpp](https://github.com/unicode-org/icu/blob/master/icu4c/source/common/utext.cpp).
