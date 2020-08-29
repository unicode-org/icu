---
layout: default
title: Resources
nav_order: 2
parent: Locales and Resources
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Resource Management
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

> :point_right: **Note**: This page describes the use of ICU4C Resource
> Management techniques and APIs. For an overview of the message localization
> process using ICU, see the related page [Localizing with ICU](localizing.md).

A software product that needs to be localized wins or loses depending on how
easy is to change the data or "resources" which affect users. From the simplest
point of view, that data is the information presented to the user (such as a
translated message) as well as the region-specific ways of doing things (such as
sorting). The process of localization will eventually involve translators and it
would be very convenient if the process of localizing could be done only by
translators and experts in the target culture. There are several points to keep
in mind when designing such a localizable software product.

### Keeping Data Separate

Obviously, one does not want to make translators wade through the source code
and make changes there. That would be a recipe for a disaster. Instead, the
translatable data should be kept separately, in a format that allows translators
easy access. A separate resource managing mechanism is hence required.
Application access data through API calls, which pick the appropriate entries
from the resources. Resources are kept in human readable/editable format with
optional tools for content editing.

The data should contain all the elements to be localized, including, but no
limited to, GUI messages, icons, formatting patterns, and collation rules. A
convenient way for keeping binary data should also be provided - often icons for
different cultures should be different.

### Keeping Data Small

It is not unlikely that the data will be same for several regions - take for
example Spanish speaking countries - names of the days and month will be the
same in both Mexico and Spain. It would be very beneficial if we can prevent the
duplication of data. This can be achieved by structuring resources in such a way
so that an unsuccessful query into a more specific resource triggers the same
query in a more general resource. A convenient way to do this is to use a tree
like structure.

Another way to reduce the data size is to allow linking of the resources that
are same for the regions that are not in general-specific relation.

### Find the Best Available Data

Sometimes, the exact data for a region is still not available. However, if the
data is structured correctly, the user can be presented with similar data. For
example, a Spanish speaking user in Mexico would probably be happier with
Spanish than with English captions, even if some of the details for Mexico are
not there.

If the data is grouped correctly, the program can automatically find the most
suitable data for the situation.

The previous points all lead to a separate mechanism that stores data separately
from the code. Software is able to access the data through the API calls. Data
is structured in a tree like structure, with the most general region in the root
(most commonly, the root region is the native language of the development team).
Branches lead to more specialized regions, usually through languages, countries
and country regions. Data that is already the same on the more general level is
not repeated.

> :point_right: **Note**: The path through languages, countries and country 
> region could be different. One may decide to go through countries and then 
> through languages spoken in the particular country. In either case, some data 
> must be duplicated - if you go through languages, the currency data for 
> different speaking parts of the same country will be duplicated (consider 
> French and English languages in Canada) - on the other side, when you go 
> through countries, you will need to duplicate day names and similar 
> information.

Here is an example of a such a resource tree structure:

```
             root                             Root
              |
  +-------+---+---+----+----+
  |       |       |    |    |
  en      de      ja  ru    zh                Language
  |       |       |    |    |
  +---+   +---+   |    |    +------+
  |   |   |   |   |    |    |      |
  |   |   |   |   |    |    Hans  Hant        Script
  |   |   |   |   |    |    |      |
  |   |   |   |   |    |    |      +----+
  |   |   |   |   |    |    |      |    |
  US  IE  DE  AT  JP   RU   CN     HK   TW    Country or Region
  |    
  POSIX                                       Variant
```

Let us assume that the root resource contains data written by the original
implementors and that this data is in English and conforms to the conventions
used in the United States. Therefore, resources for English and English in
United States would be empty and would take its data from the root resource. If
a version for Ireland is required, appropriate overriding changes can be made to
the data for English in Ireland. Special variant information could be put into
en_US_POSIX if specific legacy formatting were required, or specific sub-region
information were required. When making the version for the German speaking
region, all the German data would be in that resource, with the differences in
the Germany and Austria resources.

It is important to note that some locales have the optional script tag. This is
important for multi-script locales, like Uzbek, Azerbaijani, Serbian or Chinese.
Even though Chinese uses Han characters, the characters are usually identified
as either traditional Chinese (Hant) characters, or simplified Chinese (Hans).

Even if all the data that would go to a certain resource comes from the more
general resources, it should be made clear that the particular region is
supported by application. This can be done by having completely empty resources.

## The ICU Model

