---
layout: default
title: C++ Setup
parent: Setup for Contributors
has_children: true
---

<!--
© 2016 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# C++ Setup
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---


## C/C++ workspace structure

It is best to keep the source file tree and the build-output files separate
("out-of-source build"). It keeps your source tree clean, and you can build
multiple configurations from the same source tree (e.g., debug build, release
build, build with special flags such as no-using-namespace). You could keep the
source and build trees in parallel folders.

**Important:** If you use runConfigureICU together with CXXFLAGS or similar, the
*custom flags must be before the runConfigureICU invocation*. (So that they
are visible as environment variables in the runConfigureICU shell script, rather
than just options text.) See the sample runConfigureICU invocations below.

See the ICU4C readme's [Recommended Build
Options](https://htmlpreview.github.io/?https://github.com/unicode-org/icu/blob/master/icu4c/readme.html#RecBuild).

For example:

*   `~/icu/mine/**src**`
    *   source tree including icu (ICU4C) & icu4j folders
    *   setup: mkdir + git clone your fork (see the [Linux Tips
        subpage](linux.md)) + cd to here.
    *   Use `git checkout <branch>` to switch between branches.
    *   Use `git checkout -b <newbranchname>` to create a new branch and switch
        to it.
    *   After switching branches, remember to update your IDE's view of the
        source tree.
    *   For C++ code, you may want to `make clean` *before* switching to a
        different branch.
*   `~/icu/mine/icu4c/**bld**`
    *   release build output
    *   not-using-namespace is always recommended
    *   setup: mkdir+cd to here, then something like
        `CXXFLAGS="-DU_USING_ICU_NAMESPACE=0"
        CPPFLAGS="-DU_NO_DEFAULT_INCLUDE_UTF_HEADERS=1"
        ../../src/icu4c/source/**runConfigureICU** Linux
        --prefix=/home/*your_user_name*/icu/mine/inst > config.out 2>&1`
    *   build: `make -j5 check > out.txt 2>&1`
*   `~/icu/mine/icu4c/**dbg**`
    *   debug build output
    *   not-using-namespace is always recommended
    *   setup: mkdir+cd to here, then something like
        `CXXFLAGS="-DU_USING_ICU_NAMESPACE=0"
        CPPFLAGS="-DU_NO_DEFAULT_INCLUDE_UTF_HEADERS=1"
        ../../src/icu4c/source/**runConfigureICU** --enable-debug
        --disable-release Linux --prefix=/home/*your_user_name*/icu/mine/inst >
        config.out 2>&1`
    *   build: make -j5 check > out.txt 2>&1
    *   Be sure to test with gcc and g++ too! `CC=gcc CXX=g++
        CXXFLAGS="-DU_USING_ICU_NAMESPACE=0"
        CPPFLAGS="-DU_NO_DEFAULT_INCLUDE_UTF_HEADERS=1"
        ../../src/icu4c/source/runConfigureICU --enable-debug --disable-release
        Linux`
*   `~/icu/mine/icu4c/**nm_utf8**`
    *   not-using-namespace and default-hardcoded-UTF-8
    *   setup: mkdir+cd to here, then something like
        `../../src/icu4c/source/**configure**
        CXXFLAGS="-DU_USING_ICU_NAMESPACE=0" CPPFLAGS="-DU_CHARSET_IS_UTF8=1
        -DU_NO_DEFAULT_INCLUDE_UTF_HEADERS=1"
        --prefix=/home/*your_user_name*/icu/mine/inst > config.out 2>&1`
*   ~/icu/mine/icu4c/static
    *   gcc with static linking
    *   setup: mkdir+cd to here, then something like
        `../../src/icu4c/source/**configure**
        CXXFLAGS="-DU_USING_ICU_NAMESPACE=0"
        CPPFLAGS="-DU_NO_DEFAULT_INCLUDE_UTF_HEADERS=1 -O2 -ffunction-sections
        -fdata-sections" LDFLAGS="-Wl,--gc-sections" --enable-static
        --disable-shared --prefix=/home/*your_user_name*/icu/mine/inst >
        config.out 2>&1`
*   `~/icu/mine/`**`inst`**
    *   “make install” destination (don’t clobber your platform ICU during
        development)
*   `~/icu/**msg48**/src`
    *   Optional: You could have multiple parallel workspaces, each with their
        own git clones, to reduce switching a single workspace (and the IDE
        looking at it) from one branch to another.

### Run individual test suites

*   `cd ~/icu/mine/icu4c/dbg/test/intltest`
    *   `export LD_LIBRARY_PATH=../../lib:../../stubdata:../../tools/ctestfw`
    *   `make -j5 && ./intltest utility/ByteTrieTest utility/UCharTrieTest`
*   `cd ~/icu/mine/icu4c/dbg/test/cintltst`
    *   same relative `LD_LIBRARY_PATH` as for intltest
    *   `make -j5 && ./cintltst`

## gdb pretty-printing

Shane wrote this gdb script in 2017: It pretty-prints UnicodeString in GDB.
Instead of seeing the raw internals of UnicodeString, you will see the length,
storage type, and content of the UnicodeString in your debugger. There are
installation instructions in the top comment on the file (it's a matter of
downloading the file and adding a line to `~/.gdbinit`).

<https://gist.github.com/sffc/7b3826fd67cb78057a9e66f2b350a647>

This also works in anything that wraps GDB, like CLion and Visual Studio Code.

## Linux Tips

For more Linux-specific tips see the [Linux Tips subpage](linux.md).