---
layout: default
title: Build Updates
parent: Release & Milestone Tasks
grand_parent: Contributors
nav_order: 30
---

<!--
© 2021 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Build Updates
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Regenerate configure

Don't forget to re-run and check in the configure file along with configure.in.
This is normally supposed to be done when configure.in or aclocal.m4 are
modified. It also should be done whenever the version numbers change in
**uvernum.h**

On a Linux system,

```sh
cd icu4c/source
autoconf
```

Review the changes to configure, most commonly just an updated ICU version
number. Sometimes there are differences due to different versions of the
autoconf tool being used.

## Update urename.h

Update urename.h to catch all possible ICU4C library exports (especially on
non-Windows systems that tend to ignore export qualifiers). See
[icu4c/source/tools/genren/README](https://github.com/unicode-org/icu/blob/main/icu4c/source/tools/genren/README).

Diff the new one with the previous one; there are typically a few bad #defines
in there.

You are looking to make sure that the defines all are correct exported symbols
and that the Perl script hasn't inadvertently grabbed extraneous text. Each of
the defines should be of the format "uxxxx_NamingConvention". If not then you
need to determine if it's a script issue or a poorly named method.

Also, please look out for this type of message: "\***\*\* WARNING Bad namespace
(not 'icu') on ShoeSize::ShoeSize()**" - it probably means that there is a class
not defined inside the "icu" namespace. Consider adding **U_NAMESPACE_BEGIN**
and **U_NAMESPACE_END** around the class and member definitions.

## Update the runners

In all workflow yaml files, update macos-n, ubuntu-p.q, windows-yyyy to the version currently designated -latest on https://github.com/actions/runner-images?tab=readme-ov-file#available-images.

## Update the pool bundles

*Obsolete for ICU 64+*: The pool bundles are no longer checked in. Instead,
they are built on the fly. (And smaller if the data is filtered.)

The locale data resource bundles use pool.res bundles (one per bundle tree) for
sharing most of their resource table key strings. We should update the pool
bundles once per release, or when we get new data from CLDR, or change the tree
or key structure, to capture the changing set of key strings.

1.  Build ICU4C
2.  Check the date and size of the old pool bundles, for comparison later
    1.  `~/icu/mine/src$ find icu4c -name 'pool.res' -exec ls -l '{}' \;`
3.  Temporarily modify the data makefile: s/usePoolBundle/writePoolBundle/
    1.  If you are not on Windows, you probably need to make other changes to
        the Makefile as well. See
        [ICU-8101](https://unicode-org.atlassian.net/browse/ICU-8101) (contains
        a data/Makefile patch; use the latest version there)
    2.  I like to make a copy the Makefile, then change it, save that for
        possible reuse, and later copy the original back.
    3.  Try to patch it in rather than redoing it manually:
        `~/icu/mine/bld/icu4c/data$ patch -p0 <
        ~/Downloads/Makefile-writePoolBundle.patch`
4.  Rebuild the data (make sure it actually gets rebuilt: cd data, make clean,
    make)
5.  Copy all of the pool.res files like this
    1.  Linux ICU 63, from inside the build output's data directory:
    2.  ICUDT=icudt63l
    3.  cp out/build/$ICUDT/pool.res ../../../src/icu4c/source/data/locales
    4.  cp out/build/$ICUDT/lang/pool.res ../../../src/icu4c/source/data/lang
    5.  cp out/build/$ICUDT/region/pool.res
        ../../../src/icu4c/source/data/region
    6.  cp out/build/$ICUDT/zone/pool.res ../../../src/icu4c/source/data/zone
    7.  cp out/build/$ICUDT/curr/pool.res ../../../src/icu4c/source/data/curr
    8.  cp out/build/$ICUDT/unit/pool.res ../../../src/icu4c/source/data/unit
6.  Double-check the date and size of the old pool bundles. Sizes are usually a
    few % higher than before, due to increased CLDR data.
7.  Revert the data makefile change (copy the original back, if you kept a copy)
8.  Rebuild the data (make sure it actually gets rebuilt: make clean, make)
9.  Build ICU4C & run tests
10. Rebuild the ICU4J data .jar files
    1.  See
        [icu4c/source/data/icu4j-readme.txt](https://github.com/unicode-org/icu/blob/main/icu4c/source/data/icu4j-readme.txt)
    2.  And/or see "update Java data files" in
        [icu4c/source/data/unidata/changes.txt](https://github.com/unicode-org/icu/blob/main/icu4c/source/data/unidata/changes.txt)

If there are new bundle trees that should use pool.res files (like the "unit"
tree in ICU 54 ticket
[ICU-11092](https://unicode-org.atlassian.net/browse/ICU-11092)), then first
modify the data makefiles (Windows and Linux) to add the new pool.res to the
appropriate file lists and initially add --writePoolBundle which at the end also
needs to be turned into --usePoolBundle. Or, simpler, initially copy (svn cp)
the parent tree's pool bundle, and update it later (maybe during release task
work).
