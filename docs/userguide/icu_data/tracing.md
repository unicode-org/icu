---
layout: default
title: Resource and Data Tracing
nav_order: 2
parent: ICU Data
---
<!--
© 2019 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Resource and Data Tracing
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

When building an [ICU data filter specification](buildtool.md), it is useful to
see what resources are being used by your application so that you can select
those resources and discard the others. This guide describes how to use
*utrace.h* to inspect resource access in real time in ICU4C.

**Note:** This feature is only available in ICU4C at this time. If you are
interested in ICU4J, please see
[ICU-20656](https://unicode-org.atlassian.net/browse/ICU-20656).

## Quick Start

First, you *must* have a copy of ICU4C configured with tracing enabled.

    $ ./runConfigureICU Linux --enable-tracing

The following program prints resource and data usages to standard out:

```cpp
#include "unicode/brkiter.h"
#include "unicode/errorcode.h"
#include "unicode/localpointer.h"
#include "unicode/utrace.h"

#include <iostream>

static void U_CALLCONV traceData(
        const void *context,
        int32_t fnNumber,
        int32_t level,
        const char *fmt,
        va_list args) {
    char        buf[1000];
    const char *fnName;

    fnName = utrace_functionName(fnNumber);
    utrace_vformat(buf, sizeof(buf), 0, fmt, args);
    std::cout << fnName << " " << buf << std::endl;
}

int main() {
    icu::ErrorCode status;

    const void* context = nullptr;
    utrace_setFunctions(context, nullptr, nullptr, traceData);
    utrace_setLevel(UTRACE_VERBOSE);

    // Create a new BreakIterator
    icu::LocalPointer<icu::BreakIterator> brkitr(
        icu::BreakIterator::createWordInstance("zh-CN", status));
}
```

The following output is produced from this program:

    res-open icudt64l-brkitr/zh_CN.res
    res-open icudt64l-brkitr/zh.res
    res-open icudt64l-brkitr/root.res
    bundle-open icudt64l-brkitr/zh.res
    resc       (get) icudt64l-brkitr/zh.res @ /boundaries
    resc       (get) icudt64l-brkitr/root.res @ /boundaries/word
    resc    (string) icudt64l-brkitr/root.res @ /boundaries/word
    file-open icudt64l-brkitr/word.brk

What this means:

1. The BreakIterator constructor opened three resource files in the locale
   fallback chain for zh_CN. The actual bundle was opened for zh.
2. One string was read from that resource bundle: the one at the resource path
   "/boundaries/word" in brkitr/root.res.
3. In addition, the binary data file brkitr/word.brk was opened.

Based on that information, you can make a more informed decision when writing
resource filter rules for this simple program.

## Data Tracing API

The `traceData` function shown above takes five arguments. The following two
are most important for data tracing:

- `fnNumber` indicates what type of data access this is.
- `args` contains the details on which resources were accessed.

**Important:** When reading from `args`, the strings are valid only within the
scope of your `traceData` function. You should make copies of the strings if
you intend to save them for further processing.

### UTRACE_UDATA_RESOURCE

UTRACE_UDATA_RESOURCE is used to indicate that a value inside of a resource
bundle was read by ICU code.

When `fnNumber` is `UTRACE_UDATA_RESOURCE`, there are three C-style strings in
`args`:

1. Data type; not usually relevant for the purpose of resource filtering.
2. The internal path of the resource file from which the value was read.
3. The path to the value within that resource file.

To read each of these into different variables, you can write the code,

```cpp
const char* dataType = va_arg(args, const char*);
const char* filePath = va_arg(args, const char*);
const char* resPath = va_arg(args, const char*);
```

As stated above, you should copy the strings if you intend to save them. The
pointers will not be valid after the tracing function returns.

### UTRACE_UDATA_BUNDLE

UTRACE_UDATA_BUNDLE is used to indicate that a resource bundle was opened by
ICU code.

For the purposes of making your ICU data filter, the specific resource paths
provided by UTRACE_UDATA_RESOURCE are more precise and useful.

### UTRACE_UDATA_DATA_FILE

UTRACE_UDATA_DATA_FILE is used to indicate that a non-resource-bundle binary
data file was opened by ICU code. Such files are used for break iteration,
conversion, confusables, and a handful of other ICU services.

### UTRACE_UDATA_RES_FILE

UTRACE_UDATA_RES_FILE is used to indicate that a binary resource bundle file
was opened by ICU code. This can be helpful to debug locale fallbacks. This
differs from UTRACE_UDATA_BUNDLE because the resource *file* is typically
opened only once per application runtime.

For the purposes of making your ICU data filter, the specific resource paths
provided by UTRACE_UDATA_RESOURCE are more precise and useful.
