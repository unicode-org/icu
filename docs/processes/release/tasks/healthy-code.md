---
layout: default
title: Healthy Code
parent: Release & Milestone Tasks
grand_parent: Contributors
nav_order: 50
---

<!--
© 2021 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Healthy Code
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Check for ClassID in new class hierarchies

Check that there are no "poor man's RTTI" methods in new class hierarchies.

After ICU 50: New class hierarchies should not declare getDynamicClassID() at
all. UObject has a dummy implementation for it.

ICU 4.6..50: New class hierarchies used UOBJECT_DEFINE_NO_RTTI_IMPLEMENTATION.
See Normalizer2 for an example declaration and implementation that satisfies the
virtual-function override without adding new ClassID support.

We do need to keep and add "poor man's RTTI" in old classes, and in new classes
extending existing class hierarchies (where parent classes have @stable RTTI
functions). (For example, a new Format subclass.)

One easy way to check for this is to search through the API change report and
look for "UClassID" or similar.

*   Latest trunk report:
    [icu/icu4c/APIChangeReport.html](https://github.com/unicode-org/icu/blob/main/icu4c/APIChangeReport.html)
*   ~~The automated build system creates a new report:
    <https://cldr-build.unicode.org/jenkins/job/icu/job/icu-apidocs/>~~
    *   Click "ICU4C API Change Report vs Latest"
*   Old: was http://bugs.icu-project.org/trac/build/icu4cdocs
    *   Go into the latest successful build, the report is an attachment there.
    *   Download the style sheet next to it:
        https://github.com/unicode-org/icu/blob/main/icu4c/icu4c.css

---

## ~~Check for non-ascii characters in ICU4C source files \[obsolete\]~~

~~Note: ICU4C and ICU4J source files are UTF-8. The ASCII check is no longer
appropriate for them.~~

```sh
cd icu4c/source
find . \( -name "*.[ch]" -or -name "*.cpp" \) -exec grep -PHn [^[:ascii:]] {} \;
```

---

## Check source files for valid UTF-8 and correct text file line endings

Verify that all source and text files in the repository have plain LF line
endings.

To do this on Linux, In an up-to-date git workspace,

```sh
cd icu/icu4c/source
tools/icu-file-utf8-check.py          # reports problems
```

The same python script from the icu4c tools will also check icu4j

```sh
cd icu/icu4j
../icu4c/source/tools/icu-file-utf8-check.py
```

To double-check the line endings, the following grep will find all text files
containing \\r characters. Do not run from Windows, where \\r\\n line endings
are expected.

```sh
cd icu
grep -rPIl "\r" *
```

Even when run from Mac or Linux, some WIndows specific files (.bat, etc) will be
found by this check. This is OK.

## ~~Check UTF-8 file properties \[obsolete\]~~

*Note: As of ICU 63, the project moved from svn to GitHub. SVN file properties
are no longer relevant.*

<span style="color:red">**Note: As of ICU 59, ICU4C source files are UTF-8
encoded, and have the svn mime-type property "text/plain;charset=utf-8". They
must not have a BOM.**</span>

This is checked by the above task, *Check svn properties, valid UTF-8 and text
file line endings.*

The bomfix.py script, formerly used for this task, must *not* be run over the
ICU4C sources

<span style="color:red">**~~Note: This task is only applicable to ICU4C. ICU4J
.java source files are encoded by UTF-8, but must be without UTF-8
BOM.~~**</span>

~~Check that the following match: Files marked as UTF-8 vs. Files beginning with
the UTF-8 signature byte sequence ("BOM").~~

~~Run:~~

<pre><code><s>cd {icu}/icu4c</s>
<s>python ../tools/release/c/bomfix.py</s></code></pre>

---

## Clean up import statements

The Eclipse IDE provides a feature which allow you to organize import statements
for multiple files. Right click on projects/source folders/files, you can select
\[Source\] - \[Organize Imports\] which resolve all wildcard imports and sort
the import statements in a consistent order. (Note: You may experience OOM
problem when your run this for projects/folders which contain many files. In
this case, you may need to narrow a selection per iteration.)

---

## Check library dependencies

***ICU 64+ (2019+): Done automatically by a build bot, at least in one of the
two modes (debug/release), ok to skip as BRS task.***

We want to keep dependencies between .c/.cpp/.o files reasonable, both between
and inside ICU's libraries.

On Linux, run
[source/test/depstest/depstest.py](https://github.com/unicode-org/icu/blob/main/icu4c/source/test/depstest/dependencies.py),
for example:
```sh
~/icu/mine/src/icu4c/source/test/depstest$ ./depstest.py ~/icu/mine/dbg/icu4c
```

Do this twice: Once for a release build (optimized) and once for a debug build
(unoptimized). They pull in slightly different sets of standard library symbols
(see comments in dependencies.txt).

If everything is fine, the test will print "OK: Specified and actual
dependencies match." If not...

*Get changes reviewed by others, including Markus; including changes in
dependencies.txt, .py scripts, or ICU4C code.*

At first, the test will likely complain about .o files not listed in its
dependencies.txt or, if files were removed, the other way around. Try to add
them to groups or create new groups as appropriate.

As a rule, smaller groups with fewer dependencies are preferable. If public API
(e.g., constructing a UnicodeString via conversion) is not needed inside ICU
(e.g., unistr_cnv), make its library depend on its new group.

If the test prints "Info: group icuplug does not need to depend on platform"
then the plug-in code is disabled, as is the default since ICU 56. Consider
enabling it for dependency checking, but make sure to revert that before you
commit changes!

There are usually other "Info" messages where the tool thinks that dependencies
on system symbols are not needed. These are harmless, that is, don't try to
remove them: They are needed or not needed based on the compiler flags used. If
you remove them, then you will likely cause an error for someone with different
flags. Also, in an unoptimized build you only get half as many info messages.
You get more in an optimized build because more system stuff gets inlined.

The test might complain "is this a new system symbol?" We should be careful
about adding those. For example, we must not call printf() from library code,
nor the global operator new.

The test might complain that some .o file "imports
`icu_48::UnicodeString::UnicodeString(const char *)` but does not depend on
unistr_cnv.o". This probably means that someone passes a simple `"string literal"`
or a `char *` into a function that takes a UnicodeString, which invokes the
default-conversion constructor. We do not want that! In most cases, such code
should be fixed. Only implementations
of API that require conversion should depend on it; for example, group
formattable_cnv depends on group unistr_cnv, but then nothing inside ICU depends
on that.

---

## Verify proper memory allocation functions

Verify the following for library code (common, i18n, layout, ustdio). The
requirement is for ICU's memory management to be customizable by changing
cmemory.h and the common base class.

**Note:** The requirement remains. The techniques to fix issues are valid.
***For testing**, see the section "Check library dependencies" above.*

*   No raw malloc/free/realloc but their uprv_ versions.
*   All C++ classes must inherit the common base class UObject or UMemory
    *   But not for mixin/interface classes (pure virtual, no data members, no
        constructors) because that would break the single-inheritance model.
    *   Also not for pure-static classes (used for name scoping) declare but
        don't implement a private default constructor to prevent instantiation.
    *   Simple struct-like C++ classes (and structs) that do not have
        constructors, destructors, and virtual methods, need not inherit the
        base class but must then be allocated with uprv_malloc.
*   All simple types (UChar, int32_t, pointers(!), ...) must be allocated with
    uprv_malloc and released with uprv_free.
*   For Testing that this is the case, on Linux
    *   run the Perl script ICUMemCheck.pl. Follow the instructions in the
        script. The script is in in the ICU4C sources at
        source/tools/memcheck/ICUMemCheck.pl.
    *   also, depstest.py will show an error message if the global operator new
        is used (see section about checking dependencies)
*   For testing that this is the case, on Windows:
    *   Don't bother, as of Nov, 2004. Failures appear in many dozens of files
        from the mixin class destructors. Do the check on Linux. But, for
        reference, here are the old instructions.
        *   Make sure that in `uobject.h` `UObject::new` and `delete` are
            defined by default. Currently, this means to grep to see that
            `U_OVERRIDE_CXX_ALLOCATION` is defined to 1 (in `pwin32.h` for
            Windows).
        *   Check the \*.obj's for linkage to the global (non-UObject::)
            operators new/delete; see uobject.cpp for details.
        *   Global `new` must never be imported. Global `delete` will be
            imported and used by empty-virtual destructors in interface/mixin
            classes. However, they are not called because implementation classes
            always derive from UMemory. No other functions must use global
            delete.
        *   There are now (2002-dec-17) definitions in `utypes.h` for global
            new/delete, with inline implementations that will always crash.
            These global new/delete operators are only defined for code inside
            the ICU4C libraries (but must be there for all of those). See ticket
            #2581.
        *   If a global new/delete is used somewhere, then change the class
            inheritance and/or use uprv_malloc/free until no global new/delete
            are used in the libraries (and the tests still pass...). See the
            User Guide Coding Guidelines for details.

---

## Run static code analysis tools

(Purify, Boundary Checker, valgrind...)

Make sure we fixed all the memory leak problems that were discovered when
running these tools.

Build ICU with debug information. On Linux,

```sh
runConfigureICU --enable-debug --disable-release Linux
```

Run all of the standard tests under valgrind. For intltest, for example,

```sh
cd <where ever>/source/test/intltest
LD_LIBRARY_PATH=../../lib:../../stubdata:../../tools/ctestfw:$LD_LIBRARY_PATH  valgrind  ./intltest
```

You can grab the command line for running the tests from the output from "make
check", and then just insert "valgrind" before the executable.

---

## Check the code coverage numbers

Our goal is that all releases go out to the public with 100% API test and at
least 85% code coverage.

---

## Test ICU4C headers

### Test ICU4C public headers

Testing external dependencies in header files:

(on Unixes) Prerequisite: Configure with --prefix
(../icu4c/source/runConfigureICU Linux --prefix=/some/temp/folder) and do 'make
install'. Then set the PATH so that the installed icu-config script can be
found. (export PATH=/some/temp/folder/**bin**:$PATH)

Then go to the 'icu4c/test/hdrtst' directory (note: not 'source/test/hdrtst')
and do 'make check'. This will attempt to compile against each header file
individually to make sure there aren't any problems. Output looks like this, if
no error springs up all is in order.

~~If a C++ file fails to compile as a C file, add it to the 'cxxfiles.txt'
located in the hdrtst directory.~~

<span style="color:red">**As of ICU 65, the hdrtst is now run as part of the
regular CI builds, and the C++ headers are now guarded with the macro
"U_SHOW_CPLUSPLUS_API".**</span>

There is no longer any "cxxfiles.txt" file. Instead the public C++ headers are
all guarded with the macro "U_SHOW_CPLUSPLUS_API" which is set to 1 by default
if __cplusplus is defined. Users of ICU can explicitly set the macro before
including any ICU headers if they wish to only use the C APIs. Any new public
C++ header needs to be similarly guarded with the macro, though this should be
caught in the CI builds for a pull-request before it is merged.

Run this test with all the uconfig.h variations (see below).

```sh
ctest unicode/docmain.h
ctest unicode/icudataver.h
ctest unicode/icuplug.h
```

### Test ICU4C internal headers

Run the following script straight from the source tree (from inside the "source"
folder, not on the top level), no need to build nor install.

For a new release, also look for new tools and tests and add their folders to
the script. You can ignore messages stating that no '\*.h' files were found in a
particular directory.

The command line is simply

```sh
~/git.icu/icu4c/source$ test/hdrtst/testinternalheaders.sh
```

See https://unicode-org.atlassian.net/browse/ICU-12141 "every header file should
include all other headers if it depends on definitions from them"

<span style="color:red">**As of ICU 68, the internal header test is now
automated as part of Travis CI.**</span>

---

## Test uconfig.h variations
This test is performed automatically by a GitHub Action for each pull request.

Test ICU completely, and run the header test (above) with:

1.  **none** of the 'UCONFIG_NO_XXX' switches turned on (i.e., the normal case)
2.  **all** of the 'UCONFIG_NO_XXX' switches turned on (set to 1)
3.  For each switch, test once with **just** that one switch on. But note the
    exception regarding [UCONFIG_NO_CONVERSION test](healthy-code.md) below.

(See
[common/unicode/uconfig.h](https://github.com/unicode-org/icu/blob/main/icu4c/source/common/unicode/uconfig.h)
for more documentation.)

There is a script available which will automatically test ICU in this way on
UNIXes, it lives in:
[tools/release/c/uconfigtest.sh](https://github.com/unicode-org/icu/blob/main/tools/release/c/uconfigtest.sh).
See docs at top of script for information.

<span style="background-color:yellow">When guard conditionals (e.g. #ifndef
U_HIDE_INTERNAL_API) are removed because they cause header test failures, please
note in the header file the reason that guard conditionals cannot be used in
that location, or they will lkeiely be re-added in the future.

---

## Test C++ Namespace Use

Verify that ICU builds without enabling the default use of the ICU namespace. To
test on Linux,

```sh
./runConfigureICU Linux CXXFLAGS="-DU_USING_ICU_NAMESPACE=0"
make check
```

Any problems will show up as compilation errors.

When definitions outside the ICU C++ namespace refer to ICU C++ classes, those
need to be qualified with "`icu::`", as in "`icu::UnicodeString`". In rare
cases, a C++ type is also visible in C code (e.g., ucol_imp.h has definitions
that are visible to cintltst) and then we use `U_NAMESPACE_QUALIFIER` which is
defined to be empty when compiling for C.

The automated build system should have a machine that sets both
`-DU_USING_ICU_NAMESPACE=0` and `-DU_CHARSET_IS_UTF8=1`.

---

## Test UCONFIG_NO_CONVERSION

Note: Since ICU 73, this test has been included in the Github Actions Continuous Integration jobs.
These instructions explain how to run the test manually.

Make sure that the ICU4C common and i18n libraries build with
UCONFIG_NO_CONVERSION set to 1. We cannot do this as part of "Test uconfig.h
variations" because the test suites cannot be built like this, but the library
code must support it.

The simplest is to take an ICU4C workspace, modify uconfig.h *==temporarily==*
by changing the value of UCONFIG_NO_CONVERSION to 1, and do "make -j -l2.5" (not
"make check" or "make tests"). Verify that the stubdata, common & i18n libraries
build fine; layout should build too but toolutil will fail, that's expected.

Fix any stubdata/common/i18n issues, revert the UCONFIG_NO_CONVERSION value, and
verify that it still works with the normal setting.

If this breaks, someone probably inadvertently uses the `UnicodeString(const char *)` constructor.
See the "Check library dependencies" section.

---

## Test U_CHARSET_IS_UTF8

Verify that ICU builds with default charset hardcoded to UTF-8. To test on
Linux,

```sh
./runConfigureICU Linux CPPFLAGS="-DU_CHARSET_IS_UTF8=1"
make -j -l2.5 check
```

Any problems will show up as compilation or test errors.

Rather than setting the CPPFLAGS, you can also temporarily add `#define
U_CHARSET_IS_UTF8 1` in unicode/platform.h before it gets its default
definition, or modify the default definition there. (In ICU 4.8 and earlier,
this flag was in unicode/utypes.h.)

This works best on a machine that is set to use UTF-8 as its system charset,
which is not possible on Windows.

The automated build system should have a machine that sets both
`-DU_USING_ICU_NAMESPACE=0` and `-DU_CHARSET_IS_UTF8=1`.

---

## Test U_OVERRIDE_CXX_ALLOCATION=0

Verify that ICU builds with U_OVERRIDE_CXX_ALLOCATION=0 on Linux. Problems will
show as build failures.

```sh
CPPFLAGS="-DU_OVERRIDE_CXX_ALLOCATION=0" ./runConfigureICU Linux
make clean
make -j -l2.5 check
```

## ~~Test ICU_USE_THREADS=0 \[Obsolete\]~~

***Only necessary up to ICU4C 49.***

*   ICU 50m1 removes ICU_USE_THREADS from the runtime code (ticket
    [ICU-9010](https://unicode-org.atlassian.net/browse/ICU-9010)).
*   It is still possible to build and test intltest with ICU_USE_THREADS=0 but
    not nearly as important.
*   In ICU 50m1, the `--disable-threads` configure option is gone. If you want
    to test with ICU_USE_THREADS=0 then temporarily change this flag in
    intltest.h or in the intltest Makefile.

Verify that ICU builds and tests with threading disabled. To test on Linux,

```sh
./runConfigureICU Linux --disable-threads
make check
```

---

## Test ICU4C Samples and Demos

### Windows build and test
Note: Since ICU 73, this task has been included in the Azure DevOps Pipeline which is triggered automatically upon merging with main/maint* branches.
These instructions explain how to run the tests manually.

To build the ICU4C samples on Windows with Visual Studio, use the following
steps:

*   Open the "allinone" solution file located under
    "source\\allinone\\allinone.sln".
*   Build the Debug/Release + x86/x64 configurations (all 4 configurations).
    *   Make sure the generated data file (ex: "icu4c\\bin\\icudt64.dll") is not
        stub data, it should be ~26MB.
*   Open the "all" Solution file located under "source\\samples\\all\\all.sln".
*   Build both x86 and x64 using the "Batch Build" option. This is located under
    the menu Build > Batch Build.
    *   Click the "Select All" button.
    *   Click the "Build" button.
    *   If Visual Studio returns errors using the Batch Build option, build each
        configuration individually instead.
*   The samples should all build cleanly with no errors.

To test the sample programs, run the "source\\samples\\all\\samplecheck.bat"
script for each configuration, and ensure that they are successful.

### Linux /Unix build and test
To build and test ICU4C samples:

This test is performed automatically by a GitHub Action for each pull request. 
* In icu4c/source, run the configuration "./runConfigure Linux" (or appropriate system)
* Build and install ICU4C.
* Set PATH to include the bin directory of the installed ICU4c.
* Set LD_LIBRARY_PATH to include the libs directory on the installed ICU4c.


```
cd icu4c/source
cd samples
# To clean all the test binaries
make clean-samples-recursive
# To rebuild them all
make all-samples-recursive
# To run all tests serially
make check-samples-recursive

```


## **Test ICU4C Demos via Docker**

See <https://github.com/unicode-org/icu-demos/blob/main/icu-kube/README.md>

---

## **Test ICU4J Web Demos via Docker**

See: <https://github.com/unicode-org/icu-demos/blob/main/icu4jweb/README.md>

---

## Test ICU4J Demos

These are the demo applets, see above for the icu4jweb demos.

To test ICU4J demo applications, cd to ICU4J directory and build and run the
demo.

```sh
$ cd icu4j
$ ant jarDemos
$ java -jar icu4jdemos.jar
```

Above command invokes GUI demo applications. As such it has to connect to a
X-Server. The easiest way is to run via e.g. remote desktop on the machine on
which it is executed instead of in a ssh shell.

The demos include calendar, charset detection, holidays, RBNF and
transliterator. Check if each application is working OK.

### ICU4J Samples

ICU4J samples are located in directory <icu4j_root>/samples. Check that:

* The build succeeds.
* Each sample runs, giving results appropriate for the sample.
    
To check ICU4J samples, you may use the command line to build and then run each:
```sh
    $ cd icu4j/samples
    $ ant build
    
    # Get the list of main samples to test.
    $ grep -r main src/
      src/com/ibm/icu/samples/text/dateintervalformat/DateIntervalFormatSample.java
      ...
    
    # For each sample, execute as follows:
    $ java -cp ../icu4j.jar:out/lib/icu4j-samples.jar com.ibm.icu.samples.text.dateintervalformat.DateIntervalFormatSample
```
    
To use Eclipse, do the following:
    
* Open Eclipse workspace
* import icu4j-samples project
* Build the project
* In package explorer, right-click on "Run as"
    * Pick "Java application"
    * Choose a sample
    * Verify that the sample runs and that output appears in the console window.

---

## Test, exhaustive mode, C & J

For ICU4J,

```sh
$ ant exhaustiveCheck
```

For ICU4C, testing with an optimized build will help reduce the elapsed time
required for the tests to complete.

```sh
$ make -j -l2.5 check-exhaustive
```

---

## Test ICU4C with the Thread Sanitizer

The build bots run the thread sanitizer on the most interesting multithreaded
tests. These instructions run the sanitizer on the entire test suite. The clang
compiler is required.

```sh
$ CPPFLAGS=-fsanitize=thread LDFLAGS=-fsanitize=thread ./runConfigureICU --enable-debug --disable-release Linux --disable-renaming
$ make clean
$ make -j -l2.5 check
```