ICU bases its resource management model on the ideas presented above. All the
resource APIs are concentrated in the resource bundle framework. This framework
is closely tied in its functioning to the ICU [Locale](index.md) naming scheme.

ICU provides and relies on a set of locale specific data in the resource bundle
format. If we think that we have correct data for a requested locale, even if
all its data comes from a more general locales, we will provide an empty
resource bundle. This is reflected in our return informational codes (see the
section on APIs). A lot of ICU frameworks (collation, formatting etc.) relies on
the data stored in resource bundles.

Resource bundles rely on the ICU data framework. For more information on the
functioning of ICU data, see the appropriate [section](../icudata.md).

Users of the ICU library can also use the resource bundle framework to store and
retrieve localizable data in their projects.

Resource bundles are collections of resources. Individual resources can contain
data or other resources.

> :point_right: **Note**: ICU4J relies on the resource bundle mechanism already
> provided by JDK for its functioning. Therefore, most of the discussion here 
> pertains only to ICU4C.

### Fallback Mechanism

Essential part ICU's resource management framework is the fallback mechanism. It
ensures that if the data for the requested locale is missing, an effort will be
made to obtain the most usable data. Fallback can happen in two situations:

1.  When a resource bundle for a locale is requested. If it doesn't exist, a
    more general resource bundle will be used. If there are no such resource
    bundles, a resource bundle for default locale will be used. If this fails,
    the root resource bundle will be used. When using ICU locale data, not
    finding the requested resource bundle means that we don't know what the data
    should be for that particular locale, so you might want to consider this
    situation an error. Custom packages of resource bundles may or may not
    adhere to this contract. A special care should be taken in remote server
    situations, when the data from the default locale might not mean anything to
    the remote user (imagine a situation where a server in Japan responds to a
    Spanish speaking client by using default Japanese data.

2.  When a resource inside a resource bundle is requested. If the resource is
    not present, it will be sought after in more general resources. If at
    initial opening of a resource bundle we went through the default locale, the
    search for a resource will also go through it. For example, if a resource
    bundle for zh_Hans_CN is opened, a missing resource will be looked for in
    zh_Hans, zh and finally root. This is usually harmless, except when a
    resource is only located in the default locale or in the root resource
    bundle.

### Data Packaging

ICU allows and requires that the application specific data be stored apart from
the ICU internal data (locale, converter, transformation data etc.). Application
data should be stored in packages. ICU uses the default package (NULL) for its
data. All the ICU's build tools provide means to specify the package for your
data. More about how to package application data can be found below.

## Resource Bundle APIs

ICU4C provides both C and C++ APIs for using resource bundles. The core
implementation is in C, while the C++ APIs are only a thin wrapper around it.
Therefore, the code using C APIs will generally be faster.

Resource bundles use ICU's "open use close" paradigm. In C all the resource
bundle operations are done using the `UResourceBundle*` handle. `UResourceBundle*`
allows access to both resource bundles and individual resources. In C++, class
`ResourceBundle` should be used for both resource bundles and individual
resources.

To use the resource bundle framework, you need to include the appropriate header
file, `unicode/ures.h` for C and `unicode/resbund.h` for C++.

### Error Checking

If an operation with resource bundle fails, an error code will be set. It is
important to check for the value of the error code. In C you should frequently
use the following construct:

```c
if (U_SUCCESS(status)) {
    /* everything is fine */
} else {
    /* there was an error */
}
```

### Opening of Resource Bundles

The most common C resource bundle opening API is:

```c
UResourceBundle* ures_open(const char* package, const char* locale, UErrorCode* status)
```

The first argument specifies the package name or `NULL` for the default ICU package.
The second argument is the locale for which you want the resource bundle.
Special values for the locale are `NULL` for the default locale and `""` (empty
string) for the root locale. The third argument should be set to `U_ZERO_ERROR`
before calling the function. It will return the status of operation. Apart from
returning regular errors, it can return two informational/warning codes:
`U_USING_FALLBACK_WARNING` and `U_USING_DEFAULT_WARNING`. The first informational
code means that the requested resource bundle was not found and that a more
general bundle was returned. If you are opening ICU resource bundles, do note
that this means that we do not guarantee that the contents of opened resource
bundle will be correct for the requested locale. The situation might be
different for application packages. However, the warning `U_USING_DEFAULT_WARNING`
means that there were no more general resource bundles found and that you were
returned either a resource bundle that is the default for the system, or the root
resource bundle. This will almost certainly contain wrong data.

There are a couple of other opening APIs: `ures_openDirect` takes the same
arguments as the `ures_open` but will fail if the requested locale is not found.
Also, if opening is successful, no fallback will be performed if an individual
resource is not found. The second one, `ures_openU` takes a `UChar*` for package
name instead of `char*`.

In C++, opening is done through a constructor. There are several constructors.
Most notable difference from C APIs is that the package should be given as a
`UnicodeString` and the locale is passed as a `Locale` object. There is also a copy
constructor and a constructor that takes a C `UResourceBundle*` handle. The
result is a `ResourceBundle` object. Remarks about informational codes are also
valid for the C++ APIs.

> :point_right: **Note**: All the data accessing examples in the following
> sections use ICU's 
> [root](https://github.com/unicode-org/icu/blob/master/icu4c/source/data/locales/root.txt)
> resource bundle.

```c
UErrorCode status = U_ZERO_ERROR;
UResourceBundle* icuRoot = ures_open(NULL, "root", &status);
if (U_SUCCESS(status)) {
    /* everything is fine */
    ...
    /* do some interesting stuff here - see below */
    ...
    /* and close the bundle afterwards */
    ures_close(icuRoot); /* discussed later */
} else {
    /* there was an error */
    /* report and exit */
}
```

In C++, opening would look like this:

```c++
UErrorCode status = U_ZERO_ERROR;
// we rely on automatic construction of Locale object from a char*
ResourceBundle myResource("myPackage", "de_AT", status); 
if (U_SUCCESS(status)) {
    /* everything is fine */
    ...
    /* do some interesting stuff here */
    ...
    /* the bundle will be closed when going out of scope */
} else {
    /* there was an error */
    /* report and exit */
}
```

### Closing of Resource Bundles

After using, resource bundles need to be closed to prevent memory leaks. In C,
you should call the `void ures_close(UResourceBundle* resB)` API. In C++, if you
have just used the `ResourceBundle` objects, going out of scope will close the
bundles. When using allocated objects, make sure that you call the appropriate
delete function.

As already mentioned, resource bundles and resources share the same type. You
can close bundles and resources in any order you like. You can invoke `ures_close`
on `NULL` resource bundles. Therefore, you can always this API regardless of the
success of previous operations.

### Accessing Resources

Once you are in the possession of a valid resource bundle, you can access the
resources and data that it holds. The result of accessing operations will be a
new resource bundle object. In C, `UResourceBundle*` handles can be reused by
using the fill-in parameter. That saves you from frequent closing and
reallocating of resource bundle structures, which can dramatically improve the
performance. C++ APIs do not provide means for object reuse. All the C examples
in the following sections will use a fill-in parameter.

#### Types of Resources

Resource bundles can contain two main types of resources: complex and simple
resources. Complex resources store other resources and can have named or unnamed
elements. **Tables** store named elements, while **arrays** store unnamed ones.
Simple resources contain data which can be **string**, **binary**, **integer
array** or a single **integer**.

There are several ways for accessing data stored in the complex resources.
Tables can be accessed using keys, indexes and by iteration. Arrays can be
accessed using indexes and by iteration.

In order to be able to distinguish between resources, one needs to know the type
of the resource at hand. To find this out, use the
`UResType ures_getType(UResourceBundle* resourceBundle)` API, or the C++ analog
`UResType getType(void)`. The `UResType` is an enumeration defined in the
[unicode/ures.h](../../../icu4c/source/common/unicode/ures.h) header file.

> :point_right: **Note**: Indexes of resources in tables do not necessarily
> correspond to the order of items in a table. Due to the way binary structure is 
> organized, items in a table are sorted according to the binary ordering of the
> keys, therefore, the index of an item in a table will be the index of its key
> string in the binary order. Furthermore, the ordering of the keys are different
> on ASCII and EBCDIC platforms.
> <br>
> Starting with ICU 4.4, the order of table items is the ASCII string order on
> all platforms.
> <br>
> The iteration order of table items might change from release to release.

#### Accessing by Key

To access resources using a key, you can use the `UResourceBundle*
ures_getByKey(const UResourceBundle* resourceBundle, const char* key,
UResourceBundle* fillIn, UErrorCode* status)` API. First argument is the parent
resource bundle, which can be either a resource bundle opened using `ures_open` or
similar APIs or a table resource. The key is always specified using invariant
characters. The fill-in parameter can be either `NULL` or a valid resource bundle
handle. If it is `NULL`, a new resource bundle will be constructed. If you pass an
already existing resource bundle, it will be closed and the memory will be
reused for the new resource bundle. Status indicator can return
`U_MISSING_RESOURCE_ERROR` which indicates that no resources with that key exist,
or one of the above mentioned informational codes (`U_USING_FALLBACK_WARNING` and
`U_USING_DEFAULT_WARNING`) which do not affect the validity of data in the case of
resource retrieval.

```c
...
/* we already got zones resource from the opening example */
UResourceBundle *zones = ures_getByKey(icuRoot, "zoneStrings", NULL, &status);
if (U_SUCCESS(status)) {
    /* ... do interesting stuff - see below ... */
}
ures_close(zones);
/* clean up the rest */
...
```

In C++, the analogous API is `ResourceBundle get(const char* key, UErrorCode& status) const`.

Trying to retrieve resources by key on any other type of resource than tables
will produce a `U_RESOURCE_TYPE_MISMATCH` error.

#### Accessing by Index

Accessing by index requires you to supply an index of the resource that you want
to retrieve. Appropriate API is `UResourceBundle* ures_getByIndex(const
UResourceBundle* resourceBundle, int32_t indexR, UResourceBundle* fillIn,
UErrorCode* status)`. The arguments have the same semantics as for the
`ures_getByKey` API. The only difference is the second argument, which is the
index of the resource that you want to retrieve. Indexes start at zero. If an
index out of range is specified, `U_MISSING_RESOURCE_ERROR` is returned. To find
the size of a resource, you can use `int32_t ures_getSize(UResourceBundle* 
resourceBundle)`. The maximum index is the result of this API minus 1.

```c
...
/* we already got zones resource from the accessing by key example */
UResourceBundle *currentZone = NULL;
int32_t index = 0;
for (index = 0; index < ures_getSize(zones); index++) {
    currentZone = ures_getByIndex(zones, index, currentZone, &status);
    /* ... do interesting stuff here ... */
}
ures_close(currentZone);
/* cleanup the rest */
...
```

Accessing simple resource with an index 0 will return themselves. This is useful
for iterating over all the resources regardless of type.

C++ overloads the get API with `ResourceBundle get(int32_t index, UErrorCode& status) const`.

#### Iterating Over Resources

If you don't care about the order of the resources and want simple code, you can
use the iteration mechanism. To set up iteration over a complex resource, you
can simply start iterating using the `UResourceBundle*
ures_getNextResource(UResourceBundle* resourceBundle, UResourceBundle* fillIn,
UErrorCode* status)`. It is advisable though to reset the iterator for a
resource before starting, in order to ensure that the iteration will indeed
start from the beginning - just in case somebody else has already been playing
with this resource. To reset the iterator use `void
ures_resetIterator(UResourceBundle* resourceBundle)` API. To check whether there
are more resources, call `UBool ures_hasNext(UResourceBundle* resourceBundle)`.
If you have iterated through the whole resource, `NULL` will be returned.

```c
...
/* we already got zones resource from the accessing by key example */
UResourceBundle *currentZone = NULL;
ures_resetIterator(zones);
while (ures_hasNext(zones)) {
    currentZone = ures_getNextResource(zones, currentZone, &status);
    /* ... do interesting stuff here ... */
}
ures_close(currentZone);
/* cleanup the rest */
...
```

C++ provides analogous APIs: `ResourceBundle getNext(UErrorCode& status)`, `void resetIterator(void)`
 and `UBool hasNext(void)`.

#### Accessing Data in the Simple Resources

In order to get to the data in the simple resources, you need to use appropriate
APIs according to the type of a simple resource. They are summarized in the
tables below. All the pointers returned should be considered pointers to read
only data. Using an API on a resource of a wrong type will result in an error.

Strings:

| Language | API                                                                                                    |
| -------- | ------------------------------------------------------------------------------------------------------ |
| C        | `const UChar* ures_getString(const UResourceBundle* resourceBundle, int32_t* len, UErrorCode* status)` |
| C++      | `UnicodeString getString(UErrorCode& status) const`                                                    |

Example:

```c
...
UResourceBundle* version = ures_getByKey(icuRoot, "Version", NULL, &status); 
if (U_SUCCESS(status)) {
  int32_t versionStringLen = 0;
  const UChar* versionString = ures_getString(version, &versionStringLen, &status);
}
ures_close(version);
...
```

Binaries:

| Language | API                                                                                                      |
| -------- | -------------------------------------------------------------------------------------------------------- |
| C        | `const uint8_t* ures_getBinary(const UResourceBundle* resourceBundle, int32_t* len, UErrorCode* status)` |
| C++      | `const uint8_t* getBinary(int32_t& len, UErrorCode& status) const`                                       |

Integers, signed and unsigned:

| Language | API                                                                                                                                                                 |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| C        | `int32_t ures_getInt(const UResourceBundle* resourceBundle, UErrorCode* status)` `uint32_t ures_getUInt(const UResourceBundle* resourceBundle, UErrorCode* status)` |
| C++      | `int32_t getInt(UErrorCode& status) const` <br> `uint32_t getUInt(UErrorCode& status) const`                                                                        |

Integer Arrays:

| Language | API                                                                                                         |
| -------- | ----------------------------------------------------------------------------------------------------------- |
| C        | `const int32_t* ures_getIntVector(const UResourceBundle* resourceBundle, int32_t* len, UErrorCode* status)` |
| C++      | `const int32_t* getIntVector(int32_t& len, UErrorCode& status) const`                                       |

#### Convenience APIs

Since the vast majority of data stored in resource bundles are strings, ICU's
resource bundle framework provides a number of different convenience APIs that
directly access strings stored in resources. They are analogous to APIs already
discussed, with the difference that they return const `UChar*` or `UnicodeString`
objects.

> :point_right: **Note**: The C APIs that allow returning of `UnicodeStrings` only
> work if used in a C++ file. Trying to use them in a C file will produce a
> compiler error.

APIs that allow retrieving strings by specifying a key:

| Language (Return Type) | API                                                                                                                |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------ |
| C (UChar*)             | `const UChar* ures_getStringByKey(const UResourceBundle* resB, const char* key, int32_t* len, UErrorCode* status)` |
| C (UnicodeString)      | `UnicodeString ures_getUnicodeStringByKey(const UResourceBundle* resB, const char* key, UErrorCode* status)`       |
| C++                    | `UnicodeString getStringEx(const char* key, UErrorCode& status) const`                                             |


APIs that allow retrieving strings by specifying an index:

| Language (Return Type) | API                                                                                                                 |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------- |
| C (UChar*)             | `const UChar* ures_getStringByIndex(const UResourceBundle* resB, int32_t indexS, int32_t* len, UErrorCode* status)` |
| C (UnicodeString)      | `UnicodeString ures_getUnicodeStringByIndex(const UResourceBundle* resB, int32_t indexS, UErrorCode* status)`       |
| C++                    | `UnicodeString getStringEx(int32_t index, UErrorCode& status) const`                                                |

APIs for retrieving strings through iteration:

| Language (Return Type) | API                                                                                                                    |
| ---------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| C (UChar*)             | `const UChar* ures_getNextString(UResourceBundle* resourceBundle, int32_t* len, const char** key, UErrorCode* status)` |
| C (UnicodeString)      | `UnicodeString ures_getNextUnicodeString(UResourceBundle* resB, const char** key, UErrorCode* status)`                 |
| C++                    | `UnicodeString getNextString(UErrorCode& status)`                                                                      |

#### Other APIs

Resource bundle framework provides a number of additional APIs that allow you to
get more information on the resources you are using. They are summarized in the
following tables.

| Language | API                                                     |
| -------- | ------------------------------------------------------- |
| C        | `int32_t ures_getSize(UResourceBundle* resourceBundle)` |
| C++      | `int32_t getSize(void) const`                           |

Gets the number of items in a resource. Simple resources always return size 1.

| Language | API                                                      |
| -------- | -------------------------------------------------------- |
| C        | `UResType ures_getType(UResourceBundle* resourceBundle)` |
| C++      | `UResType getType(void)`                                 |

Gets the type of the resource. For a list of resource types, see:
[unicode/ures.h](../../../icu4c/source/common/unicode/ures.h)

| Language | API                                              |
| -------- | ------------------------------------------------ |
| C        | `const char* ures_getKey(UResourceBundle* resB)` |
| C++      | `const char* getKey(void)`                       |

Gets the key of a named resource or `NULL` if this resource is a member of an
array.

| Language | API                                                                           |
| -------- | ----------------------------------------------------------------------------- |
| C        | `void ures_getVersion(const UResourceBundle* resB, UVersionInfo versionInfo)` |
| C++      | `void getVersion(UVersionInfo versionInfo) const`                             |

Fills out the version structure for this resource.

| Language | API                                                                                     |
| -------- | --------------------------------------------------------------------------------------- |
| C        | `const char* ures_getLocale(const UResourceBundle* resourceBundle, UErrorCode* status)` |
| C++      | `const Locale& getLocale(void) const`                                                   |

Returns the locale this resource is from. This API is going to change, so stay
tuned.

### Format of Resource Bundles

Resource bundles are written in its source format. Before using them, they must
be compiled to the binary format using the `genrb` utility. Currently supported
source format is a text file. The format is defined in a [formal definition
file](https://github.com/unicode-org/icu-docs/blob/master/design/bnf_rb.txt).

This is an example of a resource bundle source file:

```
// Comments start with a '//' and extend to the end of the line 
// first, a locale name for the bundle is defined. The whole bundle is a table
// every resource, including the whole bundle has its name.
// The name consists of invariant characters, digits and following symbols: -, _. 
root {
    menu {
        id { "mainmenu" }
        items {
            {
                id { "file" }
                name { "&File" }
                items {
                    {
                        id { "open" }
                        name { "&Open" }
                    }
                    {
                        id { "save" }
                        name { "&Save" }
                    }
                    {
                        id { "exit" }
                        name { "&Exit" }
                    }
                }
            }

            {
                id { "edit" }
                name { "&Edit" }
                items {
                    {
                        id { "copy" }
                        name { "&Copy" }
                    }
                    {
                        id { "cut" }
                        name { "&Cut" }
                    }
                    {
                        id { "paste" }
                        name { "&Paste" }
                    }
                }
           }

            ...
        }
    }

    // This resource is a table, thus accessible only through iteration and indexes...
    errors {
        "Invalid Command",
        "Bad Value",

        // Add more strings here...

        "Read the Manual"
    }

    splash:import { "splash_root.gif" } // This is a binary imported file

    pgpkey:bin { a1b2c3d4e5f67890 } // a binary value

    versionInfo { // a table
        major:int { 1 } // of integers
        minor:int { 4 }
        patch:int { 7 }
    }

    buttonSize:intvector { 10, 20, 10, 20 } // an array of 32-bit integers

    // will pick up data from zoneStrings resource in en bundle in the ICU package
    simpleAlias:alias { "/ICUDATA/en/zoneStrings" }

    // will pick up data from CollationElements resource in en bundle
    // in the ICU package
    CollationElements:alias { "/ICUDATA/en" }   
}
```

Binary format is described in the [uresdata.h](../../../icu4c/source/common/uresdata.h)
header file.

### Resources Syntax

Syntax of the resources that can be stored in resource bundles is specified in
the following table:

| Data Type       | Format                                                                       | Description                                                                                                                                                                                                                                                                       |
| --------------- | ---------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Tables          | `[name][:table] { subname1 { subresource1 } ... subnameN { subresourceN } }` | Tables are a complex resource that holds named resources. If it is a part of an array, it does not have a name. At this point, a resource bundle is a table. Access is allowed by key, index, and iteration.                                                                      |
| Arrays          | `[name][:array] {subresource1, ... subresourceN }`                           | Arrays are a complex resource that holds unnamed resources. If it is a part of an array, it does not have a name. Arrays require less memory than tables (since they don't store the name of sub-resources) but the index and iteration access are as fast as with tables.        |
| Strings         | `[name][:string] { ["]UnicodeText["] }`                                      | Strings are simple resources that hold a chunk of Unicode encoded data. If it is a part of an array, it does not have a name.                                                                                                                                                     |
| Binaries        | `name:bin { binarydata } name:import{ "fileNameToImport" }`                  | Binaries are used for storing binary information (processed data, images etc). Information is stored on a byte level.                                                                                                                                                             |
| Integers        | `name:int { integervalue }`                                                  | Integers are used for storing a 32 bit integer value.                                                                                                                                                                                                                             |
| Integer Vectors | `name:intvector { integervalue, ... integervalueN }`                         | Integer vectors are used for storing 32 bit integer values.                                                                                                                                                                                                                       |
| Aliases         | `name:alias { locale and path to aliased resource }`                         | Aliases point to other resources. They are useful for preventing duplication of data in resources that are not on the same branch of the fallback chain. Alias can also have an empty path. In that case the position of the alias resource is used to find the aliased resource. |

Although specifying type for some resources can be omitted for backward
compatibility reasons, you are strongly encouraged to always specify the type of
the resources. As structure gets more complicated, some combinations of
resources that are not typed might produce unexpected results.

### Escape Sequences

String values can contain C/Java-style escape sequences like `\t`, `\r`, `\n`,
`\xhh`, `\uhhhh` and `\U00hhhhhh`, consistent with the `u_unescape()` C API, see the
[ustring.h](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/ustring_8h.html)
API documentation.

A literal backslash (\\) in a string value must be doubled (\\\\) or escaped
with `\x5C` or `\u005C`.

A literal ASCII double quote (") in a double-quoted string must be escaped with
\\" or `\x22` or `\u0022`.

You should also escape carriage return (`\r`) and line feed (`\n`) as well as
control codes, non-characters, unassigned code points and other default-invisible
characters (see the Unicode [UAX #44](https://www.unicode.org/reports/tr44/)
 `Default_Ignorable_Code_Point` property).

### Examples

The way to write your resource is to start with a table that has your locale
name. The contents of a table are between the curly brackets:

```
root:table {
}
```

Then you can start adding resources to your bundle. Resources on the first level
must be named and we suggest that you specify the type:

```
root:table {
  usage:string { "Usage: genrb [Options] files" }
  version:int { 122 }
  errorcodes:array {
    :string { "Invalid argument" }
    :string { "File not found" }
  }
}
```

The resource bundle format doesn't care about indentation and line breaks. You
can continue one string over many lines - you need to have the line break
outside of the string:

```
aVeryLongString:string {
  "This string is quite long "
  "and therefore should be "
  "broken into several lines."
}
```

For more examples on syntax, take a look at our resource files for
[locales](../../../icu4c/source/data/locales) and [test data](../../../icu4c/source/test/testdata),
especially at the [testtypes resource bundle](../../../icu4c/source/test/testdata/testtypes.txt).

### Making Your Own Resource Bundles

In order to make your own resource bundle package, you need to perform several
steps:

1.  Create your root resource bundle. This bundle should contain all the data
    for your program. You are probably best off if you fill it with data in your
    native language.

2.  Create a chain of empty resource bundles for your native language and
    region. For example, if your region is sr_CS, create all the entries in root
    in Serbian and leave bundles for sr and sr_CS locales empty. This way, users
    of your package will know whether you support a certain locale or not.

3.  If you already have some data to localize, create more bundles with
    localized data.

4.  Decide on the name of your package. You will use the package name to access
    your resources.

5.  Compile the resource bundles using the `genrb` tool. The command line format
    is `genrb [options] list-of-input-files`. Genrb expects that source files
    are in invariant encoding and `\uXXXX` characters or UTF-8/UTF-16 with BOM.
    If you need to use a different encoding, specify it using the `--encoding`
    option. You also need to specify the destination directory name for your
    resources using the `--destdir` option. This destination name needs to be the
    same as the package name. Full list of options can be retrieved by invoking
    `genrb --help`.

    You can also output Java class files. You will need to specify the
    `--write-java` option, followed by an optional encoding for the resulting
    `.java` file. Default encoding is ASCII + `\uXXXX`. You will also have to
    specify the resource bundle name using the `--bundle-name argument`.

    After using `genrb`, you will end up with files of name
    `packagename_localename.res`. For example, if you had `root.txt`, `en.txt`,
    `en_US.txt`, `es.txt` and you invoked `genrb` using the following command line:
    `genrb -d myapplication root.txt en.txt en_US.txt es.txt`, you will end up
    with `myapplication/root.res`, `myapplication/en.res`, etc. The forward slash can
    be a back slash on some platforms, like Windows. These files are now ready
    to use and you can open them using `ures_open("myapplication", "en_US", err);`.

6.  However, you might want to have only one file containing all the data. In
    that case you need to use the package data tool. It can produce either a
    memory mapped file or a dynamically linked library. For more information on
    how to use package data tool, see the appropriate [section](../icudata.md).

Rolling out your own data takes some practice, especially if you want to package
it all together. You might want to take a look at how we package data. Good
places to start (except of course ICU's own [data](../../../icu4c/source/data/))
are [source/test/testdata/](../../../icu4c/source/test/testdata/) and
[source/samples/ufortune/resources/](../../../icu4c/source/samples/ufortune/resources/)
directories.

Also, here is a sample Windows batch file that does compiling and packing of
several resources:

```bat
genrb -d myapplication root.txt en.txt en_GB.txt fr.txt es.txt es_ES.txt
echo root.res en.res en_GB.res fr.res es.res es_ES.res > packagelist.txt
mkdir tmpdir
pkgdata -p myapplication -T tmpdir -m common packagelist.txt
```

It is also possible to use the `icupkg` tool instead of `pkgdata` to generate .dat
data archives. The `icupkg` tool became available in ICU4C 3.6. If you need the
data in a shared or static library, you still need to use the `pkgdata` tool. For
easier maintenance, packaging, installation and application patching, it's
recommended that you use .dat data archives.

### Using XLIFF for Localization

ICU provides tool that allow for converting resource bundles to and from XLIFF
format. Files in XLIFF format can contain translations of resources. In that
case, more than one resulting resource bundle will be constructed.

To produce a XLIFF file from a resource bundle, use the `-x` option of `genrb` tool
from ICU4C. Assume that we want to convert a simple resource bundle to the XLIFF
format:

```
root {
   usage           {"usage: ufortune [-v] [-l locale]"}
   optionMessage   {"unrecognized command line option:"} 
}
```

To get a XLIFF file, we need to call genrb like this: `genrb -x -l en root.txt`.
Option `-x` tells `genrb` to produce XLIFF file, option `-l` specifies the language of
the resource. If the language is not specified, `genrb` will try to deduce the
language from the resource name (en, zh, sh). If the resource name is not an ISO
language code (root), default language for the platform will be used. Language
will be a source attribute for all the translation units. XLIFF file produced
from the resource above will be named `root.xlf` and will look like this:

```xml
<?xml version="1.0" encoding="utf-8"?>
<xliff version = "1.1 "xmlns = 'urn:oasis:names:tc:xliff:document:1.1'
xmlns:xsi = 'http://www.w3.org/2001/XMLSchema-instance'
xsi:schemaLocation='urn:oasis:names:tc:xliff:document:1.1
http://www.oasis-open.org/committees/xliff/documents/xliff-core-1.1.xsd'>
    <file xml:space = "preserve" source-language = "en”
         datatype = "x-icu-resource-bundle" original = "root.txt"
         date = "2007-08-17T21:17:08Z">
        <header>
            <tool tool-id = "genrb-3.3-icu-3.8" tool-name = "genrb"/>
        </header>
        <body>
            <group id = "root" restype = "x-icu-table">
                <trans-unit id = "optionMessage" resname = "optionMessage">
                    <source>unrecognized command line option:</source>
                </trans-unit>
                <trans-unit id = "usage" resname = "usage">
                    <source>usage: ufortune [-v] [-l locale]</source>
                </trans-unit>
            </group>
        </body>
    </file>
</xliff>
```

This file can be sent to translators. Using translation tools that support
XLIFF, translators will produce one or more translations for this resource.
Processed file might look a bit like this:

```xml
<?xml version="1.0" encoding="utf-8"?>
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

In order to convert this file to a set of resource bundle files, we need to use
ICU4J's `com.ibm.icu.dev.tool.localeconverter.XLIFF2ICUConverter` class.

> :point_right: **Note**: XLIFF2ICUConverter class relies on XML parser being
> available. JDK 1.4 and newer provide a XML parser out of box. For earlier
> versions, you will need to install xerces.

Command line for running XLIFF2ICUConverter should specify the file than needs
to be converted, sh.xlf in this case. Optionally, you can specify input and
output directories as well as the package name. After running this tool, two
files will be produced: en.txt and sh.txt. This is how they would look like:

```
// ***************************************************************************
// *
// * Tool: com.ibm.icu.dev.tool.localeconverter.XLIFF2ICUConverter.java
// * Date & Time: 08/17/2007 11:33:54 AM HST
// * Source File: C:\trunk\icuhtml\userguide\xliff\sh.xlf
// *
// ***************************************************************************
en:table{
    optionMessage:string{"unrecognized command line option:"}
    usage:string{"usage: ufortune [-v] [-l locale]"}
}
```

and

```
// ***************************************************************************
// *
// * Tool: com.ibm.icu.dev.tool.localeconverter.XLIFF2ICUConverter.java
// * Date & Time: 08/17/2007 11:33:54 AM HST
// * Source File: C:\trunk\icuhtml\userguide\xliff\sh.xlf
// *
// ***************************************************************************
sh:table{
    optionMessage:string{"nepoznata opcija na komandnoj liniji:"}
    usage:string{"upotreba: ufortune [-v] [-l lokal]"}
}
```

These files can be then used as all the other resource bundle files.
