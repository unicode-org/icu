---
layout: default
title: API Details
nav_order: 6
parent: Collation
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Collation API Details
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

This section describes some of the usage conventions for the ICU Collation
Service API.

## Collator Instantiation

To use the Collation Service, you must instantiate a `Collator`. The
Collator defines the properties and behavior of the sort ordering. The Collator
can be repeatedly referenced until all collation activities have been performed.
The Collator can then be closed and removed.

### Instantiating the Predefined Collators

ICU comes with a large set of already predefined collators that are suited for
specific locales. Most of the ICU locales have a predefined collator. In the worst
case, the CLDR default set of rules,
which is mostly equivalent to the UCA default ordering (DUCET), is used.
The default sort order itself is designed to work well for many languages.
(For example, there are no tailorings for the standard sort orders for
English, German, French, etc.)

To instantiate a predefined collator, use the APIs `ucol_open`, `createInstance` and
`getInstance` for C, C++ and Java codes respectively. The C API takes a locale ID
(or language tag) string argument, C++ takes a Locale object, and Java takes a
Locale or ULocale.

For some languages, multiple collation types are available; for example,
"de-u-co-phonebk" / "de@collation=phonebook". They can be enumerated via
`Collator::getKeywordValuesForLocale()`. See also the list of available collation
tailorings in the online [ICU Collation
Demo](http://demo.icu-project.org/icu-bin/collation.html).

Starting with ICU 54, collation attributes can be specified via locale keywords
as well, in the old locale extension syntax ("el@colCaseFirst=upper") or in
language tag syntax ("el-u-kf-upper"). Keywords and values are case-insensitive.

See the [LDML Collation spec, Collation
Settings](http://www.unicode.org/reports/tr35/tr35-collation.html#Collation_Settings),
and the [data
file](https://github.com/unicode-org/cldr/blob/master/common/bcp47/collation.xml) listing
the valid collation keywords and their values. (The deprecated attributes
kh/colHiraganaQuaternary and vt/variableTop are not supported.)

For the [old locale extension
syntax](http://www.unicode.org/reports/tr35/tr35.html#Old_Locale_Extension_Syntax),
the data file's alias names are used (first alias, if defined, otherwise the
name): "de@collation=phonebook;colCaseLevel=yes;kv=space"

For the language tag syntax, the non-alias names are used, and "true" values can
be omitted: "de-u-co-phonebk-kc-kv-space"

This example demonstrates the instantiation of a collator.

**C:**

```c
UErrorCode status = U_ZERO_ERROR;
UCollator *coll = ucol_open("en_US", &status);
if(U_SUCCESS(status)) {
    /* close the collator*/
    ucol_close(coll);
}
```

**C++:**

```c++
UErrorCode status = U_ZERO_ERROR;
Collator *coll = Collator::createInstance(Locale("en", "US"), status);
if(U_SUCCESS(status)) {
    //close the collator
    delete coll;
}
```

**Java:**

```java
Collator col = null;
try {
    col = Collator.getInstance(Locale.US);
} catch (Exception e) {
    System.err.println("English collation creation failed.");
    e.printStackTrace();
}
```

### Instantiating Collators Using Custom Rules

If the ICU predefined collators are not appropriate for your intended usage, you
can define your own set of rules and instantiate a collator that uses them. For more
details, please see [the section on collation customization](customization/index).

This example demonstrates the instantiation of a collator.

**C:**

```c
UErrorCode status = U_ZERO_ERROR;
U_STRING_DECL(rules, "&9 < a, A < b, B < c, C; ch, cH, Ch, CH < d, D, e, E", 52);
UCollator *coll;

U_STRING_INIT(rules, "&9 < a, A < b, B < c, C; ch, cH, Ch, CH < d, D, e, E", 52);
coll = ucol_openRules(rules, -1, UCOL_ON, UCOL_DEFAULT_STRENGTH, NULL, &status);
if(U_SUCCESS(status)) {
    /* close the collator*/
    ucol_close(coll);
}
```

**C++:**

```c++
UErrorCode status = U_ZERO_ERROR;
UnicodeString rules(u"&9 < a, A < b, B < c, C; ch, cH, Ch, CH < d, D, e, E");
Collator *coll = new RuleBasedCollator(rules, status);
if(U_SUCCESS(status)) {
    //close the collator
    delete coll;
}
```

**Java:**

```java
RuleBasedCollator coll = null;
String ruleset = "&9 < a, A < b, B < c, C; ch, cH, Ch, CH < d, D, e, E";
try {
    coll = new RuleBasedCollator(ruleset);
} catch (Exception e) {
    System.err.println("Customized collation creation failed.");
    e.printStackTrace();
}
```

## Compare

Two of the most used functions in ICU collation API, `ucol_strcoll` and `ucol_getSortKey`, have their counterparts in both Win32 and ANSI APIs:

ICU C             | ICU C++                     | ICU Java                   | ANSI/POSIX | WIN32
----------------- | --------------------------- | -------------------------- | ---------- | -----
`ucol_strcoll`    | `Collator::compare`         | `Collator.compare`         | `strcoll`  | `CompareString`
`ucol_getSortKey` | `Collator::getSortKey`      | `Collator.getCollationKey` | `strxfrm`  | `LCMapString`
&nbsp;            | `Collator::getCollationKey` | &nbsp;                     | &nbsp;     |

For more sophisticated usage, such as user-controlled language-sensitive text
searching, an iterating interface to collation is provided. Please refer to the
section below on `CollationElementIterator` for more details.

The `ucol_compare` function compares one pair of strings at a time. Comparing two
strings is much faster than calculating sort keys for both of them. However, if
comparisons should be done repeatedly on a very large number of strings, generating
and storing sort keys can improve performance. In all other cases (such as quick
sort or bubble sort of a
moderately-sized list of strings), comparing strings works very well.

The C API used for comparing two strings is `ucol_strcoll`. It requires two
`UChar *` strings and their lengths as parameters, as well as a pointer to a valid
`UCollator` instance. The result is a `UCollationResult` constant, which can be one
of `UCOL_LESS`, `UCOL_EQUAL` or `UCOL_GREATER`.

The C++ API offers the method `Collator::compare` with several overloads.
Acceptable input arguments are `UChar *` with length of strings, or `UnicodeString`
instances. The result is a member of the `UCollationResult` or `EComparisonResult` enums.

The Java API provides the method `Collator.compare` with one overload. Acceptable
input arguments are Strings or Objects. The result is an int value, which is
less than zero if source is less than target, zero if source and target are
equal, or greater than zero if source is greater than target.

There are also several convenience functions and methods returning a boolean
value, such as `ucol_greater`, `ucol_greaterOrEqual`, `ucol_equal` (in C)
`Collator::greater`, `Collator::greaterOrEqual`, `Collator::equal` (in C++) and
`Collator.equals` (in Java).

### Examples

**C:**

```c
UChar *s [] = { /* list of Unicode strings */ };
uint32_t listSize = sizeof(s)/sizeof(s[0]);
UErrorCode status = U_ZERO_ERROR;
UCollator *coll = ucol_open("en_US", &status);
uint32_t i, j;
if(U_SUCCESS(status)) {
  for(i=listSize-1; i>=1; i--) {
    for(j=0; j<i; j++) {
      if(ucol_strcoll(s[j], -1, s[j+1], -1) == UCOL_LESS) {
        swap(s[j], s[j+1]);
     }
   }
}
ucol_close(coll);
}
```

**C++:**

```c++
UnicodeString s [] = { /* list of Unicode strings */ };
uint32_t listSize = sizeof(s)/sizeof(s[0]);
UErrorCode status = U_ZERO_ERROR;
Collator *coll = Collator::createInstance(Locale("en", "US"), status);
uint32_t i, j;
if(U_SUCCESS(status)) {
  for(i=listSize-1; i>=1; i--) {
    for(j=0; j<i; j++) {
      if(coll->compare(s[j], s[j+1]) == UCOL_LESS) {
        swap(s[j], s[j+1]);
     }
   }
}
delete coll;
}
```

**Java:**

```java
String s [] = { /* list of Unicode strings */ };
try {
    Collator coll = Collator.getInstance(Locale.US);
    for (int i = s.length - 1; i > = 1; i --) {
        for (j=0; j<i; j++) {
            if (coll.compare(s[j], s[j+1]) == -1) {
                swap(s[j], s[j+1]);
            }
        }
    }
} catch (Exception e) {
    System.err.println("English collation creation failed.");
    e.printStackTrace();
}
```

## GetSortKey

The C API provides the `ucol_getSortKey` function, which requires (apart from a
pointer to a valid `UCollator` instance), an original `UChar` pointer, together with
its length. It also requires a pointer to a receiving buffer and its length.

The C++ API provides the `Collator::getSortKey` method with similar parameters as
the C version. It also provides `Collator::getCollationKey`, which produces a
`CollationKey` object instance (a wrapper around a sort key).

The Java API provides only the `Collator.getCollationKey` method, which produces a
`CollationKey` object instance (a wrapper around a sort key).

Sort keys are generally only useful in databases or other circumstances where
function calls are extremely expensive. See [Sortkeys vs
Comparison](concepts#sortkeys-vs-comparison).

### Sort Key Features

ICU writes sort keys as sequences of bytes.

Each sort key ends with one 00 byte and does not contain any other 00 byte. The
terminating 00 byte is included in the length of the sort key as returned by the
API (unlike any other ICU API where terminating NUL bytes or characters are not
counted as part of the length).

Sort key byte sequences must be compared with an unsigned-byte comparison, as
with `strcmp()`.

Comparing the sort keys of two strings from the same collator yields the same
ordering as using the collator to compare the two strings directly. That is:
`strcmp(coll.getSortKey(str1), coll.getSortKey(str2))` is equivalent to
`coll.compare(str1, str2)`.

Sort keys from different collators (different locale or strength or any other
attributes/settings) are not comparable.

Sort keys can be "merged" as described in [UTS #10 Merging Sort
Keys](http://www.unicode.org/reports/tr10/#Merging_Sort_Keys), via
`ucol_mergeSortkeys()` or Java `CollationKey.merge()`.

*   Since CLDR 1.9/ICU 4.6, the same effect can be achieved by concatenating
    strings with U+FFFE between them. The concatenation has the same sort order
    as the merged sort keys.
*   However, it is not guaranteed that the sort key of the concatenated strings
    is the same as the merged result of the individual sort keys. (That is,
    merge(getSortKey(str1), getSortKey(str2)) may differ from getSortKey(str1 +
    '\\uFFFE' + str2).)
*   In particular, a future version of ICU is likely to generate shorter sort
    keys when concatenating strings with U+FFFE between them (by using
    compression across the U+FFFE weights).
*   *The recommended way to achieve "merged" sorting is via strings with
    U+FFFE.*

Any further analysis or parsing of sort keys is not supported.

Sort keys will change from one ICU version to another; therefore, if sort keys
are stored in a database or other persistent storage, then each upgrade requires
their regeneration.

*   The details of the underlying data change with every Unicode and CLDR
    version.
*   Sort keys are also subject to enhancements and bug fixes in the builder and
    implementation code.
*   On the other hand, the sort *order* is much more stable. It is subject to
    deliberate changes to the default Unicode collation order, which is kept
    quite stable, and subject to deliberate changes in CLDR data as new data is
    added and feedback on existing data is taken into account.

Implementation notes: (Not supported as permanent constraints on sort keys)

Byte 02 was unique as a merge separator for some versions of ICU before version
ICU 53. Since ICU 53, 02 is also used in regular collation weights where there
is no conflict (to expand the number of available short weights).

Byte 01 has been unique as a level separator. This is not strictly necessary for
non-primary levels. (A level's compressible "common" weight as its level
separator would yield shorter sort keys.) However, the current implementation of
`ucol_mergeSortkeys()` relies on it. (Also, test code currently examines sort keys
for finding the strength of a comparison difference.) This may change in the
future, especially if `ucol_mergeSortkeys()` were to become deprecated.

Level separators are likely to be equivalent to single-byte weights (possibly
compressible): Multi-byte level separators would noticeably lengthen sort keys
for short strings.

The byte values used in several ICU versions for sort keys and collation
elements are documented in the [“Special Byte Values” design
doc](http://site.icu-project.org/design/collation/bytes) on the ICU site.

### Sort Key Output Buffer

`ucol_getSortKey()` can operate in 'preflighting' mode, which returns the amount
of memory needed to store the resulting sort key. This mode is automatically
activated if the output buffer size passed is set to zero. Should the sort key
become longer than the buffer provided, function again slips into preflighting
mode. The overall performance is poorer than if the function is called with a
zero output buffer. If the size of the sort key returned is greater than the
size of the buffer provided, the content of the result buffer is undefined. In
that case, the result buffer could be reallocated to its proper size and the
sort key generator function can be used again.

The best way to generate a series of sort keys is to do the following:

1.  Create a big temporary buffer on the stack. Typically, this buffer is
    allocated only once, and reused with every sort key generated. There is no
    need to keep it as small as possible. A recommended size for the temporary
    buffer is four times the length of the longest string processed.

2.  Start the loop. Call `ucol_getSortKey()` to find out how big the sort key
    buffer should be, and fill in the temporary buffer at the same time.

3.  If the temporary buffer is too small, allocate or reallocate more space.
    Fill in the sort key values in the overflow buffer.

4.  Allocate the sort key buffer with the size returned by `ucol_getSortKey()` and
    call `memcpy` to copy the sort key content from the temp buffer to the sort
    key buffer.

5.  Loop back to step 1 until you are done.

6.  Delete the overflow buffer if you created one.

### Example

```c
void GetSortKeys(const Ucollator* coll, const UChar*
const *source, uint32_t arrayLength)
{
  char[1000] buffer; // allocate stack buffer
  char* currBuffer = buffer;
  int32_t bufferLen = sizeof(buffer);
  int32_t expectedLen = 0;
  UErrorCode err = U_ZERO_ERROR;

  for (int i = 0; i < arrayLength; ++i) {
    expectedLen = ucol_getSortKey(coll, source[i], -1, currBuffer, bufferLen);
    if (expectedLen > bufferLen) {
      if (currBuffer == buffer) {
        currBuffer = (char*)malloc(expectedLen);
      } else {
        currBuffer = (char*)realloc(currBuffer, expectedLen);
      }
    }
    bufferLen = ucol_getSortKey(coll, source[i], -1, currBuffer, expectedLen);
  }
  processSortKey(i, currBuffer, bufferLen);


  if (currBuffer != buffer && currBuffer != NULL) {
    free(currBuffer);
  }
}
```

> :point_right: **Note** Although the API allows you to call
> `ucol_getSortKey` with `NULL` to see what the
> sort key length is, it is strongly recommended that you NOT determine the length
> first, then allocate and fill the sort key buffer. If you do, it requires twice
> the processing since computing the length has to do the same calculation as
> actually getting the sort key. Instead, the example shown above uses a stack buffer.

### Using Iterators for String Comparison

ICU4C's `ucol_strcollIter` API allows for comparing two strings that are supplied
as character iterators (`UCharIterator`). This is useful when you need to compare
differently encoded strings using `strcoll`. In that case, converting the strings
first would probably be wasteful, since `strcoll` usually gives the result
before whole strings are processed. This API is implemented only as a C function
in ICU4C. There are no equivalent C++ or ICU4J functions.

```c
...
/* we are arriving with two char*: utf8Source and utf8Target, with their
* lengths in utf8SourceLen and utf8TargetLen
*/
    UCharIterator sIter, tIter;
    uiter_setUTF8(&sIter, utf8Source, utf8SourceLen);
    uiter_setUTF8(&tIter, utf8Target, utf8TargetLen);
    compareResultUTF8 = ucol_strcollIter(myCollation, &sIter, &tIter, &status);
...
```

### Obtaining Partial Sort Keys

When using different sort algorithms, such as radix sort, sometimes it is useful
to process strings only as much as needed to feed into the sorting algorithm.
For that purpose, ICU provides the `ucol_nextSortKeyPart` API, which also takes
character iterators. This API allows for iterating over subsequent pieces of an
uncompressed sort key. Between calls to the API you need to save a 64-bit state.
Following is an example of simulating a string compare function using the partial
sort key API. Your usage model is bound to look much different.

```c
static UCollationResult compareUsingPartials(UCollator *coll,
                                             const UChar source[], int32_t sLen,
                                             const UChar target[], int32_t tLen,
                                             int32_t pieceSize, UErrorCode *status) {
  int32_t partialSKResult = 0;
  UCharIterator sIter, tIter;
  uint32_t sState[2], tState[2];
  int32_t sSize = pieceSize, tSize = pieceSize;
  int32_t i = 0;
  uint8_t sBuf[16384], tBuf[16384];
  if(pieceSize > 16384) {
    *status = U_BUFFER_OVERFLOW_ERROR;
    return UCOL_EQUAL;
  }
  *status = U_ZERO_ERROR;
  sState[0] = 0; sState[1] = 0;
  tState[0] = 0; tState[1] = 0;
  while(sSize == pieceSize && tSize == pieceSize && partialSKResult == 0) {
    uiter_setString(&sIter, source, sLen);
    uiter_setString(&tIter, target, tLen);
    sSize = ucol_nextSortKeyPart(coll, &sIter, sState, sBuf, pieceSize, status);
    tSize = ucol_nextSortKeyPart(coll, &tIter, tState, tBuf, pieceSize, status);
    partialSKResult = memcmp(sBuf, tBuf, pieceSize);
  }

  if(partialSKResult < 0) {
      return UCOL_LESS;
  } else if(partialSKResult > 0) {
    return UCOL_GREATER;
  } else {
    return UCOL_EQUAL;
  }
}
```

### Other Examples

A longer example is presented in the 'Examples' section. Here is an illustration
of the usage model.

**C:**

```c
#define MAX_KEY_SIZE 100
#define MAX_BUFFER_SIZE 10000
#define MAX_LIST_LENGTH 5
const char text[] = {
   "Quick",
   "fox",
   "Moving",
   "trucks",
   "riddle"
};
const UChar s [5][20];
int i;
int32_t length, expectedLen;
uint8_t temp[MAX_BUFFER _SIZE];


uint8_t *temp2 = NULL;
uint8_t keys [MAX_LIST_LENGTH][MAX_KEY_SIZE];
UErrorCode status = U_ZERO_ERROR;

temp2 = temp;

length = MAX_BUFFER_SIZE;
for( i = 0; i < 5; i++)
{
   u_uastrcpy(s[i], text[i]);
}
UCollator *coll = ucol_open("en_US",&status);
uint32_t length;
if(U_SUCCESS(status)) {
  for(i=0; i<MAX_LIST_LENGTH; i++) {
    expectedLen = ucol_getSortKey(coll, s[i], -1,temp2,length );
    if (expectedLen > length) {
      if (temp2 == temp) {
        temp2 =(char*)malloc(expectedLen);
      } else {
        temp2 =(char*)realloc(temp2, expectedLen);
      }
        length =ucol_getSortKey(coll, s[i], -1, temp2, expectedLen);
    }
    memcpy(key[i], temp2, length);
  }
}
qsort(keys, MAX_LIST_LENGTH,MAX_KEY_SIZE*sizeof(uint8_t), strcmp);
for (i = 0; i < MAX_LIST_LENGTH; i++) {
  free(key[i]);
}
ucol_close(coll);
```

**C++:**

```c++
#define MAX_LIST_LENGTH 5
const UnicodeString s [] = {
  "Quick",
  "fox",
  "Moving",
  "trucks",
  "riddle"
};
CollationKey *keys[MAX_LIST_LENGTH];
UErrorCode status = U_ZERO_ERROR;
Collator *coll = Collator::createInstance(Locale("en_US"), status);
uint32_t i;
if(U_SUCCESS(status)) {
  for(i=0; i<listSize; i++) {
    keys[i] = coll->getCollationKey(s[i], -1);
  }
  qsort(keys, MAX_LIST_LENGTH, sizeof(CollationKey),compareKeys);
  delete[] keys;
  delete coll;
}
```

**Java:**

```java
String s [] = {
  "Quick",
  "fox",
  "Moving",
  "trucks",
  "riddle"
};
CollationKey keys[] = new CollationKey[s.length];
try {
    Collator coll = Collator.getInstance(Locale.US);
    for (int i = 0; i < s.length; i ++) {
        keys[i] = coll.getCollationKey(s[i]);
    }

    Arrays.sort(keys);
}
catch (Exception e) {
    System.err.println("Error creating English collator");
    e.printStackTrace();
}
```

## Collation ElementIterator

A collation element iterator can only be used in one direction. This is
established at the time of the first call to retrieve a collation element. Once
`ucol_next` (C), `CollationElementIterator::next` (C++) or
`CollationElementIterator.next` (Java) are invoked,
`ucol_previous` (C),
`CollationElementIterator::previous` (C++) or `CollationElementIterator.previous`
(Java) should not be used (and vice versa). The direction can be changed
immediately after `ucol_first`, `ucol_last`, `ucol_reset` (in C),
`CollationElementIterator::first`, `CollationElementIterator::last`,
`CollationElementIterator::reset` (in C++) or `CollationElementIterator.first`,
`CollationElementIterator.last`, `CollationElementIterator.reset` (in Java) is
called, or when it reaches the end of string while traversing the string.

When `ucol_next` is called at the end of the string buffer, `UCOL_NULLORDER` is
always returned with any subsequent calls to `ucol_next`. The same applies to
`ucol_previous`.

An example of how iterators are used is the Boyer-Moore search implementation,
which can be found in the samples section.

### API Example

**C:**

```c
UCollator         *coll = ucol_open("en_US",status);
UErrorCode         status = U_ZERO_ERROR;
UChar              text[20];
UCollationElements *collelemitr;
uint32_t           collelem;

u_uastrcpy(text, "text");
collelemitr = ucol_openElements(coll, text, -1, &status);
collelem = 0;
do {
  collelem = ucol_next(collelemitr, &status);
} while (collelem != UCOL_NULLORDER);

ucol_closeElements(collelemitr);
ucol_close(coll);
```

**C++:**

```c++
UErrorCode    status = U_ZERO_ERROR;
Collator      *coll = Collator::createInstance(Locale::getUS(), status);
UnicodeString text("text");
CollationElementIterator *collelemitr = coll->createCollationElementIterator(text);
uint32_t      collelem = 0;
do {
  collelem = collelemitr->next(status);
} while (collelem != CollationElementIterator::NULLORDER);

delete collelemitr;
delete coll;
```

**Java:**

```java
try {
    RuleBasedCollator coll = (RuleBasedCollator)Collator.getInstance(Locale.US);
    String text = "text";
    CollationElementIterator collelemitr = coll.getCollationElementIterator(text);
    int collelem = 0;
    do {
        collelem = collelemitr.next();
    } while (collelem != CollationElementIterator.NULLORDER);
} catch (Exception e) {
    System.err.println("Error in collation iteration");
    e.printStackTrace();
}
```

## Setting and Getting Attributes

The general attribute setting APIs are `ucol_setAttribute` (in C) and
`Collator::setAttribute` (in C++). These APIs take an attribute name and an
attribute value. If the name and the value pass a syntax and range check, the
property of the collator is changed. If the name and value do not pass a syntax
and range check, however, the state is not changed and the error code variable
is set to an error condition. The Java version does not provide general
attribute setting APIs; instead, each attribute has its own setter API of
the form `RuleBasedCollator.setATTRIBUTE_NAME(arguments)`.

The attribute getting APIs are `ucol_getAttribute` (C) and `Collator::getAttribute`
(C++). Both APIs require an attribute name as an argument and return an
attribute value if a valid attribute name was supplied. If a valid attribute
name was not supplied, however, they return an undefined result and set the
error code. Similarly to the setter APIs for the Java version, no generic getter
API is provided. Each attribute has its own setter API of the form
`RuleBasedCollator.getATTRIBUTE_NAME()` in the Java version.

## References

1.  Ken Whistler, Markus Scherer: "Unicode Technical Standard #10, Unicode Collation
    Algorithm" (<http://www.unicode.org/reports/tr10/>)

2.  ICU Design doc: "Collation v2" (<http://site.icu-project.org/design/collation/v2>)

3.  Mark Davis: "ICU Collation Design Document"
    (<https://htmlpreview.github.io/?https://github.com/unicode-org/icu-docs/blob/master/design/collation/ICU_collation_design.htm>)

3.  The Unicode Standard, chapter 5, "Implementation guidelines"
    (<http://www.unicode.org/uni2book/ch05.pdf>)

4.  Laura Werner: "Efficient text searching in Java: Finding the right string in
    any language"
    (<http://icu-project.org/docs/papers/efficient_text_searching_in_java.html>)

5.  Mark Davis, Martin Dürst: "Unicode Standard Annex #15: Unicode Normalization
    Forms" (<http://www.unicode.org/reports/tr15/>).
