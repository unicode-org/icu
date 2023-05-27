---
layout: default
title: C++ Setup on Linux
grand_parent: Setup for Contributors
parent: C++ Setup
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# C++ Setup on Linux
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


## Compiler

For ICU4C 50 or newer the `configure` script picks `clang` if it is installed,
or else `gcc`. Clang produces superior error messages and warnings.

Most Linuxes should have clang available to install. On Ubuntu or other
Debian-based systems, install it with

```
sudo apt-get install clang
```

Debug builds must use compiler option `-g` and should not optimize (`-O0` is the
default). A future version of `gcc` might support `-Og` as the recommended
optimization level for debugging.

Release builds can use `-O3` for best performance. See
<http://gcc.gnu.org/onlinedocs/gcc/Optimize-Options.html>

`clang` might even benefit from `-O4` where "whole program optimization is done
at link time". See
<http://developer.apple.com/library/mac/#documentation/Darwin/Reference/Manpages/man1/clang.1.html>

## Other build flags

On a modern Linux you can configure with `CPPFLAGS="-DU_CHARSET_IS_UTF8=1"`.

## Debugging

`gdb` should work with both out-of-source and in-source builds. If not,
double-check with "`make VERBOSE=1`" that both .c and .cpp files are compiled
with `-g` and either `-O0` or no `-O*anything*` at all.

`kdbg` is a reasonable GUI frontend for gdb. It keeps the source code in sync
and updates views of variables & memory etc.

*   kdbg versions below 2.5.2 do not work with gdb 7.5; you get a message box
    with "GDB: Reading symbols from..."
*   As a workaround,
    *   Create a `~/.gdbinit` file with `set print symbol-loading off`
    *   Start kdbg, open `Settings/Global options` and remove the `--nx`
        argument to gdb.

## Portability Testing

GitHub pull requests are automatically tested on Windows, Linux with both clang
& gcc, and Macintosh. The build results show up as check results on the status
page.

Build errors will block the pull request. It's also useful to check the build
logs for new warnings on platforms other than the one used for development.

## Clang sanitizers

Clang has built-in santizers to check for several classes of problems. Here are
the configure options for building ICU with the address checker:

```
CPPFLAGS=-fsanitize=address LDFLAGS=-fsanitize=address ./runConfigureICU
--enable-debug --disable-release Linux --disable-renaming
```

The other available sanitizers are `thread`, `memory` and `undefined` behavior.
At the time of this writing, thread and address run cleanly, the others show
warnings that have not yet been resolved.

## Heap Usage (ICU4C)

HeapTrack is a useful tool for analyzing heap usage of a test program, to check
the total heap activity of a particular function or object creation, for
example. It will show totals by line in the source, and can move up and down the
stack to see more detail.

<https://github.com/KDE/heaptrack>

To install on Linux,

```
sudo apt install heaptrack
sudo apt install heaptrack-gui
```

## Quick Scripts for small test programs

I use the following simple scripts to simplify building and debugging small
stand-alone programs against ICU, without needing to set up makefiles. They
assume a program with a single .cpp file with the same name as the directory in
which it resides.

```
b: build

r: run

d: debug

v: run under valgrind
```

You will probably need to modify them to reflect where you keep your most
commonly used ICU build, and whether you routinely use an out-of-source ICU
build.

```
$ cat \`which b\`

#! /bin/sh

if \[\[ -z "${ICU_HOME}" \]\] ; then

ICU_HOME=$HOME/icu/icu4c

fi

DIR=\`pwd\`

PROG=\`basename $DIR\`

clang++ -g -I $ICU_HOME/source/common -I $ICU_HOME/source/i18n -I
$ICU_HOME/source/io -L$ICU_HOME/source/lib -L$ICU_HOME/source/stubdata -licuuc
-licui18n -licudata -o $PROG $PROG.cpp

$ cat \`which r\`

#! /bin/sh
if \[\[ -z "${ICU_HOME}" \]\] ; then
ICU_HOME=$HOME/icu/icu/icu4c
fi
DIR=\`pwd\`
PROG=\`basename $DIR\`
LD_LIBRARY_PATH=$ICU_HOME/source/lib:$ICU_HOME/source/stubdata
ICU_DATA=$ICU_HOME/source/data/out ./$PROG

cat \`which d\`
#! /bin/sh
if \[\[ -z "${ICU_HOME}" \]\] ; then
ICU_HOME=$HOME/icu/icu/icu4c
fi
DIR=\`pwd\`
PROG=\`basename $DIR\`
LD_LIBRARY_PATH=$ICU_HOME/source/lib:$ICU_HOME/source/stubdata
ICU_DATA=$ICU_HOME/source/data/out gdb ./$PROG

$ cat \`which v\`

#! /bin/sh
if \[\[ -z "${ICU_HOME}" \]\] ; then
ICU_HOME=$HOME/icu/icu/icu4c
fi
DIR=\`pwd\`
PROG=\`basename $DIR\`
LD_LIBRARY_PATH=$ICU_HOME/source/lib:$ICU_HOME/source/stubdata
ICU_DATA=$ICU_HOME/source/data/out valgrind --leak-check=full ./$PROG
```