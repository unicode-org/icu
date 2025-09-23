Unsafe-Backward Collator Data
===

This directory contains tools to build the `icu4c/source/i18n/collunsafe.h`
precomputed data.

See [Makefile](./Makefile) for more details.

* Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
* Copyright (c) 2015, International Business Machines Corporation and others. All Rights Reserved.

## Markus 2025

Using an out-of-source build on Linux.

TODO:
- This would be more robust if it used a standard Makefile.in to be configure'd as usual
  and invoked from the output folder.
- Of course, even better would be to generate the unsafe-backwards set in genuca and
  add it into ucadata.icu. --> ICU-12062

# Configure

Linux clang debug

```
ICU_DATA_BUILDTOOL_OPTS=--include_uni_core_data CXXFLAGS="-DU_USING_ICU_NAMESPACE=0 -Wimplicit-fallthrough -std=c++20" CPPFLAGS="-DU_NO_DEFAULT_INCLUDE_UTF_HEADERS=1 -fsanitize=bounds" LDFLAGS=-fsanitize=bounds ../../src/icu4c/source/runConfigureICU --enable-debug --disable-release Linux/clang --disable-renaming --prefix=/usr/local/google/home/mscherer/icu/mine/inst/icu4c > config.out 2>&1 ; tail config.out
```

# Makefile.local

```
BUILD_ROOT=/usr/local/google/home/mscherer/icu/uni/dbg/icu4c
CXX=clang++
BUILD_OPTS=-g -DU_USING_ICU_NAMESPACE=0 -DU_DISABLE_RENAMING=1 -Wimplicit-fallthrough -std=c++20 -fsanitize=bounds
```

# Build & run

In the _source_ `icu4c/source/tools/gencolusb` folder
run `make gen-file`

(This builds and invokes binaries in the output folder,
using the Makefile etc. in the source folder.)
