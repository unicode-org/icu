---
layout: default
title: ICU Design
nav_order: 5
parent: ICU
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# ICU Architectural Design
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

# Overview

This chapter discusses the ICU design structure, the ICU versioning support, and
the introduction of namespace in C++.

## Java and ICU Basic Design Structure

The JDK internationalization components and ICU components both share the same
common basic architectures with regard to the following:

1. [Locales](#locales)
2. [Data-driven services](#data-driven-services)
3. [ICU threading models and the open and close model](#icu-threading-model-and-open-and-close-model)
4. [Cloning customization](#cloning-customization)
5. [Error handling](#error-handling)
6. [Extensibility](#extensibility)
7. [Resource bundle inheritance model](#resource-bundle-inheritance-model)

There are design features in ICU4C that are not in the Java Development Kit
(JDK) due
to programming language restrictions. These features include the following:

### Locales

Locale IDs are composed of language, country, and variant information. The
following links provide additional useful information regarding ISO standards:
[ISO-639](http://lcweb.loc.gov/standards/iso639-2/englangn.html), and an ISO
Country Code,
[ISO-3166](http://www.iso.org/iso/en/prods-services/iso3166ma/02iso-3166-code-lists/list-en1.html).
For example, Italian, Italy, and Euro are designated as: it_IT_EURO.

### Data-driven Services

Data-driven services often use resource bundles for locale data. These services
map a key to data. The resources are designed not only to manage system locale
information but also to manage application-specific or general services data.
ICU supports string, numeric, and binary data types and can be structured into
nested arrays and tables.

This results in the following:

1. Data used by the services can be built at compile time or run time.
2. For efficient loading, system data is pre-compiled to .dll files or files
   that can be mapped into memory.
3. Data for services can be added and modified without source code changes.

### ICU Threading Model and Open and Close Model

The "open and close" model supports multi-threading. It enables ICU users to use
the same kind of service for different locales, either in the same thread or in
different threads.

For example, a thread can open many collators for different languages, and
different threads can use different collators for the same locale
simultaneously. Constant data can be shared so that only the current state is
allocated for each editor.

The ICU threading model is designed to avoid contention for resources, and
enable you to use the services for multiple locales simultaneously within the
same thread. The ICU threading model, like the rest of the ICU architecture, is
the same model used for the international services in Java™.

When you use a service such as collation, the client opens the service using an
ID, typically a locale. This service allocates a small chunk of memory used for
the state of the service, with pointers to shared, read-only data in support of
that service. (In Java, you call `getInstance()` to create an object; in C++,
`createInstance()`. ICU uses the open and close metaphor in C because it is more
familiar to C programmers.)

If no locale is supplied when a service is opened, ICU uses the default locale.
Once a service is open, changing the default locale has no effect. Thus, there
can not be any thread synchronization between the default locales and open
services.

When you open a second service for the same locale, another small chunk of
memory is used for the state of the service, with pointers to the same shared,
read-only data. Thus, the majority of the memory usage is shared. When any
service is closed, then the chunk of memory is deallocated. Other connections
that point to the same shared data stay valid.

Any number of services, for the same locale or different locales, can be open
within the same thread or in different threads.

#### Thread-safe const APIs

In recent ICU releases, we have worked to make any service object *thread-safe*
(usable concurrently) *as long as all of the threads are using only const APIs*:
APIs that are declared const in C++, take a const this-like service pointer in
C, or are "logically const" in Java. This is an enhancement over the original
Java/ICU threading model. (Originally, concurrent use of even only const APIs
was not thread-safe.)

However, you cannot use a reference to an open service object in two threads at
the same time *if either of them calls any non-const API*. An individual open
service object is not thread-safe for concurrent "writes". Rather, for non-const
use, you must use the clone function to create a copy of the service you want
and then pass this copy to the second thread. This procedure allows you to use
the same service in different threads, but avoids any thread synchronization or
deadlock problems.

#### Freezable

Some classes also implement the `Freezable` interface (or similar pattern in
C++), for example `UnicodeSet` or `Collator`: An object that typically starts
out mutable can be set up and then "frozen", which makes it immutable and thus
usable concurrently because all non-const APIs are disabled. A frozen object can
never be "thawed". For example, a `Collator` can be created, various attributes
set, then frozen and then used from many threads for comparing strings and
getting sort keys.

#### Clone vs. open

Clone operations are designed to be much faster than reopening the service with
initial parameters and copying the source's state. (With objects in C++ and
Java, the clone function is also much safer than trying to recreate a service,
since you get the proper subclass.) Once a service is cloned, changes will not
affect the original source service, or vice-versa.

Thus, the normal mode of operation is to:

1. Open a service with a given locale.
2. Use the service as long as needed. However, do not keep opening and closing
   a service within a tight loop.
3. Clone a service if it needs to be used in parallel in another thread.
4. Close any clones that you open as well as any instances of the services that
   are owned.

> :point_right: **Note**: These service instances may be closed in any sequence.
The preceding steps are given as an example.

### Cloning Customization

Typically, the services supplied with ICU cover the vast majority of usages.
However, there are circumstances where the service needs to be customized for a
new locale. ICU (and Java) enable you to create customized services. For
example, you can create a `RuleBasedCollator` by merging the rules for French and
Arabic to get a custom French-Arabic collation sequence. By merging these rules,
the pointer does not point to a read-only table that is shared between threads.
Instead, the pointer refers to a table that is specific to your particular open
service. If you clone the open service, the table is copied. When you close the
service, the table is destroyed.

For some services, ICU supplies registration. You can register a customized open
service under an ID; keeping a copy of that service even after you close the
original. A client in that thread or in other threads can recreate a copy of the
service by opening with that ID.

ICU may cache service instances. Therefore, registration should be done during
startup, before opening services by locale ID.

These registrations are not persistent; once your program finishes, ICU flushes
all the registrations. While you still might have multiple copies of data
tables, it is faster to create a service from a registered ID than it is to
create a service from rules.

> :point_right: **Note**: To work around the lack of persistent registration,
query the service for the parameters used to create it and then store those
parameters in a file on a disk.

For services whose IDs are locales, such as collation, the registered IDs must
also be locales. For those services (like Transliteration or Timezones) that are
cross-locale, the IDs can be any string.

Prospective future enhancements for this model are:

1. Having custom services share data tables, by making those tables reference
   counted. This will reduce memory consumption and speed clone operations (a
   performance enhancement chiefly useful for multiple threads using the same
   customized service).
2. Expanding registration for all the international services.
3. Allowing persistent registration of services.

#### Per-client Locale ID vs Per-thread Locale ID

Some application environments operate by setting a per thread (or per process)
locale ID, and then not passing the locale ID as a parameter during processing.
If this usage model were used with ICU in a multi-threaded server, it might
result in ICU being requested to constantly open, use, and then close service
objects. Instead, it is recommended that locale IDs be associated with each
client be stored with other per-client data, along with any service objects
(such as collators or formatters) that client might use. If operations involving
a single client are short-lived, it might be more efficient to keep a pool of
service objects, organized according to locale. Then, if a particular locale's
formatter is in high demand, that formatter can be used, and then returned to
the pool.

### ICU Memory Usage

ICU4C APIs are designed to allow separate heaps for its libraries vs. the
application. This is achieved by providing functions to allocate and release
objects owned by ICU4C using only ICU4C library functions. For more details see
the Memory Usage section in the [Coding Guidelines](dev/codingguidelines.md#memory-usage).

### ICU4C Initialization and Termination

The ICU library does not normally require any explicit initialization prior to
use. An application begins use simply by calling any ICU API in the usual way.
There are, however, a few functions affecting ICU's configuration, that, if used,
must be called first, before other use of ICU in a process. These are outlined below.

1. `u_setMemoryFunctions()`. This function replaces the standard library heap
allocation functions used by ICU with alternate versions, provided by the
application. If it is needed, `u_setMemoryFunctions()` must be called first, before
any other use of ICU. This functionality is not commonly used.

2. ICU Data Locating Functions, `u_setCommonData()`, `u_setDataDirectory()`, and
`u_setAppData()`. One or more of these functions will be required when ICU is
configured to load its data directly from files rather than taking it from the
default data DLL, and the files are not in the default location. Again, this is
not common. See [ICU Data](icudata#icu-data-directory).

3. Sanity check that ICU is functioning and able to access data. This is
important because configuration or installation problems that leave ICU unable
to load its data do occur, and the resulting failures can be confusing.
Since not all ICU APIs have UErrorCode parameters, in the absence of data they
may sometimes silently return incorrect results.

   The function `ulocdata_getCLDRVersion()` is suitable; it is small and light
weight, requires data, and reports the error in the absence of data.


When an application is terminating it should call the function `u_cleanup()`,
which frees all heap storage and other system resources that are held internally
by the ICU library. While the use of `u_cleanup()` is not strictly required,
failure to call it will cause memory leak checking tools to report problems for
resources being held by ICU library.

Before calling `u_cleanup()`, all ICU objects that were created by the
application must be deleted, and all ICU services (plain C APIs) must be closed.

For some platforms the configure option `--enable-auto-cleanup`, or defining
the option `UCLN_NO_AUTO_CLEANUP` to 0, will add code which automatically cleans
up ICU when its shared library is unloaded. See comments in `ucln_imp.h`

#### C++ Static Initialization and Destruction

The ICU library itself does not rely on C++ static initializers, meaning that
applications will not encounter order-of-initialization problems from the use of
ICU.

There are, however, some significant limitations for applications that make use
of ICU at C++ static initialization time:

1.  `u_setMemoryFunctions()` and the data locating functions, if needed, must
still be called before any other use of ICU. Which includes any use during the
construction of static objects.

2.  `u_cleanup()` can only be called after all other ICU-using objects have been
deleted. Finding a suitable time and place for the call to `u_cleanup()` may be
difficult, however. Refer to the C++ literature on the order of static
initialization and destruction.

3.  Destruction of static objects that are scoped to a code block. These, by the
conventions of C++, are lazily initialized when the code block is first entered,
so there are no issues during static initialization. But object destruction
happens when the program terminates, leaving the problem of where to call
`u_cleanup()`, as discussed above.

#### Dynamically Loading and Unloading ICU

Applications may arrange to dynamically load the ICU library when it is needed,
and unload it when through, repeating the process as required. The specific
details for loading and unloading, and accessing such libraries, are operating
system dependent.

For ICU to be used in this way, before unloading, all ICU objects and services
must be closed or deleted, and `u_cleanup()` must be called.

On Windows, the loading and unloading of ICU should never be done inside
[DllMain](https://docs.microsoft.com/en-us/windows/win32/dlls/dllmain). Loading
one of the ICU libraries can cause other libraries or files to be loaded,
leading to potential dead-lock.

#### Initializing ICU in Multithreaded Environments

There is one specialized case where extra care is needed to safely initialize
ICU. This situation will arise only when ALL of the following conditions occur:

1. The application main program is written in plain C, not C++.
2. The application is multithreaded, with the first use of ICU within the
   process possibly occurring simultaneously in more than one thread.
3. The application will be run on a platform that does not handle C++ static
   constructors from libraries when the main program is not in C++. Platforms
   known to exhibit this behavior are Mac OS X and HP/UX. Platforms that handle
   C++ libraries correctly include Windows, Linux and Solaris.

To safely initialize the ICU library when all of the above conditions apply, the
application must explicitly arrange for a first-use of ICU from a single thread
before the multi-threaded use of ICU begins. A convenient ICU operation for this
purpose is `uloc_getDefault()`, declared in the header file `unicode/uloc.h`.

> :point_right: **Note**: The status of this situation needs further
investigation. See issue
[ICU-21380](https://unicode-org.atlassian.net/browse/ICU-21380)


### Error Handling

In order for ICU to maximize portability, this version includes only the subset
of the C++ language that compile correctly on older C++ compilers and provide a
usable C interface. Thus, there is no use of the C++ exception mechanism in the
code or Application Programming Interface (API).

To communicate errors reliably and support multi-threading, this version uses an
error code parameter mechanism. Every function that can fail takes an error-code
parameter by reference. This parameter is always the last parameter listed for
the function.

The `UErrorCode` parameter is defined as an enumerated type. Zero represents no
error, positive values represent errors, and negative values represent non-error
status codes. Macros (`U_SUCCESS` and `U_FAILURE`) are provided to check the
error code.

The `UErrorCode` parameter is an input-output function. Every function tests the
error code before performing any other task and immediately exits if it produces
a FAILURE error code. If the function fails later on, it sets the error code
appropriately and exits without performing any other work, except for any
cleanup it needs to do. If the function encounters a non-error condition that it
wants to signal, such as "encountered an unmapped character" in conversion, the
function sets the error code appropriately and continues. Otherwise, the
function leaves the error code unchanged.

Generally, only the functions that do not take a `UErrorCode` parameter, but
call functions that do, must declare a variable. Almost all functions that take
a `UErrorCode` parameter, and also call other functions that do, merely have to
propagate the error code that they were passed to the functions they call.
Functions that declare a new `UErrorCode` parameter must initialize it to
`U_ZERO_ERROR` before calling any other functions.

ICU enables you to call several functions (that take error codes) successively
without having to check the error code after each function. Each function
usually must check the error code before doing any other processing, since it is
supposed to stop immediately after receiving an error code. Propagating the
error-code parameter down the call chain saves the programmer from having to
declare the parameter in every instance and also mimics the C++ exception
protocol more closely.

### Extensibility

There are 3 major extensibility elements in ICU:

1. **Data Extensibility**:
   The user installs new locales or conversion data to enhance the existing ICU
   support. For more details, refer to the package tool (:construction: **TODO**: need link)
   chapter for more information.
2. **Code Extensibility**:
   The classes, data, and design are fully extensible. Examples of this
   extensibility include the BreakIterator , RuleBasedBreakIterator and
   DictionaryBasedBreakIterator classes.
3. **Error Handling Extensibility**:
   There are mechanisms available to enhance the built-in error handling when
   it is necessary. For example, you can design and create your own conversion
   callback functions when an error occurs. Refer to the
   [Conversion](conversion/index.md) chapter callback section for more
   information.

### Resource Bundle Inheritance Model

A resource bundle is a set of \<key,value> pairs that provide a mapping from key
to value. A given program can have different sets of resource bundles; one set
for error messages, one for menus, and so on. However, the program may be
organized to combine all of its resource bundles into a single related set.

The set is organized into a tree with "root" at the top, the language at the
first level, the country at the second level, and additional variants below
these levels. The set must contain a root that has all keys that can be used by
the program accessing the resource bundles.

Except for the root, each resource bundle has an immediate parent. For example,
if there is a resource bundle `X_Y_Z`, then there must be the resource bundles:
`X_Y`, and `X`. Each child resource bundle can omit any \<key,value> pair that is
identical to its parent's pair. (Such omission is strongly encouraged as it
reduces data size and maintenance effort). It must override any \<key,value> pair
that is different from its parent's pair. If you have a resource bundle for the
locale ID `language_country_variant`, you must also have
a bundle for the ID `language_country` and one for the ID `language`.

If a program doesn't find a key in a child resource bundle, it can be assumed
that it has the same key as the parent. The default locale has no effect on
this. The particular language used for the root is commonly English, but it
depends on the developer's preference. Ideally, the language should contain
values that minimize the need for its children to override it.

The default locale is used only when there is not a resource bundle for a given
language. For example, there may not be an Italian resource bundle. (This is
very different than the case where there is an Italian resource bundle that is
missing a particular key.) When a resource bundle is missing, ICU uses the
parent unless that parent is the root. The root is an exception because the root
language may be completely different than its children. In this case, ICU uses a
modified lookup and the default locale. The following are different lookup
methods available:

**Lookup chain** : Searching for a resource bundle.

    en_US_<some-variant>
    en_US
    en
    <defaultLang>_<defaultCountry>
    <defaultLang>
    root

**Lookup chain** : Searching for a \<key, value> pair after
`en_US_<some-variant>` has ben loaded. ICU does not use the default locale in
this case.

    en_US_<some-variant>
    en_US
    en
    root

## Other ICU Design Principles

ICU supports extensive version code and data changes and introduces namespace
usage.

### Version Numbers in ICU

Version changes show clients when parts of ICU change. ICU; its components (such
as `Collator`); each resource bundle, including all the locale data resource
bundles; and individual tagged items within a resource bundle, have their own
version numbers. Version numbers numerically and lexically increase as changes
are made.

All version numbers are used in Application Programming Interfaces (APIs) with a
`UVersionInfo` structure. The `UVersionInfo` structure is an array of four
unsigned bytes. These bytes are:

1. Major version number
2. Minor version number
3. Milli version number
4. Micro version number

Two `UVersionInfo` structures may be compared using binary comparison (`memcmp`)
to see which is larger or newer. Version numbers may be different for different
services. For instance, do not compare the ICU library version number to the ICU
collator version number.

`UVersionInfo` structures can be converted to and from string representations as
dotted integers (such as "1.4.5.0") using the `u_versionToString()` and
`u_versionFromString()` functions. String representations may omit trailing zeros.

The interpretation of version numbers depends on what is being described.

#### ICU Release Version Number (ICU 49 and later)

The first version number field contains the ICU release version number, for
example 49. Each new version might contain new features, new locale data, and
modified behavior. (See below for more information on
[ICU Binary Compatibility](#icu-binary-compatibility)).

The second field is 1 for the initial release (e.g., 49.1). The second and
sometimes third fields are incremented for binary compatible maintenance
releases.

* For maintenance releases for only either C or J, the third field is
  incremented (e.g., ICU4C 49.1.1).
* For shared updates for C & J, the second field is incremented to 2 and
  higher (e.g., ICU4C & ICU4J 49.2).

(The second field is 0 during development, with milestone numbers in the third
field during that time. For example, 49.0.1 for 49 milestone 1.)

#### ICU Release Version Number (ICU 1.4 to ICU 4.8)

In earlier releases, the first two version fields together indicated the ICU
release, for example 4.8. The third field was 0 for the initial release, and 1
and higher for binary compatible (bug fixes only) maintenance releases (e.g.,
4.8.1). The fourth field was used for updates specific to only one of Java, C++,
or ICU-in-Eclipse.

The second version field was *even* for formal releases ("reference releases")
(e.g., 1.6 or 4.8) and *odd* during their development (unreleased unstable
snapshot versions; e.g., 4.7). During development, the third field contained the
milestone number (e.g., 4.7.1 for 4.8 milestone 1). For very old ICU code, we
published semi-formal “enhancement” releases with odd second-field numbers
(e.g., 1.7).

Library filenames and some other internal uses already used a concatenation of
the first two fields ("48" for 4.8).

#### Resource Bundles and Elements

The data stored in resource bundles is tagged with version numbers. A resource
bundle can contain a tagged string named "Version" that declares the version
number in dotted-integer format. For example,

```text
en {
    Version { "1.0.3.5" }
    ...
}
```

A resource bundle may omit the "version" element and thus, will inherit a
version along the usual chain. For example, if the resource bundle **en_US**
contained no "version" element, it would inherit "1.0.3.5" from the parent en
element. If inheritance passes all the way to the root resource bundle and it
contains no "version" resource, then the resource bundle receives the default
version number 0.

Elements within a resource bundle may also contain version numbers. For example:

```text
be {
    CollationElements {
        Version { "1.0.0.0" }
        ...
    }
}
```

In this example, the CollationElements data is version 1.0.0.0. This element
version is not related to the version of the bundle.

#### Internal version numbers

Internally, data files carry format and other version numbers. These version
numbers ensure that ICU can use the data file. The interpretation depends
entirely on the data file type. Often, the major number in the format version
stays the same for backwards-compatible changes to a data file format. The minor
format version number is incremented for additions that do not violate the
backwards compatibility of the data file.

#### Component Version Numbers

ICU component version numbers may be found using:

1. `u_getVersion()` returns the version number of ICU as a whole in C++. In C,
   `ucol_getVersion()` returns the version number of ICU as a whole.
2. `ures_getVersion()` and `ResourceBundle::getVersion()` return the version
   number of a ResourceBundle. This is a data version number for the bundle as a
   whole and subject to inheritance.
3. `u_getUnicodeVersion()` and `Unicode::getUnicodeVersion()` return the version
   number of the Unicode character data that underlies ICU. This version
   reflects the numbering of the Unicode releases. See
   <http://www.unicode.org/> for more information.
4. `Collator::getVersion()` in C++ and `ucol_getVersion()` in C return the version
   number of the Collator. This is a code version number for the collation code
   and algorithm. It is a combination of version numbers for the collation
   implementation, the Unicode Collation Algorithm data (which is the data that
   is used for characters that are not mentioned in a locale's specific
   collation elements), and the collation elements.

#### Configuration and Management

A major new feature in ICU 2.0 is the ability to link to different versions of
ICU with the same program. Using this new feature, a program can keep using ICU
1.8 collation, for example, while using ICU 2.0 for other services. ICU now can
also be unloaded if needed, to free up resources, and then reloaded when it is
needed.

### Namespace in C++

ICU 2.0 introduced the use of a C++ namespace to avoid naming collision between
ICU exported symbols and other libraries. All the public ICU C++ classes are
defined in the "icu_VersionNumber::" namespace, which is also aliased as
namespace "icu". Starting with ICU 2.0, including any public ICU C++ header by
default includes a "using namespace icu_VersionNumber" statement. This is for
backward compatibility, and should be turned off in favor of explicitly using
`icu::UnicodeString` etc. (see [How To Use ICU](howtouseicu.md)). (If entry point
renaming is turned off, then only the unversioned "icu" namespace is used.)

Starting with ICU 49, ICU4C requires namespace support.

### Library Dependencies (C++)

It is sometimes useful to see a dependency chart between the public ICU APIs and
ICU libraries. This chart can be useful to people that are new to ICU or to
people that want only certain ICU libraries.

> :construction: **TODO**: The dependency chart is currently not available.

Here are some things to realize about the chart.

1. It gives a general overview of the ICU library dependencies.
2. Internal dependencies, like the mutex API, are left out for clarity.
3. Similar APIs were lumped together for clarity (e.g. Formatting). Some of
   these dependency details can be viewed from the ICU API reference.
4. The descriptions of each API can be found in our [ICU API
   reference](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/)

### Code Dependencies (C++)

Starting with ICU 49, the dependencies of code files (.o files compiled from
.c/.cpp) are documented in
[source/test/depstest/dependencies.txt](https://github.com/unicode-org/icu/blob/master/icu4c/source/test/depstest/dependencies.txt).
Adjacent Python code is used to parse this file and to
[verify](http://site.icu-project.org/processes/release/tasks/healthy-code#TOC-Check-library-dependencies)
that it matches the actual dependencies of the code files.

The dependency list can be used to build subset libraries. In addition, by
reducing intra-library dependencies, the code size of statically linked ICU code
has been reduced.

### ICU API categories

ICU APIs, as defined in header and class files, are either "external" or
"internal". External APIs are meant to be used by applications, while internal
APIs should be used only within ICU. APIs are marked to indicate whether they
are external or internal, as follows. Every external API has a lifecycle label,
see below.

#### External ICU4C APIs

External ICU4C APIs are

1. declared in header files in unicode folders and exported at build/install
   time to an `include/unicode` folder
2. when C++ class members, are `public` or `protected`
3. do not have an `@internal` label

Exception: Layout engine header files are not in a unicode folder, although the
public ones are still copied to the `include/unicode` folder at build/install
time. External layout engine APIs are the ones that have lifecycle labels and
not an `@internal` label.

#### External ICU4J APIs

External ICU4J APIs are

1. declared in one of the ICU4J core packages (`com.ibm.icu.lang`,
   `com.ibm.icu.math`, `com.ibm.icu.text`, or `com.ibm.icu.util`).
2. `public` or `protected` class members
3. `public` or `protected` contained classes
4. do not have an `@internal` label

#### "System" APIs

"System" APIs are external APIs that are intended only for special uses for
system-level code, for example `u_cleanup()`. Normal users should not use them,
although they are public and supported. System APIs have a `@system` label
in addition to the lifecycle label that all external APIs have (see below).

#### Internal APIs

All APIs that do not fit any of the descriptions above are internal, which means
that they are for ICU internal use only and may change at any time without
notice. Some of them are member functions of public C++ or Java classes, and are
"technically public but logistically internal" for implementation reasons;
typically because programming languages don't provide sufficiently access
control (without clumsy mechanisms). In this case, such APIs have an
`@internal` label.

### ICU API compatibility

As ICU develops, it adds external APIs - functions, classes, constants, and so
on. Occasionally it is also necessary to remove or change external APIs. In
order to make this work, we use the following process:

For all API changes (and for significant/controversial/difficult implementation
changes), we use proposals to announce and discuss them. A proposal is simply an
email to the icu-design mailing list that details what is proposed to be
changed, with an expiration date of typically a week. This gives all mailing
list members a chance to review upcoming changes, and to discuss them. A
proposal often changes significantly as a result of discussion. Most proposals
will eventually find consensus among list members; otherwise, the ICU-TC decides
what to do. If the addition or change of APIs would affect you, please subscribe
to the main [icu-design mailing list](http://icu-project.org/contacts.html).

When a **new API** is added to ICU, it **is marked as draft with a `@draft ICU
x.y` label in the API documentation, **where x.y is the ICU version when the
API *signature* was introduced or last changed**. A draft API is not guaranteed
to be stable! Although we will not make gratuitous changes, sometimes the draft
APIs turns out to be unsatisfactory in actual practice and may need to be
changed or even removed. Changes of "draft" API are subject to the proposal
process described above.

**When a `@draft ICU x.y` API is changed, it must remain `@draft` and its version
number must be updated.**

In ICU4J 3.4.2 and earlier, `@draft` APIs were also marked with Java's `@deprecated`
tag, so that uses of draft APIs in client code would be flagged by the compiler.
These uses of the `@deprecated` tag were indicated with the comment “This is a
draft API and might change in a future release of ICU.” Many clients found this
confusing and/or undesireable, so ICU4J 3.4.3 no longer marks draft APIs with
the `@deprecated` tag by default. For clients who prefer the earlier behavior,
ICU4J provides an ant build target, `restoreDeprecated`, which will update the
source files to use the `@deprecated` tag. Then clients can just rebuild the ICU4J
jar as usual.

When an API is judged to be stable and has not been changed for at least one ICU
release, it is relabeled as stable with a `@stable ICU x.y**` label in the API
documentation. A stable API is expected to be available in this form for a long
time. The ICU version **x.y** indicates the last time the API *signature* was
introduced or changed. **The promotion from `@draft ICU x.y` to `@stable ICU x.y`
must not change the x.y version number.**

We occasionally make an exception and allow adding new APIs marked as
`@stable ICU x.y` APIs in the x.y release itself if we believe that they have to
be stable. We might do this for enum constants that reflect 1:1 Unicode property
aliases and property value aliases, for a Unicode upgrade in the x.y release.

We sometimes **"broaden" a `@stable`** API function by changing its signature
in a compatible way. For example, in Java, we might change an input parameter
from a `String` to a `CharSequence`. In this case we keep the `@stable` but
update the ICU version number indicating the function signature change.

Even a stable API may eventually need to become deprecated or obsolete. Such
APIs are strongly discouraged from use. Typically, an improved API is introduced
at the time of deprecation/obsolescence of the old one.

1. Use of deprecated APIs is strongly discouraged, but they are retained for
   backward compatibility. These are marked with labels like
   `@deprecated ICU x.y Use u_abc() instead.`. **The ICU version x.y shows the
   ICU release in which the API was first declared "deprecated".**
2. In ICU4J, starting with release 57, a custom Javadoc tag `@discouraged`
   was added. While similar to `@deprecated` it is used when either ICU wants
   to discourage a particular API from use but the JDK hasn't deprecated it or
   ICU needs to keep it for compatibility reasons. These are marked with labels
   like `@discouraged ICU x.y. Use u_abc() instead.`.
3. Obsolete APIs are are those whose continued retention will cause severe
   conflicts or user error, or whose continued support would be a very
   significant maintenance burden. We make every effort to keep these to a
   minimum. Obsolete APIs are marked with labels like `@obsolete ICU x.y. Use
   u_abc() instead since this API will be removed in that release.`.
   **The x.y indicates that we plan to remove it in ICU version x.y.**

Stable C or Java APIs will not be obsoleted because doing so would break
forward binary compatibility of the ICU library. Stable APIs may be
deprecated, but they will be retained in the library.

An "obsolete" API will remain unchanged until it is removed in the indicated
ICU release, which will be usually one year after the API was declared
obsolete. Sometimes we still keep it available for some time via a
compile-time switch but stop maintaining it. In rare occasions, an API must
be replaced right away because of naming conflicts or severe defects; in
such cases we provide compile-time switches (`#ifdef` or other mechanisms) to
select the old API.

For example, here is how an API might be tagged in various versions:

* **In ICU 0.2**: The API is newly introduced as a draft in this release.

  ```text
  @draft ICU 0.2
  f(x)
  ```

* **In ICU 0.4**: The draft version number is updated, because the signature
  changed.

  ```text
  @draft ICU 0.4
  f(x, y)
  ```

* **In ICU 0.6**: The API is promoted from draft to stable, but the version
  number does not change, as the signature is the same.

  ```text
  @stable ICU 0.4
  f(x, y)
  ```

* **In ICU 1.0**: The API is "broadened" in a compatible way. For example,
  changing an input parameter from char to int or from some class to a base
  class. The signature is changed (so we update the ICU version number), but old
  calling code continues to work unchanged (so we retain @stable if that's what
  it was.)

  ```text
  @stable ICU 1.0
  f(xbase, y)
  ```

* **In ICU 1.2**: The API is demoted to deprecated (or obsolete) status.

  ```text
  @deprecated ICU 1.2 Use g(x,y,z) instead.
  f(xbase, y)
  ```

  or, when this API is planned to be removed in ICU 1.4:

  ```text
  @obsolete ICU 1.4. Use g(x,y,z) instead.
  f(xbase, y)
  ```

### ICU Binary Compatibility

*Using ICU as an Operating System Level Library*

ICU4C may be configured for use as a system library in an environment where
applications that are built with one version of ICU must continue to run without
change with later versions of the ICU shared library.

Here are the requirements for enabling binary compatibility for ICU4C:

1. Applications must use only APIs that are marked as stable.
2. Applications must use only plain C APIs, never C++.
3. ICU must be built with function renaming disabled.
4. Applications must be built using an ICU that was configured for binary
   compatibility.
5. Use ICU version 3.0 or later.
6. Provide both “common” and “i18n” libraries, or build a combined library.

**Stable APIs Only.** APIs in the ICU library that are tagged as being stable
will be maintained in future versions of the library. Stable functions will
continue to exist with the same signature and the same meaning, allowing
applications to continue to work without change.

Stable APIs do not guarantee that the results from every function will always be
completely identical between ICU versions (see the
[Version Numbers in ICU](#version-numbers-in-icu) section above). Bugs may be
fixed. The Unicode character data may change with new versions of the Unicode
standard. Locale data may be updated or changed, yielding different results for
operations like formatting or collation. Applications that require exact
bit-for-bit, bug-for-bug compatibility of ICU results should not rely on ICU
release-to-release binary compatibility, but should instead link against a
specific version of ICU.

To verify that an application uses only stable APIs, build it with the C
preprocessor symbols `U_HIDE_DRAFT_API` and `U_HIDE_DEPRECATED_API` defined. This
will produce build errors if any draft, deprecated or obsolete APIs are used. An
operating system level installation of ICU may set this option permanently.

**C APIs only.** Only plain C APIs remain compatible across ICU releases. The
reason C++ binary compatibility is not supported is primarily because the design
of C++ language and runtime environments present extreme technical difficulties
to doing so. Stable C++ APIs are *source* compatible, but applications using
them must be recompiled when moving between ICU releases.

**Function renaming disabled.** Function renaming is an ICU feature that allows
an application to explicitly link against a specific version of the ICU library,
and to continue to use that version even when other ICU versions exist in the
runtime environment. This is the exact opposite of release-to-release binary
compatibility – instead of being able to transparently change ICU versions, an
application is explicitly tied to one specific version.

Function renaming is enabled by default, and must be disabled at ICU build time
to enable release to release binary compatibility. To disable renaming, use the
configure option

```shell
configure -–disable-renaming [other configure options]
```

(Configure options may also be passed to the runConfigureICU script.)

To enable release-to-release binary compatibility, ICU must be built with
`--disable-renaming`, *and* applications must be built using the headers and
libraries that resulted from the `–-disable-renaming` ICU build

**ICU Version 3.0 or Later.** Binary compatibility of ICU releases is supported
beginning with ICU version 3.0. Older versions of ICU (2.8 and earlier) do not
provide for binary compatibility between versions.

**Provide both “common” and “i18n” libraries, or build a combined library.**
It is rare but possible that services/APIs move from one library to another.
For example, many years ago we moved the BreakIterator APIs from i18n to common,
so that word titlecasing functions no longer needed separate code to find
titlecasing or word break opportunities.

More recently, the ListFormatter moved from the common library to i18n
when its features grew beyond primitive patterns to also support
FieldPosition and FormattedValue features.

There is also a third, “io” library.
It is possible that some of its functionality may be moved to the i18n or common
libraries.
(A likely candidate might be `operator<<(std::ostream& stream, const UnicodeString& s)`,
although there are no actual plans to do so at the time of this writing.)

One can build a combined library which provides the exports from
both the “common” and “i18n” libraries,
in order to provide a single library for linking against.

This may be needed for some platforms where there is a strong relationship
between an API and the library that implements it.
For example, on Windows platforms, attempting to find an API that has been moved
with a `LoadLibrary`/`GetProcAddress` approach will fail,
unless you are using a combined library.

#### Linking against multiple versions of ICU4C

This section is intended to aid software developers who are implementing or
integrating solutions based on ICU, that may need to consider having multiple
versions of ICU running within the same executable (address space) at once.
Typically, users of ICU are encouraged to update to the latest stable version.
Under certain circumstances, however, behavior from earlier versions is desired,
or else, an application is linking together code which is already built against
a different version of ICU.

The major and minor numbers are the first and second numbers in a version
number, separated by a period. For example, in the version numbers 3.4.2.1,
3.4.2, or 3.4, "3" is the major, and "4" is the minor. Normally, ICU employs
"symbol renaming", such that the C function names and C++ object names are
`#defined` to contain the major and minor numbers. So, for example, if your
application calls the function `ucnv_open()`, it will link against
`ucnv_open_3_4` if compiled against ICU 3.4, 3.4.2, or even 3.4.2.1. However, if
compiled against ICU 3.8, the same code will link against `ucnv_open_3_8`.
Similarly, `UnicodeString` is renamed to `UnicodeString_3_4`, etc. This is normally
transparent to the user, however, if you inspect the symbols of the library or
your code, you will see the modified symbols.

If there are multiple versions of ICU being linked against in one application,
it will need to link against all relevant libraries for each version, for
example, common, i18n, and data. ICU uses standard library renaming, where, for
example, `libicuuc.so` on one platform will actually be a symbolic link to
`libicuuc.so.3.4`. When multiple ICU versions are used, the application may need
to explicitly link against the exact versions of ICU being used.

To disable renaming, build ICU with `--disable-renaming` passed to configure.
Or, set the equivalent `#define U_DISABLE_RENAMING 1`. Renaming must be disabled
both in the ICU build, and in the calling application.

### ICU Data Compatibility

Starting in ICU 3.8 and later, the data library that comes with ICU is binary
compatible and structurally compatible with versions of ICU with the same major
and minor version, or a maintenance release. This allows multiple maintenance
releases of ICU to share the same data, but generally the latest maintenance
release of the data should be used.

The binary compatibility of the data refers to the resource bundle binary format
that is contains the locale data, charset conversion tables and other file
formats supported by ICU. These binary formats are readable by many versions of
ICU. For example, resource bundles written with ICU 3.6 are readable by ICU 3.8.

The structural compatibility of the data refers to the structural contents of
the ICU data. The structure of the locale data may change between reference
releases, but the keys to reference specific types of data will be the same
between maintenance releases. This means that resource keys to access data
within resource bundles will work between maintenance releases of a specific
reference release. For example, an ICU 3.8 calendar will be able to use ICU
3.8.1 data, and vis versa; however ICU 3.6 may not be able to read ICU 3.8
locale data. Generally, these keys are not accessible by ICU users because only
the ICU implementation uses these resource keys.

The contents of the data library may change between ICU maintenance releases and
give you different results due to important updates and bug fixes. An example of
an important update would be a timezone rule update for when a country changes
when daylight saving time occurs. So the results may be different between
maintenance releases.

### ICU4J Serialization Compatibility

Starting in ICU4J 3.6, ICU4J stable API classes (marked as `@stable`) implementing
`java.io.Serializable` support serialized objects to be deserialized by ICU4J 3.6
or newer version of ICU4J. Some classes perform only shallow serialization,
therefore, it is not guaranteed that a deserialized object behaves exactly same
with the original object across ICU4J versions. Also, when it is difficult to
maintain serialization compatibility in a certain class across different ICU4J
versions for technical or other reasons, the ICU project committee may approve
the breakage. In such event, a note explaining the compatibility issue will be
posted in the ICU public mailing lists and also documented in the release note
of the new ICU4J version introducing the incompatibility.
